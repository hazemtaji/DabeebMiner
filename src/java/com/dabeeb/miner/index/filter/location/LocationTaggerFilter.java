package com.dabeeb.miner.index.filter.location;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.crawl.Crawler;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.provider.geo.GeographicalDataProvider;
import com.dabeeb.miner.data.provider.geo.Location;
import com.dabeeb.miner.data.provider.geo.LocationType;
import com.dabeeb.miner.data.provider.geo.MatchResult;
import com.dabeeb.miner.data.provider.geo.ValuedRadixTree;
import com.dabeeb.miner.fetch.Fetcher;
import com.dabeeb.miner.index.IndexFilterPlugin;
import com.dabeeb.miner.parse.html.HTMLBoilerpipeParser;

public class LocationTaggerFilter implements IndexFilterPlugin {

	public static Logger logger = LogManager.getFormatterLogger(LocationTaggerFilter.class);
	private Configuration conf;
	
	private static final String CONFIG_PREFIX = "locationTagger";
	
	private static final float TITLE_WEIGHT = (float) 1.0;
	private static final float BODY_WEIGHT = (float) 0.3;
	private static final float MIN_THREASHOLD = (float)0.6;
	private static final float MIN_DIFFERENCE = (float)0.3;
	
	ValuedRadixTree<Location> wordsTree;
	
	HashMap<Location, Float> locationScore;
	
	HashSet<HashSet<Location>> similarLocations;
	
