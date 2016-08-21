package com.dabeeb.miner.net.urlnormalizer.regex;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import com.dabeeb.miner.crawl.Crawler;
import com.dabeeb.miner.net.urlnormalizer.URLNormalizerPlugin;
import com.dabeeb.miner.net.urlnormalizer.URLNormalizers;

/**
 * Allows users to do regex substitutions on all/any URLs that are encountered,
 * which is useful for stripping session IDs from URLs.
 * 
 * <p>
 * This class uses the <tt>urlnormalizer.regex.file</tt> property. It should be
 * set to the file name of an xml file which should contain the patterns and
 * substitutions to be done on encountered URLs.
 * </p>
 * <p>
 * This class also supports different rules depending on the scope. Please see
 * the javadoc in {@link com.dabeeb.miner.net.urlnormalizer.nutch.net.URLNormalizers} for more details.
 * </p>
 * 
 * @author Hazem Taji
 */
public class RegexURLNormalizer implements URLNormalizerPlugin {

	private static final Logger logger = LogManager.getFormatterLogger(RegexURLNormalizer.class);

	private static final String CONFIG_PREFIX = "urlnormalizer.regex";
	private static final String CONFIG_FILE = CONFIG_PREFIX + ".file";
	private static final String CONFIG_RULES = CONFIG_PREFIX + ".rules";
	
	private Configuration conf;

	/**
	 * Class which holds a compiled pattern and its corresponding substition string.
	 */
	private static class Rule {
		public Pattern pattern;

		public String substitution;
	}

	private ThreadLocal<HashMap<String, List<Rule>>> scopedRulesThreadLocal = new ThreadLocal<HashMap<String, List<Rule>>>() {
		protected java.util.HashMap<String, java.util.List<Rule>> initialValue() {
			return new HashMap<String, List<Rule>>();
		};
	};

	public HashMap<String, List<Rule>> getScopedRules() {
		return scopedRulesThreadLocal.get();
	}

	private List<Rule> defaultRules;

	private static final List<Rule> EMPTY_RULES = Collections.emptyList();

	/**
	 * The default constructor which is called from UrlNormalizerFactory
	 * (normalizerClass.newInstance()) in method: getNormalizer()*
	 */
	public RegexURLNormalizer() {
	}

	public void setConf(Configuration conf) {
		this.conf = conf;

		String filename = conf.getString(CONFIG_FILE);
		String stringRules = conf.getString(CONFIG_RULES);
		
		Reader reader = null;
		if (stringRules != null) {
			reader = new StringReader(stringRules);
		} else {
			try {
				reader = new FileReader(filename);
			} catch (FileNotFoundException e) {
				logger.error("Rule file not found");
			}
		}
		
		List<Rule> rules = null;
		if (reader == null) {
			logger.warn("Can't load the default rules! ");
			rules = EMPTY_RULES;
		} else {
			try {
				rules = readConfiguration(reader);
			} catch (Exception e) {
				logger.warn("Couldn't read default config:", e);
				rules = EMPTY_RULES;
			}
		}
		defaultRules = rules;
	}

	// used in JUnit test.
	void setConfiguration(Reader reader, String scope) {
		List<Rule> rules = readConfiguration(reader);
		getScopedRules().put(scope, rules);
		
		logger.debug("Set config for scope '%s': %s rules.", scope, rules.size());
	}

	/**
	 * This function does the replacements by iterating through all the regex
	 * patterns. It accepts a string url as input and returns the altered
	 * string.
	 */
	public String regexNormalize(String urlString, String scope) {
		HashMap<String, List<Rule>> scopedRules = getScopedRules();
		List<Rule> curRules = scopedRules.get(scope);
		if (curRules == null) {
			// try to populate
			String configFile = conf.getString(CONFIG_FILE + "." + scope);
			if (configFile != null) {
				logger.debug("resource for scope '" + scope + "': " + configFile);
				try {
					Reader reader = new FileReader(configFile);
					curRules = readConfiguration(reader);
					scopedRules.put(scope, curRules);
				} catch (Exception e) {
					logger.warn("Couldn't load resource '" + configFile + "': " + e);
				}
			}
			if (curRules == EMPTY_RULES || curRules == null) {
				logger.info("can't find rules for scope '%s', using default", scope);
				scopedRules.put(scope, EMPTY_RULES);
			}
		}
		if (curRules == EMPTY_RULES || curRules == null) {
			curRules = defaultRules;
		}
		Iterator<Rule> i = curRules.iterator();
		while (i.hasNext()) {
			Rule r = (Rule) i.next();

			Matcher matcher = r.pattern.matcher(urlString);

			urlString = matcher.replaceAll(r.substitution);
		}
		return urlString;
	}

