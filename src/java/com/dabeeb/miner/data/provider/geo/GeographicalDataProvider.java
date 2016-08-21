package com.dabeeb.miner.data.provider.geo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.crawl.Crawler;
import com.dabeeb.miner.data.DBConnectionManager;
import com.dabeeb.miner.data.provider.DataProvider;

public class GeographicalDataProvider extends DataProvider {
	public static Logger logger = LogManager.getFormatterLogger(GeographicalDataProvider.class);
	
	private static final String CONFIG_PREFIX = "geographicalDataProvider";
	private static final String CONFIG_CONNECTION = CONFIG_PREFIX + ".connection";
	
	private Hashtable<String, Country> countries = new Hashtable<>();
	private Hashtable<String, Region> regions = new Hashtable<>();
	private Hashtable<String, City> cities = new Hashtable<>();
	
	ValuedRadixTree<Location> wordsTree = new ValuedRadixTree<Location>(3);
	
	private	PreparedStatement countryStmt = null;
	private PreparedStatement regionStmt = null;
	private PreparedStatement cityStmt = null;
	private	PreparedStatement countryAliasStmt = null;
	private PreparedStatement regionAliasStmt = null;
	private PreparedStatement cityAliasStmt = null;
	
	private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(true);
	
	private static GeographicalDataProvider instance = new GeographicalDataProvider();
	
	protected GeographicalDataProvider() {
	}
	
	public static GeographicalDataProvider getInstance() {
		return instance;
	}

	@Override
	public void updateCache() {
		Connection connection = null;
		try {
			rwLock.writeLock().lock();
			
			countries.clear();
			regions.clear();
			cities.clear();
			
			connection = DBConnectionManager.getInstance().getConnection(getConf().getString(CONFIG_CONNECTION));
			countryStmt = connection.prepareStatement("SELECT countryId, name, code, arabicName, capitalId FROM country");
			regionStmt = connection.prepareStatement("SELECT regionId, name, countryId, arabicName, latitude, longitude, timeDiff, timeZoneId, code FROM region WHERE countryId=?");
			cityStmt = connection.prepareStatement("SELECT cityId, name, countryId, arabicName, latitude, longitude, timeDiff, timeZoneId, code, regionId FROM city WHERE regionId=?");

			countryAliasStmt = connection.prepareStatement("SELECT alias, lang FROM country_alias WHERE countryId=? and problematic=0");
			regionAliasStmt = connection.prepareStatement("SELECT alias, lang FROM region_alias WHERE regionId=? and problematic=0");
			cityAliasStmt = connection.prepareStatement("SELECT alias, lang FROM city_alias WHERE cityId=? and problematic=0");
			
			populateCountries();
			
			buildRadixTree();
			
		} catch (SQLException e) {
			logger.error("SQL Error while updating goegraphical data cache", e);
		} finally {
			rwLock.writeLock().unlock();
			
			try {
				if (countryStmt != null)
					countryStmt.close();
				
				if(cityStmt != null)
					cityStmt.close();
				
				if(regionStmt != null)
					regionStmt.close();
				
				if (countryAliasStmt != null)
					countryAliasStmt.close();
				
				if(regionAliasStmt != null)
					regionAliasStmt.close();
				
				if(cityAliasStmt != null)
					cityAliasStmt.close();
				
				if(connection!= null) {
					connection.close();
				}
				
			} catch (SQLException e) {
				logger.error("Error closing connections while updating goegraphical data cache", e);
			}
		}
	}
	
	private void buildRadixTree() {
		wordsTree = new ValuedRadixTree<Location>(3);
		
		ArrayList<Location> locations = new ArrayList<Location>();
		locations.addAll(GeographicalDataProvider.getInstance().getCountries());
		locations.addAll(GeographicalDataProvider.getInstance().getRegions());
		locations.addAll(GeographicalDataProvider.getInstance().getCities());
		
		for(Location loc : locations) {
			if(loc.getAllAliases().size() > 0) {
				for(String k : loc.getAllAliases()) {
					ValuedRadixTreeNode<Location> node = wordsTree.getOrCreateNode(k.trim());
					boolean isLocRegion = loc.type == LocationType.PROVINCE ? true : false;
					boolean nodeContainsSameRegion = false;
					boolean nodeContainsSameCity = false;
					for(Location l : node.value) {
						if(isLocRegion) {
							if(l.parent == loc)
								nodeContainsSameCity = true;
						} else {
							if(loc.parent == l)
								nodeContainsSameRegion = true;
						}
					}
					if(!isLocRegion && nodeContainsSameRegion) {
						node.value.remove(loc.parent);
						node.value.add(loc);
					} else if((isLocRegion && !nodeContainsSameCity) || (!isLocRegion && !nodeContainsSameRegion)) {
						wordsTree.addString(k.trim(), loc);
					}
				}
			}
		}
	}

	private void populateCountries() throws SQLException {
		ResultSet countryRset = countryStmt.executeQuery();
		while(countryRset.next()) {
			Country country = new Country();
			country.setNameAr(countryRset.getString("arabicName"));
			country.setNameEn(countryRset.getString("name"));
			country.setCode(countryRset.getString("code").toLowerCase());
			
			country.setCapitalId(countryRset.getInt("capitalId"));
			country.setCountryId(countryRset.getInt("countryId"));
			
			countries.put(country.getCode(), country);
		}
		countryRset.close();
		
		for(Country country : countries.values()) {
			populateAliases(countryAliasStmt, country, country.getCountryId());
			populateRegions(country);
		}
	}

