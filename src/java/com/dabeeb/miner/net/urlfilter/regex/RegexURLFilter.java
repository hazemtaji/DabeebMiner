package com.dabeeb.miner.net.urlfilter.regex;

// JDK imports
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.crawl.Crawler;
import com.dabeeb.miner.net.urlfilter.URLFilterPlugin;

/**
 * Generic {@link org.apache.nutch.net.URLFilter URL filter} based on regular
 * expressions.
 * 
 * <p>
 * The regular expressions rules are expressed in a file. The file of rules is
 * provided by each implementation using the
 * {@link #getRulesFile(Configuration)} method.
 * </p>
 * 
 * <p>
 * The format of this file is made of many rules (one per line):<br/>
 * <code>
 * [+-]&lt;regex&gt;
 * </code><br/>
 * where plus (<code>+</code>)means go ahead and index it and minus (
 * <code>-</code>)means no.
 * </p>
 * 
 * @author J&eacute;r&ocirc;me Charron
 */
public class RegexURLFilter implements URLFilterPlugin {

	private final static Logger logger = LogManager.getFormatterLogger(RegexURLFilter.class);

	private RegexRulesLoader loader;

	/** The current configuration */
	private Configuration conf;

	public RegexURLFilter(RegexRulesLoader loader) {
		this.loader = loader;
	}

	// Inherited Javadoc
	public String filter(String url) {
		for (RegexRule rule : loader.getRules()) {
			if (rule.match(url)) {
				return rule.accept() ? url : null;
			}
		}
		return null;
	}

	public void setConf(Configuration conf) {
		this.conf = conf;
	}
	
	public static void main(String args[]) throws IOException, ConfigurationException {
		Configuration conf = Crawler.initialize();
		
		RegexURLFilterFactory factory = new RegexURLFilterFactory();
		factory.setConf(conf);
		
		RegexURLFilter filter = factory.createInstance();
		filter.setConf(conf);

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line;
		while ((line = in.readLine()) != null) {
			String out = filter.filter(line);
			if (out != null) {
				System.out.print("+");
				System.out.println(out);
			} else {
				System.out.print("-");
				System.out.println(line);
			}
		}
	}
}
