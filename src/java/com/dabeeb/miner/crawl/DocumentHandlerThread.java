package com.dabeeb.miner.crawl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;

import javax.jms.JMSException;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.data.DatabaseClient;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.model.Host;
import com.dabeeb.miner.data.dao.CacheDAO;
import com.dabeeb.miner.data.dao.HostDAO;
import com.dabeeb.miner.data.dao.FetchPoolDAO;
import com.dabeeb.miner.fetch.Fetcher;
import com.dabeeb.miner.fetch.Fetcher.FetchStatus;
import com.dabeeb.miner.index.Indexer;
import com.dabeeb.miner.inject.InjectionStatus;
import com.dabeeb.miner.messaging.MessagingManager;
import com.dabeeb.miner.messaging.MessagingManager.InvalidMessageTypeException;
import com.dabeeb.miner.parse.Parser;
import com.dabeeb.miner.update.Updater;

public class DocumentHandlerThread extends Thread{
	
	public static Logger logger = LogManager.getFormatterLogger(DocumentHandlerThread.class);
	
	private CacheDAO cacheDAO;
	private FetchPoolDAO fetchPoolDAO;
	private HostDAO hostDAO;
	private Configuration conf;
	private int identifier;
	private boolean reindexer;
	
	private Fetcher fetcher;
	private Parser parser;
	private Updater updater;
	private Indexer indexer;
	
	private DocumentHandlerStatus status;
	
	public DocumentHandlerThread(Configuration conf, int identifier, boolean reindexer) {
		this.conf = conf;
		this.identifier = identifier;
		this.reindexer = false;
		
		DatabaseClient client = DatabaseClient.getInstance();
		fetchPoolDAO = client.getFetchPoolDAO();
		cacheDAO = client.getCacheDAO();
		hostDAO = client.getHostDAO();
		
		fetcher = new Fetcher(conf);
		parser = new Parser(conf);
		updater = new Updater(conf);
		indexer = new Indexer(conf);
		
		status = new DocumentHandlerStatus(identifier);
	}
	
	@Override
	public void run() {
		
		//while(!crawler.signal_shutdown && !crawler.isAllThreadsDone()) {
		while(!DabeebShutdownHook.signal_shutdown) {
			
			FetchStatus fetchStatus = null;
			Document doc = null;
			Host host = null;
			
			status.phase = DocumentHandlerPhase.WAITING;
			
			try {
				String url;
				
				status.waitStartTime = new Date();
				if(reindexer) {
					url = MessagingManager.getInstance().listenToRevisitUrl();
				} else {
					url = MessagingManager.getInstance().listenToUrlReady();
				}
				
				status.url = url;
				status.phase = DocumentHandlerPhase.FETCH;

				try {
					//grab the document
					if(reindexer) {
						doc = cacheDAO.getURL(url);
						doc.setUrl(doc.getFinalUrl());
						doc.setDbUrl(url);
					} else {
						doc = fetchPoolDAO.getURL(url);
					}
					
					if(doc != null)
					{
						String hostName = doc.getUrl().getHost();
						host = hostDAO.getHost(hostName);
						if(host == null) {
							host = new Host(hostName);
							hostDAO.addHost(host);
						}

						fetchStatus = fetchDocument(doc, host);
					}
				} catch (Exception e) {
					logger.error("Error fetching document", e);
				} finally {
					if(!reindexer) {
						if(doc == null) {
							//toggle no wait since no real fetching was done
							MessagingManager.getInstance().informFetchDone('*' + url);
						} else {
							MessagingManager.getInstance().informFetchDone(url);
						}
					}
				}
				
				if(logger.isInfoEnabled()) {
					if(doc != null)
						logger.info("Done fetching %s", url);
					else
						logger.info("No need to fetch %s", url);
				}
				
				if(doc == null)
					continue;
					
				if(fetchStatus != null) {
					InjectorThread[] threads = Crawler.getInstance().getInjectorThreadsPool();
					
					if(fetchStatus == FetchStatus.FETCHED) {
						processDocument(doc);
						if(doc.getDepth() == 1) {
							for(InjectorThread thread : threads) {
								thread.getInjector().reportSuccess(doc.getDbUrl());
							}
						}
					} else {
						if(logger.isInfoEnabled()) {
							logger.info("Fetch failed for %s with status %s", url, fetchStatus.toString());
						}
						
						//Report problems with injected links
						if(doc.getDepth() == 1) {
							InjectionStatus status = null;
							switch (fetchStatus) {
							case FAILED:
								status = InjectionStatus.FETCH_FAILED;
								break;
							case REJECTED:
								status = InjectionStatus.TYPE_MISMATCH;
								break;
							default:
								break;
							}
							
							for(InjectorThread thread : threads) {
									thread.getInjector().reportFailure(doc.getDbUrl(), status);
							}
						}
						if (host != null && fetchStatus == FetchStatus.FAILED) {
							hostDAO.incrementFailures(host);
						}
					}
				}
				
			} catch (JMSException | InvalidMessageTypeException e) {
				logger.error("Messaging error", e);
			} catch (Exception e) {
				//Persevere! and log
				logger.error("Generic error", e);
			}
						
			
		}
		
		logger.info("Terminating thread: %s", identifier);
	}
	