	public void populateRegions(Country country) throws SQLException {
		regionStmt.setInt(1, country.getCountryId());
		ResultSet regionRset = regionStmt.executeQuery();
		while(regionRset.next()) {
			Region region = new Region();
			region.setRegionId(regionRset.getInt("regionId"));
			region.setNameAr(regionRset.getString("arabicName"));
			region.setNameEn(regionRset.getString("name"));
			String regionCode = regionRset.getString("code");
			if(regionCode != null) {
				region.setCode(regionCode.toLowerCase());
			}
			region.setTimeZone(regionRset.getString("timeZoneId"));
			region.setLatitude(regionRset.getDouble("latitude"));
			region.setLongitude(regionRset.getDouble("longitude"));
			region.setTimeDiff(regionRset.getInt("timeDiff"));
			region.setCountry(country);
			
			country.addRegion(region);
			if(region.getCode() != null)
				regions.put(region.getCode(), region);
		}
		regionRset.close();
		
		for(Region region : country) {
			populateAliases(regionAliasStmt, region, region.getRegionId());
			populateCities(region);
		}
	}

	public void populateCities(Region region) throws SQLException {
		cityStmt.setInt(1, region.getRegionId());
		ResultSet cityRset = cityStmt.executeQuery();
		while(cityRset.next()) {
			City city = new City();
			city.setCityId(cityRset.getInt("cityId"));
			city.setNameAr(cityRset.getString("arabicName"));
			city.setNameEn(cityRset.getString("name"));
			String cityCode = cityRset.getString("code");
			if(cityCode != null)
				city.setCode(cityCode.toLowerCase());
			city.setTimeZone(cityRset.getString("timeZoneId"));
			city.setLatitude(cityRset.getDouble("latitude"));
			city.setLongitude(cityRset.getDouble("longitude"));
			city.setTimeDiff(cityRset.getInt("timeDiff"));
			city.setCountry(region.getCountry());
			city.setRegion(region);
			
			region.addCity(city);
			if(city.getCode() != null)
				cities.put(city.getCode(), city);
			
			if(city.getCityId() == region.getCountry().getCapitalId())
				region.getCountry().setCapital(city);
		}
		cityRset.close();
		
		for(City city : region) {
			populateAliases(cityAliasStmt, city, city.getCityId());
		}
	}
	
	private void populateAliases(PreparedStatement stmt, Aliasable aliasable, int id) throws SQLException {
		stmt.setInt(1, id);
		ResultSet rset = stmt.executeQuery();
		
		while(rset.next()) {
			String lang = rset.getString("lang");
			String alias = rset.getString("alias");
			
			Hashtable<String, Set<String>> aliases = aliasable.getAliasesTable();
			Set<String> langAliases = aliases.get(lang);
			if(langAliases == null) {
				langAliases = new HashSet<>();
				aliases.put(lang, langAliases);
			}
			
			langAliases.add(alias);
		}
	}
	
	public List<Country> getCountries() {
		List<Country> res = null;
		
		rwLock.readLock().lock();
		try {
			res = new ArrayList<>(countries.values());
		} finally {
			rwLock.readLock().unlock();
		}
		
		return res;
	}
	
	public ValuedRadixTree<Location> getWordsTree() {
		
		ValuedRadixTree<Location> res = null;
		
		rwLock.readLock().lock();
		try {
			res = wordsTree;
		} finally {
			rwLock.readLock().unlock();
		}
		
		return res;
	}
	
	public List<City> getCities() {
		List<City> res = null;
		
		rwLock.readLock().lock();
		try {
			res = new ArrayList<>(cities.values());
		} finally {
			rwLock.readLock().unlock();
		}
		
		return res;
	}
	
	public List<Region> getRegions() {
		List<Region> res = null;
		
		rwLock.readLock().lock();
		try {
			res = new ArrayList<>(regions.values());
		} finally {
			rwLock.readLock().unlock();
		}
		
		return res;
	}
	
	public City getCityByCode(String cityCode) {
		City res = null;
		
		//if update is in progress, wait for the new version
		rwLock.readLock().lock();
		try {
			res = cities.get(cityCode);
		} finally {
			rwLock.readLock().unlock();
		}
		
		return res;
	}
	
	public Region getRegionByCode(String regionCode) {
		Region res = null;
		
		//if update is in progress, wait for the new version
		rwLock.readLock().lock();
		try {
			res = regions.get(regionCode);
		} finally {
			rwLock.readLock().unlock();
		}
		
		return res;
	}
	
	public Country getCountryByCode(String countryCode) {
		Country res = null;
		
		//if update is in progress, wait for the new version
		rwLock.readLock().lock();
		try {
			res = countries.get(countryCode);
		} finally {
			rwLock.readLock().unlock();
		}

		return res;
	}
	
	public static void main(String[] args) throws ConfigurationException {
		GeographicalDataProvider provider = new GeographicalDataProvider();
		provider.setConf(Crawler.initialize());
		provider.updateCache();
		
		for(Country country : provider.countries.values()) {
			System.out.print(country.getNameEn());
			System.out.print(" (");
			for(String alias : country.getAllAliases()) {
				System.out.print(alias);
				System.out.print(", ");
			}
			System.out.println(")");
			for(Region region : country) {
				System.out.print("--");
				System.out.print(region.getNameEn());
				System.out.print(" (");
				for(String alias : region.getAllAliases()) {
					System.out.print(alias);
					System.out.print(", ");
				}
				System.out.println(")");
				for(City city : region) {
					System.out.print("----");
					System.out.print(city.getNameEn());
					System.out.print(" (");
					for(String alias : city.getAllAliases()) {
						System.out.print(alias);
						System.out.print(", ");
					}
					System.out.println(")");
				}
			}
		}
	}
}
