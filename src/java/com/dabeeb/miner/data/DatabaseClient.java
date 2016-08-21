package com.dabeeb.miner.data;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.conf.Configurable;
import com.dabeeb.miner.data.dao.CacheDAO;
import com.dabeeb.miner.data.dao.FetchPoolDAO;
import com.dabeeb.miner.data.dao.HostDAO;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.WriteType;
import com.datastax.driver.core.policies.RetryPolicy;

public class DatabaseClient implements Configurable {
	private static final String CONFIG_PREFIX = "cassandra";
	private static final String CONFIG_CONTACT_POINTS = CONFIG_PREFIX + ".contactpoint";
	private static final int DEFAULT_TIMEOUT_WAIT = 5000;
	
	public static Logger logger = LogManager.getFormatterLogger(DatabaseClient.class);
	public static final int replicationFactor = 1;

	public static int clusterPort = 9042;
	
	private Cluster cluster;
	private Session session;
	private Configuration conf;
	
	private static DatabaseClient instance = new DatabaseClient();
	
	private CacheDAO cacheDAO;
	private FetchPoolDAO fetchPoolDAO;
	private HostDAO hostDAO;
	
	private DatabaseClient() {
	}
	
	public static DatabaseClient getInstance() {
		return instance;
	}

	public void connect() {
		Builder builder = Cluster.builder().withPort(clusterPort);
		String[] contactPoints = conf.getStringArray(CONFIG_CONTACT_POINTS);
		for(String contactPoint : contactPoints) {
			builder.addContactPoint(contactPoint);
		}
		builder.withRetryPolicy(new DabeebRetryPolicy());
		cluster = builder.build();
		Metadata metadata = cluster.getMetadata();
		logger.info("Connected to cluster: %s", metadata.getClusterName());
		
		session = cluster.connect();
		
		if(hasSchema()) {
			logger.info("Schema already created");
		}
		else {
			createSchema();
		}
		
		cacheDAO = new CacheDAO(this);
		fetchPoolDAO = new FetchPoolDAO(this);
		hostDAO = new HostDAO(this);
	}
	
	public boolean hasSchema() {
		ResultSet results = session.execute("SELECT * from system.schema_keyspaces WHERE keyspace_name = 'dabeeb';");
		return results.one() != null;
	}
	
	public void createSchema(){
		logger.info("Creating schema...");
		
		session.execute("CREATE KEYSPACE dabeeb WITH replication = {'class':'SimpleStrategy', 'replication_factor':" + replicationFactor +"};");
		session.execute(
				"CREATE TABLE dabeeb.cache (" +
						"url text PRIMARY KEY," +
						"redirect text," +
			            "raw blob," + 
			            "encoding text," +
			            "title text," +
			            "article text," +
			            "discovery timestamp," +
			            "publish timestamp," +
			            "metadata map<text,text>," +
			            ");");
		session.execute(
			    "CREATE TABLE dabeeb.fetchpool (" +
				        "url text PRIMARY KEY," +
			            "score int," +
			            "retries int, " + 
			            "depth int," +
			            "raw blob," +
			            "html text," +
			            "parsed text," + 
			            "discovery timestamp," +
			            "publish timestamp," +
			            "inlinkUrl text," +
			            "inlinkAnchor text," + 
			            "metadata map<text,text>," +
			            ");");
		session.execute(
				"CREATE TABLE dabeeb.host (" +
						"host text PRIMARY KEY," +
						"lastfetch timestamp," +		//When did the last fetch from this host occur
						"lastduration int," +			//How much time did the last fetch take
						");");
		session.execute(
				"CREATE TABLE dabeeb.hostfailures (" +
						"host text PRIMARY KEY," +
						"failures counter," +				//Number of consecutive failures
						");");
		
		//session.execute("CREATE INDEX crawlerids ON dabeeb.operation( crawlerid );");

		logger.info("Schema created successfully");
	}

	public void close() {
		cluster.close();
	}
	
	public Session getSession() {
		return session;
	}
	
	public CacheDAO getCacheDAO() {
		return cacheDAO;
	}
	
	public FetchPoolDAO getFetchPoolDAO() {
		return fetchPoolDAO;
	}
	
	public HostDAO getHostDAO() {
		return hostDAO;
	}
	
	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}
	
	private static class DabeebRetryPolicy implements RetryPolicy {

		@Override
		public RetryDecision onReadTimeout(Statement statement, ConsistencyLevel cl, int requiredResponses, int receivedResponses,
				boolean dataRetrieved, int nbRetry) {
			logger.error("Cassandra read timed out, retry no: %s", nbRetry);
			try {
				Thread.sleep(DEFAULT_TIMEOUT_WAIT);
			} catch (InterruptedException e) {
			}
			return RetryDecision.retry(cl);
		}

		@Override
		public RetryDecision onWriteTimeout(Statement statement, ConsistencyLevel cl, WriteType writeType, int requiredAcks, int receivedAcks,
				int nbRetry) {
			logger.error("Cassandra write timed out, retry no: %s", nbRetry);
			try {
				Thread.sleep(DEFAULT_TIMEOUT_WAIT);
			} catch (InterruptedException e) {
			}

			return RetryDecision.retry(cl);
		}

		@Override
		public RetryDecision onUnavailable(Statement statement, ConsistencyLevel cl, int requiredReplica, int aliveReplica, int nbRetry) {
			logger.error("Cassandra unavailable, retry no: %s", nbRetry);
			try {
				Thread.sleep(DEFAULT_TIMEOUT_WAIT);
			} catch (InterruptedException e) {
			}

			return RetryDecision.retry(cl);
		}
		
	}
}
