package com.dabeeb.miner.parse.rss;

import com.dabeeb.miner.plugin.PluginFactory;

public class RSSParserFactory extends PluginFactory<RSSParser> {

	@Override
	public RSSParser createInstance() {
		return new RSSParser();
	}

}
