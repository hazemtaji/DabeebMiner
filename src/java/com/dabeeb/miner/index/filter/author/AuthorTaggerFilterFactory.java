package com.dabeeb.miner.index.filter.author;

import com.dabeeb.miner.plugin.PluginFactory;

public class AuthorTaggerFilterFactory extends PluginFactory<AuthorTaggerFilter> {
	
	@Override
	public AuthorTaggerFilter createInstance() {
		return new AuthorTaggerFilter();
	}
}
