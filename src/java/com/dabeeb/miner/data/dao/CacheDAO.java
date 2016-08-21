package com.dabeeb.miner.data.dao;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Hashtable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.data.DatabaseClient;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.model.Parse;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class CacheDAO {
	
	public static Logger logger = LogManager.getFormatterLogger(CacheDAO.class);
	private Session session;
	private PreparedStatement addDocStatement;
	private PreparedStatement addRedirectStatement;
	private PreparedStatement getDocStatement;
	private PreparedStatement updateParsedDocStatement;
	
	public CacheDAO(DatabaseClient dbClient) {
		session = dbClient.getSession();
		addDocStatement = session.prepare("INSERT INTO dabeeb.cache (url, raw, encoding, title, article, discovery, publish, metadata) VALUES (?,?,?,?,?,?,?,?);");
		addRedirectStatement = session.prepare("INSERT INTO dabeeb.cache (url, redirect, discovery, publish) VALUES (?,?,?,?);");
		getDocStatement = session.prepare("SELECT * FROM dabeeb.cache WHERE url=?");
		updateParsedDocStatement = session.prepare("UPDATE dabeeb.cache SET title = ?, article = ? WHERE url = ?");
	}
	
	public boolean addDocument(Document doc) {
		byte[] rawContent = doc.getRawContent();
		ByteBuffer byteBuffer = ByteBuffer.allocate(rawContent.length);
		byteBuffer.put(rawContent);
		
		BoundStatement bndAddDoc = addDocStatement.bind(
				doc.getFinalUrl().toString(),
				byteBuffer,
				doc.getEncoding(),
				doc.getParsedContent().getTitle(),
				doc.getParsedContent().getText(),
				doc.getFetchTime(),
				doc.getPublishTime(),
				doc.getMetadata()
				);
		session.execute(bndAddDoc);
		
		if(!doc.getFinalUrl().equals(doc.getUrl())) {
			BoundStatement bndRedirectDoc = addRedirectStatement.bind(
					doc.getUrl().toString(),
					doc.getFinalUrl().toString(),
					doc.getFetchTime(),
					doc.getPublishTime()
					);
			session.execute(bndRedirectDoc);
		}
		
		//if(success){
			logger.info("Moved URL to cache: %s", doc.getFinalUrl());
			return true;
		//}
		//return false;
	}
	
	public boolean updateParsedDoc(Document doc) {
		BoundStatement bndUpdateDoc = updateParsedDocStatement.bind(
				doc.getParsedContent().getTitle(),
				doc.getParsedContent().getText(),
				doc.getFinalUrl().toString()
				);
		session.execute(bndUpdateDoc);
		
		return true;
	}

	public boolean checkURL(String filteredURL) {
		BoundStatement bnd = getDocStatement.bind(filteredURL);
		ResultSet rset = session.execute(bnd);
		return rset.one() != null;
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
		
		if(row.getString("redirect") != null) {
			return null;
		}
		
		Document doc = new Document();
		
		try {
			doc.setFinalUrl(new URL(row.getString("url")));
		} catch (MalformedURLException e1) { }
		
		ByteBuffer buff = row.getBytes("raw");
		if(buff == null)
			return null;
		byte[] raw = new byte[buff.remaining()];
		buff.get(raw);
		doc.setRawContent(raw);
		
		doc.setEncoding(row.getString("encoding"));
		doc.setFetchTime(row.getDate("discovery"));
		doc.setPublishTime(row.getDate("publish"));
		
		Parse parsedContent = new Parse();
		parsedContent.setTitle(row.getString("title"));
		parsedContent.setText(row.getString("article"));
		doc.setParsedContent(parsedContent);
		
		Hashtable<String, String> metadata = new Hashtable<>(row.getMap("metadata", String.class, String.class));
		doc.setMetadata(metadata);
		
		String injectType = metadata.get("inject-type");
		if(injectType != null && injectType.equals("rss")) {
			return null;
		}
		
		return doc;
	}

}
