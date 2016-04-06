package com.roughindustries.air.scrap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class DocumentRunnable implements Runnable {

	final static Logger logger = Logger.getLogger(DocumentRunnable.class);

	String wikiUrl;
	
	public DocumentRunnable(String wikiURL){
		this.wikiUrl = wikiURL;
	}

	@Override
	public void run() {
		Document d;
		try {
			d = Jsoup.parse(new URL("https://en.wikipedia.org" + this.wikiUrl), 30000);
			logger.debug(d.title());
			
		} catch (MalformedURLException e) {
			logger.debug(this.wikiUrl);
			e.printStackTrace();
		} catch (IOException e) {
			logger.debug(this.wikiUrl);
			e.printStackTrace();
		} finally {
			d = null;
		}
				
	}
	
}
