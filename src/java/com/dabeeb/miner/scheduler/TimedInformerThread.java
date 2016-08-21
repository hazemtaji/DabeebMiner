package com.dabeeb.miner.scheduler;

import javax.jms.JMSException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.messaging.MessagingManager;

public class TimedInformerThread extends Thread{
	public static Logger logger = LogManager.getFormatterLogger(TimedInformerThread.class);

	private int timeInMilliseconds;
	private String[] urls;
	
	public TimedInformerThread(int timeInMilliseconds, String... urls) {
		this.timeInMilliseconds = timeInMilliseconds;
		this.urls = urls;
	}
	
	@Override
	public void run() {
		try {
			sleep(timeInMilliseconds);
			MessagingManager.getInstance().informUrlsReady(urls);
			if(logger.isDebugEnabled()) {
				for(String url : urls)
					logger.debug("URL Ready: %s", url);
			}
		} catch (JMSException | InterruptedException e) {
			logger.error("Generic exception", e);
		}
	}
}
