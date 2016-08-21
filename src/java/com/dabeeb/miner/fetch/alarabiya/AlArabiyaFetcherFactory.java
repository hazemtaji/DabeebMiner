package com.dabeeb.miner.fetch.alarabiya;

import com.dabeeb.miner.plugin.PluginFactory;

public class AlArabiyaFetcherFactory extends PluginFactory<AlArabiyaFetcher>{
	
	@Override
	public AlArabiyaFetcher createInstance() {
		return new AlArabiyaFetcher();
	}
}