	boolean debug = false;
	
	
	
	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;		
	}

	@Override
	public boolean filter(Document doc) {
		
		String locationMeta = doc.getMetadata().get("location");
		if(locationMeta != null) {
			if(locationMeta.contains("auto")) {
				locationMeta = null;
			} else {
				logger.info("LocationTagger: location is not null, location: " + doc.getMetadata().get("location"));
				return true;
			}
		}
		
		wordsTree = GeographicalDataProvider.getInstance().getWordsTree();
		
		String title = doc.getParsedContent().getTitle();
		String body = doc.getParsedContent().getText();
		
		List<MatchResult<Set<Location>>> titleRes = wordsTree.findAllWords(title);
		List<MatchResult<Set<Location>>> bodyRes = wordsTree.findAllWords(body);
		
		locationScore = new HashMap<Location, Float>();
		
		similarLocations = new HashSet<HashSet<Location>>();
		
		for(MatchResult<Set<Location>> res : titleRes) {
			
			if(debug) {
				logger.info("LocationTagger: titleRes: " + res.value + " " + title.substring(res.start, res.end));
			}
			
			if(res.value.size() > 1) {
				HashSet<Location> l = new HashSet<Location>();
				l.addAll(res.value);
				similarLocations.add(l);
			}
			for(Location loc : res.value) {
				if(locationScore.containsKey(res.value)) {
					locationScore.put(loc, locationScore.get(loc) + TITLE_WEIGHT*1);
				} else {
					locationScore.put(loc, TITLE_WEIGHT*1);
				}
				
				if(loc.getParent() != null) {
					
					if(locationScore.containsKey(loc.getParent())) {
						locationScore.put(loc.getParent(), locationScore.get(loc.getParent()) + TITLE_WEIGHT*1);
					}
					else {
						locationScore.put(loc.getParent(), TITLE_WEIGHT*1);
					}
					
					if(loc.getParent().getParent() != null) {
						if(locationScore.containsKey(loc.getParent().getParent())) {
							locationScore.put(loc.getParent().getParent(), locationScore.get(loc.getParent().getParent()) + TITLE_WEIGHT*1);
						} else {
							locationScore.put(loc.getParent().getParent(), TITLE_WEIGHT*1);
						}
					}
					
				}
			}
		}
		
		for(MatchResult<Set<Location>> res : bodyRes) {
			
			if(debug) {
				logger.info("LocationTagger: bodyRes: " + body.substring(res.start-10, res.end+10) + "----------" + res.value);
			}
			
			if(res.value.size() > 1) {
				HashSet<Location> l = new HashSet<Location>();
				l.addAll(res.value);
				similarLocations.add(l);
			}
			for(Location loc : res.value) {
				if(locationScore.containsKey(loc)) {
					locationScore.put(loc, locationScore.get(loc) + BODY_WEIGHT * 1);
				} else {
					locationScore.put(loc, BODY_WEIGHT * 1);
				}
				
				if(loc.getParent() != null) {
					
					if(locationScore.containsKey(loc.getParent())) {
						locationScore.put(loc.getParent(), locationScore.get(loc.getParent()) + BODY_WEIGHT * 1);
					}
					else {
						locationScore.put(loc.getParent(), BODY_WEIGHT * 1);
					}
					
					if(loc.getParent().getParent() != null) {
						if(locationScore.containsKey(loc.getParent().getParent())) {
							locationScore.put(loc.getParent().getParent(), locationScore.get(loc.getParent().getParent()) + BODY_WEIGHT * 1);
						} else {
							locationScore.put(loc.getParent().getParent(), BODY_WEIGHT * 1);
						}
					}
					
				}
			}
		}
		HashSet<Location> taggableLocations = new HashSet<Location>();
		
		if(debug) {
			logger.info("LocationTagger: " + "------------------locations score-----------------");
			logger.info("LocationTagger: " + title);
		}
		
		for(Location loc : locationScore.keySet()) {
			
			if(debug)
				logger.info("LocationTagger: " + loc.getLocationId() + " " + loc.getNameEn() + " " + loc.getAllAliases() + " " + loc.getCode() + ": " + locationScore.get(loc));
			
			if(loc.getType() == LocationType.COUNTRY) {
				if(locationScore.get(loc) > MIN_THREASHOLD) {
					taggableLocations.add(loc);
				}
			} else if(loc.getType() == LocationType.PROVINCE) {
				if(locationScore.get(loc) > MIN_THREASHOLD) {
					taggableLocations.add(loc);					
				}
			} else if(loc.getType() == LocationType.CITY) {
				if(locationScore.get(loc) > MIN_THREASHOLD) {
					taggableLocations.add(loc);					
				}
			}
		}
		
		if(similarLocations != null) {
			float maxScore = (float)0.0;
			Location maxLoc;
						
			for(HashSet<Location> locSet : similarLocations) {
				maxScore = (float)0.0;
				maxLoc = null;
				for(Location loc : locSet) {
					if(taggableLocations.contains(loc)) {
						if(locationScore.get(loc) > maxScore) {
							
							if(maxLoc != null)
								taggableLocations.remove(maxLoc);
							
							maxScore = locationScore.get(loc);
							maxLoc = loc;
						} else {
							taggableLocations.remove(loc);
						}
					}
				}
			}
		}
		
		//logger.info("LocationTagger: article: " + doc.getRawContent());
		StringBuilder locationTagStr = new StringBuilder(); 
		for(Location loc : taggableLocations) {
			//logger.info(loc.getCode());
			if(loc.getType() == LocationType.COUNTRY) {
				locationTagStr.append(loc.getCode());
				locationTagStr.append(',');
			} else {
				if(taggableLocations.contains(loc.getParent())) {
					locationTagStr.append(loc.getCode());
					locationTagStr.append(',');
				}
			}
		}
		if(locationTagStr.length() > 1)
			locationTagStr.append("auto");
		doc.getMetadata().put("location", locationTagStr.toString());
		
		return true;
	}
	
	public static void main(String args[]) throws ClientProtocolException, IOException, ConfigurationException, URISyntaxException {
		XMLConfiguration conf = Crawler.initialize();

		//String url = "http://www.alarabiya.net/ar/politics/2014/07/09/%D9%85%D8%A7-%D9%82%D9%84%D8%AA%D9%87-%D9%84%D9%84%D8%B1%D8%A6%D9%8A%D8%B3.html";
		//String url = "http://ara.reuters.com/article/businessNews/idARAKBN0FE2AL20140709";
		//String url = "http://ara.reuters.com/article/sportsNews/idARAKBN0FE27O20140709";
		//String url = "http://www.alriyadh.com/955359";
		String url = "http://www.shorouknews.com/news/view.aspx?cdate=07082014&id=06e21cb9-3b83-49d5-a0b2-8816609abc22";
		Fetcher fetcher = new Fetcher(conf);
		fetcher.setConf(conf);

		Document doc = new Document();
		doc.setUrl(new URL(url));

		fetcher.fetchUrl(doc);
		
		HTMLBoilerpipeParser parser = new HTMLBoilerpipeParser();
		parser.setConf(conf);
		parser.parse(doc);
		
		LocationTaggerFilter tagger = new LocationTaggerFilter();
		tagger.setConf(conf);
		tagger.filter(doc);
		
		System.out.println(doc.getParsedContent().getTitle());
		System.out.println(doc.getParsedContent().getText());
		System.out.println(doc.getMetadata().get("location"));
	}

}
