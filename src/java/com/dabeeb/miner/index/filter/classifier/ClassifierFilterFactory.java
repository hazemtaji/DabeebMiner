package com.dabeeb.miner.index.filter.classifier;

import com.dabeeb.miner.plugin.PluginFactory;

public class ClassifierFilterFactory extends PluginFactory<ClassifierFilter> {
	
	@Override
	public ClassifierFilter createInstance() {
		return new ClassifierFilter();
	}
}
