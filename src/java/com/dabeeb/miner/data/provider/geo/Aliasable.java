package com.dabeeb.miner.data.provider.geo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public abstract class Aliasable {
	private Hashtable<String, Set<String>> aliases;

	public Aliasable() {
		aliases= new Hashtable<>();
	}

	protected Hashtable<String, Set<String>> getAliasesTable() {
		return aliases;
	}
	
	public Set<String> getAllAliases() {
		HashSet<String> res = new HashSet<>();
		
		for(Set<String> langAliases : aliases.values()) {
			res.addAll(langAliases);
		}
		
		return res;
	}
	
	public Set<String> getAliasesByLanguage(String lang) {
		return Collections.unmodifiableSet(aliases.get(lang));
	}
}
