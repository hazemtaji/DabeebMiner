package com.dabeeb.miner.index.filter.article;

import com.dabeeb.miner.plugin.PluginFactory;

public class ArticleExtractorFilterFactory extends PluginFactory<ArticleExtractorFilter> {
	
	@Override
	public ArticleExtractorFilter createInstance() {
		return new ArticleExtractorFilter();
	}
}
