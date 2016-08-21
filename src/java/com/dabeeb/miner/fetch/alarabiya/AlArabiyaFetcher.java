package com.dabeeb.miner.fetch.alarabiya;

import org.apache.commons.configuration.Configuration;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.fetch.FetcherPlugin;

public class AlArabiyaFetcher implements FetcherPlugin{
	public static Logger logger = LogManager.getFormatterLogger(AlArabiyaFetcher.class);

	private static final String CONFIG_PREFIX = "fetcher.alarabiya";
	private static final String CONFIG_COOKIE_NAME = CONFIG_PREFIX + ".cookiename";
	private static final String CONFIG_IP_ADDRESS = CONFIG_PREFIX + ".ipaddress";
	
	private static final String HOST_NAME = "www.alarabiya.net";
	private static final String[] HOST_NAMES = {"www.alarabiya.net", "alarabiya.net"};
	
	private Configuration conf;
	
	@Override
	public void prefetch(HttpGet httpGet, HttpContext context) {
		RequestConfig localConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.BROWSER_COMPATIBILITY).build();
		httpGet.setConfig(localConfig);
		
		CookieStore cookieStore = new BasicCookieStore();
		
		BasicClientCookie myCookie = new BasicClientCookie(conf.getString(CONFIG_COOKIE_NAME), conf.getString(CONFIG_IP_ADDRESS));
		myCookie.setVersion(1);
		myCookie.setDomain(HOST_NAME);
		myCookie.setPath("/");
		myCookie.setSecure(false);
		
		// Set attributes EXACTLY as sent by the server 
		myCookie.setAttribute(ClientCookie.VERSION_ATTR, "1");
		myCookie.setAttribute(ClientCookie.DOMAIN_ATTR, HOST_NAME);
		cookieStore.addCookie(myCookie);
		
		context.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
	}
	
	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	@Override
	public void postResponse(CloseableHttpResponse response) {
	}

	@Override
	public void processRawContent(byte[] contentData) {
	}

	@Override
	public String[] getHostNames() {
		return HOST_NAMES;
	}
}
