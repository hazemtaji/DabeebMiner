package com.dabeeb.miner.crawl;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CrawlStatistics {
	private AtomicLong processedDocuments = new AtomicLong();
	private AtomicInteger underProcessDocuments = new AtomicInteger();
	private Date startTime;
	
	public CrawlStatistics() {
		startTime = new Date();
	}
	
	public long getProcessedDocuments() {
		return processedDocuments.get();
	}
	
	public long incrementProcessedDocuments() {
		return processedDocuments.incrementAndGet();
	}
	
	public int getUnderProcessDocuments() {
		return underProcessDocuments.get();
	}
	
	public int incrementUnderProcessDocuments() {
		return underProcessDocuments.incrementAndGet();
	}
	
	public int decrementUnderProcessDocuments() {
		return underProcessDocuments.decrementAndGet();
	}
	
	public Date getStartTime() {
		return startTime;
	}
}
