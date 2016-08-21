package com.dabeeb.miner.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dabeeb.miner.crawl.CrawlStatistics;
import com.dabeeb.miner.crawl.Crawler;
import com.dabeeb.miner.crawl.DocumentHandlerThread.DocumentHandlerStatus;
import com.dabeeb.miner.crawl.DocumentReindexerThread.DocumentReindexerStatus;

@SuppressWarnings("serial")
public class ConsoleServlet extends HttpServlet {
	
	public static DateFormat dateFormatter = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.LONG);
	static {
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
		
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"><link rel=\"stylesheet\" type=\"text/css\" href=\"css/main.css\"><title>Dabeeb Miner Remote Management Console</title></head><body>");
        printHeader(out);
        out.println("<div class=\"page\"><div class=\"container\">");
        printGeneralInfo(out);
        printStatistics(out);
        printCrawlingThreadStatus(out);
        printReindexingThreadStatus(out);
        out.println("</div></div></body></html>");
        out.flush();
    }
	
	private void printHeader(PrintWriter out) {
		out.println("<div class=\"header\"><div class=\"container\"><div class=\"title\">Dabeeb Miner Remote Management Console</div><div class=\"logo\"></div></div></div>");
		
		/*<div class="nav">
		<div class="container">
			<ul>
				<li><a href="/sources/index.action">Sources</a></li>
				<li><a href="http://node2.dabeeb.com:8080/" target="_blank">Monitoring</a></li>
				<li><a href="/extractor/index.action">Extraction Test</a></li>
				<li>Feed Discovery</li>
			</ul>
		</div>
		</div>*/
	}

	private void printGeneralInfo(PrintWriter out) {
		out.println("<h1>General Information</h1>");

        out.println("<table>");

        out.print("<tr><th>Crawler Identifier:</th><td>Crawler-");
		out.print(Crawler.getInstance().getCrawlerId());
		out.println("</td></tr>");
		
		Date now = new Date();
		
		out.print("<tr><th>Time Now on Server:</th><td>");
		out.print(dateFormatter.format(now));
		out.println("</td></tr>");
        
		out.println("</table>");
        
	}

	private void printCrawlingThreadStatus(PrintWriter out) {
        out.println("<h1>Crawling Thread Status</h1>");
        
        DocumentHandlerStatus[] statuses = Crawler.getInstance().getDocumentHandlersStatus();
        boolean hasStatus = false;
        
        if(statuses.length == 0) {
        	out.println("<p>No crawling threads have been created.</p>");
        	return;
        }

        for(DocumentHandlerStatus status : statuses) {
        	if(status != null) {
        		hasStatus = true;
	        	out.print("<h3>Thread-");
	        	out.print(status.getIdentifier());
	            out.println("</h3><table>");
	            
		        out.print("<tr><th>Phase:</th><td>");
				out.print(status.getPhase().getFriendlyName());
				out.print(" - ");
				out.print(status.getSubPhase());
				out.println("</td></tr>");
				
				out.print("<tr><th>URL:</th><td>");
				out.print(status.getUrl());
				out.println("</td></tr>");
				
				long ms = System.currentTimeMillis() - status.getWaitStartTime().getTime();
				
				out.print("<tr><th>Idle Time:</th><td>");
				out.print( ms / 1000 );
				out.println("</td></tr>");
				
		        out.println("</table>");
        	}
        }

        if(!hasStatus) {
        	out.println("<p>No threads reported any status information.</p>");
        }
	}
	
	private void printReindexingThreadStatus(PrintWriter out) {
        out.println("<h1>Reindexing Thread Status</h1>");
        
        DocumentReindexerStatus[] statuses = Crawler.getInstance().getDocumentReindexersStatus();
        boolean hasStatus = false;
        
        if(statuses.length == 0) {
        	out.println("<p>No reindexing threads have been created.</p>");
        	return;
        }
        
        for(DocumentReindexerStatus status : statuses) {
        	if(status != null) {
        		hasStatus = true;
        		
	        	out.print("<h3>Thread-");
	        	out.print(status.getIdentifier());
	            out.println("</h3><table>");
	            
		        out.print("<tr><th>Phase:</th><td>");
				out.print(status.getPhase().getFriendlyName());
				out.println("</td></tr>");
				
				out.print("<tr><th>URL:</th><td>");
				out.print(status.getUrl());
				out.println("</td></tr>");
				
		        out.println("</table>");
        	}
        }
        
        if(!hasStatus) {
        	out.println("<p>No threads reported any status information.</p>");
        }
	}

	private void printStatistics(PrintWriter out) {
        out.println("<h1>Statistics</h1>");
		CrawlStatistics stats = Crawler.getInstance().getCrawlStatistics();

        out.println("<table>");

        out.print("<tr><th>Processed Documents:</th><td>");
		out.print(stats.getProcessedDocuments());
		out.println("</td></tr>");
		
		out.print("<tr><th>Currently Processed Documents:</th><td>");
		out.print(stats.getUnderProcessDocuments());
		out.println("</td></tr>");

		out.print("<tr><th>Crawler Start Time:</th><td>");
		out.print(dateFormatter.format(stats.getStartTime()));
		out.println("</td></tr>");
		
        out.println("</table>");
	}
}
