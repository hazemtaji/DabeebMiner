package com.dabeeb.miner.data.model;

import java.net.MalformedURLException;
import java.net.URL;

public class Inlink {
	private URL fromUrl;
	private String anchor;
	
	public Inlink(URL fromUrl, String anchor) throws MalformedURLException {
		this.fromUrl = fromUrl;
		if (anchor == null)
			anchor = "";
		this.anchor = anchor;
	}
	
	public String getAnchor() {
		return anchor;
	}
	
	public void setAnchor(String anchor) {
		this.anchor = anchor;
	}
	
	public URL getFromUrl() {
		return fromUrl;
	}
	
	public void setFromUrl(URL fromUrl) {
		this.fromUrl = fromUrl;
	}
}
