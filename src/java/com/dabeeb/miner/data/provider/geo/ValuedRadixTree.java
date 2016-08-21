package com.dabeeb.miner.data.provider.geo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class ValuedRadixTree<T>
{
	ValuedRadixTreeNode<T> root;
	private int maxPrefixCount = 0;
	
	public ValuedRadixTree(int maxPrefixCount)
	{
		root = new ValuedRadixTreeNode<T>();
		this.maxPrefixCount = maxPrefixCount;
	}
	
	public void addString(String str, T value)
	{
		ValuedRadixTreeNode<T> lastNode = root;
		for(int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			lastNode = lastNode.addChild(c);
		}
		
		if(lastNode.value == null) {
			lastNode.value = new HashSet<T>();
			lastNode.value.add(value);
		} else {
			lastNode.value.add(value);
		}
	}
	
	public ValuedRadixTreeNode<T> getOrCreateNode(String str) {
		
		ValuedRadixTreeNode<T> lastNode = root;
		for(int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			lastNode = lastNode.addChild(c);
		}
		
		if(lastNode.value == null) {
			lastNode.value = new HashSet<T>();
		}
		
		return lastNode;
	}
	
	public void addValue(ValuedRadixTreeNode<T> node, T value) {
		if(node.value == null) {
			node.value = new HashSet<T>();
			node.value.add(value);
		} else {
			node.value.add(value);
		}
	}
	
	public MatchResult<Set<T>> findLargestWord(String str)
	{
		char[] buffer = str.toCharArray();
		return findLargestWord(buffer, 0, buffer.length);
	}
	
	public MatchResult<Set<T>> findLargestWord(char[] buffer, int offset, int length)
	{
		MatchResult<Set<T>> res = new MatchResult<Set<T>>();
		
		for(int skip = 0; skip < maxPrefixCount; skip++)
		{
			ValuedRadixTreeNode<T> lastNode = root;
			for(int i = skip + offset; i < length; i++)
			{
				char c = buffer[i];
				lastNode = lastNode.get(c);
				if(lastNode == null)
					break;
				
				if(lastNode.value != null)
				{
					//make sure new result is larger
					if(i - skip > res.size())
					{
						res.start = skip;
						res.end = i;
						res.value = lastNode.value;
					}
				}
			}
		}
		
		return res;
	}
	
	public List<MatchResult<Set<T>>> findAllWords(String text) {
		return findAllWords(text.toCharArray());
	}
	public List<MatchResult<Set<T>>> findAllWords(char[] buffer) {
		
		ArrayList<MatchResult<Set<T>>> results = new ArrayList<MatchResult<Set<T>>>();
		
		ValuedRadixTreeNode<T> lastNode = root;
		ValuedRadixTreeNode<T> prevNode = root;
		MatchResult<Set<T>> lastResult = new MatchResult<Set<T>>();
				
		int i = 0;
		int start = -1;
		int wordIndex = 0;
		boolean ignoreAl = false;
		
		char firstLetter = '\0';
		char secondLetter = '\0';
		char lastLetter = '\0';
		boolean endOfWord = false;
		
		for(i = 0; i < buffer.length; i++) {
			
			char c = buffer[i];
			if(c == ' ')
				endOfWord = true;
			else
				endOfWord = false;
			
			if(ignoreAl) {
				c = 'ا';
			}
			
			prevNode = lastNode;
			lastNode = lastNode.get(c);
			
			if(wordIndex == 0)
				firstLetter = c;
			if(wordIndex == 1)
				secondLetter = c;

			ignoreAl = false;
			if(lastNode == null) {
				
				if(wordIndex > 3) {
					while(buffer[i] != ' ' && i < buffer.length-1)
						i++;
					wordIndex = -1;
				} else if(firstLetter == 'ب' || firstLetter == 'و') {
					i -= (wordIndex);
					firstLetter = '\0';
					secondLetter = '\0';
				} else if (firstLetter == 'ل' && secondLetter == 'ل') {
					i -= (wordIndex-1);
					ignoreAl = true;
					firstLetter = '\0';
					secondLetter = '\0';
				} else if(firstLetter == 'ا' && secondLetter == 'ل') {
					i -= (wordIndex-1);
					firstLetter = '\0';
					secondLetter = '\0';
				}
				//i++;
				
				if(lastResult.value != null) {
					if(endOfWord && lastLetter == prevNode.character && prevNode.value != null)
						results.add(lastResult);
				}
				
				start = -1;
				lastResult = new MatchResult<Set<T>>();
				
				lastNode = root;
				wordIndex++;
				continue;
			} else {
				if(start == -1)
					start = i;
				lastLetter = c;
			}
			
			if(lastNode.value != null)
			{
				//make sure new result is larger
				if(i - start > lastResult.size())
				{
					lastResult.start = start;
					lastResult.end = i;
					lastResult.value = lastNode.value;
				}
			}
			
			wordIndex++;
		}
		
		if(lastResult.value != null)
			results.add(lastResult);
		
		return results;
	}
	
	public static void main(String[] args)
	{
		ValuedRadixTree<String> tree = new ValuedRadixTree<String>(3);
			
		tree.addString("abcdef", "1");
		tree.addString("abc", "2");
		tree.addString("2abc", "3");
		tree.addString("2abc", "4");
		tree.addString("zxcv", "5");
		tree.addString("zxcv", "6");
		tree.addString("zxcv", "7");
		
		ValuedRadixTreeNode<String> r = tree.root;
		Stack<ValuedRadixTreeNode<String>> s = new Stack<ValuedRadixTreeNode<String>>();
		s.add(r);
		System.out.println("----tree------");
		while(!s.empty()) {
			r = s.pop();
			s.addAll(r.getChildren());
			System.out.println(r.value);
			
		}
		System.out.println("----tree------");
		
		MatchResult<Set<String>> res = tree.findLargestWord("12abc abcdef32 zxcv");
		
		System.out.println(res.start + " " + res.end + " " + res.value);
	}
}