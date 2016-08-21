package com.dabeeb.miner.inject.mysql;

import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.data.DBConnectionManager;
import com.dabeeb.miner.inject.InjectableURL;
import com.dabeeb.miner.inject.InjectionStatus;
import com.dabeeb.miner.inject.Injector;
import com.dabeeb.miner.net.urlfilter.URLFilterException;
import com.dabeeb.miner.net.urlfilter.URLFilters;
import com.dabeeb.miner.net.urlnormalizer.URLNormalizers;

public class MySQLInjector implements Injector {
	public static Logger logger = LogManager.getFormatterLogger(MySQLInjector.class);
	
	private static final String CONFIG_PREFIX = "injector.mysql";
	private static final String CONFIG_CONNECTION = CONFIG_PREFIX + ".connection";
	
	private Configuration conf;
	private Hashtable<String, Feed> feeds = new Hashtable<>();

	public MySQLInjector(Configuration conf){
		this.conf = conf;
	}
	
	public List<InjectableURL> getUrls() {
		List<InjectableURL> res = new LinkedList<>();
		feeds.clear();
		
		Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
        	connection = DBConnectionManager.getInstance().getConnection(conf.getString(CONFIG_CONNECTION));
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT feedId, url, categories, fetchFreq, type FROM due_feeds;");

            while (resultSet.next()) {
                String url = resultSet.getString(2);
            	
                Feed feed = new Feed();
                feed.setUrl(url);
                feed.setId(resultSet.getInt(1));
                feed.setCategories(resultSet.getString(3));
                feed.setFetchFrequency(resultSet.getInt(4));
                feed.setType(resultSet.getString(5));
                
                try {
					url = URLNormalizers.getInstance(conf).normalize(url, URLNormalizers.SCOPE_INJECT);
					url = URLFilters.getInstance(conf).filter(url);
				} catch (MalformedURLException e) {
					logger.warn("Injection for URL: %s failed at normalization", url);
					url = null;
				} catch (URLFilterException e) {
					logger.warn("Injection for URL: %s failed at filtering", url);
				}
                
                if(url != null) {
	                Hashtable<String, String> metadata = new Hashtable<>();
	                
	                if(feed.getCategories() != null)
	                	metadata.put("dabeeb-categories", feed.getCategories());
	                //if(feed.getTags() != null)
	                //	metadata.put("tags", feed.getTags());
	                if(feed.getType() != null)
	                	metadata.put("inject-type", feed.getType());
	                
	                feeds.put(url, feed);
	                res.add(new InjectableURL(url, metadata));
                }
            }
            
            PreparedStatement getFeedTags = connection.prepareStatement("SELECT tag, value FROM feed_tag WHERE feedID = ?");
            
            for(InjectableURL injectable : res) {
            	Feed feed = feeds.get(injectable.getUrl());
            	getFeedTags.setInt(1, feed.getId());
            	resultSet = getFeedTags.executeQuery();
            	while (resultSet.next()) {
            		String tag = resultSet.getString(1);
            		String value = resultSet.getString(2);
            		feed.getTags().put(tag, value);
            		
            		injectable.getMetadata().put("dabeeb-" + tag, value);
            	}
            }

        } catch (SQLException e) {
        	//if connection error reset connection
        	if(e.getErrorCode() >= 2000) {
        		//try to close the connection if possible
        		reconnect();
        	}
        	
            logger.error("Error retrieving data from MySQL", e);
        } finally {
            try {
                if(resultSet != null) {
                	resultSet.close();
                }
                if(statement != null) {
                	statement.close();
                }
                if(connection != null) {
                	connection.close();
                }

            } catch (SQLException e) {
                logger.error("Error closing MySQL connection", e);
            }
        }
        
        return res;
	}

	@Override
	public void reportInjected(List<InjectableURL> injectables) {
        Connection connection = null;
		PreparedStatement statement = null;
        try {
        	connection = DBConnectionManager.getInstance().getConnection(conf.getString(CONFIG_CONNECTION));
            statement = connection.prepareStatement("UPDATE feed SET checkDue = ? WHERE url = ?");
            
            for(InjectableURL injectable : injectables) {
            	Feed feed = feeds.get(injectable.getUrl());
            	
            	Timestamp time = new Timestamp(System.currentTimeMillis() + (feed.getFetchFrequency() * 1000));
            	statement.setTimestamp(1, time);
            	statement.setString(2, injectable.getUrl());
            	statement.execute();
            }
            

        } catch (SQLException e) {
        	//if connection error reset connection
        	if(e.getErrorCode() >= 2000) {
        		//try to close the connection if possible
        		reconnect();
        	}
        	
            logger.error("Error writing data to MySQL", e);
        } finally {
            try {
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

	@Override
	public void reportFailure(String url, InjectionStatus status) {
		Connection connection = null;
		PreparedStatement statement = null;

        try {
        	connection = DBConnectionManager.getInstance().getConnection(conf.getString(CONFIG_CONNECTION));
            statement = connection.prepareStatement("UPDATE feed SET problem = ? WHERE url = ?");
            statement.setString(1, status.getMessage());
            statement.setString(2, url);
        	statement.execute();
            

        } catch (SQLException e) {
        	//if connection error reset connection
        	if(e.getErrorCode() >= 2000) {
        		//try to close the connection if possible
        		reconnect();
        	}
        	
            logger.error("Error writing data to MySQL", e);
        } finally {
            try {
                if (statement != null)
                	statement.close();
                
                if(connection != null)
                	connection.close();

            } catch (SQLException e) {
                logger.error("Error closing MySQL connection", e);
            }
        }
	}

	private void reconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reportSuccess(String url) {
		Connection connection = null;
		PreparedStatement statement = null;

        try {
        	connection = DBConnectionManager.getInstance().getConnection(conf.getString(CONFIG_CONNECTION));
            statement = connection.prepareStatement("UPDATE feed SET problem = 'NONE' WHERE url = ?");
            statement.setString(1, url);
        	statement.execute();
            

        } catch (SQLException e) {
        	//if connection error reset connection
        	if(e.getErrorCode() >= 2000) {
        		//try to close the connection if possible
        		reconnect();
        	}
        	
            logger.error("Error writing data to MySQL", e);
        } finally {
            try {
                if (statement != null)
                	statement.close();
                
                if(connection != null)
                	connection.close();

            } catch (SQLException e) {
                logger.error("Error closing MySQL connection", e);
            }
        }
	}
}
