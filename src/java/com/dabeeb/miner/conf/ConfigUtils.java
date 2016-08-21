package com.dabeeb.miner.conf;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;

public class ConfigUtils {
	
	public static Map<String, String> getMap(Configuration conf, String root, String keyName, String valueName) {
		Map<String, String> map = new LinkedHashMap<>();
		 Configuration subset = conf.subset(root);
		 
		 String[] keys = subset.getStringArray("[@" + keyName + "]");
		 String[] values = subset.getStringArray("[@" + valueName + "]");
		 
		 for(int i = 0; i < keys.length; i++) {
			 map.put(keys[i], values[i]);
		 }
		 
		 return map;
	}
	
}
