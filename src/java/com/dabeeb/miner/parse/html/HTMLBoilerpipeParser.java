package com.dabeeb.miner.parse.html;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.model.Outlink;
import com.dabeeb.miner.data.model.Parse;
import com.dabeeb.miner.parse.ParserPlugin;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.Anchor;
import de.l3s.boilerpipe.document.Image;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLDocument;

public class HTMLBoilerpipeParser implements ParserPlugin {
	public static Logger logger = LogManager.getFormatterLogger(HTMLBoilerpipeParser.class);
	public static final String[] MIME_TYPES = { "text/html", "application/xhtml+xml" };

	public HTMLBoilerpipeParser() {
	}

	@Override
	public void parse(Document doc) {
		try {
			if (doc.getEncoding() == null)
				doc.setEncoding("UTF-8");
			Charset charset = Charset.forName("UTF-8");
			try {
				byte[] raw = doc.getRawContent();
				
				int start = -1;
				for(int i = 0; i < raw.length - 20 && i < 300; i++) {
					if(raw[i] == 'c' && raw[i + 1] == 'h' && raw[i + 2] == 'a' && raw[i + 3] == 'r' && raw[i + 4] == 's' && raw[i + 5] == 'e' && raw[i + 6] == 't' && raw[i + 7] == '=') {
						start = i + 8;
						break;
					}
				}
				
				if(start > -1) {
					String encoding = "";
					for(int i = start; i < raw.length; i++) {
						char c = (char) raw[i];
						if(Character.isAlphabetic(c) || c == '-' || Character.isDigit(c)) {
							encoding += c;
						} else {
							break;
						}
					}
					doc.setEncoding(encoding);
					charset = Charset.forName(doc.getEncoding());
				}
				
				//Charset.forName(doc.getEncoding());
			} catch (IllegalCharsetNameException | UnsupportedCharsetException e){
				logger.error(e);
			}
			
			
			HTMLDocument htmlDoc = new HTMLDocument(doc.getRawContent(), charset);

			final TextDocument textDoc = new BoilerpipeSAXInput(htmlDoc.toInputSource()).getTextDocument();
			ArticleExtractor.INSTANCE.process(textDoc);

			// final InputSource is = htmlDoc.toInputSource();
			// HTMLHighlighter.newExtractingInstance().process(textDoc, is);

			List<Anchor> anchors = textDoc.getAnchors();
			List<Outlink> outlinks = new ArrayList<>(anchors.size());

			URL baseUrl = doc.getFinalUrl();

			for (int i = 0; i < anchors.size(); i++) {
				Anchor anchor = anchors.get(i);
				if(anchor == null)
					continue;
				try {
					URL url = new URL(baseUrl, anchor.getUrl());
					Outlink outlink = new Outlink(url.toString(), anchor.getText().toString());
					outlinks.add(outlink);
				} catch (MalformedURLException e) {
				}
			}

			Parse parsedContent = new Parse();
			String title = textDoc.getSuperTitle();
			String content = textDoc.getContent();

			if(title == null)
				title = textDoc.getExtractedTitle();
			if(title == null)
				title = textDoc.getTitle();
			
			if(title == null || content == null)
				return;
			
			parsedContent.setTitle(title);
			
			if(content.startsWith(title)) {
				content = content.substring(title.length() + 1);
			}
			
			parsedContent.setText(content);
			parsedContent.setOutlinks(outlinks.toArray(new Outlink[outlinks.size()]));
			doc.setParsedContent(parsedContent);
			
			if(textDoc.getSuperImage() != null) {
				try {
					URL url = new URL(doc.getUrl(), textDoc.getSuperImage().getUrl());
					doc.getMetadata().put("image", url.toString());
				} catch (MalformedURLException e) {
					logger.error("Error parsing image url", e);
				}
			} else if(textDoc.getImages() != null) {
				for(Image img : textDoc.getImages()){
					try {
						URL url = new URL(doc.getUrl(), img.getUrl());
						
						if(doc.getMetadata().get("image") == null) {
							doc.getMetadata().put("image", url.toString());
							break;
						}
						
					} catch (MalformedURLException e) {
					} 
				}
			}

		} catch (BoilerpipeProcessingException | SAXException e) {
			logger.error("Error extracting text from doc", e);
		}
	}

	@Override
	public String[] getMimeTypes() {
		return MIME_TYPES;
	}

	@Override
	public void setConf(Configuration conf) {
	}
}
