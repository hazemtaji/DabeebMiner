package com.dabeeb.miner.index.filter.article;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.LinkedList;
import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.model.Inlink;
import com.dabeeb.miner.data.model.Parse;
import com.dabeeb.miner.index.IndexFilterPlugin;
import com.dabeeb.miner.index.filter.article.NewsExtractor.ExtractionResult;

public class ArticleExtractorFilter implements IndexFilterPlugin {
	public static Logger logger = LogManager.getFormatterLogger(ArticleExtractorFilter.class);
	private Configuration conf;
	
	private NewsExtractor extractor;
	
	private static Hashtable<String, VideoURLFixer> videoFixerPlugins = new Hashtable<String, VideoURLFixer>();
	public static final String[] videoExt = { ".flv", ".mp4", ".mpg", ".avi", ".asx" };
	static
	{
		videoFixerPlugins.put("arabic.euronews.com", new EuroNewsVideoURLFixer());
	}
	
	public ArticleExtractorFilter() {
		extractor = new NewsExtractor();
	}

	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	@Override
	public boolean filter(Document doc) {
		try
		{
			URL objURL = doc.getFinalUrl();
		    
		    LinkedList<String> inlinks = new LinkedList<String>(); 
		    
		    for(Inlink inlink : doc.getInlinks())
		    {
		    	//skip self linking
		    	if(!doc.getFinalUrl().toString().equalsIgnoreCase(inlink.getFromUrl().toString()))
		    	{
		    		inlinks.add(inlink.getAnchor().replace("^", ""));
		    	}
		    }
			
			Parse content = doc.getParsedContent();
			
		    String[] anchors = new String[inlinks.size()];
		    anchors = inlinks.toArray(anchors);
			ExtractionResult extract = extractor.extract(content.getText(), content.getTitle(), anchors);
		
			
			//String title = doc.getFieldValue("title");
			
			content.setText(extract.article);
			content.setTitle(extract.title);
			
			StringBuffer tagsBuff = new StringBuffer();
			if(doc.getMetadata() != null) {
				String tags = doc.getMetadata().get("tags");
				if(tags != null)
					tagsBuff.append(tags);
			}
			
			if(extract.image != null)
			{
				tagsBuff.append(" image:");
				tagsBuff.append((new URL( objURL, extract.image)).toString());
			}
			
			if(extract.videoFile != null)
			{
				URL videoFile = checkVideoFile(objURL, extract.videoFile);
				tagsBuff.append(" videoFile:");
				tagsBuff.append(videoFile.toString());
				tagsBuff.append(" videoThumb:");
				tagsBuff.append((new URL( objURL, extract.videoThumbnail)).toString());
			}
			
			String tags = tagsBuff.toString().trim();
			if(extract.author != null)
			{
				tags = tags + " " + extract.author;
			}
			
			doc.getMetadata().put("tags", tags);

			//extractor.extract( TableUtil.toString(page.getText()).replaceAll("<img.*?/>", ""), TableUtil.toString(page.getTitle()), inlinks.toArray(anchors));
			
			return true;
		}
		catch (Exception e)
		{
			logger.error("Error in text extractior.", e);
		}
		return false;
	}
	


	private URL checkVideoFile(URL context, String videoFile) throws MalformedURLException {
		
		VideoURLFixer fixer = videoFixerPlugins.get(context.getHost());
		if(fixer != null)
			return new URL(fixer.fixVideoURL(videoFile));
		
		URL[] toTry = new URL[videoExt.length + 1];
		toTry[0] = new URL(context, videoFile);
		
		int i = 0;
		for(String ext : videoExt){
			
			if(videoFile.endsWith(ext))
				return toTry[0];
			
			toTry[i++] = new URL(context, videoFile + "." + ext);
		}
		
		for(URL url : toTry){
			try {
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				if (conn.getResponseCode() == 200)
				{
					return url;
				}
			} catch (IOException e) { }
		}
		
		return null;
	}

}
