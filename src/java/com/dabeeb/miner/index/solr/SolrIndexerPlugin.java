package com.dabeeb.miner.index.solr;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.provider.geo.City;
import com.dabeeb.miner.data.provider.geo.Country;
import com.dabeeb.miner.data.provider.geo.GeographicalDataProvider;
import com.dabeeb.miner.data.provider.geo.Region;
import com.dabeeb.miner.index.IndexerPlugin;
import com.dabeeb.miner.index.filter.source.Source;

public class SolrIndexerPlugin implements IndexerPlugin {
	public static Logger logger = LogManager.getFormatterLogger(SolrIndexerPlugin.class);
	
	private static final String CONFIG_PREFIX = "indexer.solr";
	private static final String CONFIG_TYPE = CONFIG_PREFIX + ".type";
	private static final String CONFIG_URL = CONFIG_PREFIX + ".url";
	private static final String CONFIG_COLLECTION = CONFIG_PREFIX + ".collection";
	private static final String CONFIG_COMMIT_SIZE = CONFIG_PREFIX + ".commitsize";
	
	private HashSet<String> onlyStoredFields = new HashSet<>();
	private HashSet<String> ignoreMetaData = new HashSet<>();
	
	Configuration conf;
	static CloudSolrServer cloud = null;
	SolrServer solr;
	int commitSize;
	
	public SolrIndexerPlugin() {
		onlyStoredFields.add("image");
		onlyStoredFields.add("description");
		
		ignoreMetaData.add("inject-type");
	}
	
	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
		
