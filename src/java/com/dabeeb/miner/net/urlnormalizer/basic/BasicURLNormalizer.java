package com.dabeeb.miner.net.urlnormalizer.basic;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.net.urlnormalizer.URLNormalizerPlugin;

/** Converts URLs to a normal form . */
public class BasicURLNormalizer implements URLNormalizerPlugin {
    public static final Logger logger = LogManager.getFormatterLogger(BasicURLNormalizer.class);

    private final Rule relativePathRule;
    private final Rule leadingRelativePathRule;
    private final Rule currentPathRule;
    private final Rule adjacentSlashRule;

    private Configuration conf;
    
    public static void main(String[] args) throws MalformedURLException {
		BasicURLNormalizer norm = new BasicURLNormalizer();
		
		String url = "http://www.ALjazeera.com:80/test/.././///test2";
		String scope = "http://www.aljazeera.com/";
		
		String result = norm.normalize(url, scope);
		System.out.println(result);
	}

    public BasicURLNormalizer() {
      //try {
        // this pattern tries to find spots like "/xx/../" in the url, which
        // could be replaced by "/" xx consists of chars, different then "/"
        // (slash) and needs to have at least one char different from "."
        relativePathRule = new Rule();
        relativePathRule.pattern = Pattern.compile("(/[^/]*[^/.]{1}[^/]*/\\.\\./)");
        relativePathRule.substitution = "/";

        // this pattern tries to find spots like leading "/../" in the url,
        // which could be replaced by "/"
        leadingRelativePathRule = new Rule();
        leadingRelativePathRule.pattern = Pattern.compile("^(/\\.\\./)+");
        leadingRelativePathRule.substitution = "/";

        // this pattern tries to find spots like "/./" in the url,
        // which could be replaced by "/"
        currentPathRule = new Rule();
        currentPathRule.pattern = Pattern.compile("(/\\./)");
        currentPathRule.substitution = "/";

        // this pattern tries to find spots like "xx//yy" in the url,
        // which could be replaced by a "/"
        adjacentSlashRule = new Rule();
        adjacentSlashRule.pattern = Pattern.compile("/{2,}");     
        adjacentSlashRule.substitution = "/";
        
      //} //catch (MalformedPatternException e) {
        //throw new RuntimeException(e);
      //}
    }

    public String normalize(String urlString, String scope) throws MalformedURLException {
        if ("".equals(urlString))                     // permit empty
            return urlString;

        urlString = urlString.trim();                 // remove extra spaces

        URL url = new URL(urlString);

        String protocol = url.getProtocol();
        String host = url.getHost();
        int port = url.getPort();
        String file = url.getFile();

        boolean changed = false;

        if (!urlString.startsWith(protocol))        // protocol was lowercased
            changed = true;

        if ("http".equals(protocol) || "ftp".equals(protocol)) {

            if (host != null) {
                String newHost = host.toLowerCase();    // lowercase host
                if (!host.equals(newHost)) {
                    host = newHost;
                    changed = true;
                }
            }

            if (port == url.getDefaultPort()) {       // uses default port
                port = -1;                              // so don't specify it
                changed = true;
            }

            if (file == null || "".equals(file)) {    // add a slash
                file = "/";
                changed = true;
            }

            if (url.getRef() != null) {                 // remove the ref
                changed = true;
            }

            // check for unnecessary use of "/../"
            String file2 = substituteUnnecessaryRelativePaths(file);

            if (!file.equals(file2)) {
                changed = true;
                file = file2;
            }

        }

        if (changed)
            urlString = new URL(protocol, host, port, file).toString();

        return urlString;
    }

    private String substituteUnnecessaryRelativePaths(String file) {
        String fileWorkCopy = file;
        int oldLen = file.length();
        int newLen = oldLen - 1;

        // All substitutions will be done step by step, to ensure that certain
        // constellations will be normalized, too
        //
        // For example: "/aa/bb/../../cc/../foo.html will be normalized in the
        // following manner:
        //   "/aa/bb/../../cc/../foo.html"
        //   "/aa/../cc/../foo.html"
        //   "/cc/../foo.html"
        //   "/foo.html"
        //
        // The normalization also takes care of leading "/../", which will be
        // replaced by "/", because this is a rather a sign of bad webserver
        // configuration than of a wanted link.  For example, urls like
        // "http://www.foo.com/../" should return a http 404 error instead of
        // redirecting to "http://www.foo.com".
        //

        while (oldLen != newLen) {
            // substitue first occurence of "/xx/../" by "/"
            oldLen = fileWorkCopy.length();
            fileWorkCopy = substitute(relativePathRule, fileWorkCopy);

            // remove leading "/../"
            fileWorkCopy = substitute(leadingRelativePathRule, fileWorkCopy);

            // remove unnecessary "/./"
            fileWorkCopy = substitute(currentPathRule, fileWorkCopy);
            
            // collapse adjacent slashes with "/"
            fileWorkCopy = substitute(adjacentSlashRule, fileWorkCopy);
            
            newLen = fileWorkCopy.length();
        }

        return fileWorkCopy;
    }


    private String substitute(Rule rule, String text) {
    	Matcher matcher = rule.pattern.matcher(text);
    	String res = matcher.replaceFirst(rule.substitution);
		return res;
	}


	/**
     * Class which holds a compiled pattern and its corresponding substition
     * string.
     */
    private static class Rule {
        public Pattern pattern;
        public String substitution;
    }


	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}
}

