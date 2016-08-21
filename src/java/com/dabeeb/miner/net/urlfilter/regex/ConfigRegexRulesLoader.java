package com.dabeeb.miner.net.urlfilter.regex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigRegexRulesLoader implements RegexRulesLoader {
	private final static Logger logger = LogManager.getFormatterLogger(ConfigRegexRulesLoader.class);
	
	private static final String CONFIG_PREFIX = "urlfilter.regex";
	private static final String CONFIG_FILE = CONFIG_PREFIX + ".file";
	private static final String CONFIG_RULES = CONFIG_PREFIX + ".rules";

	private List<RegexRule> rules;

	public ConfigRegexRulesLoader() {
	}

	/**
	 * Constructs a new ConfigRegexRulesLoader and init it with a file of rules.
	 * 
	 * @param filename
	 *            is the rules file.
	 */
	public ConfigRegexRulesLoader(File file) throws IOException, PatternSyntaxException {
		this(new FileReader(file));
	}

	/**
	 * Constructs a new ConfigRegexRulesLoader and init it with a Reader of rules.
	 * 
	 * @param reader
	 *            is a reader of rules.
	 */
	private ConfigRegexRulesLoader(Reader reader) throws IOException, IllegalArgumentException {
		rules = readRules(reader);
	}
	
	/**
	 * Constructs a new ConfigRegexRulesLoader and inits it with a list of rules.
	 * 
	 * @param rules
	 *            string with a list of rules, one rule per line
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public ConfigRegexRulesLoader(String rules) throws IOException, IllegalArgumentException {
		this(new StringReader(rules));
	}

	@Override
	public void setConf(Configuration conf) {
		Reader reader = null;
		try {
			reader = getRulesReader(conf);
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error(e.getMessage());
			}
			throw new RuntimeException(e.getMessage(), e);
		}
		try {
			rules = readRules(reader);
		} catch (IOException e) {
			if (logger.isErrorEnabled()) {
				logger.error(e.getMessage());
			}
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public List<RegexRule> getRules() {
		return rules;
	}

	/**
	 * Read the specified file of rules.
	 * 
	 * @param reader
	 *            is a reader of regular expressions rules.
	 * @return the corresponding {@RegexRule rules}.
	 */
	private List<RegexRule> readRules(Reader reader) throws IOException, IllegalArgumentException {

		BufferedReader in = new BufferedReader(reader);
		List<RegexRule> rules = new ArrayList<RegexRule>();
		String line;

		while ((line = in.readLine()) != null) {
			if (line.length() == 0) {
				continue;
			}
			char first = line.charAt(0);
			boolean sign = false;
			switch (first) {
			case '+':
				sign = true;
				break;
			case '-':
				sign = false;
				break;
			case ' ':
			case '\n':
			case '#': // skip blank & comment lines
				continue;
			default:
				throw new IOException("Invalid first character: " + line);
			}

			String regex = line.substring(1);
			if (logger.isTraceEnabled()) {
				logger.trace("Adding rule [" + regex + "]");
			}
			RegexRule rule = new RegexRule(sign, regex);
			rules.add(rule);
		}
		return rules;
	}

	/**
	 * Rules specified as a config property will override rules specified as a
	 * config file.
	 */
	private Reader getRulesReader(Configuration conf) throws IOException {
		String stringRules = conf.getString(CONFIG_RULES);
		if (stringRules != null) {
			return new StringReader(stringRules);
		}
		String fileRules = conf.getString(CONFIG_FILE);
		return new FileReader(fileRules);
	}

	@Override
	public void updateCache() {
		// TODO Auto-generated method stub
		
	}

}
