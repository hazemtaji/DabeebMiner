package com.dabeeb.miner.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CachablePluginsRepository {
	public static Logger logger = LogManager.getFormatterLogger(CachablePluginsRepository.class);
	public static CachablePluginsRepository instance = new CachablePluginsRepository();
	private List<Cachable> plugins = new ArrayList<>();
	private Semaphore accessSem = new Semaphore(1); 
	
	private CachablePluginsRepository() {
	}
	
	public static CachablePluginsRepository getInstance() {
		return instance;
	}
	
	public void registerPlugin(Cachable plugin) {
		try {
			accessSem.acquire();
			plugins.add(plugin);
			accessSem.release();
		} catch (InterruptedException e) {
			logger.error("Error registering plugin", e);
		}
	}
	
	public void unregisterPlugin(Cachable plugin) {
		try {
			accessSem.acquire();
			plugins.remove(plugin);
			accessSem.release();
		} catch (InterruptedException e) {
			logger.error("Error unregistering plugin", e);
		}
	}
	
	/**
	 * Synchronized method to allow access to a unique copy of the plugins list
	 * 
	 * @return
	 */
	protected List<Cachable> getCachablePlugins() {
		ArrayList<Cachable> res = null;
		try {
			accessSem.acquire();
			res = new ArrayList<>(plugins);
			accessSem.release();
		} catch (InterruptedException e) {
			logger.error("Error aquiring cachable plugins", e);
		}
		return res;
	}
}
