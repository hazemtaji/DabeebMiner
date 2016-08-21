package com.dabeeb.miner.plugin;

import org.apache.commons.configuration.Configuration;

import com.dabeeb.miner.conf.Configurable;

public abstract class PluginFactory<T extends Plugin> implements Configurable{
	private Configuration conf;
	
	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}
	
	protected Configuration getConf() {
		return conf;
	}
	
	public abstract T createInstance();
}
