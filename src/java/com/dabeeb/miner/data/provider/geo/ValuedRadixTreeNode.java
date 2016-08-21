package com.dabeeb.miner.data.provider.geo;
import java.util.Collections;
import java.util.Set;
import java.util.Vector;

public class ValuedRadixTreeNode<T> implements Comparable<ValuedRadixTreeNode<T>>
{
	protected char character;
	private Vector<ValuedRadixTreeNode<T>> children = new Vector<ValuedRadixTreeNode<T>>();
	protected Set<T> value;
	
	public ValuedRadixTreeNode()
	{
	}
	
	public ValuedRadixTreeNode(char character)
	{
		this.character = character;
	}
	
	public Vector<ValuedRadixTreeNode<T>> getChildren() {
		return children;
	}
	
	public ValuedRadixTreeNode<T> get(char c)
	{
		int low = 0;
		int high = children.size() - 1;
		int mid;
		while( low <= high )
		{
			mid = ( low + high ) / 2;
			
			if( children.get(mid).compareTo( c ) < 0 )
				low = mid + 1;
			else if( children.get(mid).compareTo( c ) > 0 )
				high = mid - 1;
			else
				return children.get(mid);
		}
		
		return null;
	}
	
	public ValuedRadixTreeNode<T> addChild(char c)
	{
		
		ValuedRadixTreeNode<T> res = get(c);
		if(res == null)
		{
			res = new ValuedRadixTreeNode<T>(c);
			children.add(res);
			Collections.sort(children);
		}
		return res;
	}
	
	public ValuedRadixTreeNode<T> addChild(ValuedRadixTreeNode<T> node)
	{
		ValuedRadixTreeNode<T> res = get(node.character);
		if(res != null)
		{
			res = node;
			children.add(node);
			Collections.sort(children);
		}
		return res;
	}
	
	public int compareTo(ValuedRadixTreeNode<T> o)
	{
		return character - o.character ;
	}
	
	public int compareTo(char c)
	{
		return character - c;
	}
}