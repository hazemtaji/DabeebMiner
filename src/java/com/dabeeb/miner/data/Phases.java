package com.dabeeb.miner.data;

public enum Phases {
	INJECT,		//Injected URLs should always get priority in cycle, next step is FETCH
	READY,		//Ready URLs are produced from previous depth levels, next step is FETCH
	FETCH,		//Download the said URLs, follow redirects, next step is PARSE
	PARSE,		//Parse downloaded content, next step is OUTLINK
	OUTLINK,	//Get URLs from parsed content, filter it and add it as READY, next step is INDEX
	INDEX,		//Run the various indexing filters one by one and index the item, next step is DONE
	DONE
}
