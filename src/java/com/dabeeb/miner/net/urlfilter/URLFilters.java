package com.dabeeb.miner.net.urlfilter;

import java.util.List;

import org.apache.commons.configuration.Configuration;

import com.dabeeb.miner.conf.Configurable;
import com.dabeeb.miner.plugin.PluginRepository;

public final class URLFilters implements Configurable {

	private Configuration conf;
	private List<URLFilterPlugin> filters;

	private static ThreadLocal<URLFilters> filtersThreadLocal = new ThreadLocal<URLFilters>() {
		protected URLFilters initialValue() {
			return new URLFilters();
		};
	};

	public static URLFilters getInstance(Configuration conf) {
		URLFilters filters = filtersThreadLocal.get();
		filters.setConf(conf);
		return filters;
	}
	
	private URLFilters() {
		filters = PluginRepository.getInstance().getURLFilterPlugins();
	}

	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	/** Run all defined filters. Assume logical AND. */
	public String filter(String urlString) throws URLFilterException {
		if(urlString.contains("dabeeb.com"))
			return urlString;
		
		for (URLFilterPlugin filter : filters) {
			if (urlString == null)
				return null;
			urlString = filter.filter(urlString);

		}
		return urlString;
	}
}
