package com.dabeeb.miner.data.provider.geo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Region extends Location implements Iterable<City>{
	private String timeZone;
	private Double latitude, longitude;
	private int timeDiff, regionId;
	private Country country;
	private List<City> cities;
	
	public Region() {
		cities = new ArrayList<>();
		this.type = LocationType.PROVINCE;
	}

	public String getTimeZone() {
		return timeZone;
	}

	protected void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public Double getLatitude() {
		return latitude;
	}

	protected void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	protected void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public int getTimeDiff() {
		return timeDiff;
	}

	protected void setTimeDiff(int timeDiff) {
		this.timeDiff = timeDiff;
	}

	public Country getCountry() {
		return country;
	}

	protected void setCountry(Country country) {
		this.country = country;
		this.parent = country;
	}
	
	protected void addCity(City ciy) {
		cities.add(ciy);
	}
	
	public List<City> getCities() {
		return Collections.unmodifiableList(cities);
	}
	
	protected int getRegionId() {
		return regionId;
	}
	
	protected void setRegionId(int regionId) {
		this.regionId = regionId;
		this.locationId = regionId;
	}

	@Override
	public Iterator<City> iterator() {
		return getCities().iterator();
	}
}
