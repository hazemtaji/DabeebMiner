package com.dabeeb.miner.net.urlfilter.regex;

import java.util.regex.Pattern;

/**
 * A generic regular expression rule.
 */
public class RegexRule {

	private final boolean sign;
	private Pattern pattern;

	/**
	 * Constructs a new regular expression rule.
	 * 
	 * @param sign
	 *            specifies if this rule must filter-in or filter-out. A
	 *            <code>true</code> value means that any url matching this rule
	 *            must be accepted, a <code>false</code> value means that any
	 *            url matching this rule must be rejected.
	 * @param regex
	 *            is the regular expression used for matching (see
	 *            {@link #match(String)} method).
	 */
	public RegexRule(boolean sign, String regex) {
		this.sign = sign;
		pattern = Pattern.compile(regex);
	}

	/**
	 * Return if this rule is used for filtering-in or out.
	 * 
	 * @return <code>true</code> if any url matching this rule must be accepted,
	 *         otherwise <code>false</code>.
	 */
	protected boolean accept() {
		return sign;
	}

	/**
	 * Checks if a url matches this rule.
	 * 
	 * @param url
	 *            is the url to check.
	 * @return <code>true</code> if the specified url matches this rule,
	 *         otherwise <code>false</code>.
	 */
	protected boolean match(String url) {
		return pattern.matcher(url).find();
	}

}
