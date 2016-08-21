package com.dabeeb.miner.data.provider.geo;

public class City extends Location {
	
	private String timeZone;
	private Country country;
	private double latitude, longitude;
	private int timeDiff, cityId;
	private Region region;
	
	public City() {
		this.type = LocationType.CITY;
	}

	public String getTimeZone() {
		return timeZone;
	}

	protected void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public Country getCountry() {
		return country;
	}

	protected void setCountry(Country country) {
		this.country = country;
	}

	public double getLatitude() {
		return latitude;
	}

	protected void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	protected void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public int getTimeDiff() {
		return timeDiff;
	}

	protected void setTimeDiff(int timeDiff) {
		this.timeDiff = timeDiff;
	}

	public Region getRegion() {
		return region;
	}

	protected void setRegion(Region region) {
		this.region = region;
		this.parent = region;
	}
	
	public int getCityId() {
		return cityId;
	}
	
	protected void setCityId(int cityId) {
		this.cityId = cityId;
		this.locationId = cityId;
	}
}
