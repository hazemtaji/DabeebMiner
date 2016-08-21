package com.dabeeb.miner.index;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.crawl.Crawler;
import com.dabeeb.miner.crawl.DocumentHandlerThread.DocumentHandlerStatus;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.model.Inlink;
import com.dabeeb.miner.fetch.Fetcher;
import com.dabeeb.miner.parse.Parser;
import com.dabeeb.miner.plugin.PluginRepository;

public class Indexer {
	public static Logger logger = LogManager.getFormatterLogger(Indexer.class);
	private List<IndexFilterPlugin> filters;
	private List<IndexerPlugin> indexers;
	private DocumentHandlerStatus status = new DocumentHandlerStatus(0);
	
	public Indexer(Configuration conf) {
		filters = PluginRepository.getInstance().getIndexFilterPlugins();
		indexers = PluginRepository.getInstance().getIndexerPlugins();
	}
	
	public void index(Document doc, DocumentHandlerStatus status) {
		index(doc, false, status);
	}
	
	public void index(Document doc, boolean forceReindex, DocumentHandlerStatus status) {
		if(!doc.isIndex())
			return;
		
		if(status != null)
			this.status = status;
		
		doc.getParsedContent().setLanguage("ar");
		
		filterDoc(doc);
		
		status.setSubPhase("submit");
		
		if(doc.isIndex())
		{
			for(IndexerPlugin indexer : indexers) {
				indexer.index(doc, forceReindex);
			}
		}
		
		status.setSubPhase("");
	}
	
	public void flush() {
		for(IndexerPlugin indexer : indexers) {
			indexer.flush();
		}
	}
	
	private void filterDoc(Document doc) {
		for(IndexFilterPlugin filter : filters) {
			status.setSubPhase(filter.getClass().getName());
			if(!filter.filter(doc)) {
				doc.setIndex(false);
				break;
			}
		}
	}
	
	public static void main(String[] args) throws ClientProtocolException, IOException, URISyntaxException, ConfigurationException {
		//String url = "http://arabic.euronews.com/2014/04/14/ukraine-tensions-1404";
		//Inlink inlink = new Inlink("http://arabic.euronews.com/", "الإنفصاليون الموالون لروسيا في شرق أوكرانيا يطالبون موسكو بالتدخل");
		
		String url = "http://www.aljazeera.net/news/pages/1b970bec-7612-4980-b6e4-69ff03093bdc";
		Inlink inlink = new Inlink(new URL("http://aljazeera.net/"), "أنباء عن دخول قوات روسية مدينتين بشرق أوكرانيا");
		
		
		
		XMLConfiguration conf = Crawler.initialize();
		Fetcher fetcher = new Fetcher(conf);
		
		List<Inlink> inlinks = new ArrayList<>(1);
		inlinks.add(inlink);
		
		Document doc = new Document();
		doc.setUrl(new URL(url));
		doc.setInlinks(inlinks);
		
		fetcher.fetchUrl(doc);
		
		Parser parser = new Parser(conf);
		parser.parse(doc);
		
		Indexer indexer = new Indexer(conf);
		indexer.filterDoc(doc);

		System.out.println("-------------------- Title --------------------");
		System.out.println(doc.getParsedContent().getTitle());
		System.out.println("-------------------- Article --------------------");
		System.out.println(doc.getParsedContent().getText());
	}
}
