package com.dabeeb.miner.crawl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DabeebShutdownHook extends Thread {

	public static Logger logger = LogManager.getLogger();
	public static boolean signal_shutdown = false;
	Crawler crawler;
	
	public DabeebShutdownHook(Crawler crawler) {
		this.crawler = crawler;
	}
	
	@Override
	public void run() {
		signal_shutdown = true;
		logger.warn("Shutting Down...");
		
		Thread[] threadPool = crawler.getThreadPool();
		for (int i = 0; i < threadPool.length; i++) {
			try {
				threadPool[i].interrupt();
				threadPool[i].join();
				logger.debug("Thread Exit: No. {}", i);
			} catch (InterruptedException e) {
				logger.error("Main thread interrupted", e);
			}
		}
		
		logger.warn("Shutdown Hook Done");
	}

}
