package com.dabeeb.miner.messaging;

import java.util.Enumeration;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ScheduledMessage;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.conf.Configurable;
import com.dabeeb.miner.crawl.Crawler;

public class MessagingManager implements Configurable {
	public static Logger logger = LogManager.getFormatterLogger(MessagingManager.class);
	private static final String CONFIG_PREFIX = "messaging";
	private static final String CONFIG_URL = CONFIG_PREFIX + ".url";
	
	/**
	 * New URLs that have just been discovered
	 */
	private static final String URL_NEW_SUBJECT = "URL_NEW_QUEUE";
	
	/**
	 * Urls that are done
	 */
	private static final String URL_FETCHED_SUBJECT = "URL_FETCHED_QUEUE";
	
	/**
	 * Urls that are ready to be fetched (no objection on host connections)
	 */
	private static final String URL_READY_SUBJECT = "URL_READY_QUEUE";
	
	/**
	 * Urls that are ready to be reindexed (from cache)
	 */
	private static final String URL_REINDEX_SUBJECT = "URL_REINDEX_QUEUE";
	
	/**
	 * Urls that are ready to be reindexed (from source)
	 */
	private static final String URL_REVISIT_SUBJECT = "URL_REVISIT_QUEUE";
	
	/**
	 * Test queue for this class
	 */
	private static final String testSubject = "TEST_QUEUE";
	private ConnectionFactory connectionFactory;
	private Connection connection;
	
	private boolean connected = false;
	
	private static MessagingManager instance = new MessagingManager();
	
	protected MessagingManager() {
	}
	
	@Override
	public void setConf(Configuration conf) {
		String url = conf.getString(CONFIG_URL);
		connectionFactory = new ActiveMQConnectionFactory(url);
	}
	
	public static MessagingManager getInstance() {
		return instance;
	}
	
	public void start() {
		try {
			connection = connectionFactory.createConnection();
			connection.start();
			connected = true;
			
			synchronized (this) {
				notifyAll();
			}
		} catch (JMSException e) {
			logger.error("Messaging manager failed to start", e);
		}
	}
	
	public void close() {
		try {
			connection.close();
		} catch (JMSException e) {
			logger.error("Messaging manager failed to stop", e);
		}
	}
	
