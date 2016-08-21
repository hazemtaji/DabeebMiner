package com.dabeeb.miner.index;

import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.plugin.Plugin;

public interface IndexerPlugin extends Plugin {
	public boolean index(Document doc, boolean forceReindex);
	public void flush();
}
