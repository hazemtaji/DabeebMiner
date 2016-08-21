package com.dabeeb.miner.data.provider.geo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Country extends Location implements Iterable<Region> {
	private City capital;
	private List<Region> regions;
	
	private int countryId, capitalId;
	
	
	public Country() {
		regions = new ArrayList<>();
		this.type = LocationType.COUNTRY;
		this.parent = null;
	}

	public City getCapital() {
		return capital;
	}

	protected void setCapital(City capital) {
		this.capital = capital;
	}
	
	protected void addRegion(Region region) {
		regions.add(region);
	}
	
	public List<Region> getRegions() {
		return Collections.unmodifiableList(regions);
	}
	
	protected int getCountryId() {
		return countryId;
	}

	protected void setCountryId(int countryId) {
		this.countryId = countryId;
		this.locationId = countryId;
	}

	protected int getCapitalId() {
		return capitalId;
	}

	protected void setCapitalId(int capitalId) {
		this.capitalId = capitalId;
	}

	@Override
	public Iterator<Region> iterator() {
		return getRegions().iterator();
	}
}