	private FetchStatus fetchDocument(Document doc, Host host) throws IOException, URISyntaxException {
		long beforeFetch = System.currentTimeMillis();
		FetchStatus fetched = fetcher.fetchUrl(doc);
		int timeToFetch = (int)(System.currentTimeMillis() - beforeFetch);
		
		if(fetched == FetchStatus.FETCHED) {
			host.setLastDuration(timeToFetch);
			host.setLastFetch(new Date());
			hostDAO.reportFetch(host);
			hostDAO.resetFailures(host);
		}
		
		return fetched;
	}

	private void processDocument(Document doc) {
		Crawler.getInstance().getCrawlStatistics().incrementUnderProcessDocuments();
		
		status.phase = DocumentHandlerPhase.PARSE;
		parser.parse(doc);
		if(doc.getParsedContent() != null)
		{
			status.phase = DocumentHandlerPhase.UPDATE;
			updater.update(doc);
			
			status.phase = DocumentHandlerPhase.INDEX;
			indexer.index(doc, status);
			
			boolean success = cacheDAO.addDocument(doc);
			if(success) {
				Crawler.getInstance().getCrawlStatistics().incrementProcessedDocuments();
				if(!reindexer) {
					fetchPoolDAO.removeURL(doc.getDbUrl());
				
					String url = doc.getFinalUrl().toString();
					if(url == null) {
						url = doc.getDbUrl();
					}
					
					/*try {
						MessagingManager.getInstance().scheduleRevisit(url);
					} catch (JMSException e) {
						logger.error(e);
					}*/
				}
			}
		}
		Crawler.getInstance().getCrawlStatistics().decrementUnderProcessDocuments();
	}
	
	public DocumentHandlerStatus getStatus() {
		
		return status;
	}
	
	public static class DocumentHandlerStatus {
		private DocumentHandlerPhase phase = DocumentHandlerPhase.SETUP;
		private String url;
		private String subPhase = "";
		private int identifier;
		private Date waitStartTime;
		
		public DocumentHandlerStatus(int identifier) {
			this.identifier = identifier;
		}
		
		public DocumentHandlerPhase getPhase() {
			return phase;
		}
		
		public String getUrl() {
			return url;
		}
		
		public int getIdentifier() {
			return identifier;
		}
		
		public Date getWaitStartTime() {
			return waitStartTime;
		}
		
		public String getSubPhase() {
			return subPhase;
		}
		
		public void setSubPhase(String subPhase) {
			this.subPhase = subPhase;
		}
	}
	
	public static enum DocumentHandlerPhase {
		SETUP("Thread Setting Up"),
		WAITING("Waiting For Document"),
		FETCH("Fetching Document"),
		PARSE("Parsing Document"),
		UPDATE("Updating Link Database"),
		INDEX("Indexing Document");
		
		private String friendlyName;
		DocumentHandlerPhase(String friendlyName) {
			this.friendlyName = friendlyName;
		}
		
		public String getFriendlyName() {
			return friendlyName;
		}
	}
}
