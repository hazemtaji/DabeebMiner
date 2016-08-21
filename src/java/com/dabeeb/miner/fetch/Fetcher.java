package com.dabeeb.miner.fetch;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.conf.Configurable;
import com.dabeeb.miner.crawl.Crawler;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.parse.rss.RSSParser;
import com.dabeeb.miner.plugin.PluginFactory;
import com.dabeeb.miner.plugin.PluginRepository;

public class Fetcher implements Configurable{

	public static Logger logger = LogManager.getFormatterLogger(Fetcher.class);

	private static final String CONFIG_PREFIX = "fetcher";
	private static final String CONFIG_TIMEOUT = CONFIG_PREFIX + ".timeout";
	private static final String CONFIG_MAX_REDIRECTS = CONFIG_PREFIX + ".maxredirects";
	private static final String CONFIG_SIZE_LIMIT = CONFIG_PREFIX + ".sizelimit";
	private static final String CONFIG_ALLOWED_MIMETYPES = CONFIG_PREFIX + ".allowedMimeTypes.mimetype";
	
	public static final String HTTP_HEADERS = "http-headers";
	public static final String USER_AGENT = "Mozilla/5.0";
	public static final String defaultMimeType = "text/html";
	
	private CloseableHttpClient httpclient;

	private int responseSizeLimit;
	private byte[] copyBuffer = new byte[8192];
	private HashSet<String> allowedMimeTypes;
	
	DefaultFetcher defaultFetcher;
	HashMap<String, FetcherPlugin> pluginDB = new HashMap<>();

	private Configuration conf;
	
	public Fetcher(Configuration conf) {
		setConf(conf);
	}
	
	@Override
	public void setConf(Configuration conf) {
		int timeout = conf.getInt(CONFIG_TIMEOUT);
		int maxRedirects = conf.getInt(CONFIG_MAX_REDIRECTS);
		responseSizeLimit = conf.getInt(CONFIG_SIZE_LIMIT);

		allowedMimeTypes = new HashSet<>();
		String[] mimeTypes = conf.getStringArray(CONFIG_ALLOWED_MIMETYPES);
		for (String mimeType : mimeTypes) {
			allowedMimeTypes.add(mimeType);
		}
		
		ConnectionConfig.Builder connectionBuilder = ConnectionConfig.custom();
		connectionBuilder.setCharset(Charset.forName("utf-8"));

		RequestConfig.Builder requestBuilder = RequestConfig.custom();
		requestBuilder = requestBuilder.setConnectTimeout(timeout);
		requestBuilder = requestBuilder.setConnectionRequestTimeout(timeout);
		requestBuilder = requestBuilder.setStaleConnectionCheckEnabled(true);
		requestBuilder = requestBuilder.setMaxRedirects(maxRedirects);

		HttpClientBuilder builder = HttpClientBuilder.create();
		builder.setDefaultRequestConfig(requestBuilder.build());
		builder.setDefaultConnectionConfig(connectionBuilder.build());
		builder.setUserAgent(USER_AGENT);
		
		httpclient = builder.build(); // HttpClients.createDefault();
		
		this.conf = conf;
		
		createFetchers();
	}

	private void createFetchers() {
		defaultFetcher = new DefaultFetcher();
		defaultFetcher.setConf(conf);
		
		pluginDB.clear();
		for(PluginFactory<FetcherPlugin> pluginFactory : PluginRepository.getInstance().getFetcherFactories()) {
			FetcherPlugin plugin = pluginFactory.createInstance();
			plugin.setConf(conf);
			for(String hostName : plugin.getHostNames()) {
				pluginDB.put(hostName, plugin);
			}
		}
	}

