package com.dabeeb.miner.crawl;

import java.net.URL;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;

import com.dabeeb.miner.index.filter.author.AuthorTaggerFilter;
import com.dabeeb.miner.servlets.ConsoleServlet;
import com.dabeeb.miner.servlets.HtmlParserServlet;
import com.dabeeb.miner.servlets.ReindexServlet;

public class WebServerThread extends Thread {

	public static Logger logger = LogManager.getFormatterLogger(WebServerThread.class);
	
	private static final String CONFIG_PREFIX = "console";
	private static final String CONFIG_USERNAME = CONFIG_PREFIX + ".username";
	private static final String CONFIG_PASSWORD = CONFIG_PREFIX + ".password";
	private static final String CONFIG_PORT = CONFIG_PREFIX + ".port";
	
	private Configuration conf;
	private static WebServerThread instance = new WebServerThread();
	
	private WebServerThread() {
		//setDaemon(true);
		setDaemon(false);
	}
	
	public static WebServerThread getInstance() {
		return instance;
	}
	
	public void setConf(Configuration conf) {
		this.conf = conf;
	}
	
	public Configuration getConf() {
		return conf;
	}
	
	@Override
	public void run() {
		Server httpServer = new Server(conf.getInt(CONFIG_PORT, 8080));
		try {
			
			//String webDir = this.getClass().getClassLoader().getResource("WebContent").toExternalForm();
			String webDir = "/opt/dabeeb/WebContent/";
			
			logger.info("Jetty static content loading from: %s", webDir);
			
			ResourceHandler resource_handler = new ResourceHandler();
	        resource_handler.setDirectoriesListed(true);
	        resource_handler.setWelcomeFiles(new String[]{ "index.html" });
	        resource_handler.setResourceBase(webDir);

			ServletContextHandler rootContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
			rootContext.setContextPath("/");
			rootContext.addServlet(ConsoleServlet.class, "/console");
			rootContext.setHandler(resource_handler);
			
			ServletContextHandler htmlParserContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
			htmlParserContext.setContextPath("/htmlParser");
			htmlParserContext.addServlet(HtmlParserServlet.class, "/parse");
			
			ServletContextHandler reindexerContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
			reindexerContext.setContextPath("/reindexer");
			reindexerContext.addServlet(ReindexServlet.class, "/reindex");
	        
			//ContextHandler ch = new ContextHandler("/res");
			//ch.setHandler(resource_handler);
			
			ContextHandlerCollection contexts = new ContextHandlerCollection();
			Handler[] handlers = new Handler[] { reindexerContext, htmlParserContext, rootContext };
			/*for(ServletContextHandler handler : handlers) {
				
				handler.setSecurityHandler(securityHandler);
			}*/
			
			contexts.setHandlers(handlers);
			
			SecurityHandler securityHandler = getBasicAuthSecurityHandler(conf.getString(CONFIG_USERNAME, "admin"), conf.getString(CONFIG_PASSWORD, "admin"), "Dabeeb Administration");
			securityHandler.setHandler(contexts);
	        
			
	        httpServer.setHandler(securityHandler);
	        
	        
			
			httpServer.start();
			logger.info("Started embedded Jetty server on port 8080...");
			
			httpServer.join();
			logger.info("Stopped Jetty server...");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static final SecurityHandler getBasicAuthSecurityHandler(String username, String password, String realm) {

    	HashLoginService loginService = new HashLoginService();
    	loginService.putUser(username, Credential.getCredential(password), new String[] {"user"});
    	loginService.setName(realm);
        
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"user"});
        constraint.setAuthenticate(true);
         
        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");
        
        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setRealmName("myrealm");
        csh.addConstraintMapping(cm);
        csh.setLoginService(loginService);
        
        return csh;
    	
    }
}
