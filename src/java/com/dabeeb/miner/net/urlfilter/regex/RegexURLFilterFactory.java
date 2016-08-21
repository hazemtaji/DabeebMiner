package com.dabeeb.miner.net.urlfilter.regex;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.configuration.Configuration;

import com.dabeeb.miner.cache.CachablePluginsRepository;
import com.dabeeb.miner.plugin.PluginFactory;
import com.dabeeb.miner.plugin.PluginRepository;

public class RegexURLFilterFactory extends PluginFactory<RegexURLFilter> {
	
	private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
	private static RegexRulesLoader loader;
	
	public RegexURLFilterFactory() {
	}
	
	@Override
	public void setConf(Configuration conf) {
		super.setConf(conf);
		rwLock.readLock().lock();
		if(loader == null) {
			rwLock.readLock().unlock();
			
			rwLock.writeLock().lock();
			if(loader == null) {
				loader = createRuleLoader(conf);
				CachablePluginsRepository.getInstance().registerPlugin(loader);
			}
			rwLock.writeLock().unlock();
		}
		else {
			rwLock.readLock().unlock();
		}
	}

	private RegexRulesLoader createRuleLoader(Configuration conf) {
		String loaderClassName = getConf().getString("regexUrlFilter.loader");
		RegexRulesLoader loader = PluginRepository.createObjectFromClassName(loaderClassName);
		loader.setConf(conf);
		loader.updateCache();
		return loader;
	}

	@Override
	public RegexURLFilter createInstance() {
		return new RegexURLFilter(loader);
	}

}
