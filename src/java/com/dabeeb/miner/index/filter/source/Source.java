package com.dabeeb.miner.index.filter.source;

import java.util.ArrayList;
import java.util.List;

public class Source {
	private String nameAr, nameEn, country, region, city, mainUrl;
	private List<String> aliases;

	public Source() {
		aliases = new ArrayList<>();
	}
	
	public String getNameAr() {
		return nameAr;
	}

	protected void setNameAr(String nameAr) {
		this.nameAr = nameAr;
	}

	public String getNameEn() {
		return nameEn;
	}

	protected void setNameEn(String nameEn) {
		this.nameEn = nameEn;
	}

	public String getCountry() {
		return country;
	}

	protected void setCountry(String country) {
		this.country = country;
	}
	
	public String getRegion() {
		return region;
	}
	
	protected void setRegion(String region) {
		this.region = region;
	}

	public String getCity() {
		return city;
	}

	protected void setCity(String city) {
		this.city = city;
	}
	
	public String getMainUrl() {
		return mainUrl;
	}
	
	protected void setMainUrl(String mainUrl) {
		this.mainUrl = mainUrl;
	}

	public List<String> getAliases() {
		return aliases;
	}

	protected void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}

	public void addAlias(String alias) {
		aliases.add(alias);
	}
	
	@Override
	public String toString() {
		return nameEn + " (" + mainUrl + ')';
	}
}