	public String normalize(String urlString, String scope) throws MalformedURLException {
		return regexNormalize(urlString, scope);
	}

	/** Reads the configuration file and populates a List of Rules. */
	private List<Rule> readConfigurationFile(String filename) {
		if (logger.isInfoEnabled()) {
			logger.info("loading %s", filename);
		}
		try {
			FileReader reader = new FileReader(filename);
			return readConfiguration(reader);
		} catch (Exception e) {
			logger.error("Error loading rules from '" + filename + "': " + e);
			return EMPTY_RULES;
		}
	}

	private List<Rule> readConfiguration(Reader reader) {
		List<Rule> rules = new ArrayList<Rule>();
		try {

			// borrowed heavily from code in Configuration.java
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(reader));
			Element root = doc.getDocumentElement();
			if ((!"regex-normalize".equals(root.getTagName())) && (logger.isErrorEnabled())) {
				logger.error("bad conf file: top-level element not <regex-normalize>");
			}
			NodeList regexes = root.getChildNodes();
			for (int i = 0; i < regexes.getLength(); i++) {
				Node regexNode = regexes.item(i);
				if (!(regexNode instanceof Element))
					continue;
				Element regex = (Element) regexNode;
				if ((!"regex".equals(regex.getTagName())) && (logger.isWarnEnabled())) {
					logger.warn("bad conf file: element not <regex>");
				}
				NodeList fields = regex.getChildNodes();
				String patternValue = null;
				String subValue = null;
				for (int j = 0; j < fields.getLength(); j++) {
					Node fieldNode = fields.item(j);
					if (!(fieldNode instanceof Element))
						continue;
					Element field = (Element) fieldNode;
					if ("pattern".equals(field.getTagName()) && field.hasChildNodes())
						patternValue = ((Text) field.getFirstChild()).getData();
					if ("substitution".equals(field.getTagName()) && field.hasChildNodes())
						subValue = ((Text) field.getFirstChild()).getData();
					if (!field.hasChildNodes())
						subValue = "";
				}
				if (patternValue != null && subValue != null) {
					Rule rule = new Rule();
					rule.pattern = Pattern.compile(patternValue);
					rule.substitution = subValue;
					rules.add(rule);
				}
			}
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error("error parsing conf file: " + e);
			}
			return EMPTY_RULES;
		}
		if (rules.size() == 0)
			return EMPTY_RULES;
		return rules;
	}

	/** Spits out patterns and substitutions that are in the configuration file. 
	 * @throws ConfigurationException */
	public static void main(String args[]) throws PatternSyntaxException, IOException, ConfigurationException {
		RegexURLNormalizer normalizer = new RegexURLNormalizer();
		normalizer.setConf(Crawler.initialize());
		HashMap<String, List<Rule>> scopedRules = normalizer.getScopedRules();
		Iterator<Rule> i = normalizer.defaultRules.iterator();
		System.out.println("* Rules for 'DEFAULT' scope:");
		while (i.hasNext()) {
			Rule r = i.next();
			System.out.print("  " + r.pattern.pattern() + " -> ");
			System.out.println(r.substitution);
		}
		// load the scope
		if (args.length > 1) {
			normalizer.normalize("http://test.com", args[1]);
		}
		if (scopedRules.size() > 1) {
			Iterator<String> it = scopedRules.keySet().iterator();
			while (it.hasNext()) {
				String scope = it.next();
				if (URLNormalizers.SCOPE_DEFAULT.equals(scope))
					continue;
				System.out.println("* Rules for '" + scope + "' scope:");
				i = ((List<Rule>) scopedRules.get(scope)).iterator();
				while (i.hasNext()) {
					Rule r = (Rule) i.next();
					System.out.print("  " + r.pattern.pattern() + " -> ");
					System.out.println(r.substitution);
				}
			}
		}
		if (args.length > 0) {
			System.out.println("\n---------- Normalizer test -----------");
			String scope = URLNormalizers.SCOPE_DEFAULT;
			if (args.length > 1)
				scope = args[1];
			System.out.println("Scope: " + scope);
			System.out.println("Input url:  '" + args[0] + "'");
			System.out.println("Output url: '" + normalizer.normalize(args[0], scope) + "'");
		}
		System.exit(0);
	}

}
