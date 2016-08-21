package com.dabeeb.miner.crawl;

import java.io.IOException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.cache.CacheUpdateThread;
import com.dabeeb.miner.concurrency.CuratorManager;
import com.dabeeb.miner.crawl.DocumentHandlerThread.DocumentHandlerStatus;
import com.dabeeb.miner.crawl.DocumentReindexerThread.DocumentReindexerStatus;
import com.dabeeb.miner.data.DBConnectionManager;
import com.dabeeb.miner.data.DatabaseClient;
import com.dabeeb.miner.data.dao.FetchPoolDAO;
import com.dabeeb.miner.messaging.MessagingManager;
import com.dabeeb.miner.plugin.PluginRepository;

public class Crawler {
	public static Logger logger = LogManager.getLogger();
	
	private static final String CONFIG_PREFIX = "crawler";
	private static final String CONFIG_ID = CONFIG_PREFIX + ".id";
	private static final String CONFIG_THREAD_COUNT = CONFIG_PREFIX + ".threadcount";
	
	private CrawlStatistics crawlStatistics = new CrawlStatistics();
	
	private DocumentHandlerThread[] threadPool;
	private InjectorThread[] injectorThreadsPool;
	private DocumentReindexerThread[] reindexerThreadsPool;
	private CacheUpdateThread cacheUpdateThread;
	private boolean threadDone[];
	private Configuration conf;
	
	private int crawlerId;
	
	private static Crawler instance = new Crawler();
	
	public static void main(String[] args) throws IOException {
		
		try {
			XMLConfiguration conf = initialize();
			setupDatabase(conf);
			
			boolean republish = false;
			
			for(String arg : args) {
				if(arg.equals("--republish"))
					republish = true;
			}

			Crawler crawler = Crawler.getInstance();
			crawler.setConf(conf);
			
			WebServerThread webServerThread = WebServerThread.getInstance();
			webServerThread.setConf(conf);
			webServerThread.start();
			
			MessagingManager.getInstance().setConf(conf);
			MessagingManager.getInstance().start();
			
			if(republish)
				crawler.republish();
			
			CuratorManager.getInstance().setConf(conf);
			CuratorManager.getInstance().nominateScheduler("Crawler-" + crawler.crawlerId);
			
			
			crawler.crawl();

			CuratorManager.getInstance().shutdown();
			MessagingManager.getInstance().close();
			DatabaseClient.getInstance().close();
			
		} catch (ConfigurationException e) {
			logger.error("Error reading configuration", e);
		}
	}
	
	public static Crawler getInstance() {
		return instance;
	}

	private void republish() {
		FetchPoolDAO fpDAO = new FetchPoolDAO(DatabaseClient.getInstance());
		fpDAO.republish();
	}

	public static XMLConfiguration initialize() throws ConfigurationException {
		XMLConfiguration conf = new XMLConfiguration("dabeeb-instance.xml");
		conf.load("dabeeb-default.xml");
		
		//Start dbConnection manager and establish connections first
		//Some plugins might need database connections to initialize
		DBConnectionManager dbManager = DBConnectionManager.getInstance();
		dbManager.setConf(conf);
		
		PluginRepository repository = PluginRepository.getInstance();
		repository.setConf(conf);
		
		return conf;
	}
	
	public static void setupDatabase(XMLConfiguration conf) {
		DatabaseClient client = DatabaseClient.getInstance(); 
		client.setConf(conf);
		client.connect();
	}
	
	private Crawler() {
	}
	
	public void setConf(Configuration conf) {
		this.conf = conf;
		crawlerId = conf.getInt(CONFIG_ID);
	}
	
