package com.dabeeb.miner.index.filter.classifier;
public class WordFreq implements Comparable<WordFreq>
{
	int freq = 0;
	String word;

	public WordFreq(String word) 
	{
		this.word = word;
	}

	public int compareTo(WordFreq o) 
	{
		return o.freq - freq;
	}

	public String toString()
	{
		return word;
	}
}