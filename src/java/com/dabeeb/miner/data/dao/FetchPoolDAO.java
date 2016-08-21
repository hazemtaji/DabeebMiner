package com.dabeeb.miner.data.dao;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.data.DatabaseClient;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.model.Inlink;
import com.dabeeb.miner.messaging.MessagingManager;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class FetchPoolDAO {
	
	public static Logger logger = LogManager.getFormatterLogger(FetchPoolDAO.class);
	private Session session;
	private PreparedStatement addStatement;
	private PreparedStatement deleteDocStatement;
	private PreparedStatement getDocStatement;
	private PreparedStatement getAllStatement;
	
	public FetchPoolDAO(DatabaseClient dbClient) {
		session = dbClient.getSession();
		addStatement = session.prepare("INSERT INTO dabeeb.fetchpool (url, score, depth, metadata, discovery, publish, inlinkUrl, inlinkAnchor) VALUES (?,?,?,?,?,?,?,?) IF NOT EXISTS;");
		getDocStatement = session.prepare("SELECT * FROM dabeeb.fetchpool WHERE url=?;");
		getAllStatement = session.prepare("SELECT url FROM dabeeb.fetchpool;");
		deleteDocStatement = session.prepare("DELETE FROM dabeeb.fetchpool WHERE url=?;");
	}
	
	public boolean addURL(String url, Map<String, String> metadata, int depth, Date discoveryDate, Date publishDate, Inlink inlink) {
		String fromUrl = null;
		String anchor = null;
		
		if(inlink != null) {
			fromUrl = inlink.getFromUrl().toString();
			anchor = inlink.getAnchor();
		}
		
		BoundStatement bnd = addStatement.bind(url, 1, depth, metadata, discoveryDate, publishDate, fromUrl, anchor);
		ResultSet rset = session.execute(bnd);
		boolean success = rset.one().getBool(0);
		if(success) {
			logger.trace("Added URL: %s", url);
		}
		return success;
	}
	
	public void removeURL(String url) {
		BoundStatement bnd = deleteDocStatement.bind(url);
		session.execute(bnd);
	}
	
	public Document getURL(String url) {
		BoundStatement bnd = getDocStatement.bind(url);
		ResultSet rs =  session.execute(bnd);
		Row row = rs.one();
		if(row == null)
			return null;
		
		Document doc = createDocument(row);
		
		return doc;
	}

	public Document createDocument(Row row) {
		Document doc = new Document();
		doc.setDbUrl(row.getString("url"));
		Hashtable<String, String> metadata = new Hashtable<>(row.getMap("metadata", String.class, String.class));
		doc.setMetadata(metadata);
		doc.setDepth(row.getInt("depth"));
		doc.setPublishTime(row.getDate("publish"));
		
		//int phase = row.getInt("phase");
		//int score = row.getInt("score");
		
		List<Inlink> inlinks = new ArrayList<>(1);
		try {
			inlinks.add(new Inlink(new URL(row.getString("inlinkUrl")), row.getString("inlinkAnchor")));
		} catch (MalformedURLException e) {
		}
		doc.setInlinks(inlinks);
		return doc;
	}

	public void republish() {
		logger.info("Republishing URLs");
		
		MessagingManager mm = MessagingManager.getInstance();
		
		ArrayList<String> buffer = new ArrayList<>(256);
		
		BoundStatement bnd = getAllStatement.bind();
		ResultSet rs =  session.execute(bnd);
		
		for(Row row : rs) {
			String url = row.getString("url");
			buffer.add(url);
			
			if(buffer.size() == 256) {
				try {
					logger.info("Publishing batch...");
					mm.informNewUrls(buffer.toArray(new String[0]));
					buffer.clear();
				} catch (JMSException e) {
					logger.error("Error publishing batch", e);
				}
			}
		}
		
		if(buffer.size() > 0) {
			try {
				logger.info("Publishing last batch...");
				mm.informNewUrls(buffer.toArray(new String[0]));
				buffer.clear();
			} catch (JMSException e) {
				logger.error("Error publishing last batch", e);
			}
		}
		
		logger.info("Done republishing...");
	}
}
