package com.dabeeb.miner.index.filter.location;

import com.dabeeb.miner.plugin.PluginFactory;

public class LocationTaggerFilterFactory extends PluginFactory<LocationTaggerFilter> {
	
	@Override
	public LocationTaggerFilter createInstance() {
		return new LocationTaggerFilter();
	}
}