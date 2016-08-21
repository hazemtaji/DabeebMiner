package com.dabeeb.miner.data.model;

import java.util.Date;

public class Host {
	private String hostName;
	private Date lastFetch;
	private int lastDuration;
	private long failures;
	
	public Host(String hostName) {
		this.hostName = hostName;
	}
	
	public String getHostName() {
		return hostName;
	}
	
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
	public Date getLastFetch() {
		return lastFetch;
	}
	
	public void setLastFetch(Date lastFetch) {
		this.lastFetch = lastFetch;
	}
	
	public int getLastDuration() {
		return lastDuration;
	}
	
	public void setLastDuration(int lastDuration) {
		this.lastDuration = lastDuration;
	}
	
	public long getFailures() {
		return failures;
	}
	
	public void setFailures(long failures) {
		this.failures = failures;
	}
}
