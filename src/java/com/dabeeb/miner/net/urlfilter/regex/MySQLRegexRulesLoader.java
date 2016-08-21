package com.dabeeb.miner.net.urlfilter.regex;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.data.DBConnectionManager;

public class MySQLRegexRulesLoader implements RegexRulesLoader{
	private final static Logger logger = LogManager.getFormatterLogger(MySQLRegexRulesLoader.class);
	
	private static final String CONFIG_PREFIX = "mysqlRegexRulesLoader";
	private static final String CONFIG_CONNECTION = CONFIG_PREFIX + ".connection";
	
	private Configuration conf;
	private List<RegexRule> rules;
	private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);

	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
		rules = new ArrayList<>();
	}

	@Override
	public List<RegexRule> getRules() {
		List<RegexRule> res = null;
		
		rwLock.readLock().lock();
		try {
			res = new ArrayList<RegexRule>(rules);
		} finally {
			rwLock.readLock().unlock();
		}
		
		return res;
	}

	@Override
	public void updateCache() {
		rwLock.writeLock().lock();
		
		Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        
		try {
            connection = DBConnectionManager.getInstance().getConnection(conf.getString(CONFIG_CONNECTION));
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT filter.filter FROM filter, source WHERE filter.srcID = source.srcID AND source.live = 1 ORDER BY filter.filterOrder ASC;");
            
            while (resultSet.next()) {
            	
                String filter = resultSet.getString(1);
                
                char first = filter.charAt(0);
    			boolean sign = false;
    			switch (first) {
    			case '+':
    				sign = true;
    				break;
    			case '-':
    				sign = false;
    				break;
    			default:
    				logger.warn("Invalid first character: " + filter);
    			}

    			String regex = filter.substring(1);
    			if (logger.isTraceEnabled()) {
    				logger.trace("Adding rule [" + regex + "]");
    			}
    			RegexRule rule = new RegexRule(sign, regex);
    			rules.add(rule);
            }

        } catch (SQLException e) {
            logger.error("Error retrieving data form MySQL", e);
        } finally {
        	rwLock.writeLock().unlock();
            try {
                if (resultSet != null) {
                	resultSet.close();
                }
                if (statement != null) {
                	statement.close();
                }
                if(connection != null) {
                	connection.close();
                }

            } catch (SQLException e) {
                logger.error("Error closing MySQL connection", e);
            }
        }
	}

}
