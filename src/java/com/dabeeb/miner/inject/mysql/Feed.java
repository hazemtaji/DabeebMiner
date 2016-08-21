package com.dabeeb.miner.inject.mysql;

import java.util.HashMap;

public class Feed {
	private int id;
	private String url;
	private String categories;
	private HashMap<String, String> tags = new HashMap<>();
	private int fetchFrequency;
	private String type;

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getCategories() {
		return categories;
	}

	public void setCategories(String categories) {
		this.categories = categories;
	}

	public HashMap<String, String> getTags() {
		return tags;
	}

	public void setTags(HashMap<String, String> tags) {
		this.tags = tags;
	}

	public int getFetchFrequency() {
		return fetchFrequency;
	}

	public void setFetchFrequency(int fetchFrequency) {
		this.fetchFrequency = fetchFrequency;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
