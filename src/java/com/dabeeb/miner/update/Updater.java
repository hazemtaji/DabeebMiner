package com.dabeeb.miner.update;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.JMSException;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.data.DatabaseClient;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.model.Inlink;
import com.dabeeb.miner.data.model.Outlink;
import com.dabeeb.miner.data.dao.CacheDAO;
import com.dabeeb.miner.data.dao.FetchPoolDAO;
import com.dabeeb.miner.messaging.MessagingManager;
import com.dabeeb.miner.net.urlfilter.URLFilterException;
import com.dabeeb.miner.net.urlfilter.URLFilters;
import com.dabeeb.miner.net.urlnormalizer.URLNormalizers;

public class Updater {

	private static final Logger logger = LogManager.getFormatterLogger(Updater.class);

	private final static String CONFIG_PREFIX = "updater";
	private final static String CONFIG_MAX_DEPTH = "crawler.maxDepth";
	
	private Configuration conf;
	private FetchPoolDAO fetchPoolDAO;
	private CacheDAO cacheDAO;

	private int maxDepth;
	
	public Updater(Configuration conf) {
		this.conf = conf;
		fetchPoolDAO = DatabaseClient.getInstance().getFetchPoolDAO();
		cacheDAO = DatabaseClient.getInstance().getCacheDAO();
		maxDepth = conf.getInt(CONFIG_MAX_DEPTH);
	}

	public void update(Document doc) {
		
		URLNormalizers normalizers = URLNormalizers.getInstance(conf);
		URLFilters filters = URLFilters.getInstance(conf);
		
		Outlink[] outlinks = doc.getParsedContent().getOutlinks();
		for(int i = 0; i < outlinks.length; i++){

			try {
				String normalizedURL = normalizers.normalize(outlinks[i].getToUrl(), URLNormalizers.SCOPE_OUTLINK);
				String filteredURL = filters.filter(normalizedURL);
				if(filteredURL == null){
					outlinks[i] = null;
					continue;
				}
				
				outlinks[i].setToUrl(filteredURL);
				
				boolean alreadyAdded = cacheDAO.checkURL(filteredURL);
				if(alreadyAdded)
					outlinks[i] = null;
				
			} catch(URLFilterException | MalformedURLException e){
				outlinks[i] = null;	//Skip this URL
			}
		}
		
		if(doc.getDepth() == maxDepth)
			return;
		
		ArrayList<String> addedUrls = new ArrayList<>();
		
		for(int i = 0; i < outlinks.length; i++) {
			if(outlinks[i] != null) {
				Map<String,String> metadata = new Hashtable<>();
				
				
				if(doc.getDepth() == 1) {
					// Copy relevant metadata from original document (happens only at depth 1)
					for(Entry<String, String> entry : doc.getMetadata().entrySet()) {
						if(entry.getKey().startsWith("dabeeb-")) {
							metadata.put(entry.getKey().substring(7), entry.getValue());
						}
					}
				}

				
				for(Entry<String, String> tagEntry : outlinks[i].getTags().entrySet()) {
					metadata.put(tagEntry.getKey(), tagEntry.getValue());
				}
				
				Inlink inlink = null;
				try {
					inlink = new Inlink(doc.getFinalUrl(), outlinks[i].getAnchor());
				} catch (MalformedURLException e) {
				}
				boolean success = fetchPoolDAO.addURL(outlinks[i].getToUrl(), metadata, doc.getDepth() + 1, outlinks[i].getDiscoveryDate(), outlinks[i].getPublishDate(), inlink);
				if(success) {
					addedUrls.add(outlinks[i].getToUrl());
				}
			}
		}
		
		try {
			MessagingManager.getInstance().informNewUrls(addedUrls.toArray(new String[0]));
		} catch (JMSException e) {
			logger.error("Messaging exception while informing new urls", e);
		}
	}

}
