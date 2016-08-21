package com.dabeeb.miner.index.filter.classifier;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.LinkedList;

public class SVMDataBuilder
{
	public static void main(String[] args)
	{
		try
		{
			//PrintWriter outFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream());
			String vectorString = new String(VectorGenerator.readFile("vector.txt"), "UTF-8");
			String[] wordsVector = vectorString.split(" ");
			
			
			File docsDir = new File("C:\\ExtractedText");
			LinkedList<DocumentCluster> clusters = new LinkedList<DocumentCluster>();
			for(File categoryDir : docsDir.listFiles())
			{
				String catName = categoryDir.getName().replace(" ", "").replace("&", "_");
				DocumentCluster cluster = new DocumentCluster(catName);
				clusters.add(cluster);
				for(File docFile : categoryDir.listFiles())
				{
					StringBuffer buff = new StringBuffer();
					Hashtable<String, WordFreq> wordsTable = new Hashtable<String, WordFreq>();
					String doc = new String(VectorGenerator.readFile(docFile.toString()), "UTF-8");
					
					LinkedList<String> words = VectorGenerator.docToWords(doc);
					for(String word : words)
					{
						WordFreq freq = wordsTable.get(word);
						if(freq == null)
						{
							freq = new WordFreq(word);
							wordsTable.put(freq.word, freq);
						}
						freq.freq++;
					}
					
					for(int i = 0; i < wordsVector.length; i++)
					{
						WordFreq freq = wordsTable.get(wordsVector[i]);
						if(freq != null)
						{
							buff.append(i + 1);
							buff.append(':');
							buff.append(freq.freq);
							buff.append(' ');
						}
					}
					if(buff.length() > 1)
					{
						buff.deleteCharAt(buff.length() - 1);
						cluster.docs.add(buff.toString());
					}
				}
			}
			
			for(DocumentCluster positiveCluster : clusters)
			{
				System.out.println(positiveCluster.clusterName);
				PrintWriter trainFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(positiveCluster.clusterName + ".train.txt")));
				PrintWriter testFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream(positiveCluster.clusterName + ".test.txt")));
				int i = 0;
				for(String doc : positiveCluster.docs)
				{
					i++;
					if(i < (positiveCluster.docs.size() / 5))
					{
						testFile.print("1 ");
						testFile.println(doc);
					}
					else
					{
						trainFile.print("1 ");
						trainFile.println(doc);
					}
				}
				int posSize = positiveCluster.docs.size();
				int negClustersNum = clusters.size() - 1;
				int perCluster = posSize / negClustersNum;
				System.out.println(perCluster);
				for(DocumentCluster negativeCluster : clusters)
				{
					if(positiveCluster != negativeCluster)
					{
						i = 0;
						for(String doc : negativeCluster.docs)
						{
							if(i == perCluster)
								break;
							i++;
							if(i < (perCluster / 5))
							{
								testFile.print("-1 ");
								testFile.println(doc);
							}
							else
							{
								trainFile.print("-1 ");
								trainFile.println(doc);
							}
						}
					}
				}
				trainFile.close();
				testFile.close();
			}
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
				
	public static class DocumentCluster
	{
		LinkedList<String> docs = new LinkedList<String>();
		String clusterName;
		
		public DocumentCluster(String name)
		{
			this.clusterName = name;
		}
	}
}
