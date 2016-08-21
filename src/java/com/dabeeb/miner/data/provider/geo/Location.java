package com.dabeeb.miner.data.provider.geo;

public class Location extends Aliasable {
	
	protected int locationId;
	private String nameEn, nameAr, code;
	protected LocationType type;
	protected Location parent;
	
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("{");
		str.append(nameEn);
		str.append(", ");
		str.append(nameAr);
		if(parent != null) {
			str.append(" -- ");
			str.append(parent.nameEn);
			if(parent.parent != null) {
				str.append(" -- ");
				str.append(parent.parent.nameEn);
			}
		}
		str.append("}");
		return str.toString();
	}

	public int getLocationId() {
		return locationId;
	}

	public void setLocationId(int locationId) {
		this.locationId = locationId;
	}

	public String getNameEn() {
		return nameEn;
	}

	public void setNameEn(String nameEn) {
		this.nameEn = nameEn;
	}

	public String getNameAr() {
		return nameAr;
	}

	public void setNameAr(String nameAr) {
		this.nameAr = nameAr;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public LocationType getType() {
		return type;
	}

	public void setType(LocationType type) {
		this.type = type;
	}

	public Location getParent() {
		return parent;
	}

	public void setParent(Location parent) {
		this.parent = parent;
	}	

}
