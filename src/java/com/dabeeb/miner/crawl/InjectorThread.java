package com.dabeeb.miner.crawl;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.data.DatabaseClient;
import com.dabeeb.miner.data.dao.FetchPoolDAO;
import com.dabeeb.miner.inject.InjectableURL;
import com.dabeeb.miner.inject.Injector;
import com.dabeeb.miner.inject.mysql.MySQLInjector;
import com.dabeeb.miner.messaging.MessagingManager;

public class InjectorThread extends Thread{
	public static Logger logger = LogManager.getFormatterLogger(InjectorThread.class);
	private Configuration conf;
	private Injector injector;
	private FetchPoolDAO fetchPoolDAO;
	
	public InjectorThread(Configuration conf) {
		setDaemon(true);
		this.conf = conf;
		
		injector = new MySQLInjector(conf);
		fetchPoolDAO = DatabaseClient.getInstance().getFetchPoolDAO();
	}
	
	@Override
	public void run() {
		int sleepTime = 10 * 1000;
		while(!DabeebShutdownHook.signal_shutdown) {
			
			int count = 0;
			
			List<InjectableURL> injectables = injector.getUrls();
			List<InjectableURL> injected = new ArrayList<>(injectables.size());
			for(InjectableURL injectable : injectables) {
				boolean added = fetchPoolDAO.addURL(injectable.getUrl(), injectable.getMetadata(), 1, null, null, null);
				try {
					//if(added) {
						MessagingManager.getInstance().informNewUrls(injectable.getUrl());
						injected.add(injectable);
					//}
				} catch (JMSException e1) {
					logger.error(e1);
				}
				if(count >= 10) {
					//spread out fetches a bit
					try {
						sleep(sleepTime);
						count = 0;
					} catch (InterruptedException e) {
						logger.error("Sleep interrupted", e);
					}
				}
			}
			
			injector.reportInjected(injected);
			
			try {
				sleep(sleepTime);
			} catch (InterruptedException e) {
				logger.error("Sleep interrupted", e);
			}
		}
	}
	
	public Injector getInjector() {
		return injector;
	}
}
