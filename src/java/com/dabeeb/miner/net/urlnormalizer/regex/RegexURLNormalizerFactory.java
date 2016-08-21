package com.dabeeb.miner.net.urlnormalizer.regex;

import com.dabeeb.miner.plugin.PluginFactory;

public class RegexURLNormalizerFactory extends PluginFactory<RegexURLNormalizer> {

	@Override
	public RegexURLNormalizer createInstance() {
		return new RegexURLNormalizer();
	}

}
