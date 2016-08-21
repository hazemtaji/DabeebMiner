package com.dabeeb.miner.index.solr;

import com.dabeeb.miner.plugin.PluginFactory;

public class SolrIndexerFactory extends PluginFactory<SolrIndexerPlugin> {
	
	@Override
	public SolrIndexerPlugin createInstance() {
		return new SolrIndexerPlugin();
	}
}
