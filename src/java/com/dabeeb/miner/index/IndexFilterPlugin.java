package com.dabeeb.miner.index;

import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.plugin.Plugin;

public interface IndexFilterPlugin extends Plugin {
	public boolean filter(Document doc);
}