	public void crawl() {
		
		//recover from last run
		/*CrawlerDAO crawlerDAO = DatabaseClient.getInstance().getCrawlerDAO();
		List<String> urls = crawlerDAO.getCrawlerUrls(crawlerId);
		if(urls != null) {
			try {
				MessagingManager.getInstance().informNewUrls(urls.toArray(new String[0]));
			} catch (JMSException e) {
				logger.error(e);
			}
			crawlerDAO.clearUrls(crawlerId);
		} else {
			crawlerDAO.addCrawler(crawlerId);
		}*/

		//first clear locks from old runs
		/*FetchPoolDAO operationDAO = new FetchPoolDAO(client);
		operationDAO.clearLocks(crawlerId);*/
		
		startCacheUpdater();
		startInjectors();
		startReindexers();
		startWorkers();
				
		
	}

	private void startCacheUpdater() {
		cacheUpdateThread = new CacheUpdateThread(conf);
		cacheUpdateThread.setName("Cache Update Thread");
		cacheUpdateThread.start();
	}

	private void startInjectors() {
		injectorThreadsPool = new InjectorThread[1];
		
		injectorThreadsPool[0] = new InjectorThread(conf);
		injectorThreadsPool[0].setName("Injector Thread 0");
		injectorThreadsPool[0].start();
	}
	
	private void startReindexers() {
		reindexerThreadsPool = new DocumentReindexerThread[1];
		
		for(int i = 0; i < reindexerThreadsPool.length; i++) {
			reindexerThreadsPool[i] = new DocumentReindexerThread(conf, i);
			reindexerThreadsPool[i].setName("Reindexer Thread " + i);
			reindexerThreadsPool[i].start();
		}
	}

	private void startWorkers() {
		// initialize thread pool
		int threadCount = conf.getInt(CONFIG_THREAD_COUNT);
		threadPool = new DocumentHandlerThread[threadCount];
		threadDone = new boolean[threadCount];

		for (int i = 0; i < threadPool.length; i++) {
			threadPool[i] = new DocumentHandlerThread(conf, i, (i%3 == 0?true:false)); //assign one third for reindexing
			threadPool[i].setName("Document Handler Thread " + i);
			threadPool[i].start();

			logger.debug("Running thread no: {}", i);
		}
		
		// add shutdown hook
		Runtime.getRuntime().addShutdownHook(new DabeebShutdownHook(this));

		for (int i = 0; i < threadPool.length; i++) {
			try {
				threadPool[i].join();
				logger.debug("Thread Exit: No. {}", i);
			} catch (InterruptedException e) {
				logger.error("Main thread interrupted", e);
			}
		}
	}
	
	public int getCrawlerId() {
		return crawlerId;
	}

	public boolean isAllThreadsDone() {
		for(boolean done : threadDone)
			if(!done)
				return false;
		return true;
	}

	public void reportDone(int threadId) {
		threadDone[threadId] = true;
	}

	public void reportBusy(int threadId) {
		threadDone[threadId] = false;
	}
	
	public DocumentHandlerThread[] getThreadPool() {
		return threadPool;
	}
	
	public InjectorThread[] getInjectorThreadsPool() {
		return injectorThreadsPool;
	}
	
	public CrawlStatistics getCrawlStatistics() {
		return crawlStatistics;
	}
	
	public DocumentReindexerStatus[] getDocumentReindexersStatus() {
		int length = 0;
		
		if(reindexerThreadsPool != null)
			length = reindexerThreadsPool.length;
		
		DocumentReindexerStatus[] res = new DocumentReindexerStatus[length];
		
		for(int i = 0; i < res.length; i++) {
			if(reindexerThreadsPool[i] != null)
				res[i] = reindexerThreadsPool[i].getStatus();
		}
		
		return res;
	}
	
	public DocumentHandlerStatus[] getDocumentHandlersStatus() {
		int length = 0;
		
		if(threadPool != null)
			length = threadPool.length;
		
		DocumentHandlerStatus[] res = new DocumentHandlerStatus[length];
		
		for(int i = 0; i < res.length; i++) {
			if(threadPool[i] != null)
				res[i] = threadPool[i].getStatus();
		}
		
		return res;
	}
}
