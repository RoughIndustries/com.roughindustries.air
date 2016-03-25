package com.roughindustries.air.scrapers;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.roughindustries.air.resources.GlobalProperties;

/**
 * @author roughindustries
 *
 */
public class GeoHackScraper{

	/**
	 * 
	 */
	final static Logger logger = Logger.getLogger(GeoHackScraper.class);

	/**
	 * 
	 */
	GlobalProperties Props = GlobalProperties.getInstance();

	public Elements parseGeoHackPageForLatLong(String geoHackURL) {
		boolean quit = false;
		int max_attempts = 5;
		int attempts = 0;
		Elements geo = null;
		while (!quit) {
			try {
				Document page = Jsoup.connect(geoHackURL).get();
				//got the page so we can quit
				quit = true;
				geo = page.select("span[class*=geo] > span");
				
				page = null;
			} catch (Exception e) {
				attempts++;
				if (attempts > max_attempts) {
					quit = true;
				}
				logger.debug("Failed to get page "+geoHackURL+". Trying again.");
			}
		}
		return geo;

	}
}
