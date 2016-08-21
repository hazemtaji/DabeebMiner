package com.dabeeb.miner.net.urlfilter.regex;

import java.util.List;

import com.dabeeb.miner.cache.Cachable;
import com.dabeeb.miner.conf.Configurable;

public interface RegexRulesLoader extends Configurable, Cachable  {
	
	public abstract List<RegexRule> getRules();
}
