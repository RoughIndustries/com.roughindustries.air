package com.roughindustries.air.scrapers;

import java.net.URL;

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
				Document page = Jsoup.parse(new URL(geoHackURL), 30000);
				geo = page.select("span[class*=geo] > span");
				//got the page so we can quit
				quit = true;
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
