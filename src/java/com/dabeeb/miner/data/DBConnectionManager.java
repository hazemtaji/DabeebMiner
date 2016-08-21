package com.dabeeb.miner.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;

import javax.sql.DataSource;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class DBConnectionManager {
	public static Logger logger = LogManager.getFormatterLogger(DBConnectionManager.class);

	private static final String CONFIG_PREFIX = "dbConnectionManager";
	private static final String CONFIG_CONNECTION = CONFIG_PREFIX + ".connection";
	private static final String CONFIG_URL = "url";
	private static final String CONFIG_USER = "user";
	private static final String CONFIG_PASSWORD = "password";
	private static final String CONFIG_NAME = "name";
	
	
	private static DBConnectionManager instance = new DBConnectionManager();
	private Configuration conf;
	private Hashtable<String, DataSource> dataSources = new Hashtable<>();
	
	private DBConnectionManager() {
	}
	
	public static DBConnectionManager getInstance() {
		return instance;
	}
	
	public void setConf(Configuration conf) {
		this.conf = conf;
		establishConnections();
	}

	private void establishConnections() {
		for(int i = 0; true; i++) {
			Configuration sub = conf.subset(CONFIG_CONNECTION + "(" + i + ")");
			if(sub.isEmpty())
				return;
			
			String dbUrl = sub.getString(CONFIG_URL);
			String dbUser = sub.getString(CONFIG_USER);
			String dbPassword = sub.getString(CONFIG_PASSWORD);
			String connectionName = sub.getString(CONFIG_NAME);
				
			ComboPooledDataSource cpds = new ComboPooledDataSource();       
			cpds.setJdbcUrl( dbUrl );
			cpds.setUser(dbUser);                                  
			cpds.setPassword(dbPassword);
			
			cpds.setMinPoolSize(5);                                     
			cpds.setAcquireIncrement(5);
			cpds.setMaxPoolSize(20);
				
			dataSources.put(connectionName, cpds);
		} 
	}
	
	public Connection getConnection(String name) throws SQLException {
		DataSource ds = dataSources.get(name);
		return ds.getConnection();
	}
	
	
}
