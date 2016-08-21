package com.dabeeb.miner.data.dao;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.data.DatabaseClient;
import com.dabeeb.miner.data.model.Host;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class HostDAO {
	
	public static Logger logger = LogManager.getFormatterLogger(HostDAO.class);
	private Session session;
	private PreparedStatement findHostStatement, findHostFailuresStatement;
	
	private PreparedStatement addStatement;
	private PreparedStatement incrementFailures;
	private PreparedStatement resetFailures;
	private PreparedStatement reportFetch;
	
	public HostDAO(DatabaseClient dbClient) {
		session = dbClient.getSession();
		
		findHostStatement = session.prepare("SELECT * FROM dabeeb.host WHERE host=?");
		addStatement = session.prepare("INSERT INTO dabeeb.host (host) VALUES (?) IF NOT EXISTS;");
		reportFetch = session.prepare("UPDATE dabeeb.host SET lastfetch=?, lastduration=? WHERE host = ?");
		
		resetFailures = session.prepare("DELETE FROM dabeeb.hostfailures WHERE host = ?");
		incrementFailures = session.prepare("UPDATE dabeeb.hostfailures SET failures = failures + 1 WHERE host = ?");
		findHostFailuresStatement = session.prepare("SELECT * FROM dabeeb.hostfailures WHERE host=?");
	}
	
	public Host getHost(String hostName) {
		BoundStatement bnd = findHostStatement.bind(hostName);
		ResultSet rset = session.execute(bnd);
		Row row = rset.one();
		
		if(row == null){
			return null;
		}
		
		Host res = new Host(row.getString("host"));
		res.setLastDuration(row.getInt("lastduration"));
		res.setLastFetch(row.getDate("lastfetch"));
		
		bnd = findHostFailuresStatement.bind(hostName);
		rset = session.execute(bnd);
		row = rset.one();
		
		if(row != null){
			res.setFailures(row.getLong("failures"));
		}
		
		return res;
	}

	public void addHost(Host host) {
		BoundStatement bnd = addStatement.bind(host.getHostName());
		session.execute(bnd);
	}
	
	public boolean incrementFailures(Host host) {

		BoundStatement bnd = incrementFailures.bind(host.getHostName());
		ResultSet rset = session.execute(bnd);
		Row row = rset.one();
		if(row == null)
			return false;
		boolean success = row.getBool(0);
		
		return success;
	}

	public void reportFetch(Host host) {
		BoundStatement bnd = reportFetch.bind(host.getLastFetch(), host.getLastDuration(), host.getHostName());
		session.execute(bnd);
	}

	public void resetFailures(Host host) {
		BoundStatement bnd = resetFailures.bind(host.getHostName());
		session.execute(bnd);
	}

}
