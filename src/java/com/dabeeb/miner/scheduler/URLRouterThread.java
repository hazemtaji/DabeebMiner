package com.dabeeb.miner.scheduler;

import java.net.MalformedURLException;
import java.net.URL;
import javax.jms.JMSException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.messaging.MessagingManager;
import com.dabeeb.miner.messaging.MessagingManager.InvalidMessageTypeException;

public class URLRouterThread extends Thread {
	public static Logger logger = LogManager.getFormatterLogger(URLRouterThread.class);
	
	private Scheduler scheduler;
	
	public URLRouterThread(Scheduler scheduler) {
		setDaemon(true);
		this.scheduler = scheduler;
	}
	
	@Override
	public void run() {
		while(!scheduler.isShutdown()) {
			try {
				String urlStr = MessagingManager.getInstance().listenToNewUrl();
				URL url = new URL(urlStr);
				String hostName = url.getHost();
				if(scheduler.isHostFree(hostName)) {
					scheduler.incrementHostConnections(hostName);
					TimedInformerThread tit = new TimedInformerThread(scheduler.getHostFetchInterval(), urlStr);
					tit.start();
				} else {
					String[] urls = new String[] { urlStr };
					MessagingManager.getInstance().queueUrlsOnHost(hostName, urls);
				}
				
			} catch (JMSException e) {
				logger.error("Generic exception", e);
			} catch (InvalidMessageTypeException e) {
				logger.error("Generic exception", e);
			} catch (MalformedURLException e) {
			}
		}
	}
}
