package com.dabeeb.miner.scheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.crawl.DabeebShutdownHook;

public class Scheduler {
	public static Logger logger = LogManager.getFormatterLogger(Scheduler.class);

	private static final String CONFIG_PREFIX = "scheduler.policy.host";
	private static final String CONFIG_MAX_CONNECTIONS = CONFIG_PREFIX + ".maxconnections";
	private static final String CONFIG_INTERVAL = CONFIG_PREFIX + ".interval";
	private static final String CONFIG_TOLERANCE = CONFIG_PREFIX + ".tolerance";
	private static final String CONFIG_BLACKLIST_DURATION = CONFIG_PREFIX + ".blacklistduration";

	private int hostFetchInterval, maxHostConnections, maxHostTolerance, blacklistDuration;

	private Configuration conf;
	private Map<String, AtomicInteger> connectionCounter = new ConcurrentHashMap<>();
	
	private URLRouterThread routerThread;
	private FetchCompleteListenerThread fetchCompleteListenerThread;
	
	public Scheduler(Configuration conf) {
		this.conf = conf;

		maxHostConnections = conf.getInt(CONFIG_MAX_CONNECTIONS);
		hostFetchInterval = conf.getInt(CONFIG_INTERVAL);
		maxHostTolerance = conf.getInt(CONFIG_TOLERANCE);
		blacklistDuration = conf.getInt(CONFIG_BLACKLIST_DURATION);
	}
	
	public boolean isShutdown() {
		return DabeebShutdownHook.signal_shutdown;
	}

	public void start() {
		routerThread = new URLRouterThread(this);
		routerThread.setName("Scheduler - Router Thread");
		routerThread.start();
		

		fetchCompleteListenerThread = new FetchCompleteListenerThread(this);
		fetchCompleteListenerThread.setName("Scheduler - Fetch Complete Listener Thread");
		fetchCompleteListenerThread.start();
	}

	public void join() throws InterruptedException {
		routerThread.join();
		fetchCompleteListenerThread.join();
	}

	public boolean isHostFree(String host) {
		AtomicInteger aInt = connectionCounter.get(host);
		if(aInt == null)
			return true;
		int currentConnections = aInt.intValue();
		return currentConnections < maxHostConnections;
	}
	
	public void incrementHostConnections(String host) {
		AtomicInteger aInt = connectionCounter.get(host);
		if(aInt == null) {
			connectionCounter.put(host, new AtomicInteger(1));
		} else {
			aInt.incrementAndGet();
		}
	}

	public void decrementHostConnections(String host) {
		AtomicInteger aInt = connectionCounter.get(host);
		if(aInt == null) {
			connectionCounter.put(host, new AtomicInteger(0));
		} else {
			aInt.decrementAndGet();
		}
	}
	
	public int getMaxHostConnections() {
		return maxHostConnections;
	}
	
	public int getHostFetchInterval() {
		return hostFetchInterval;
	}
}
