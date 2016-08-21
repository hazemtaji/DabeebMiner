package com.dabeeb.miner.concurrency;

import java.io.Closeable;
import java.io.IOException;

import org.apache.commons.configuration.Configuration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.scheduler.Scheduler;

public class SchedulerSelectorClient extends LeaderSelectorListenerAdapter implements Closeable {
	public static Logger logger = LogManager.getFormatterLogger(SchedulerSelectorClient.class);
	
	private final String name;
	private final LeaderSelector leaderSelector;
	private Configuration conf;
	
	public SchedulerSelectorClient(CuratorFramework client, String path, String name, Configuration conf) {
		this.name = name;
		this.conf = conf;
		leaderSelector = new LeaderSelector(client, path, this);
		leaderSelector.setId(name);
		leaderSelector.autoRequeue();
	}

	public void start() throws IOException {
		leaderSelector.start();
	}

	@Override
	public void close() throws IOException {
		leaderSelector.close();
	}

	@Override
	public void takeLeadership(CuratorFramework client) throws Exception {

		logger.info(name + " is now the leader...");
		try {
			Scheduler scheduler = new Scheduler(conf);
			
			scheduler.start();
			scheduler.join();
			
		} catch (InterruptedException e) {
			System.err.println(name + " was interrupted.");
			Thread.currentThread().interrupt();
		} finally {
			logger.info(name + " relinquishing leadership.");
		}
	}
}
