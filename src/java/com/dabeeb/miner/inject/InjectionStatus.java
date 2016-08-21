package com.dabeeb.miner.inject;

public enum InjectionStatus {
	FETCH_FAILED("URL Fetch Failed"),
	TYPE_MISMATCH("URL Type does not match injection type"),
	PARSE_ERRROR("URL could not be parsed");
	
	private String message;
	InjectionStatus(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
}
