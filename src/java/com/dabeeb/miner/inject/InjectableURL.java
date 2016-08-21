package com.dabeeb.miner.inject;

import java.util.Map;

public class InjectableURL {
	private String url;
	private Map<String, String> metadata;
	
	public InjectableURL() {
	}
	
	public InjectableURL(String url,Map<String, String> metadata) {
		this.url = url;
		this.metadata = metadata;
	}

	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public Map<String, String> getMetadata() {
		return metadata;
	}
	
	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
}
