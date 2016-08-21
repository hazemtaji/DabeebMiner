package com.dabeeb.miner.parse.html;

import com.dabeeb.miner.parse.ParserPlugin;
import com.dabeeb.miner.plugin.PluginFactory;

public class HTMLParserFactory extends PluginFactory<ParserPlugin> {

	@Override
	public ParserPlugin createInstance() {
		//return new HTMLParser();
		return new HTMLBoilerpipeParser();
	}

}
