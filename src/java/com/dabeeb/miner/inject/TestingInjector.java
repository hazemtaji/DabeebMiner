package com.dabeeb.miner.inject;

import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.net.urlfilter.URLFilterException;
import com.dabeeb.miner.net.urlfilter.URLFilters;
import com.dabeeb.miner.net.urlnormalizer.URLNormalizers;

public class TestingInjector implements Injector {
	public static Logger logger = LogManager.getFormatterLogger(TestingInjector.class);
	private Configuration conf;

	public TestingInjector(Configuration conf) {
		this.conf = conf;
	}
	
	@Override
	public List<InjectableURL> getUrls() {
		List<InjectableURL> res = new LinkedList<>();

		String url = "http://www.alriyadh.com/2014/03/29/article922414.html";
		//String url = "http://www.aljazeera.net/aljazeerarss/9ff80bf7-97cf-47f2-8578-5a9df7842311/497f8f74-88e0-480d-b5d9-5bfae29c9a63";
		
		try {
			url = URLNormalizers.getInstance(conf).normalize(url, URLNormalizers.SCOPE_INJECT);
			url = URLFilters.getInstance(conf).filter(url);
		} catch (MalformedURLException e) {
			logger.warn("Injection for URL: '$s' failed at normalization", url);
			url = null;
		} catch (URLFilterException e) {
			logger.warn("Injection for URL: '$s' failed at filtering", url);
		}
		
		if(url != null) {
            Hashtable<String, String> metadata = new Hashtable<>();
    		metadata.put("categories", "politics");
    		metadata.put("tags", "author:hazem");
            res.add(new InjectableURL(url, metadata));
        }
		
		return res;
		
	}

	@Override
	public void reportInjected(List<InjectableURL> injectables) {
		//Whatever!
	}
	
	@Override
	public void reportFailure(String url, InjectionStatus status) {
		//Whatever!
		logger.error("Injected URL reported problem: '%s' / '%s'", status, url);	
	}

	@Override
	public void reportSuccess(String url) {
		//Whatever!
		
	}
}
