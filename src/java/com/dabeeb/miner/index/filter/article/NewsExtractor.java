package com.dabeeb.miner.index.filter.article;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class NewsExtractor
{
	public static final char[] ENDS = {'.','?','!', ':'};
	

	public static final String[] authors = { "توماس فريدمان", "جدعون ليفي", "إبراهيم الأمين", "طلال سلمان", "فهمي هويدي", "ابراهيم الأمين", "ابراهيم الامين", "إبراهيم الامين"  };
	public static final String[] authorTags = { "author:friedman", "author:gideon.levy", "author:ibrahim.alamin", "author:talal.salman", "author:howeidy", "author:ibrahim.alamin", "author:ibrahim.alamin", "author:ibrahim.alamin" };
	
	public static String[][] monthNames = {
		{"Jan", "January", "يناير", "كانون الثاني"},
		{"Feb", "February", "فبراير", "شباط"},
		{"Mar", "March", "مارس", "آذار"},
		{"Apr", "April", "أبريل", "نيسان"},
		{"May", "May", "مايو", "أيار"},
		{"Jun", "June", "يونيو", "حزيران"},
		{"Jul", "July", "يوليو", "تموز"},
		{"Aug", "August", "أغسطس", "آب"},
		{"Sep", "September", "سبتمبر", "أيلول"},
		{"Oct", "October", "اكتوبر", "تشرين الأول"},
		{"Nov", "November", "نوفمبر", "تشرين الثاني"},
		{"Dec", "December", "ديسمبر", "كانون الأول"}
	};
	
	public static String[] datePatterns = 
	{
		"\\d{1,2}.{1,3}\\d{1,2}.{1,3}\\d\\d", //dd.mm.yyyy, mm.dd.yyyy, dd.mm.yy, mm.dd.yy
		"\\d{1,2}.{1,3}mmm.{1,3}\\d\\d", //dd.mmm.yyyy, dd.mmm.yy
		"\\d{1,2}.{1,3}mmmm.{1,3}\\d\\d", //dd.mmmm.yyyy, dd.mmmm.yy
		"mmm.{1,3}\\d{1,2}.{1,3}\\d\\d", //mmm.dd.yyyy, mmm.dd.yy
		"mmmm.{1,3}\\d{1,2}.{1,3}\\d\\d", //mmmm.dd.yyyy, mmmm.dd.yy
	};
	
	public static LinkedList<Pattern> regexDatePatterns; 
	
	public static final Pattern videoTagPattern = Pattern.compile("<video file=\"(.*?)\" thumbnail=\"(.*?)\" />");
	public static final Pattern imageTagPattern = Pattern.compile("<img src=\"(.*?)\" />");
	public static final Pattern linkPattern = Pattern.compile("^.*?^");
	
	public NewsExtractor()
	{
		if(regexDatePatterns == null)
		{
			regexDatePatterns = new LinkedList<Pattern>();
			for(String datePattern : datePatterns)
			{
				if(datePattern.contains("mmmm"))
				{
					for(int i = 0; i < monthNames.length; i++)
					{
						regexDatePatterns.add(Pattern.compile(datePattern.replace("mmmm", monthNames[i][1])));
						regexDatePatterns.add(Pattern.compile(datePattern.replace("mmmm", monthNames[i][2])));
						regexDatePatterns.add(Pattern.compile(datePattern.replace("mmmm", monthNames[i][3])));
					}
				}
				else if(datePattern.contains("mmm"))
				{
					for(int i = 0; i < monthNames.length; i++)
					{
						regexDatePatterns.add(Pattern.compile(datePattern.replace("mmm", monthNames[i][0])));
					}
				}
				else
				{
					regexDatePatterns.add(Pattern.compile(datePattern));
				}
			}
		}
	}
	
	public ExtractionResult extract(String doc, String docTitle, String[] anchors)
	{
		doc = doc.replaceAll("\\n\\n","\n");
		
		ExtractionResult res = new ExtractionResult();
		res.title = docTitle;
		StringBuffer buf = new StringBuffer();
		
		int contentStart = doc.indexOf("//$$StartContent$$//");
		int contentEnd = doc.indexOf("//$$EndContent$$//");
		
		if(contentStart != -1 && contentEnd != -1)
		{
			if(contentStart > contentEnd)
			{
				int temp = contentStart;
				contentStart = contentEnd;
				contentEnd = temp - 2;  //compensate for the difference between //$$StartContent$$// and //$$EndContent$$//
			}
			
			res.article = doc.substring(contentStart + "//$$StartContent$$//".length(), contentEnd);
			res.comments = "";
			return res;
		}
	    String[] lines = doc.split("\\r?\\n");
	    
	    List<String> linesList = new LinkedList<>(Arrays.asList(lines));
	    
	    cleanLinks(linesList);
	    removeNulls(linesList);
	    
	    lines = linesList.toArray(new String[0]);
	    
	    /*for(int i = 0; i < lines.length; i++) {
	    	System.out.print(i);
	    	System.out.print("  -  ");
	    	System.out.println(lines[i]);
	    }*/
	    
		LineStats[] lineStats = new LineStats[lines.length];
		LinkedList<Integer> comments = new LinkedList<Integer>();
		//int comments = -1;
		int titleLineNo = -1;
		int titleMatchCount = 0;
		
		int anchorLineNo = -1;
		String anchorTitle = null;
		boolean headlineTitle = false;
		
		for(int lineNo = 0; lineNo < lines.length; lineNo++)
		{
			String line = lines[lineNo].replace("<.*?/>", "").replace("\u00A0", " ").trim();
			
			if(line.contains("//$$StartContent$$//")) {
				contentStart = lineNo + 1;
			}
			
			boolean headLine = false;
			if(line.startsWith("${") && line.endsWith("}$"))
			{
				headLine = true;
				line = line.substring(2, line.length() - 2).replace("^", "").trim();
			}
			
			if(lineNo > 0 && lines[lineNo - 1].equals("${}$")) {

				headLine = true;
			}
			
			String enhancedLine = line.replace(".", "").trim();
			String enhancedDocTitle = docTitle.replace(".", "").trim();
			
			if(line.length() > 5 && enhancedDocTitle.contains(enhancedLine) && line.contains(" "))
			{
				if(headlineTitle)
				{
					//accept only headline titles
					if(headLine && line.length() > titleMatchCount)
					{
						titleLineNo = lineNo;
						res.title = line;
						
						if(line.length() != docTitle.length())
							titleMatchCount = line.length();
					}
				}
				else if(headLine || line.length() > titleMatchCount)
				{
					if(headLine)
						headlineTitle = true;

					titleLineNo = lineNo;
					res.title = line;
					
					if(line.length() != docTitle.length())
						titleMatchCount = line.length();
				}
			}
			
			for(String anchor : anchors)
			{
				if(!anchor.isEmpty() && (line.startsWith(anchor) || line.endsWith(anchor)))
				{
					anchorLineNo = lineNo;
					anchorTitle = anchor;
				}
			}
			
			lineStats[lineNo] = new LineStats();
			checkSpecialLine(line, lineStats[lineNo]);
			
			if(!lineStats[lineNo].isTag){
				countWords(line, lineStats[lineNo]);
				//System.out.print(lineStats[lineNo].wordCount + " " + lineStats[lineNo].spaceCount);
				
				
				for(int i = 0; i < line.length(); i++)
				{
					for(char end : ENDS)
					{
						if(line.charAt(i) == end)
						{
							if((i > 0 && (line.charAt(i - 1) < '0' || line.charAt(i - 1) > '9')) || (i < (line.length() - 1) && (line.charAt(i + 1) < '0' || line.charAt(i + 1) > '9')))
							{
								lineStats[lineNo].punctuated = true;
								break;
							}
						}
					}
				}
				if(line.trim().endsWith("..."))
				{
					lineStats[lineNo].punctuated = false;
				}
			}
			//System.out.println(" " + lineStats[lineNo].punctuated);
			if(lineStats[lineNo].wordCount < 6 && ((line.contains("تعليق") || line.contains("comment") || line.contains("شارك"))))
			{
				comments.add(lineNo);
			}
		}
		
		if((titleLineNo == -1 || anchorLineNo - titleLineNo > 10) && anchorLineNo != -1 && !headlineTitle)
		{
			res.title = anchorTitle;
			titleLineNo = anchorLineNo;
		}
		
		//System.out.println(comments);
		int articleStart = 0;
		int articleEnd = 0;
		int lastSize = 0;
		int finalCommentsLine = 0;

		if(titleLineNo >= 0)
		{
			articleStart = titleLineNo;
		}
		
		if(contentStart != -1) {
			articleStart = contentStart;
		}
		
		for(int i = 0; i < comments.size(); i++)
		{
			if(comments.get(i) < articleStart)
			{
				comments.remove(i);
				i--;
			}
		}
		
		if(comments.size() == 0)
			comments.add(lineStats.length - 1);
		
		for(int commentsLine : comments)
		{
			int articleLimit = commentsLine;
			if(articleLimit == -1)
				articleLimit = lines.length - 1;
			
			articleEnd = articleLimit;
			
			if(titleLineNo == -1)
			{
				articleStart = 0;
				int bestCandidate = 0;
				int bestCandidateCount = 0;
				
				for(int i = 0; i < articleLimit; i++)
				{
					if((lineStats[i].punctuated && lineStats[i].wordCount > 15 || lineStats[i].wordCount > 30) && (lineStats[i].spaceCount - lineStats[i].wordCount) < 15)
					{
						if(lineStats[i].wordCount > bestCandidateCount)
							bestCandidate = i;
						
						if(i < lineStats.length - 2)
						{
							if((lineStats[i + 2].punctuated && lineStats[i + 2].wordCount > 12 || lineStats[i + 2].wordCount > 30) && (lineStats[i + 2].spaceCount - lineStats[i + 2].wordCount) < 15)
							{
								articleStart = i;
								break;
							}
						}
						if(i  < lineStats.length - 1)
						{
							if((lineStats[i + 1].punctuated && lineStats[i + 1].wordCount > 12 || lineStats[i + 1].wordCount > 30) && (lineStats[i + 1].spaceCount - lineStats[i + 1].wordCount) < 15)
							{
								articleStart = i;
								break;
							}
						}
						else
						{
							articleStart = i;
							break;
						}
					}
					if((lineStats[i].wordCount > 90 && (lineStats[i].spaceCount - lineStats[i].wordCount) < 15))
					{
						articleStart = i;
						break;
					}
				}
				
				if(articleStart == 0)
					articleStart = bestCandidate;
			}
			
			//int fallbackEnd = -1;
			
			for(int i = articleStart; i <= articleLimit  ; i++)
			{
				String line = lines[i].trim();
				if(line.startsWith("copyright") || line.startsWith("Copyright") || line.startsWith("حقوق الطبع محفوظة") || line.contains("All Rights Reserved") || line.startsWith("جميع الحقوق محفوظة"))
				{
					articleLimit = i;
					break;
				}
				if((line.contains("روابط") || line.contains("علاقة") || line.contains("صلة") || line.contains("إضافية")) && lineStats[i].wordCount < 5 )
				{
					if(isLookingCredible(articleStart, i, lineStats))
					{
						articleLimit = i;
						break;
					}
				}
				
			}
			
			boolean endFound = false;
			for(int i = articleLimit; i >= articleStart ; i--)
			{
				if(lineStats[i].punctuated && lineStats[i].wordCount > 5 || lineStats[i].wordCount > 30)
				{
					if(i - 2 >= 0)
					{
						if(lineStats[i - 1].punctuated  && lineStats[i - 1].wordCount > 15 || lineStats[i - 2].punctuated  && lineStats[i - 2].wordCount > 15)
						{
							if(isLookingCredible(articleStart, i, lineStats))
							{
								endFound = true;
								articleEnd = i;
								break;
							}
						}
					}
					else
					{
						if(isLookingCredible(articleStart, i, lineStats)) {
							endFound = true;
							articleEnd = i;
							break;
						}
					}
				}
			}
			
			if(!endFound)
			{
				if(isLookingCredible(articleStart, articleLimit, lineStats))
				{
					articleEnd = articleLimit - 1;
				}
				else
					articleEnd = articleStart;// + 2;
			}
			
			if(articleStart > 2 && titleLineNo == -1)
			{
				articleStart -= 2;
				if(containsDate(lines[articleStart]) || containsDate(lines[articleStart + 1]) )
				{
					if(articleStart > 1)
					{
						articleStart--;
					}
				}
			}
			
			if(articleStart < lineStats.length - 1 && lineStats[articleStart].spaceCount - lineStats[articleStart].wordCount > 15)
			{
				articleStart++;
			}
			
			if(articleStart < lineStats.length - 1 && lineStats[articleStart].spaceCount - lineStats[articleStart].wordCount > 15)
			{
				articleStart++;
			}
			
			if(articleEnd >= lines.length)
			{
				articleEnd = lines.length - 1;
			}
			
			int size = articleEnd - articleStart;
			if(size > lastSize)
			{
				finalCommentsLine = commentsLine;
			}
			if(size < lastSize)
			{
				break;
			}
			lastSize = size;
		}
		
		for(int a = 0; a < authors.length; a++)
		{
			if(res.title.contains(authors[a])) {
				res.author = authorTags[a];
				break;
			}
		}
		
	    boolean collapseSpace = false;
	    int realLineNo = 0;
		for(int i = articleStart; i <= articleEnd; i++)
		{
			String line = lines[i].trim();
			if(line.length() == 0)
			{
				if(collapseSpace)
					continue;
				collapseSpace = true;
			}
			
			if(res.author == null)
			{
				if(realLineNo < 4 || articleEnd - i < 2 )
				{
					for(int a = 0; a < authors.length; a++)
					{
						if(line.contains(authors[a]) && lineStats[i].wordCount < 7) {
							res.author = authorTags[a];
							break;
						}
					}
				}
			}
			
			if(skipLine(line))
			{
				continue;
			}
			else
				realLineNo++;
			String toAppend = line.replace("^", "");
			if(toAppend.startsWith("${"))
			{
				toAppend = toAppend.substring(2, toAppend.length() - 2);
			}
			buf.append(toAppend.trim());
			buf.append('\n');
		}
		
		res.article = buf.toString();
		buf = new StringBuffer();
		
		int badLines = 0;
		int commentsEnd = lines.length - 1;
		for(int i = articleEnd + 1; i < lines.length; i++)
		{
			if(lineStats[i].punctuated || lineStats[i].wordCount > 20)
			{
				badLines = 0;
			}
			else
			{
				badLines++;
			}
			
			if(badLines == 3)
			{
				commentsEnd = i - badLines;
				break;
			}
		}
		
		if(finalCommentsLine > 0)
		{
			for(int i = finalCommentsLine+1; i <= commentsEnd; i++)
			{
				buf.append(lines[i].replace("^", ""));
				buf.append('\n');
			}
		}
		
		res.comments = buf.toString();
		
		Matcher videoMatcher = videoTagPattern.matcher(res.article);
		if(videoMatcher.find())
		{
			res.videoFile = videoMatcher.group(1);
			res.videoThumbnail = videoMatcher.group(2);
		}
		
		Matcher imageMatcher = imageTagPattern.matcher(res.article);
		if(imageMatcher.find())
		{
			res.image = imageMatcher.group(1);
		}
		
		res.article = res.article.replaceAll("<img.*?>", "");
		res.article = res.article.replaceAll("<video.*?>", "");
		
	    return res;
	}

	private void removeNulls(List<String> lines) {
		for(int i = 0; i < lines.size(); i++) {
			if(lines.get(i) == null) {
				lines.remove(i);
				i--;
			}
		}
	}

	private void cleanLinks(List<String> lines) {
		
		int linkStart = -1;
		for(int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if(line.length() == 0)
				continue;
			
			if(linkStart == -1) {
				if(line.charAt(0) == '^') {
					//look for termination
					if(line.charAt(line.length() - 1) == '^')
						continue;
					
					int index = line.indexOf('^', 1);
					if(index != -1)
						continue;
					
					linkStart = i;
				}
			} else {
				int index = line.indexOf('^', 1);
				if(index != -1) {
					//end found, time to collapse!
					StringBuffer buff = new StringBuffer();
					for(int y = linkStart; y <= i; y++) {
						buff.append(lines.get(y));
						if(y != i) {
							buff.append(' ');
						}
						lines.set(y, null);
					}
					lines.set(linkStart, buff.toString());
					linkStart = -1;
				}
			}
		}
	}

	private void checkSpecialLine(String line, LineStats lineStats) {
		int lastChar = line.length() - 1;
		if(lastChar < 2)
			return;
		
		if(line.charAt(0) == '<' && line.charAt(lastChar) == '>' && line.charAt(lastChar - 1) == '/')
			lineStats.isTag = true;
		
		if(line.charAt(0) == '^' && line.charAt(lastChar) == '^')
			lineStats.isLink = true;
	}

	private boolean isLookingCredible(int articleStart, int articleEnd, LineStats[] lineStats)
	{
		int consecutiveSuspiciousLines = 0;
		int totalWords = 0;
		for(int i = articleStart; i <= articleEnd; i++)
		{
			if(lineStats[i].isLink || lineStats[i].isTag)
				continue;
			
			if(i != articleStart)
				totalWords += lineStats[i].wordCount;
			
			if( !lineStats[i].punctuated && lineStats[i].wordCount < 15 && lineStats[i].wordCount > 0)
				consecutiveSuspiciousLines++;
			else
				consecutiveSuspiciousLines = 0;
			
			if(consecutiveSuspiciousLines > 4)
			{
				return false;
			}
		}
		
		if(totalWords < 20)
			return false;
		
		return true;
	}

	private boolean skipLine(String line)
	{
		String lowerCase = line.toLowerCase();
		
		if(!line.matches(".*\\p{L}+.*"))
			return true;
		
		if(line.startsWith("^"))
			return true;
		
		//Long lines
		if(lowerCase.length() > 25)
		{
			//check flash
			if((lowerCase.contains("flash") || lowerCase.contains("silverlight") || lowerCase.contains("java"))  && lowerCase.contains("download"))
				return true;
		}
		//short lines
		else
		{
			if(!lowerCase.contains("^"))
			{
				if(lowerCase.contains("مواضيع إضافية") || lowerCase.contains("متعلقات"))
					return true;
				return false;
			}
			
			if(lowerCase.contains("facebook") 
				|| lowerCase.contains("twitter")
				|| lowerCase.contains("print")
				|| lowerCase.contains("send")
				|| lowerCase.contains("طباعة")
				|| lowerCase.contains("اطبع")
				|| lowerCase.contains("ارسال")
				|| lowerCase.contains("ارسل")
				|| lowerCase.contains("إرسال")
				|| lowerCase.contains("أرسل")
				|| lowerCase.contains("إطبع"))
				return true;
		}
		
		return false;
	}

	private boolean containsDate(String string)
	{
		for(Pattern regexDatePattern : regexDatePatterns)
		{
			if(regexDatePattern.matcher(string).find())
				return true;
		}
		return false;
	}

	private void countWords(String line, LineStats stats)
	{
		if(line.length() == 0)
		{
			stats.wordCount = 0;
			stats.spaceCount = 0;
			return;
		}
		
		boolean started = false;
		boolean lastWasWhite = false;
		int count = 0;
		int wsCount = 0;
		for(int i = 0; i < line.length(); i++)
		{
			char c = line.charAt(i);
			
			if(c == '^')
				continue;
			
			if(!isWhiteSpace(c))
				started = true;
			
			if(started)
			{
				if(!lastWasWhite && isWhiteSpace(c))
				{
					lastWasWhite = true;
					count++;
					wsCount++;
				}
				else if(!isWhiteSpace(c))
					lastWasWhite = false;
				else
					wsCount++;
			}
		}
		if(started)
			count++;
		stats.wordCount = count;
		stats.spaceCount = wsCount;
	}

	private boolean isWhiteSpace(char c)
	{
		if(c == ' ' || c == '\t' || c == '-' || c == (char)160)
		{
			return true;
		}
		return false;
	}

	/*private String looksValid(String line)
	{
		int consWS = 0;
		char[] arr = line.toCharArray();
		for(char c : arr)
		{
			if(c==' ' || c == '\t')
				consWS++;
			else
				consWS = 0;
			if(consWS == line.length() || consWS > 2)
				return null;
		}
		return line;
	}*/
	
	public static class ExtractionResult
	{
		public String article;
		public String comments;
		public String title;
		public String image;
		public String videoFile;
		public String videoThumbnail;
		public String author;
		
		@Override
		public String toString()
		{
			StringBuffer buff = new StringBuffer();
			
			buff.append("Author: ");
			buff.append(author);
			
			buff.append("\nImage: ");
			buff.append(image);
			
			buff.append("\nVideo File: ");
			buff.append(videoFile);
			
			buff.append("\nVideo Thumbnail: ");
			buff.append(videoThumbnail);
			
			buff.append("\nTitle: ");
			buff.append(title);

			buff.append("\n-------------------- start article ------------------- \n");
			buff.append(article);
			buff.append("\n -------------------- end article ------------------- \n");
			
			buff.append("-------------------- start comments ------------------- \n");
			buff.append(comments);
			buff.append("\n -------------------- end comments ------------------- \n");
			
			return buff.toString();
		}
	}
	
	private static class LineStats
	{
		public int wordCount;
		public int spaceCount;
		public boolean punctuated = false;
		public boolean isTag = false;
		public boolean isLink = false;
	}
}
