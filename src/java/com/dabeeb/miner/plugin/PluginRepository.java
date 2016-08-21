package com.dabeeb.miner.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.conf.Configurable;
import com.dabeeb.miner.data.provider.DataProvider;
import com.dabeeb.miner.fetch.FetcherPlugin;
import com.dabeeb.miner.index.IndexFilterPlugin;
import com.dabeeb.miner.index.IndexerPlugin;
import com.dabeeb.miner.net.urlfilter.URLFilterPlugin;
import com.dabeeb.miner.net.urlnormalizer.URLNormalizerPlugin;
import com.dabeeb.miner.parse.ParserPlugin;

public class PluginRepository implements Configurable{
	public static Logger logger = LogManager.getFormatterLogger(PluginRepository.class);
	public static PluginRepository repository = new PluginRepository();
	
	private List<PluginFactory<FetcherPlugin>> fetcherFactories;
	private List<PluginFactory<ParserPlugin>> parserFactories;
	private List<PluginFactory<URLNormalizerPlugin>> urlNormalizerFactories;
	private List<PluginFactory<URLFilterPlugin>> urlFilterFactories;
	private List<PluginFactory<IndexFilterPlugin>> indexFilterPluginFactories;
	private List<PluginFactory<IndexerPlugin>> indexerPluginFactories;
	
	private Configuration conf;
	
	public static PluginRepository getInstance() {
		return repository;
	}
	
	private PluginRepository() {
	}
	
	/*public static Hashtable<String, ParserPlugin> getParsers(Configuration conf) {
		return getPlugins(conf, "plugins.parsers.parser", "mime", "class");
	}*/
	
	/*public static <T> Hashtable<String, T> getPlugins(Configuration conf, String root, String keyName, String valueName) {
		Hashtable<String, T> plugins = new Hashtable<>();
		Map<String, String> pluginNames = ConfigUtils.getMap(conf, root, keyName, valueName);
		for(Entry<String, String> entry : pluginNames.entrySet()) {
			Object obj = createObjectFromClassName(entry.getValue());
			if(obj != null) {
				//if(T.class.isInstance(obj)) {
					plugins.put(entry.getKey(), (T)obj);
				//} else {
				//	logger.error("Invalid plugin %s", entry.getValue());
				//}
			}
		}
		return plugins;
	}*/
	
	public List<ParserPlugin> getParserPlugins() {
		return (List<ParserPlugin>) getPluginsFromFactories(parserFactories);
	}
	
	public List<URLNormalizerPlugin> getURLNormalizerPlugins() {
		return (List<URLNormalizerPlugin>) getPluginsFromFactories(urlNormalizerFactories);
	}

	public List<URLFilterPlugin> getURLFilterPlugins() {
		return (List<URLFilterPlugin>) getPluginsFromFactories(urlFilterFactories);
	}
	
	public List<IndexFilterPlugin> getIndexFilterPlugins() {
		return (List<IndexFilterPlugin>) getPluginsFromFactories(indexFilterPluginFactories);
	}

	public List<IndexerPlugin> getIndexerPlugins() {
		return (List<IndexerPlugin>) getPluginsFromFactories(indexerPluginFactories);
	}
	
	public List<PluginFactory<FetcherPlugin>> getFetcherFactories() {
		return fetcherFactories;
	}
	
	private <T extends Plugin> List<T> getPluginsFromFactories(List<PluginFactory<T>> factories) {
		List<T> res = new ArrayList<>(factories.size());
		
		for(PluginFactory<T> factory : factories) {
			T plugin = factory.createInstance();
			plugin.setConf(conf);
			res.add(plugin);
		}
		
		return res;
	}