		String solrType = conf.getString(CONFIG_TYPE, "standalone");
		if(solrType.equals("standalone")) {
			solr = new HttpSolrServer(conf.getString(CONFIG_URL));
		} else if(solrType.equals("cloud")) {
			if(cloud == null) {
				cloud = new CloudSolrServer(conf.getString(CONFIG_URL));
				cloud.setDefaultCollection(conf.getString(CONFIG_COLLECTION));
			}
			solr = cloud;
		}
		commitSize = conf.getInt(CONFIG_COMMIT_SIZE, 1000);
	}

	@Override
	public boolean index(Document doc, boolean forceReindex) {
		SolrInputDocument inputDoc = getSolrDocument(doc);
		String urlId = inputDoc.getFieldValue("url").toString();
		
		// inputDoc.setDocumentBoost(doc.getScore());

		try {
			if(forceReindex) {
				if(logger.isInfoEnabled()) {
					logger.info("Reindexing document: %s", urlId);
				}
				UpdateResponse response = solr.deleteByQuery("url:\"" + urlId + "\"");
				solr.add(inputDoc);
			} else {
				SolrQuery query = new SolrQuery();
				query.setFilterQueries("url:\"" + urlId + "\"");
				query.setQuery("*");
				
				try {
					QueryResponse response = solr.query(query);
					long count = response.getResults().getNumFound();
					if (count == 0) {
						if(logger.isInfoEnabled()) {
							logger.info("Adding document: %s", urlId);
						}
						solr.add(inputDoc);
					} else {
						if(logger.isInfoEnabled()) {
							logger.info("Document already in index");
						}
					}
				} catch (final SolrServerException e) {
					logger.error("Error looking up document", e);
					return false;
				}
			}
		} catch (final SolrServerException e) {
			logger.error("Error adding documents", e);
			return false;
		} catch (IOException e) {
			logger.error("Error adding documents", e);
			return false;
		}

		return true;
	}
	
	@Override
	public void flush() {
		
		// Auto Commit enabled in Solr
	}

	public SolrInputDocument getSolrDocument(Document doc) {
		
		SolrInputDocument inputDoc = new SolrInputDocument();
		
		Map<String, String> metadata = doc.getMetadata();

		if(doc.getFinalUrl() != null)
			inputDoc.addField("url", doc.getFinalUrl().toString());
		else
			inputDoc.addField("url", doc.getUrl().toString());
		inputDoc.addField("title", doc.getParsedContent().getTitle());
		inputDoc.addField("article", doc.getParsedContent().getText());
		inputDoc.addField("comments", "");
		
		/* Timestamp Field */
		if(doc.getPublishTime() != null)
			inputDoc.addField("tstamp", doc.getPublishTime());
		else
			inputDoc.addField("tstamp", doc.getFetchTime());
		
		/* Host Field */
		URL url = doc.getUrl();
		inputDoc.addField("host", url.getHost());
		
		HashSet<String> locationCodes = new HashSet<String>();
		
		/* Dynamic Fields */
		for(Entry<String,String> entry : metadata.entrySet()) {
			String fieldName = entry.getKey();
			/* Category Field */
			if(fieldName.equals("category")) {
				for(String category : entry.getValue().split(" ")) 
					inputDoc.addField("category", category);
				continue;
			}
			
			/*  Tags Field */
			if(fieldName.equals("tags")) {
				for(String tag : entry.getValue().split(" ")) {
					inputDoc.addField("tags", tag);
				}
				continue;
			}
			
			/* Location Fields: location, region, city, country */
			if(fieldName.equals("location") || fieldName.equals("region") || fieldName.equals("city") || fieldName.equals("country")) {
				String[] locationTags = entry.getValue().split(",");
				for(String tag : locationTags) {
					//store in a hashset to kill duplicates
					locationCodes.add(tag.toLowerCase());
				}
				continue;
			}
			
			if(onlyStoredFields.contains(entry.getKey())) {
				inputDoc.addField("prop_" + entry.getKey(), entry.getValue());
			} else {
				inputDoc.addField("tag_" + entry.getKey(), entry.getValue());
			}
		}
		
		
		HashSet<String> locationNames = new HashSet<String>();
		for(String locationCode : locationCodes) {
			inputDoc.addField("location", locationCode);
			
			Country country = GeographicalDataProvider.getInstance().getCountryByCode(locationCode);
			if(country != null) {
				if(country.getNameAr() != null)
					locationNames.add(country.getNameAr());
				if(country.getNameEn() != null)
					locationNames.add(country.getNameEn());
				for(String alias : country.getAllAliases()) {
					locationNames.add(alias);
				}
			} else {
				Region region = GeographicalDataProvider.getInstance().getRegionByCode(locationCode);
				if(region != null) {
					if(region.getNameAr() != null)
						locationNames.add(region.getNameAr());
					if(region.getNameEn() != null)
						locationNames.add(region.getNameEn());
					for(String alias : region.getAllAliases()) {
						locationNames.add(alias);
					}
				} else {
					City city = GeographicalDataProvider.getInstance().getCityByCode(locationCode);
					if(city != null) {
						if(city.getNameAr() != null)
							locationNames.add(city.getNameAr());
						if(city.getNameEn() != null)
							locationNames.add(city.getNameEn());
						for(String alias : city.getAllAliases()) {
							locationNames.add(alias);
						}
					}
				}
			}
		}
		
		for(String locationName : locationNames) {
			inputDoc.addField("location", locationName);
		}
		
		Source source = doc.getData("source");
		if(source != null) {
			indexSource(inputDoc, source);
		}

		
		inputDoc.addField("boost", 1);
		inputDoc.addField("lang", "ar");
		
		return inputDoc;
	}

	public void indexSource(SolrInputDocument inputDoc, Source source) {
		inputDoc.addField("source_name", source.getNameAr());
		inputDoc.addField("source_name", source.getNameEn());
		for(String alias : source.getAliases()) {
			inputDoc.addField("source_name", alias);
		}
		
		HashSet<String> locations = new HashSet<String>();
		
		String countryCode = source.getCountry().toLowerCase();
		if(countryCode != null) {
			locations.add("cntry-" + countryCode);
			Country country = GeographicalDataProvider.getInstance().getCountryByCode(countryCode);
			if(country != null) {
				if(country.getNameAr() != null)
					locations.add(country.getNameAr());
				if(country.getNameEn() != null)
					locations.add(country.getNameEn());
				for(String alias : country.getAllAliases()) {
					locations.add(alias);
				}
			} else {
				logger.warn("No country found with the following code: %s", countryCode);
			}
		} else {
			logger.warn("Source: '%s' has no country location defined", source.toString());
		}

		if(source.getRegion() != null) {
			String regionCode = source.getRegion().toLowerCase();
			locations.add("rgn-" + source.getRegion());
			Region region = GeographicalDataProvider.getInstance().getRegionByCode(regionCode);
			
			if(region != null) {
				if(region.getNameAr() != null)
					locations.add(region.getNameAr());
				if(region.getNameEn() != null)
					locations.add(region.getNameEn());
				for(String alias : region.getAllAliases()) {
					locations.add(alias);
				}
			} else {
				logger.warn("No region found with the following code: %s", regionCode);
			}
		} else {
			logger.warn("Source: '%s' has no region location defined", source.toString());
		}

		if(source.getCity() != null) {
			String cityCode = source.getCity().toLowerCase();
			locations.add("cty-" + source.getCity());
			City city = GeographicalDataProvider.getInstance().getCityByCode(cityCode);
			
			if(city != null) {
				if(city.getNameAr() != null)
					locations.add(city.getNameAr());
				if(city.getNameEn() != null)
					locations.add(city.getNameEn());
				for(String alias : city.getAllAliases()) {
					locations.add(alias);
				}
			} else {
				logger.warn("No city found with the following code: %s", cityCode);
			}
		}
		
		for(String location : locations) {
			inputDoc.addField("source_location", location);
		}
	}
}
