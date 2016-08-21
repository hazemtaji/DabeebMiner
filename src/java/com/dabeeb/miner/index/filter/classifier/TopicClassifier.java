package com.dabeeb.miner.index.filter.classifier;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.LinkedList;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.crawl.Crawler;
import com.madar.libsvm.SVMUtils;
import com.madar.libsvm.structures.SVMModel;
import com.madar.libsvm.structures.SVMNode;

public class TopicClassifier {
	public static Logger logger = LogManager.getFormatterLogger(TopicClassifier.class);

	private static final String CLASSIFIER_FILENAME = "classifier-vector.txt";
	private static final String CONFIG_PREFIX = "topic-classifier";
	private static final String CONFIG_LANGUAGES = CONFIG_PREFIX + ".languages";
	private static final String CONFIG_CATEGORIES = CONFIG_PREFIX + ".categories";
	private static final String CONFIG_KEYWORDS = CONFIG_PREFIX + ".keywords";
	
	
	private Category[] categories;
	private String[] wordsVector;
	private Hashtable<String, String> customCats;
	
	
	public static final Log LOG = LogFactory.getLog(TopicClassifier.class);
	
	public TopicClassifier(Configuration config)
	{
		customCats = new Hashtable<String, String>();
		
		try
		{
			String[] languages = config.getStringArray(CONFIG_LANGUAGES);
			String[] categoryNames = config.getStringArray(CONFIG_CATEGORIES);
			
			categories = new Category[categoryNames.length];
			
			InputStream is = this.getClass().getClassLoader().getResourceAsStream(CLASSIFIER_FILENAME);
			String vectorString = new String(readStream(is), "UTF-8");
			
			wordsVector = vectorString.split(" ");
			
			for(int i = 0; i < categories.length; i++)
			{
				for(String lang : languages)
				{
					String category = categoryNames[i];
					is = this.getClass().getClassLoader().getResourceAsStream("classifier/models/" + lang + "/" + category + ".model");
					categories[i] = new Category(category);
					SVMModel model = SVMModel.loadModel(is);
					categories[i].addModel(lang, model);
				}
			}
			
			for(Category cat : categories)
			{
				cat.keywords = config.getStringArray(CONFIG_KEYWORDS + "." + cat.getName());
			}
		}
		catch (IOException e) 
		{
			LOG.error("Error initializing TopicClassifier automatic classification", e);
		}
	}
	
	public String classifyURL(URL url)
	{
		for(Category cat : categories)
		{
			if(cat.testURL(url))
				return cat.getName();
		}
		return null;
	}
	
	/**
	 * Classifies Feed URL based on categories set in the database.
	 * 
	 * @param feedURL
	 * @return category
	 */
	public String feedClassify(String feedURL)
	{
		String cached = customCats.get(feedURL);
		if(cached == null)
		{
			/* TODO: switch this to read from Gora /*
			
			/*FeedDAO feedDAO = factory.getFeedDAO();
			
			Transaction trans = HibernateUtil.getSessionFactory().getCurrentSession().beginTransaction();
			Feed feed = feedDAO.getByURL(fromURL);
			if(feed != null)
			{
				String cats = feed.getCategories();
				if(cats!= null && cats.trim().length() > 0)
					customCats.put(fromURL, cats);
				trans.commit();
				if(cats == null || cats.trim().length() == 0)
					return null;
				return cats;
			}
			trans.commit();*/
		}
		return cached;
	}
	
	public String classify(LinkedList<String> doc, String language)
	{
		
		LinkedList<SVMNode> nodesList = new LinkedList<SVMNode>();
		
		Hashtable<String, WordFreq> wordsTable = new Hashtable<String, WordFreq>();
		
		
		for(String text : doc)
		{
			LinkedList<String> words = VectorGenerator.docToWords(text);
			
			for(String word : words)
			{
				WordFreq freq = wordsTable.get(word);
				if(freq == null)
				{
					freq = new WordFreq(word);
					wordsTable.put(freq.word, freq);
				}
				freq.freq++;
			}
		}
		
		for(int i = 0; i < wordsVector.length; i++)
		{
			WordFreq freq = wordsTable.get(wordsVector[i]);
			if(freq != null)
			{
				SVMNode node = new SVMNode(i + 1, freq.freq);
				nodesList.add(node);
			}
		}
		
		SVMNode nodes[] = new SVMNode[nodesList.size()];
		nodesList.toArray(nodes);
		CatProbability high = null;
		CatProbability nextHigh = null;
		
		for(Category category : categories)
		{
			SVMModel model = category.getModel(language);
			if(model != null)
			{
				double probability = predict(nodes, model);
				if(high == null || high.probability < probability)
				{
					nextHigh = high;
					high = new CatProbability(category.getName(), probability);
				}
				else if(nextHigh == null || nextHigh.probability < probability)
				{
					nextHigh = new CatProbability(category.getName(), probability);
				}
			}
		}
		
		if(high == null)
			return null;
		
		if(nextHigh == null || nextHigh.probability < 0.8)
			return(high.cat);
		
		return high.cat + " " + nextHigh.cat;
	}
	
	private static double predict(SVMNode[] nodes, SVMModel model)
	{
		double[] prob_estimates = new double[model.getClassCount()];
		SVMUtils.predict_probability(model, nodes, prob_estimates);
		//model.predictProbability(nodes, prob_estimates);
		return prob_estimates[0];
	}
	
	private static class CatProbability
	{
		double probability;
		String cat;
		
		public CatProbability(String cat, double probability)
		{
			this.cat = cat;
			this.probability = probability;
		}
	}
	
	private class Category
	{
		private String name;
		private Hashtable<String, SVMModel> languageModel;
		private String[] keywords;
		
		public Category(String name)
		{
			this.name = name;
			languageModel = new Hashtable<String, SVMModel>();
		}
		
		public void addModel(String lang, SVMModel model)
		{
			languageModel.put(lang, model);
		}
		
		public SVMModel getModel(String lang)
		{
			return languageModel.get(lang);
		}
		
		public String getName()
		{
			return name;
		}
		
		public boolean testURL(URL url)
		{
			String urlStr = url.toString().toLowerCase();
			for(String keyword : keywords)
			{
				if(urlStr.matches(keyword) || urlStr.contains(keyword))
					return true;
			}
			return false;
		}
	}
	
	public static byte[] readStream(InputStream is)
	{
		ByteArrayOutputStream bytes = null;
		try
		{
			bytes = new ByteArrayOutputStream(is.available());
			byte[] buffer = new byte[256];
			int bytesRead = 0;
			while (true)
            {
                bytesRead = is.read(buffer);
                if (bytesRead == -1)
                    break;
                bytes.write(buffer, 0, bytesRead);
            }
			is.close();
		}
		catch (IOException e) {
		}
		return bytes.toByteArray();
	}
	
	public static void main(String[] args) throws ConfigurationException, MalformedURLException
	{
		Configuration config = Crawler.initialize();
		
		TopicClassifier classifier = new TopicClassifier(config);
		String language = args[0];
		LinkedList<String> doc = new LinkedList<String>();
		for(int i = 1; i < args.length; i++)
		{
			doc.add(args[i]);
		}
		System.out.println(classifier.classify(doc, language));
		System.out.println(classifier.classifyURL(new URL("http://www.elaph.com/ElaphWeb/Economics/2008/5/335571.htm")));
	}
}