	public FetchStatus fetchUrl(Document doc) throws IOException, ClientProtocolException, URISyntaxException {
		logger.info("Fetching: %s", doc.getUrl());
		
		FetcherPlugin plugin = getPlugin(doc);
		String injectType = doc.getMetadata().get("inject-type");

		URL url = doc.getUrl();
		URI uri;
		try {
			uri = url.toURI();
		} catch (URISyntaxException e) {
			uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
			logger.info("Converted url from: %s to %s", doc.getUrl(), uri.toString());
		}
		
		HttpGet httpGet = new HttpGet(uri);
		HttpContext context = new BasicHttpContext();
		
		//httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml,text/xml;q=0.9,*/*;q=0.8");
		//httpGet.setHeader("Accept-Encoding", "gzip,deflate,sdch");
		//httpGet.setHeader("Accept-Language", "en-US,en;q=0.8,ar;q=0.6");
		httpGet.setHeader("User-Agent", "Dabeeb/1.0");
		
		try {
			plugin.prefetch(httpGet, context);
			
			CloseableHttpResponse response = httpclient.execute(httpGet, context);
			
			
			plugin.postResponse(response);

			try {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode >= 200 && statusCode < 300) {

					URI finalUrl = httpGet.getURI();
					RedirectLocations locations = (RedirectLocations) context.getAttribute(HttpClientContext.REDIRECT_LOCATIONS);
					if (locations != null) {
						finalUrl = locations.getAll().get(locations.getAll().size() - 1);
						logger.info("Redirected from: %s to %s", doc.getUrl(), finalUrl.toString());
					}

					doc.setFinalUrl(finalUrl.toURL());

					HttpEntity entity = response.getEntity();

					// String encoding = entity.getContentEncoding().getValue();
					String mimeType = defaultMimeType;
					Header cTypeHeader = entity.getContentType();
					if (cTypeHeader != null)
						mimeType = entity.getContentType().getValue().split(";")[0].trim();		// Remove the encoding

					doc.setData(HTTP_HEADERS, response.getAllHeaders());

					if (!allowedMimeTypes.contains(mimeType)) {
						logger.info("Rejected mime type: %s", mimeType);
						return FetchStatus.REJECTED;
					}

					long contentSize = entity.getContentLength() > responseSizeLimit ? responseSizeLimit : entity.getContentLength();
					if (contentSize < 1024)
						contentSize = 1024;

					// Download Content
					InputStream is = entity.getContent();
					ByteArrayOutputStream baos = new ByteArrayOutputStream((int) contentSize);
					byte[] contentData = null;
					try {
						if(injectType != null && injectType.equals("rss")) {
							if(!RSSParser.isRSS(mimeType)) {
								//some idiots still give the wrong mime type, dont give up yet!
								
								boolean isRss = true;
								
								byte[] sniffed = new byte[4];
								int read = is.read(sniffed, 0, 4);
								contentSize -= read;
								
								if(read < 4){
									isRss = false;
								} else {
									long result = 0xFF000000 & (sniffed[0] << 24) | 0x00FF0000 & (sniffed[1] << 16) | 0x0000FF00 & (sniffed[2] << 8) | 0x000000FF & sniffed[3];
									if (result != 0x3C3F786D) {
										isRss = false;
									}
								}
								
								if(!isRss){
									doc.setIndex(false);
									logger.info("Expected RSS but found: '%s' for url: %s", mimeType, doc.getUrl());
									return FetchStatus.REJECTED;
								} else {
									mimeType = "text/xml";
									baos.write(sniffed);
								}
							}
						}
	
						int byteCount = 0;
						int len;
						while ((len = is.read(copyBuffer)) != -1 && byteCount <= responseSizeLimit) {
							byteCount += len;
							baos.write(copyBuffer, 0, len);
						}
	
						if (byteCount >= responseSizeLimit) {
							logger.info("Truncating content for: %s", finalUrl.toString());
						}
						
						contentData = baos.toByteArray();

					} finally {
						baos.close();
					}
					
					if(contentData == null) {
						return FetchStatus.FAILED;
					}

					plugin.processRawContent(contentData);

					doc.setRawContent(contentData);
					doc.setMimeType(mimeType);
					doc.setFetchTime(new Date());

					EntityUtils.consume(entity);
				} else if (statusCode >= 400 && statusCode < 500) {
					logger.info("URL Fetch failed with status code %s: %s", statusCode, doc.getUrl());
					return FetchStatus.FAILED;
				}
			} finally {
				response.close();
			}
		} catch (SocketTimeoutException e) {
			logger.info("URL Fetch failed: Cannot connect to host");
			return FetchStatus.FAILED;
		}
		return FetchStatus.FETCHED;
	}
	
	private FetcherPlugin getPlugin(Document doc) {
		String host = doc.getUrl().getHost();
		
		FetcherPlugin plugin = pluginDB.get(host);
		if(plugin == null)
			plugin = defaultFetcher;
		
		return plugin;
	}

	public static void main(String args[]) throws ClientProtocolException, IOException, ConfigurationException, URISyntaxException {
		XMLConfiguration conf = Crawler.initialize();
		
		String url = "http://www.bbc.co.uk/arabic/index.xml";
		Fetcher fetcher = new Fetcher(conf);
		fetcher.setConf(conf);

		Document doc = new Document();
		doc.setUrl(new URL(url));

		fetcher.fetchUrl(doc);

	}

	public static enum FetchStatus {
		FETCHED, DELAYED, REJECTED, FAILED
	}
}