	public void informNewUrls(String... urls) throws JMSException {
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		Session session = null;
		try{
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(URL_NEW_SUBJECT);
			MessageProducer producer = session.createProducer(destination);
			
			for(String url : urls) {
				TextMessage message = session.createTextMessage(url);
				producer.send(message);
			}
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public void scheduleRevisit(String... urls) throws JMSException {
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		Session session = null;
		try{
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(URL_REVISIT_SUBJECT);
			MessageProducer producer = session.createProducer(destination);
			
			for(String url : urls) {
				TextMessage message = session.createTextMessage(url);
				
				long period = 30 * 60 * 1000;	//Every 30 minutes
				int repeat = 36 - 1;			//For 36 times
				
				message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, period);
				message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_PERIOD, period);
				message.setIntProperty(ScheduledMessage.AMQ_SCHEDULED_REPEAT, repeat);
				
				producer.send(message);
			}
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public void informReindexUrls(String... urls) throws JMSException {
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		Session session = null;
		try{
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(URL_REINDEX_SUBJECT);
			MessageProducer producer = session.createProducer(destination);
			
			for(String url : urls) {
				TextMessage message = session.createTextMessage(url);
				producer.send(message);
			}
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public void informFetchDone(String url) throws JMSException {
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		Session session = null;
		try{
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(URL_FETCHED_SUBJECT);
			MessageProducer producer = session.createProducer(destination);
			
			TextMessage message = session.createTextMessage(url);
			producer.send(message);
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public void informUrlsReady(String... urls) throws JMSException {
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		Session session = null;
		try{
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(URL_READY_SUBJECT);
			MessageProducer producer = session.createProducer(destination);
			
			for(String url : urls) {
				TextMessage message = session.createTextMessage(url);
				producer.send(message);
			}
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public String listenToUrlReindex() throws JMSException, InvalidMessageTypeException {
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		Session session = null;
		try{
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(URL_REINDEX_SUBJECT);
			MessageConsumer consumer = session.createConsumer(destination);
			Message message = consumer.receive();
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				return textMessage.getText();
			} else {
				throw new InvalidMessageTypeException();
			}
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public String listenToUrlReady() throws JMSException, InvalidMessageTypeException {
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		Session session = null;
		try{
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(URL_READY_SUBJECT);
			MessageConsumer consumer = session.createConsumer(destination);
			Message message = consumer.receive();
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				return textMessage.getText();
			} else {
				throw new InvalidMessageTypeException();
			}
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public String listenToFetchDone() throws JMSException, InvalidMessageTypeException{
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		Session session = null;
		try{
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(URL_FETCHED_SUBJECT);
			MessageConsumer consumer = session.createConsumer(destination);
			Message message = consumer.receive();
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				return textMessage.getText();
			} else {
				throw new InvalidMessageTypeException();
			}
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public String listenToRevisitUrl() throws JMSException, InvalidMessageTypeException{
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		Session session = null;
		try{
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(URL_REVISIT_SUBJECT);
			MessageConsumer consumer = session.createConsumer(destination);
			Message message = consumer.receive();
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				return textMessage.getText();
			} else {
				throw new InvalidMessageTypeException();
			}
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public String listenToNewUrl() throws JMSException, InvalidMessageTypeException{
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		Session session = null;
		try{
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(URL_NEW_SUBJECT);
			MessageConsumer consumer = session.createConsumer(destination);
			Message message = consumer.receive();
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				return textMessage.getText();
			} else {
				throw new InvalidMessageTypeException();
			}
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public void queueUrlsOnHost(String hostName, String[] urls) throws JMSException {
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		Session session = null;
		try{
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue("HOST." + hostName.toUpperCase());
			MessageProducer producer = session.createProducer(destination);
			
			for(String url : urls) {
				TextMessage message = session.createTextMessage(url);
				producer.send(message);
			}
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public String dequeueUrlFromHost(String hostName) throws JMSException, InvalidMessageTypeException {
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		Session session = null;
		try{
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue("HOST." + hostName.toUpperCase());
			MessageConsumer consumer = session.createConsumer(destination);
			//receiveNoWait returns null prematurely
			Message message = consumer.receive(1000);
			if(message == null) {
				logger.info("No messages in host queue for host: %s", hostName.toUpperCase());
				return null;
			}
			if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage) message;
				return textMessage.getText();
			} else {
				throw new InvalidMessageTypeException();
			}
		} finally {
			if(session != null)
				session.close();
		}
	}
	
	public void sendMessage() throws JMSException {
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination destination = session.createQueue(testSubject);
		MessageProducer producer = session.createProducer(destination);
		for(int i = 0; i < 10; i++) {
			TextMessage message = session.createTextMessage("Hello-" + i);
			producer.send(message);
			System.out.println("Sent message '" + message.getText() + "' with ID: '" + message.getJMSMessageID() + "'");
		}
		
		session.close();
		
	}
	
	public void recieveMessage() throws JMSException {
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue destination = session.createQueue(testSubject);
        MessageConsumer consumer = session.createConsumer(destination);
		for(int i = 0; i < 10; i++) {
	        Message message = consumer.receive();
	        if (message instanceof TextMessage) {
	            TextMessage textMessage = (TextMessage) message;
	            System.out.println("Received message '" + textMessage.getText() + "'");
	        }
		}
        
		session.close();
	}
	
	public void browseMessages() throws JMSException {
		while(!connected) {
			try {
				synchronized (this) {
					wait();
				}
			} catch (InterruptedException e) {
				logger.error("Interrupted while waiting for connection to start", e);
			}
		}
		
		System.out.println("Browsing messages...");
		
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue destination = session.createQueue(testSubject);
        QueueBrowser browser = session.createBrowser(destination);
        Enumeration<Message> enumeration = browser.getEnumeration();
        while(enumeration.hasMoreElements()) {
        	Message message = enumeration.nextElement();
        	if (message instanceof TextMessage) {
        		TextMessage textMessage = (TextMessage) message;
        		System.out.println("Browsed message '" + textMessage.getText() + "'");
        	}
        }
        
        System.out.println("Done browsing");
        
		session.close();
	}

	public static void main(String[] args) throws JMSException, ConfigurationException {
		Configuration conf = Crawler.initialize();
		
		MessagingManager manager = MessagingManager.getInstance();
		manager.setConf(conf);
		manager.start();
		
		manager.sendMessage();
		manager.browseMessages();
		manager.recieveMessage();
		
		manager.close();
	}

	public static class InvalidMessageTypeException extends Exception{
		private static final long serialVersionUID = 1L;
	}
}
