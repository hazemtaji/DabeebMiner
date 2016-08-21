package com.dabeeb.miner.data.provider;

import org.apache.commons.configuration.Configuration;

import com.dabeeb.miner.cache.Cachable;
import com.dabeeb.miner.cache.CachablePluginsRepository;
import com.dabeeb.miner.plugin.Plugin;

/**
 * 
 * @author user
 *
 */
public abstract class DataProvider implements Plugin, Cachable {
	private Configuration conf;
	
	public void register() {
		CachablePluginsRepository.getInstance().registerPlugin(this);
	}

	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}
	
	protected Configuration getConf() {
		return conf;
	}
}
