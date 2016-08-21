package com.dabeeb.miner.index.filter.classifier;

import java.util.LinkedList;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dabeeb.miner.data.model.Document;
import com.dabeeb.miner.data.model.Parse;
import com.dabeeb.miner.index.IndexFilterPlugin;

public class ClassifierFilter implements IndexFilterPlugin {
	public static Logger logger = LogManager.getFormatterLogger(ClassifierFilter.class);
	private Configuration conf;
	private TopicClassifier classifier;
	
	private static final String[] INCIDENT_KEYWORDS = {"مصرع", "تحرش", "هتك" , "حريق", "شرطة", "حادث", "وفاة", "اغتصاب", "اباحية", "مقتل", "حبس", "إعدام", "سرقة", "سطو", "عصابة", "تصادم", "انفجار", "مخدرات", "القبض", "الدعارة", "الرذيلة"};
	private static final String[] EDUCATION_KEYWORDS = {"مدرسون", "مدرسات", "مدرسين", "تلميذ", "تلميذة", "تلميذات", "تلاميذ", "تلامذة", "حضانة", "حضانات", "روضات", "مدرس", "مدرسة", "مدرسات", "مدارس", "جامعة", "جامعات", "كلية", "كليات", "معهد", "معاهد", "طالب", "طلاب", "طالبات", "طالبة"};
	
	public ClassifierFilter() {
	}

	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;

		classifier = new TopicClassifier(conf);
	}

	@Override
	public boolean filter(Document doc) {
		
		String cat = doc.getMetadata().get("category");
		if(cat == null) {
			cat = doc.getMetadata().get("categories");
			if(cat != null)
				doc.getMetadata().remove("categories");
		}
		
		if(cat != null) {
			//this document was tagged, and being reindexed
			if(cat.contains("auto")) {
				cat = null;
			} else {
				if(!cat.contains("manual")) {
					cat += " manual";
				}
				doc.getMetadata().put("category", cat);
				return true;
			}
		}
		
		if(cat == null) {
			if(doc.getParsedContent() != null && doc.getParsedContent().getTitle() != null) {
				String[] words = doc.getParsedContent().getTitle().split("\\P{L}+");
				for(String incidentKeyword : INCIDENT_KEYWORDS) {
					for(String word : words) {
						if(word.equals(incidentKeyword)) {
							cat = "incident auto";
							doc.getMetadata().put("category", cat);
							return true;
						}
					}
				}
				
				for(String educationKeyword : EDUCATION_KEYWORDS) {
					for(String word : words) {
						if(word.equals(educationKeyword)) {
							cat = "education auto";
							doc.getMetadata().put("category", cat);
							return true;
						}
					}
				}
			}
		}
		
		if(cat == null)
		{
			cat = classifier.classifyURL(doc.getFinalUrl());
			if(cat == null)
				cat = classifier.classifyURL(doc.getUrl());
			
		}
		
		if (cat != null) {
			if(!cat.contains("manual")) {
				cat += " manual";
			}
		} else {
			cat = classifier.classify(getText(doc), doc.getParsedContent().getLanguage());
		} 
		
		if (cat != null) {
			cat += " auto";
		} else {
			cat = "";
		}
		
		
		
		doc.getMetadata().put("category", cat);
		
		return true;
	}

	private LinkedList<String> getText(Document doc) {
		LinkedList<String> text = new LinkedList<>();
		
		Parse parse = doc.getParsedContent();
		if(parse != null) {
			if(parse.getText() != null)
				text.add(parse.getText());
			
			if(parse.getTitle() != null)
				text.add(parse.getTitle());
		}
		
		return text;
	}
}
