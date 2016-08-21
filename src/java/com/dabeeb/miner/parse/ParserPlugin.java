package com.dabeeb.miner.parse;

import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.plugin.Plugin;

public interface ParserPlugin extends Plugin {
	public void parse(Document doc);
	public String[] getMimeTypes();
}
