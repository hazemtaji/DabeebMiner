package com.dabeeb.miner.index.filter.classifier;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;

public class VectorGenerator
{
	public static void main(String[] args) throws IOException
	{
		try
		{
			PrintWriter outFile = new PrintWriter(new OutputStreamWriter(new FileOutputStream("vector.txt"), "UTF8"));
			/*outFile.println("@relation news");
			
			outFile.println("@attribute text string");
			outFile.println("@attribute class {Business,Culture_Arts,Health,Politics,Religion,Science_Technology,Sports}");
			outFile.println("@data");*/
			File docsDir = new File("C:\\ExtractedText");
			Hashtable<String, WordFreq> wordsTable = new Hashtable<String, WordFreq>();
			for(File categoryDir : docsDir.listFiles())
			{
				//String catName = categoryDir.getName().replace(" ", "").replace("&", "_");
				for(File docFile : categoryDir.listFiles())
				{
					String doc = new String(readFile(docFile.toString()), "UTF-8");
					LinkedList<String> words = docToWords(doc);
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
				}
			}
				
			LinkedList<WordFreq> freqs = new LinkedList<WordFreq>(wordsTable.values());
			Collections.sort(freqs);
			
			LinkedList<WordFreq> finalWords = new LinkedList<WordFreq>();
			for(WordFreq freq : freqs)
			{
				if(freq.freq < 10)
					break;
				if(freq.word.length() > 3)
					finalWords.add(freq);
			}
			
			for(WordFreq freq : finalWords)
			{
				outFile.print(freq.word + " ");
			}
			outFile.close();
		}
		catch(Exception exp)
		{
			exp.printStackTrace();
		}
		
		/*
		String text = new String(readFile("sample.txt"), "UTF-8");
		
        TextCategorizer guesser = new TextCategorizer();
        guesser.setConfFile("myconf.conf");
        System.out.println(guesser.categorize(text));*/
     }
	
	public static LinkedList<String> docToWords(String doc)
	{
		LinkedList<String> result = new LinkedList<String>();
		StringBuffer word = new StringBuffer();
		for(int i = 0; i < doc.length(); i++)
		{
			char c = doc.charAt(i);
			if(c == ' ' || c == '\n')
			{
				if(word.length() > 0)
				{
					result.add(word.toString());
					word = new StringBuffer();
				}
			}
			else if((c >= 'A' && c <= 'Z') ||  (c >= 'a' && c <= 'z') || (c >= 'ء' && c <= 'ي'))
			{
				if(c == 'أ' || c == 'إ' || c == 'آ')
					c = 'ا';
				else if(c == 'ؤ' || c == 'ئ')
					c = 'ء';
				word.append(c);
			}
		}
		
		return result;
	}
	
	public static byte[] readFile(String fileName)
	{
		ByteArrayOutputStream bytes = null;
		try
		{
			FileInputStream inFile = new FileInputStream(fileName);
			bytes = new ByteArrayOutputStream(inFile.available());
			byte[] buffer = new byte[256];
			int bytesRead = 0;
			while (true)
            {
                bytesRead = inFile.read(buffer);
                if (bytesRead == -1)
                    break;
                bytes.write(buffer, 0, bytesRead);
            }
			inFile.close();
		}
		catch (IOException e) {
		}
		return bytes.toByteArray();
	}
}
