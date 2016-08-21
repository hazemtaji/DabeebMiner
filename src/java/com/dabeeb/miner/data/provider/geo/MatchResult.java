package com.dabeeb.miner.data.provider.geo;
public class MatchResult<T>
{
	public int start;
	public int end;
	public T value;
	
	public int size()
	{
		return end - start + 1;
	}
}