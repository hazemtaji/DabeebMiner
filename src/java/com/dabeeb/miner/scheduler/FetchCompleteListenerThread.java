package com.dabeeb.miner.scheduler;

import java.net.MalformedURLException;
import java.net.URL;

import javax.jms.JMSException;

import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.concurrency.CuratorManager;
import com.dabeeb.miner.messaging.MessagingManager;
import com.dabeeb.miner.messaging.MessagingManager.InvalidMessageTypeException;

public class FetchCompleteListenerThread extends Thread {
	public static Logger logger = LogManager.getFormatterLogger(FetchCompleteListenerThread.class);
	
	private Scheduler scheduler;
	
	public FetchCompleteListenerThread(Scheduler scheduler) {
		setDaemon(true);
		this.scheduler = scheduler;
	}
	
	@Override
	public void run() {
		while(!scheduler.isShutdown()) {
			try {
				String urlStr = MessagingManager.getInstance().listenToFetchDone();
				
				//check for nowait marker
				boolean noWait = false;
				if(urlStr.startsWith("*")) {
					noWait = true;
					urlStr = urlStr.substring(1);
				}
				URL url = new URL(urlStr);
				String hostName = url.getHost();
				
				//InterProcessSemaphoreMutex sem = CuratorManager.getInstance().getSemaphore("hostqueue/" + hostName);
				try {
					//sem.acquire();
					String newURL = MessagingManager.getInstance().dequeueUrlFromHost(hostName);
					
					if(newURL != null) {
						if(noWait) {
							TimedInformerThread tit = new TimedInformerThread(0, newURL);
							tit.start();
						} else {
							TimedInformerThread tit = new TimedInformerThread(scheduler.getHostFetchInterval(), newURL);
							tit.start();
						}
						
					} else {
						scheduler.decrementHostConnections(hostName);
					}
				} catch (Exception e) {
					logger.error("Generic exception", e);
				} /*finally {
					try {
						sem.release();
					} catch (Exception e) {
						logger.error("Generic exception", e);
					}
				}*/
			} catch (JMSException e) {
				logger.error("Messaging exception", e);
			} catch (InvalidMessageTypeException e) {
				logger.error("Messaging exception", e);
			} catch (MalformedURLException e) {
			}
		}
	}
}
