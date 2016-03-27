package com.roughindustries.air.scrapers;

import java.net.URL;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.roughindustries.air.App;
import com.roughindustries.air.model.Airlines;
import com.roughindustries.air.resources.GlobalProperties;

public class AirlinePageForAirportPageParser implements Runnable{

	/**
	 * 
	 */
	final static Logger logger = Logger.getLogger(AirlinePageForAirportPageParser.class);

	/**
	 * 
	 */
	GlobalProperties Props = GlobalProperties.getInstance();

	private App root;
	private AirportPageForAirportInfoParser caller;
	int recordNumber;
	Airlines al;

	public AirlinePageForAirportPageParser(App root, AirportPageForAirportInfoParser caller, int recordNumber, Airlines al) {
		this.root = root;
		this.caller = caller;
		this.recordNumber = recordNumber;
		this.al = al;
	}

	@Override
	public void run() {
		boolean quit = false;
		int max_attempts = 5;
		int attempts = 0;
		while (!quit) {
			try {
				Document page = Jsoup.parse(new URL("https://en.wikipedia.org" + al.getWikiUrl()), 10000);
				if (al.getIataCode() == null || "".equals(al.getIataCode())) {
					Elements iata_code = page
							.select("[href*=/wiki/IATA_airline_designator]");
					if (iata_code != null & !iata_code.isEmpty()) {
						al.setIataCode(iata_code.text().replaceAll("\\P{L}", " "));
					}
					iata_code = null;
				}
				
				quit = true;
				page = null;
				logger.debug(al.getIataCode() + " " + al.getName() + " airline Page Processed");
			} catch (Exception e) {
				attempts++;
				if (attempts > max_attempts) {
					quit = true;
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				logger.debug(
						"Failed to parse page " + "https://en.wikipedia.org" + al.getWikiUrl() + ". Trying again.");
			}
		}
		root.updateAirline(recordNumber, al);
		caller.airlineUpdateDone();
	}
	
	
}
