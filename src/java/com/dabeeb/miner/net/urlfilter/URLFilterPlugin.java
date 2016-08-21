package com.dabeeb.miner.net.urlfilter;

import com.dabeeb.miner.plugin.Plugin;

/**
 * Interface used to limit which URLs enter Nutch. Used by the injector and the
 * db updater.
 */
public interface URLFilterPlugin extends Plugin {

	/*
	 * Interface for a filter that transforms a URL: it can pass the original
	 * URL through or "delete" the URL by returning null
	 */
	public String filter(String urlString);

}
