package com.dabeeb.miner.servlets;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;

import com.dabeeb.miner.crawl.WebServerThread;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.fetch.Fetcher;
import com.dabeeb.miner.parse.ParserPlugin;
import com.dabeeb.miner.parse.html.HTMLParserFactory;
import com.google.gson.Gson;

@SuppressWarnings("serial")
public class HtmlParserServlet extends HttpServlet {
	
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
		Configuration conf = WebServerThread.getInstance().getConf();
		Gson gson = new Gson();
		
		String url = request.getParameter("url");
		Fetcher fetcher = new Fetcher(conf);
		fetcher.setConf(conf);

		Document doc = new Document();
		doc.setUrl(new URL(url));

		try {
			fetcher.fetchUrl(doc);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		HTMLParserFactory factory = new HTMLParserFactory();
		factory.setConf(conf);
		ParserPlugin parser = factory.createInstance();
		parser.parse(doc);
		
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        
        ParsedContentWrapper res = new ParsedContentWrapper();
        res.title = doc.getParsedContent().getTitle();
        res.article = doc.getParsedContent().getText();
        res.imageUrl = doc.getMetadata().get("image");
        
        response.getWriter().println(gson.toJson(res));
    }

	@SuppressWarnings("unused")
	private static class ParsedContentWrapper {
		String title;
		String article;
		String imageUrl;
	}
}