	public static <T> T createObjectFromClassName(String className) {
		try {
			@SuppressWarnings("unchecked")
			Class<T> cls = (Class<T>) Class.forName(className);
			return cls.getConstructor().newInstance();
		}
		catch(ClassNotFoundException e) {
			logger.error("Plugin factory not found %s", className);
		} catch (InstantiationException e) {
			logger.error("Error creating plugin factory", e);
		} catch (IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
			logger.error("Invalid plugin factory", e);
		} catch (ClassCastException e) {
			logger.error("Objects do not match %s", className);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public void setConf(Configuration conf) {
		this.conf = conf;
		String[] pluginClassNames = conf.getStringArray("plugins.plugin");

		fetcherFactories = new ArrayList<>(pluginClassNames.length);
		parserFactories = new ArrayList<>(pluginClassNames.length);
		urlNormalizerFactories = new ArrayList<>(pluginClassNames.length);
		urlFilterFactories = new ArrayList<>(pluginClassNames.length);
		indexFilterPluginFactories = new ArrayList<>(pluginClassNames.length);
		indexerPluginFactories = new ArrayList<>(pluginClassNames.length);
		
		for(String className : pluginClassNames) {
			PluginFactory<?> factory = createObjectFromClassName(className);
			
			
			if(factory != null) {
				factory.setConf(conf);
				Class<?> persistentClass = (Class<?>) ((ParameterizedType) factory.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
				
				if(FetcherPlugin.class.isAssignableFrom(persistentClass)) {
					fetcherFactories.add((PluginFactory<FetcherPlugin>) factory);
				}
				else if(ParserPlugin.class.isAssignableFrom(persistentClass)) {
					parserFactories.add((PluginFactory<ParserPlugin>) factory);
				}
				else if(URLNormalizerPlugin.class.isAssignableFrom(persistentClass)) {
					urlNormalizerFactories.add((PluginFactory<URLNormalizerPlugin>) factory);
				}
				else if(URLFilterPlugin.class.isAssignableFrom(persistentClass)) {
					urlFilterFactories.add((PluginFactory<URLFilterPlugin>) factory);
				}
				else if(IndexFilterPlugin.class.isAssignableFrom(persistentClass)) {
					indexFilterPluginFactories.add((PluginFactory<IndexFilterPlugin>) factory);
				}
				else if(IndexerPlugin.class.isAssignableFrom(persistentClass)) {
					indexerPluginFactories.add((PluginFactory<IndexerPlugin>) factory);
				}
				else if(DataProvider.class.isAssignableFrom(persistentClass)) {
					DataProvider dp = ((PluginFactory<DataProvider>) factory).createInstance();
					dp.setConf(conf);
					dp.updateCache();
					dp.register();
				}
			}
		}
		
		/*String[] providerClassNames = conf.getStringArray("plugins.provider");
		for(String className : pluginClassNames) {
			PluginFactory<?> factory = createObjectFromClassName(className);*/
		
		
		if(logger.isInfoEnabled()) {
			StringBuffer buff = new StringBuffer();
			buff.append("Loaded the following plugin factories:\n");
			buff.append("--------------------------------------------\n");
			buff.append("Fetcher Plugins:\n");
			for(PluginFactory<? extends FetcherPlugin> factory : fetcherFactories) {
				buff.append(factory.getClass().getName());
				buff.append("\n");
			}
			buff.append("--------------------------------------------\n");
			buff.append("Parser Plugins:\n");
			for(PluginFactory<? extends ParserPlugin> factory : parserFactories) {
				buff.append(factory.getClass().getName());
				buff.append("\n");
			}
			buff.append("--------------------------------------------\n");
			buff.append("URL Normalization Plugins:\n");
			for(PluginFactory<? extends URLNormalizerPlugin> factory : urlNormalizerFactories) {
				buff.append(factory.getClass().getName());
				buff.append("\n");
			}
			buff.append("--------------------------------------------\n");
			buff.append("URL Filter Plugins:\n");
			for(PluginFactory<? extends URLFilterPlugin> factory : urlFilterFactories) {
				buff.append(factory.getClass().getName());
				buff.append("\n");
			}
			buff.append("--------------------------------------------\n");
			buff.append("Index Filter Plugins:\n");
			for(PluginFactory<? extends IndexFilterPlugin> factory : indexFilterPluginFactories) {
				buff.append(factory.getClass().getName());
				buff.append("\n");
			}
			buff.append("--------------------------------------------\n");
			buff.append("Indexer Plugins:\n");
			for(PluginFactory<? extends IndexerPlugin> factory : indexerPluginFactories) {
				buff.append(factory.getClass().getName());
				buff.append("\n");
			}
			buff.append("--------------------------------------------\n");
			
			logger.info(buff.toString());
		}
	}
}
