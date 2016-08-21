package com.dabeeb.miner.fetch;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;

import com.dabeeb.miner.plugin.Plugin;

public interface FetcherPlugin extends Plugin {
	public void prefetch(HttpGet httpGet, HttpContext context);
	public void postResponse(CloseableHttpResponse response);
	public void processRawContent(byte[] contentData);
	public String[] getHostNames();
}
