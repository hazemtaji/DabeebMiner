package com.dabeeb.miner.index.filter.source;

import com.dabeeb.miner.plugin.PluginFactory;

public class SourceTaggerFilterFactory extends PluginFactory<SourceTaggerFilter> {
	
	@Override
	public SourceTaggerFilter createInstance() {
		return new SourceTaggerFilter();
	}
}
