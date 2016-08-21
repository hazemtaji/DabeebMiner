package com.dabeeb.miner.fetch;

import org.apache.commons.configuration.Configuration;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DefaultFetcher implements FetcherPlugin {
	public static Logger logger = LogManager.getFormatterLogger(DefaultFetcher.class);
	
	@Override
	public void prefetch(HttpGet httpGet, HttpContext context) {
	}

	@Override
	public void setConf(Configuration conf) {
	}

	@Override
	public void postResponse(CloseableHttpResponse response) {
	}

	@Override
	public void processRawContent(byte[] contentData) {
	}

	@Override
	public String[] getHostNames() {
		return new String[0];
	}

}
