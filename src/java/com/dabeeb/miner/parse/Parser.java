package com.dabeeb.miner.parse;

import java.net.URL;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mozilla.universalchardet.UniversalDetector;

import com.dabeeb.miner.conf.Configurable;
import com.dabeeb.miner.crawl.Crawler;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.model.Outlink;
import com.dabeeb.miner.fetch.Fetcher;
import com.dabeeb.miner.plugin.PluginRepository;

public class Parser implements Configurable{

	public static Logger logger = LogManager.getFormatterLogger(Parser.class);

	private static final String CONFIG_PREFIX = "parser";
	private static final String CONFIG_ENCODED_TYPES = CONFIG_PREFIX + ".encodedTypes.type";
	
	private String[] encodedTypes;
	private Hashtable<String, ParserPlugin> parsers;
	
	private Configuration conf;
	
	public Parser(Configuration conf) {
		setConf(conf);
		
		parsers = new Hashtable<>();
		List<ParserPlugin> plugins = PluginRepository.getInstance().getParserPlugins();
		for(ParserPlugin plugin : plugins) {
			for(String mimeType : plugin.getMimeTypes()) {
				parsers.put(mimeType, plugin);
			}
		}
		
	}
	
	public void setConf(Configuration conf) {
		this.conf = conf;
		
		encodedTypes = conf.getStringArray(CONFIG_ENCODED_TYPES);
		Arrays.sort(encodedTypes);
	}
	
	public static void main(String args[]) throws Exception{
		XMLConfiguration conf = Crawler.initialize();
		
		String url = "http://korabia.com/140141";
		Fetcher fetcher = new Fetcher(conf);
		
		Document doc = new Document();
		doc.setUrl(new URL(url));
		
		fetcher.fetchUrl(doc);
		
		Parser parser = new Parser(conf);
		parser.parse(doc);
		
		//System.out.println(doc.getParsedContent().getText());
		for(Outlink link : doc.getParsedContent().getOutlinks()) {
			System.out.println(link.getToUrl());
		}
	}
	
	public void parse(Document doc) {
		
		byte[] rawContent = doc.getRawContent();
		
		if(doc.getMimeType() == null) {
			logger.error("Cannot identify document MIME type %s", doc.getDbUrl());
			return;
		}
		
		//Identify Encoding
		if(isEncodedType(doc.getMimeType())) {
		    UniversalDetector detector = new UniversalDetector(null);
		    detector.handleData(rawContent, 0, rawContent.length);
		    detector.dataEnd();
		    String charset = detector.getDetectedCharset();
		    doc.setEncoding(charset);
		}
		
		ParserPlugin parser = parsers.get(doc.getMimeType());
		if(parser != null)
			parser.parse(doc);
	}

	private boolean isEncodedType(String mimeType) {
		if(mimeType != null) {
			int index = Arrays.binarySearch(encodedTypes, mimeType);
			return index > 0;
		}
		
		return false;
	}

}
