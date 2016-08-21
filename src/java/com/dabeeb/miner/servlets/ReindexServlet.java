package com.dabeeb.miner.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.jms.JMSException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dabeeb.miner.crawl.CrawlStatistics;
import com.dabeeb.miner.crawl.Crawler;
import com.dabeeb.miner.crawl.DocumentHandlerThread.DocumentHandlerStatus;
import com.dabeeb.miner.crawl.DocumentReindexerThread.DocumentReindexerStatus;
import com.dabeeb.miner.messaging.MessagingManager;

@SuppressWarnings("serial")
public class ReindexServlet extends HttpServlet {
	
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
		
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter out = response.getWriter();
        
        out.println("<html><head><title>Dabeeb Miner Remote Management Console</title></head><body><h1>Dabeeb Miner Remote Management Console - Reindexer</h1>");
        printForm(out);

        String url = request.getParameter("url");
        if(url != null) {
        	scheduleReindex(out, url);
        }
        
        out.println("</body></html>");
        out.flush();
    }

	private void scheduleReindex(PrintWriter out, String url) {
		out.print("<div>");
		try {
			MessagingManager.getInstance().informReindexUrls(url);
		} catch (JMSException e) {
			e.printStackTrace();
		}
		out.print("URL: ");
		out.print(url);
		out.print(" is being reindexed.");
		out.print("</div>");
	}

	private void printForm(PrintWriter out) {
		out.print("<form action=\"reindex\" method=\"get\">");
		
		out.print("<div>");
		out.print("<label for=\"url\">URL:</label>");
		out.print("<input id=\"url\" name=\"url\" type=\"text\" />");
		out.print("</div>");
		
		out.print("<input type=\"submit\" value=\"Reindex\">");
		out.print("</form>");
		
	}

	
}
