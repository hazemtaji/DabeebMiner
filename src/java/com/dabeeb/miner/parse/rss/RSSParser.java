package com.dabeeb.miner.parse.rss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import com.sun.syndication.feed.module.mediarss.MediaEntryModule;
import com.sun.syndication.feed.module.mediarss.MediaModule;
import com.sun.syndication.feed.module.mediarss.types.MediaContent;
import com.sun.syndication.feed.module.mediarss.types.UrlReference;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.crawl.Crawler;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.model.Outlink;
import com.dabeeb.miner.data.model.Parse;
import com.dabeeb.miner.fetch.Fetcher;
import com.dabeeb.miner.parse.ParserPlugin;

public class RSSParser implements ParserPlugin {
	public static Logger logger = LogManager.getFormatterLogger(RSSParser.class);
	public static final String[] MIME_TYPES =  { "application/rss+xml", "text/xml", "application/xml" };

	@Override
	public void setConf(Configuration conf) {
	}

	@SuppressWarnings("unchecked")
	@Override
	public void parse(Document doc) {
		doc.setIndex(false);
		
		ByteArrayInputStream inputStream = new ByteArrayInputStream(doc.getRawContent());
		
		try {
	        SyndFeedInput input = new SyndFeedInput();
	        SyndFeed feed = input.build(new XmlReader(inputStream, false, "utf-8"));
	        List<SyndEntry> entries = feed.getEntries();

        	//Test encoding
	        if(entries.size() > 0) {
		        for(char c : entries.get(0).getTitle().toCharArray()) {
		        	int charType = Character.getType(c);
		        	if(charType == Character.UNASSIGNED) {
		        		//Bad encoding (retry)
		        		inputStream.reset();
		        		feed = input.build(new XmlReader(inputStream, false, "cp1256"));
		    	        entries = feed.getEntries();
		        	}
		        }
	        }
	        
	        Parse content = new Parse();
	        Outlink[] outlinks = new Outlink[entries.size()];
	        
	        int i = 0;
	        for(SyndEntry entry : entries) {
	        	
	        	Outlink outlink = new Outlink(entry.getLink(), entry.getTitle());
	        	outlink.setPublishDate(entry.getPublishedDate());
	        	
	        	boolean foundMedia = false;
	        	
	        	MediaEntryModule mediaModule = (MediaEntryModule)entry.getModule(MediaModule.URI);
	        	if(mediaModule != null) {
	        	
		        	MediaContent[] mcs = mediaModule.getMediaContents();
		        	
		        	MediaContent largest = null;
		        	long largestArea = 0;
		        	
		        	for(MediaContent mc : mcs) {
		        		int width = (mc.getWidth() != null) ? mc.getWidth() : 1;
		        		int height = (mc.getHeight() != null) ? mc.getHeight() : 1;
		        		long area = width * height;
		        		
		        		
		        		if(mc.getReference() instanceof UrlReference)
		        		{
		        			if(area > largestArea) {
		    	        		UrlReference ref = (UrlReference)mc.getReference();
		    	        		String imageUrl = ref.toString().toLowerCase();
		        				if(imageUrl.endsWith(".jpg") || imageUrl.endsWith(".png"))
		        					largest = mc;
		        			}
		        		}
		        	}
		        	
		        	if(largest != null) {
		        		UrlReference ref = (UrlReference)largest.getReference();
		        		outlink.getTags().put("image", ref.toString());
		        		foundMedia = true;
		        	}
	        	}
	        	
	        	if(!foundMedia) {
	        		for(Object obj : entry.getEnclosures()) {
	        			SyndEnclosure enclosure = (SyndEnclosure)obj;
	        			String mimeType = enclosure.getType().toLowerCase();
	        			if(mimeType.equals("image/jpeg") || mimeType.equals("image/png") || mimeType.equals("image/jpg")) {
			        		outlink.getTags().put("image", enclosure.getUrl());
			        		foundMedia = true;
			        		break;
	        			}
	        		}
	        	}
	        	
	        	SyndContent description = entry.getDescription();
	        	if(description != null && (description.getType() == null || description.getType().equals("text/plain") || description.getType().equals("text/html"))) {
	        		if(description.getValue() != null && description.getValue().length() > 0) {
	        			outlink.getTags().put("description", description.getValue());
	        		}
	        	}
	        	
	        	outlinks[i] = outlink;
	        	i++;
	        }
	        
        	content.setOutlinks(outlinks);
	        doc.setParsedContent(content);
	        
		} catch (Exception e) {
			logger.error("Error parsing RSS Feed: " + doc.getDbUrl(), e);
		}
	}

	@Override
	public String[] getMimeTypes() {
		return MIME_TYPES;
	}

	public static boolean isRSS(String mimeType) {
		for(String type : MIME_TYPES) {
			if(type.equalsIgnoreCase(mimeType))
				return true;
		}
		return false;
	}
	
	public static void main(String[] args) throws ConfigurationException, ClientProtocolException, IOException, URISyntaxException {
		XMLConfiguration conf = Crawler.initialize();

		//String url = "http://dabeeb.com:8080/html2rss/html2rss?url=http%3A%2F%2Fwww.al-watan.com%2FWriterProfile.aspx%3Fn%3DF921837C-077C-4638-B3E8-39399D18AE85%26writer%3D0&encoding=utf-8&linkPattern=news.aspx.n.*&titlePattern=&htmlStartPattern=&htmlEndPattern=";
		//String url = "http://www.edueast.gov.sa/portal/modules/staticPages/pages/rss/news-rss.php";
		String url = "http://dabeeb.com:8080/html2rss/html2rss?url=http%3A%2F%2Fwww.moi.gov.kw%2Fportal%2Fvarabic%2F&encoding=UTF-8&linkPattern=ShowPage.aspx%5C%3FnewsID%3D.*&titlePattern=&htmlStartPattern=&htmlEndPattern=";
		Fetcher fetcher = new Fetcher(conf);
		fetcher.setConf(conf);

		Document doc = new Document();
		doc.setUrl(new URL(url));

		fetcher.fetchUrl(doc);
		
		RSSParser parser = new RSSParser();
		parser.parse(doc);
		
		System.out.println("--------------------------------------------");
		System.out.println("Found the following outlinks:");
		System.out.println("--------------------------------------------");
		for(Outlink outlink : doc.getParsedContent().getOutlinks()) {
			System.out.println(outlink.getAnchor() + "\t" + outlink.getToUrl());
		}
	}

}