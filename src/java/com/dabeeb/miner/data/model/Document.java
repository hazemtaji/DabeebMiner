package com.dabeeb.miner.data.model;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Document {
	private String dbUrl;
	private URL url;
	private URL finalUrl;
	
	private Map<String, String> metadata = new Hashtable<>();
	private Map<String, Object> dataStore = new Hashtable<>();
	private List<Inlink> inlinks;
	
	private byte[] rawContent;
	private String mimeType;
	private String encoding;
	
	private int depth;
	
	private Date publishTime;
	private Date fetchTime;
	
	private Parse parsedContent;
	
	private boolean index = true;
	
	public String getDbUrl() {
		return dbUrl;
	}
	
	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}
	
	public URL getUrl() {
		if(url == null) {
			try {
				url = new URL(dbUrl);
			} catch (MalformedURLException e) {
			}
		}
		return url;
	}
	
	public void setUrl(URL url) {
		this.url = url;
	}
	
	public URL getFinalUrl() {
		return finalUrl;
	}
	
	public void setFinalUrl(URL finalUrl) {
		this.finalUrl = finalUrl;
	}
	
	public Map<String, String> getMetadata() {
		return metadata;
	}
	
	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
	
	public void setData(String key, Object obj) {
		dataStore.put(key, obj);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getData(String key) {
		return (T)dataStore.get(key);
	}
	
	public byte[] getRawContent() {
		return rawContent;
	}
	
	public void setRawContent(byte[] rawContent) {
		this.rawContent = rawContent;
	}
	
	public String getEncoding() {
		return encoding;
	}
	
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public String getMimeType() {
		return mimeType;
	}
	
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	public Date getFetchTime() {
		return fetchTime;
	}
	
	public void setFetchTime(Date fetchTime) {
		this.fetchTime = fetchTime;
	}
	
	public Date getPublishTime() {
		return publishTime;
	}
	
	public void setPublishTime(Date publishTime) {
		this.publishTime = publishTime;
	}
	
	public Parse getParsedContent() {
		return parsedContent;
	}
	
	public void setParsedContent(Parse parsedContent) {
		this.parsedContent = parsedContent;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}

	/**
	 * Determines if document should be indexed
	 * 
	 * @return should index document;
	 */
	public boolean isIndex() {
		return index;
	}

	/**
	 * Change the status of document indexing, default is true, don't set to true unless you know what you are doing
	 * 
	 * @param index
	 */
	public void setIndex(boolean index) {
		this.index = index;
	}
	
	public List<Inlink> getInlinks() {
		return inlinks;
	}
	
	public void setInlinks(List<Inlink> inlinks) {
		this.inlinks = inlinks;
	}
}
