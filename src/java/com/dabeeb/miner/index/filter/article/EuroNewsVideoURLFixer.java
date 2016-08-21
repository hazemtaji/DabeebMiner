package com.dabeeb.miner.index.filter.article;

public class EuroNewsVideoURLFixer implements VideoURLFixer {

	@Override
	public String fixVideoURL(String url) {
		return "http://video.euronews.com/" + url + ".flv";
	}

	@Override
	public String fixThumbnailURL(String url) {
		return url;
	}
	
}
