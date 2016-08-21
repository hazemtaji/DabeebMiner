package com.dabeeb.miner.crawl;


import javax.jms.JMSException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.data.DatabaseClient;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.dao.CacheDAO;
import com.dabeeb.miner.index.Indexer;
import com.dabeeb.miner.messaging.MessagingManager;
import com.dabeeb.miner.messaging.MessagingManager.InvalidMessageTypeException;
import com.dabeeb.miner.parse.Parser;

public class DocumentReindexerThread extends Thread{
	
	public static Logger logger = LogManager.getFormatterLogger(DocumentReindexerThread.class);
	
	private CacheDAO cacheDAO;
	private Configuration conf;
	
	private Parser parser;
	private Indexer indexer;
	private int identifier;
	
	private DocumentReindexerStatus status;
	
	public DocumentReindexerThread(Configuration conf, int identifier) {
		this.conf = conf;
		
		DatabaseClient client = DatabaseClient.getInstance();
		
		cacheDAO = client.getCacheDAO();
		parser = new Parser(conf);
		indexer = new Indexer(conf);
		
		status = new DocumentReindexerStatus(identifier);
		
		this.identifier = identifier;
	}
	
	@Override
	public void run() {
		while(!DabeebShutdownHook.signal_shutdown) {
			status.phase = DocumentReindexerPhase.WAITING;
			try {
				String url = MessagingManager.getInstance().listenToUrlReindex();
				reindexUrl(url);
			} catch (JMSException | InvalidMessageTypeException e) {
				logger.error("Messaging error", e);
			} catch (Exception e) {
				//Persevere! and log
				logger.error("Generic error", e);
			}
		}
	}
	
	
	private void reindexUrl(String url) {
		//grab the document
		Document doc = cacheDAO.getURL(url);
		
		if(doc == null)
			return;
		
		doc.setUrl(doc.getFinalUrl());
		doc.setDbUrl(url);
		
		status.url = url;
		processDocument(doc);
		
		cacheDAO.updateParsedDoc(doc);
	}

	private void processDocument(Document doc) {
		
		status.phase = DocumentReindexerPhase.PARSE;
		parser.parse(doc);
		if(doc.getParsedContent() != null)
		{
			status.phase = DocumentReindexerPhase.INDEX;
			indexer.index(doc, true, null);
		}
	}
	
	public DocumentReindexerStatus getStatus() {
		return status;
	}
	
	public static void main(String[] args) throws ConfigurationException {
		XMLConfiguration conf = Crawler.initialize();
		Crawler.setupDatabase(conf);
		DocumentReindexerThread reindexer = new DocumentReindexerThread(conf, 0);
		
		String url = args[0];
		reindexer.reindexUrl(url);
	}
	
	public static class DocumentReindexerStatus {
		private DocumentReindexerPhase phase = DocumentReindexerPhase.SETUP;
		private String url;
		private int identifier;
		
		public DocumentReindexerStatus(int identifier) {
			this.identifier = identifier;
		}
		
		public DocumentReindexerPhase getPhase() {
			return phase;
		}
		
		public String getUrl() {
			return url;
		}
		
		public int getIdentifier() {
			return identifier;
		}
	}
	
	public static enum DocumentReindexerPhase {
		SETUP("Thread Setting Up"),
		WAITING("Waiting For Document"),
		PARSE("Parsing Document"),
		INDEX("Indexing Document");
		
		private String friendlyName;
		
		private DocumentReindexerPhase(String friendlyName) {
			this.friendlyName = friendlyName;
		}
		
		public String getFriendlyName() {
			return friendlyName;
		}
	}
}
