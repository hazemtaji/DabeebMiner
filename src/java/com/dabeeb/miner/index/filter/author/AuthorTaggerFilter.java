package com.dabeeb.miner.index.filter.author;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.crawl.Crawler;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.fetch.Fetcher;
import com.dabeeb.miner.index.IndexFilterPlugin;
import com.dabeeb.miner.parse.html.HTMLBoilerpipeParser;

public class AuthorTaggerFilter implements IndexFilterPlugin {
	public static Logger logger = LogManager.getFormatterLogger(AuthorTaggerFilter.class);
	private Configuration conf;
	
	private static final String CONFIG_PREFIX = "authorTagger";
	HashSet<String> namesDB;
	
	public AuthorTaggerFilter() {
		namesDB = new HashSet<String>();
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader("conf/namesDB.txt"));
			while(true) {
				String line = reader.readLine();
				if(line == null)
					break;
				
				namesDB.add(line);
			}
			reader.close();
		} catch (IOException e) {
			logger.error("Error initializing Author Tagger", e);
		}
	}
	
	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}
	
	@Override
	public boolean filter(Document doc) {
		
		if(doc.getMetadata().get("author") != null)
			return true;
		
		String title = doc.getParsedContent().getTitle();
		String[][] words = doc.getParsedContent().getWords();
		int maxPercentage = 0;
		
		String author = tryTitle(title);
		if(author != null) {
			doc.getMetadata().put("author", author.toString());
			return true;
		}
		
		for(int i = 0; i < words.length; i++) {
			if(words[i].length < 10) {
				int ignoredWords = 0;
				int consecutiveIgnoredWords = 0;
				int matchedWords = 0;
				int firstMatch = -1;
				int lastMatch = -1;
				for(int j = 0; j < words[i].length; j++) {
					if(words[i][j].length() < 3) {
						ignoredWords++;
						consecutiveIgnoredWords++;
					}
					if(namesDB.contains(words[i][j])) {
						matchedWords++;
						if(firstMatch == -1 || consecutiveIgnoredWords > 1) {
							if(consecutiveIgnoredWords > 1 && lastMatch != firstMatch) {
								break;
							}
							firstMatch = j;
						}
						
						lastMatch = j;
						consecutiveIgnoredWords = 0;
					} else {
						consecutiveIgnoredWords++;
					}
				}

				author = null;
				if(matchedWords > 0 && (lastMatch - firstMatch) > 1) {
					int percentage = (int)((double)(ignoredWords + matchedWords) / words[i].length * 100);
					if(percentage > maxPercentage) {
						author = constructString(words[i], firstMatch, lastMatch);
					}
				}
				if(author != null) {
					doc.getMetadata().put("author", author.toString());
				}
				
			}
		}
		
		return true;
	}
	
	private String tryTitle(String title) {
		String[] phrases = title.split("[/\\-\\\\]+");
		for(String phrase : phrases) {
			phrase = phrase.trim();
			boolean takeAll = false;
			if(phrase.startsWith("بقلم :")) {
				phrase = phrase.substring(6).trim();
				takeAll = true;
			}
			else if(phrase.startsWith("بقلم:") || phrase.startsWith("قلم :")) {
				phrase = phrase.substring(5).trim();
				takeAll = true;
			}
			else if(phrase.startsWith("بقلم") || phrase.startsWith("قلم:")) {
				phrase = phrase.substring(4).trim();
				takeAll = true;
			}
			
			if(takeAll) {
				return phrase;
			} else {
				if(isName(phrase)) {
					return phrase;
				}
			}
		}
		return null;
	}

	private boolean isName(String phrase) {
		String[] words = phrase.split("\\P{L}+");
		for(String word : words) {
			if(!namesDB.contains(word))
				return false;
		}
		return true;
	}

	public static String constructString(String[] words, int start, int end) {
		 StringBuilder res = new StringBuilder();
		for(int j = start; j <= end; j++) {
			res.append(words[j]);
			if(j != end)
				res.append(" ");
		}
		return res.toString();
	}
	
	public static void main(String args[]) throws ClientProtocolException, IOException, ConfigurationException, URISyntaxException {
		XMLConfiguration conf = Crawler.initialize();

		//String url = "http://www.alarabiya.net/ar/politics/2014/07/09/%D9%85%D8%A7-%D9%82%D9%84%D8%AA%D9%87-%D9%84%D9%84%D8%B1%D8%A6%D9%8A%D8%B3.html";
		//String url = "http://ara.reuters.com/article/businessNews/idARAKBN0FE2AL20140709";
		//String url = "http://ara.reuters.com/article/sportsNews/idARAKBN0FE27O20140709";
		String url = "http://atls.ps/ar/index.php?act=post&id=7256";
		Fetcher fetcher = new Fetcher(conf);
		fetcher.setConf(conf);

		Document doc = new Document();
		doc.setUrl(new URL(url));

		fetcher.fetchUrl(doc);
		
		HTMLBoilerpipeParser parser = new HTMLBoilerpipeParser();
		parser.setConf(conf);
		parser.parse(doc);
		
		AuthorTaggerFilter tagger = new AuthorTaggerFilter();
		tagger.setConf(conf);
		tagger.filter(doc);
		
		System.out.println(doc.getParsedContent().getText());
		System.out.println(doc.getMetadata().get("author"));
	}
}