package com.dabeeb.miner.cache;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.crawl.DabeebShutdownHook;


public class CacheUpdateThread extends Thread {
	public static Logger logger = LogManager.getFormatterLogger(CacheUpdateThread.class);
	private final int sleepTime = 30 * 1000; 
	private Configuration conf;
	
	public CacheUpdateThread(Configuration conf) {
		setDaemon(true);
		this.conf = conf;
	}
	
	@Override
	public void run() {
		while(!DabeebShutdownHook.signal_shutdown) {
			List<Cachable> plugins = CachablePluginsRepository.getInstance().getCachablePlugins();
			
			for(Cachable plugin : plugins) {
				plugin.updateCache();
			}
			
			try {
				sleep(sleepTime);
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
	}
}
