package com.dabeeb.miner.data.model;

import com.dabeeb.miner.data.model.Outlink;

public class Parse {
	String text;
	String title;
	Outlink[] outlinks;
	String language;
	
	String[] lines = null;
	String[][] words = null;
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public Outlink[] getOutlinks() {
		return outlinks;
	}
	
	public void setOutlinks(Outlink[] outlinks) {
		this.outlinks = outlinks;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public void setLanguage(String language) {
		this.language = language;
	}

	public String[] getLines() {
		if(lines == null)
			lines = text.split("\n");
		return lines;
	}
	
	public String[][] getWords() {
		if(words == null) {
			String[] lines = getLines();
			words = new String[lines.length][];
			for(int i = 0; i < lines.length; i++) {
				String line = lines[i];
				words[i] = line.split("\\P{L}+");
			}
		}
		return words;
	}
}
