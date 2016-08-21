package com.dabeeb.miner.index.filter.source;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.data.DBConnectionManager;
import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.index.IndexFilterPlugin;

public class SourceTaggerFilter implements IndexFilterPlugin {
	public static Logger logger = LogManager.getFormatterLogger(SourceTaggerFilter.class);
	private Configuration conf;
	
	private static final String CONFIG_PREFIX = "sourceTagger";
	private static final String CONFIG_CONNECTION = CONFIG_PREFIX + ".connection";
	
	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}
	
	@Override
	public boolean filter(Document doc) {
		URL url = doc.getFinalUrl();
		String host = url.getHost();
		
		if(host.startsWith("www."))
			host = host.substring(4);
		
		Connection connection = null;
		PreparedStatement stmtSource = null;
		PreparedStatement stmtSrcNames = null;
		ResultSet rsetSource = null;
		
		try {
			connection = DBConnectionManager.getInstance().getConnection(conf.getString(CONFIG_CONNECTION));
			stmtSource = connection.prepareStatement("SELECT srcId, nameAr, nameEn, country, region, city, mainURI FROM source WHERE mainURI LIKE ?");
			stmtSrcNames = connection.prepareStatement("SELECT name FROM source_name WHERE srcId=?");
			boolean hostFound = false;
			while(host.indexOf('.', host.indexOf('.')) != -1) {
				stmtSource.setString(1, '%' + host + '%');
				rsetSource = stmtSource.executeQuery();
				if(rsetSource.next()) {
					hostFound = true;
					
					Source source = new Source();
					
					source.setNameAr(rsetSource.getString("nameAr"));
					source.setNameEn(rsetSource.getString("nameEn"));
					source.setCountry(rsetSource.getString("country"));
					source.setRegion(rsetSource.getString("region"));
					source.setCity(rsetSource.getString("city"));
					source.setMainUrl(rsetSource.getString("mainURI"));
					
					int srcId = rsetSource.getInt("srcId");
					stmtSrcNames.setInt(1, srcId);
					ResultSet rsetSrcNames = stmtSrcNames.executeQuery();
					while(rsetSrcNames.next()) {
						source.addAlias(rsetSrcNames.getString("name"));
					}
					
					doc.setData("source", source);
					break;
				}
				
				host = host.substring(host.indexOf('.') + 1);
			}
			
			if(!hostFound) {
				//This should not happen
				logger.error("Cannot find matching source for host: %s" , host);
				doc.setIndex(false);
				return false;
			}
			
		} catch (SQLException e) {
			logger.error("SQL Error while tagging source", e);
		} finally {
			try {
				if(rsetSource != null)
					rsetSource.close();
				
				if(stmtSource != null)
					stmtSource.close();
				
				if(stmtSrcNames != null)
					stmtSrcNames.close();
				
				if(connection != null)
					connection.close();
			
			} catch (SQLException e) {
                logger.error("Error closing MySQL connection", e);
            }
			
		}
		
		return true;
	}

}
