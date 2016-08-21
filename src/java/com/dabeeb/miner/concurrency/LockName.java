package com.dabeeb.miner.concurrency;

public enum LockName {
	
	HOST_CONNECTIONS("database/host-connections"),
	HOST_DOC_QUEUE("database/host-doc-queue");
	
	private String path;
	private LockName(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}
}
