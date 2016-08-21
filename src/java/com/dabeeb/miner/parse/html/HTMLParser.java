package com.dabeeb.miner.parse.html;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.configuration.Configuration;
import org.apache.html.dom.HTMLDocumentImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cyberneko.html.parsers.DOMFragmentParser;
import org.w3c.dom.DocumentFragment;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.model.Outlink;
import com.dabeeb.miner.data.model.Parse;
import com.dabeeb.miner.parse.HTMLMetaTags;
import com.dabeeb.miner.parse.ParserPlugin;

public class HTMLParser implements ParserPlugin {
	public static Logger logger = LogManager.getFormatterLogger(HTMLParser.class);
	public static final String[] MIME_TYPES =  { "text/html", "application/xhtml+xml" };
	private DOMContentUtils utils;
	
	public HTMLParser() {
	}

	@Override
	public void parse(Document doc) {
	    HTMLMetaTags metaTags = new HTMLMetaTags();
		InputSource input = new InputSource(new ByteArrayInputStream(doc.getRawContent()));
		
		Parse parse = new Parse();
		doc.setParsedContent(parse);
		
		try {
			DocumentFragment root = parseNeko(input);
			
			// get meta directives
			HTMLMetaProcessor.getMetaTags(metaTags, root, doc.getFinalUrl().toString());
			logger.trace("Meta tags for " + doc.getFinalUrl() + ": " + metaTags.toString());
			
			// check meta directives
			if (!metaTags.getNoIndex()) {
				// okay to index				
				logger.trace("Getting text...");
				StringBuilder sb = new StringBuilder();
				utils.getText(sb, root);          // extract text
				parse.setText(sb.toString());
				sb.setLength(0);
				
				logger.trace("Getting title...");
				utils.getTitle(sb, root);         // extract title
				parse.setTitle(sb.toString().trim());
			}

			if (!metaTags.getNoFollow()) {
				// okay to follow links
				ArrayList<Outlink> l = new ArrayList<Outlink>();   // extract outlinks
				URL baseUrl = utils.getBase(root); //try finding base form BASE tag in HEAD
				if(baseUrl == null) {
					baseUrl = doc.getFinalUrl();
				}
				logger.trace("Getting links...");
				utils.getOutlinks(baseUrl, l, root);
				parse.setOutlinks(l.toArray(new Outlink[l.size()]));
		        logger.trace("found %s outlinks in %s", parse.getOutlinks().length, doc.getFinalUrl());
		    }
			
			HTMLLanguageParser parser = new HTMLLanguageParser();
			parser.detectIdentifyLanguage(doc, root);

		    //XXX parse = htmlParseFilters.filter(url, page, parse, metaTags, root);

			//XXX implement no cache directive
		    /*if (metaTags.getNoCache()) {             // not okay to cache
		      page.putToMetadata(new Utf8(Nutch.CACHING_FORBIDDEN_KEY),
		          ByteBuffer.wrap(Bytes.toBytes(cachingPolicy)));
		    }*/
			
		} 
		catch (Exception x) { 
			logger.error("Failed with the following Exception: ", x);
		}
	}
	
	private DocumentFragment parseNeko(InputSource input) throws SAXException, IOException {
		DOMFragmentParser parser = new DOMFragmentParser();
		try {
			parser.setFeature("http://cyberneko.org/html/features/augmentations", true);
			parser.setProperty("http://cyberneko.org/html/properties/default-encoding", "UTF-8");
			parser.setFeature("http://cyberneko.org/html/features/scanner/ignore-specified-charset", true);
			parser.setFeature("http://cyberneko.org/html/features/balance-tags/ignore-outside-content", false);
			parser.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment", true);
			parser.setFeature("http://cyberneko.org/html/features/report-errors", logger.isTraceEnabled());
		} catch (SAXException e) {}
	    
		// convert Document to DocumentFragment
		HTMLDocumentImpl doc = new HTMLDocumentImpl();
		doc.setErrorChecking(false);
		DocumentFragment res = doc.createDocumentFragment();
		DocumentFragment frag = doc.createDocumentFragment();
		parser.parse(input, frag);
		res.appendChild(frag);
		
		try {
			while(true) {
				frag = doc.createDocumentFragment();
				parser.parse(input, frag);
				
				if (!frag.hasChildNodes())
					break;
				
				logger.info(" - new frag, " + frag.getChildNodes().getLength() + " nodes.");
				
				res.appendChild(frag);
			}
		} catch (Exception x) { 
			logger.error("Failed with the following Exception: ", x);
		}
		return res;
	}

	@Override
	public String[] getMimeTypes() {
		return MIME_TYPES;
	}

	@Override
	public void setConf(Configuration conf) {
		utils = new DOMContentUtils(conf);		
	}
}
