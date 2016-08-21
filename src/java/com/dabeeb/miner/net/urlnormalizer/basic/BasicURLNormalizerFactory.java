package com.dabeeb.miner.net.urlnormalizer.basic;

import com.dabeeb.miner.plugin.PluginFactory;

public class BasicURLNormalizerFactory extends PluginFactory<BasicURLNormalizer> {

	@Override
	public BasicURLNormalizer createInstance() {
		return new BasicURLNormalizer();
	}

}
