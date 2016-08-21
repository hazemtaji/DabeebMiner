package com.dabeeb.miner.concurrency;

import java.io.IOException;

import org.apache.commons.configuration.Configuration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CuratorManager {
	public static Logger logger = LogManager.getFormatterLogger(CuratorManager.class);
	private static CuratorManager instance = new CuratorManager();
	
	private static final String CONFIG_ZOOKEEPER_PREFIX = "zookeeper";
	private static final String CONFIG_ZOOKEEPER_SERVERS = CONFIG_ZOOKEEPER_PREFIX + ".servers.server";
	
	private Configuration conf;
	
	private CuratorFramework client;
	private SchedulerSelectorClient ssClient;
	
	protected CuratorManager() {
	}
	
	public void setConf(Configuration conf) {
		this.conf = conf;
		String[] zkServers = conf.getStringArray(CONFIG_ZOOKEEPER_SERVERS);
		String connectionString = commaSeperateJoin(zkServers);
		client = CuratorFrameworkFactory.newClient(connectionString, new ExponentialBackoffRetry(1000, 3));
		client.start();
	}
	
	public InterProcessReadWriteLock getRWLock(LockName lockName) {
		return new InterProcessReadWriteLock(client, "/rw-locks/" + lockName.getPath());
	}
	
	public InterProcessReadWriteLock getRWLock(String path) {
		return new InterProcessReadWriteLock(client, "/rw-locks/" + path);
	}
	
	public InterProcessSemaphoreMutex getSemaphore(String path) {
		return new InterProcessSemaphoreMutex(client, "/semaphores/mutex/" + path);
	}
	
	public void nominateScheduler(String myName) {
		ssClient = new SchedulerSelectorClient(client, "/scheduler/leader", myName, conf);
		try {
			ssClient.start();
		} catch (IOException e) {
			logger.error("Error nominating scheduler", e);
		}
	}
	
	public void shutdown() {
		try {
			ssClient.close();
		} catch (IOException e) {
			logger.error("Error shutting down", e);
		} finally {
			client.close();
		}
	}
	
	public static CuratorManager getInstance() {
		return instance;
	}
	
	private static String commaSeperateJoin(String[] parts) {
		StringBuffer sb = new StringBuffer();
		
		for(int i = 0; i < parts.length; i++) {
			if(i != 0)
				sb.append(',');
			sb.append(parts[i]);
		}
		
		return sb.toString();
	}
}
