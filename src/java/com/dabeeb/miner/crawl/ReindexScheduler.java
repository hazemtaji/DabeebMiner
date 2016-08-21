package com.dabeeb.miner.crawl;

import javax.jms.JMSException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import com.dabeeb.miner.data.DatabaseClient;
import com.dabeeb.miner.messaging.MessagingManager;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class ReindexScheduler {
	public static void main(String[] args) {
		try {
			
			XMLConfiguration conf = Crawler.initialize();
			Crawler.setupDatabase(conf);
			
			MessagingManager.getInstance().setConf(conf);
			MessagingManager.getInstance().start();
			
			Session session = DatabaseClient.getInstance().getSession();
			ResultSet rset = session.execute("SELECT url FROM dabeeb.cache");
			long count = 0;
			for(Row row : rset) {
				try {
					MessagingManager.getInstance().informReindexUrls(row.getString("url"));
					count++;
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
			
			MessagingManager.getInstance().close();
			DatabaseClient.getInstance().close();
			
			System.out.print("Scheduled ");
			System.out.print(count);
			System.out.println(" URLs for reindexing.");
			
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		
	}
}
