package com.dabeeb.miner.data.provider.geo;

import com.dabeeb.miner.plugin.PluginFactory;

public class GeographicalDataProviderFactory extends PluginFactory<GeographicalDataProvider> {
		
	@Override
	public GeographicalDataProvider createInstance() {
		return GeographicalDataProvider.getInstance();
	}
}
