package com.dabeeb.miner.data.model;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;

public class Outlink {
	private String toUrl;
	private String anchor;
	private HashMap<String, String> tags = new HashMap<>();
	private Date publishDate;
	private Date discoveryDate;
	
	public Outlink(String toUrl, String anchor) throws MalformedURLException {
		this.toUrl = toUrl;
		if (anchor == null)
			anchor = "";
		this.anchor = anchor;
		discoveryDate = new Date();
	}
	
	public String getAnchor() {
		return anchor;
	}
	
	public void setAnchor(String anchor) {
		this.anchor = anchor;
	}
	
	public String getToUrl() {
		return toUrl;
	}
	
	public void setToUrl(String toUrl) {
		this.toUrl = toUrl;
	}
	
	public void setDiscoveryDate(Date discoveryDate) {
		this.discoveryDate = discoveryDate;
	}
	
	public Date getDiscoveryDate() {
		return discoveryDate;
	}
	
	public void setPublishDate(Date publishDate) {
		this.publishDate = publishDate;
	}
	
	public Date getPublishDate() {
		return publishDate;
	}
	
	public HashMap<String, String> getTags() {
		return tags;
	}
	
	public void setTags(HashMap<String, String> tags) {
		this.tags = tags;
	}
}
