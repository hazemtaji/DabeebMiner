package com.dabeeb.miner.parse.html;

// JDK imports
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.language.LanguageIdentifier;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.dabeeb.miner.conf.Configurable;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.model.Parse;
import com.dabeeb.miner.fetch.Fetcher;
import com.dabeeb.miner.util.NodeWalker;

/**
 * Adds metadata identifying language of document if found We could also run
 * statistical analysis here but we'd miss all other formats
 */
public class HTMLLanguageParser implements Configurable {
	public static Logger logger = LogManager.getFormatterLogger(HTMLLanguageParser.class);

	private Configuration conf;
	private int detect = -1;
	private int identify = -1;
	private boolean onlyCertain;

	/* A static Map of ISO-639 language codes */
	private static Map<String, String> LANGUAGES_MAP = new HashMap<String, String>();
	static {
		try {
			Properties p = new Properties();
			p.load(HTMLLanguageParser.class.getResourceAsStream("langmappings.properties"));
			Enumeration<?> keys = p.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				String[] values = p.getProperty(key).split(",", -1);
				LANGUAGES_MAP.put(key, key);
				for (int i = 0; i < values.length; i++) {
					LANGUAGES_MAP.put(values[i].trim().toLowerCase(), key);
				}
			}
		} catch (Exception e) {
			if (logger.isErrorEnabled()) {
				logger.error(e.toString());
			}
		}
	}
	
	public void setConf(Configuration conf) {
		this.conf = conf;
		onlyCertain = conf.getBoolean("lang.identification.only.certain", false);
		String[] policy = conf.getStringArray("lang.extraction.policy");
		for (int i = 0; i < policy.length; i++) {
			if (policy[i].equals("detect")) {
				detect = i;
			} else if (policy[i].equals("identify")) {
				identify = i;
			}
		}
	}

	/**
	 * Scan the HTML document looking at possible indications of content language<br>
	 * <li>1. html lang attribute (http://www.w3.org/TR/REC-html40/struct/dirlang.html#h-8.1)
	 * <li>2. meta dc.language (http://dublincore.org/documents/2000/07/16/usageguide/qualified-html.shtml#language)
	 * <li>3. meta http-equiv (content-language) (http://www.w3.org/TR/REC-html40/struct/global.html#h-7.4.4.2) <br>
	 */
	public void detectIdentifyLanguage(Document doc, DocumentFragment frag) {
		String lang = null;
		
		Header[] headers = doc.getData(Fetcher.HTTP_HEADERS);
		if(headers == null)
			headers = new Header[0];
		
		Parse parse = doc.getParsedContent();

		if (detect >= 0 && identify < 0) {
			lang = detectLanguage(headers, frag, doc.getMetadata());
		} else if (detect < 0 && identify >= 0) {
			lang = identifyLanguage(parse);
		} else if (detect < identify) {
			lang = detectLanguage(headers, frag, doc.getMetadata());
			if (lang == null) {
				lang = identifyLanguage(parse);
			}
		} else if (identify < detect) {
			lang = identifyLanguage(parse);
			if (lang == null) {
				lang = detectLanguage(headers, frag, doc.getMetadata());
			}
		} else {
			logger.warn("No configuration for language extraction policy is provided");
			return;
		}

		parse.setLanguage(lang);
	}

	/** Try to find the document's language from page headers and metadata */
	private String detectLanguage(Header[] headers, DocumentFragment frag, Map<String, String> metadata) {
		String lang = getLanguageFromMetadata(metadata);
		if (lang == null) {
			LanguageParser parser = new LanguageParser(frag);
			lang = parser.getLanguage();
		}

		if (lang != null) {
			return lang;
		}

		for(Header header : headers) {
			if(header.getName().equals(HttpHeaders.CONTENT_LANGUAGE)){
				lang = header.getValue();
			}
		}

		return lang;
	}

	/** Use statistical language identification to extract page language */
	private String identifyLanguage(Parse parse) {
		StringBuilder text = new StringBuilder();
		if (parse != null) {
			String title = parse.getTitle();
			if (title != null) {
				text.append(title.toString());
			}

			String content = parse.getText();
			if (content != null) {
				text.append(" ").append(content.toString());
			}

			String finalText = text.toString().replace("^", "").replaceAll("<img.*?/>", "");
			LanguageIdentifier identifier = new LanguageIdentifier(finalText);

			if (onlyCertain) {
				if (identifier.isReasonablyCertain()) {
					return identifier.getLanguage();
				}
			} else {
				return identifier.getLanguage();
			}
		}
		return null;
	}

	// Check in the metadata whether the language has already been stored there by Tika
	private static String getLanguageFromMetadata(Map<String, String> metadata) {
		if (metadata == null)
			return null;

		// dublin core
		String lang = metadata.get("dc.language");
		if (lang != null)
			return lang;
		// meta content-language
		lang = metadata.get("content-language");
		if (lang != null)
			return lang;
		// lang attribute
		return metadata.get("lang");
	}

	static class LanguageParser {

		private String dublinCore = null;
		private String htmlAttribute = null;
		private String httpEquiv = null;
		private String language = null;

		LanguageParser(Node node) {
			parse(node);
			if (htmlAttribute != null) {
				language = htmlAttribute;
			} else if (dublinCore != null) {
				language = dublinCore;
			} else {
				language = httpEquiv;
			}
		}

		String getLanguage() {
			return language;
		}

		void parse(Node node) {

			NodeWalker walker = new NodeWalker(node);
			while (walker.hasNext()) {

				Node currentNode = walker.nextNode();
				String nodeName = currentNode.getNodeName();
				short nodeType = currentNode.getNodeType();

				if (nodeType == Node.ELEMENT_NODE) {

					// Check for the lang HTML attribute
					if (htmlAttribute == null) {
						htmlAttribute = parseLanguage(((Element) currentNode).getAttribute("lang"));
					}

					// Check for Meta
					if ("meta".equalsIgnoreCase(nodeName)) {
						NamedNodeMap attrs = currentNode.getAttributes();

						// Check for the dc.language Meta
						if (dublinCore == null) {
							for (int i = 0; i < attrs.getLength(); i++) {
								Node attrnode = attrs.item(i);
								if ("name".equalsIgnoreCase(attrnode.getNodeName())) {
									if ("dc.language".equalsIgnoreCase(attrnode.getNodeValue())) {
										Node valueattr = attrs.getNamedItem("content");
										if (valueattr != null) {
											dublinCore = parseLanguage(valueattr.getNodeValue());
										}
									}
								}
							}
						}

						// Check for the http-equiv content-language
						if (httpEquiv == null) {
							for (int i = 0; i < attrs.getLength(); i++) {
								Node attrnode = attrs.item(i);
								if ("http-equiv".equalsIgnoreCase(attrnode.getNodeName())) {
									if ("content-language".equals(attrnode.getNodeValue().toLowerCase())) {
										Node valueattr = attrs.getNamedItem("content");
										if (valueattr != null) {
											httpEquiv = parseLanguage(valueattr.getNodeValue());
										}
									}
								}
							}
						}
					}
				}

				if ((dublinCore != null) && (htmlAttribute != null) && (httpEquiv != null)) {
					return;
				}
			}
		}

		/**
		 * Parse a language string and return an ISO 639 primary code, or
		 * <code>null</code> if something wrong occurs, or if no language is
		 * found.
		 */
		final static String parseLanguage(String lang) {

			if (lang == null) {
				return null;
			}

			String code = null;
			String language = null;

			// First, split multi-valued values
			String langs[] = lang.split(",| |;|\\.|\\(|\\)|=", -1);

			int i = 0;
			while ((language == null) && (i < langs.length)) {
				// Then, get the primary code
				code = langs[i].split("-")[0];
				code = code.split("_")[0];
				// Find the ISO 639 code
				language = (String) LANGUAGES_MAP.get(code.toLowerCase());
				i++;
			}

			return language;
		}

	}
}
