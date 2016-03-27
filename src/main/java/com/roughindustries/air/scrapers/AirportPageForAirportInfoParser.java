package com.roughindustries.air.scrapers;

import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.roughindustries.air.App;
import com.roughindustries.air.model.Airlines;
import com.roughindustries.air.model.Airports;
import com.roughindustries.air.resources.GlobalProperties;

public class AirportPageForAirportInfoParser implements Runnable {

	/**
	 * 
	 */
	final static Logger logger = Logger.getLogger(AirportPageForAirportInfoParser.class);

	/**
	 * 
	 */
	GlobalProperties Props = GlobalProperties.getInstance();

	private App caller;
	int recordNumber;
	Airports ai;

	private Timer timer;
	private boolean completed = false;

	public AirportPageForAirportInfoParser(App caller, int recordNumber, Airports ai) {
		this.caller = caller;
		this.recordNumber = recordNumber;
		this.ai = ai;
	}

	@Override
	public void run() {
		boolean quit = false;
		int max_attempts = 5;
		int attempts = 0;
		while (!quit) {
			try {
				Document page = Jsoup.parse(new URL("https://en.wikipedia.org" + ai.getWikiUrl()), 10000);
				if (ai.getIataCode() == null || "".equals(ai.getIataCode())) {
					Elements iata_code = page
							.select("[href*=/wiki/International_Air_Transport_Association_airport_code] + b");
					if (iata_code != null & !iata_code.isEmpty()) {
						ai.setIataCode(iata_code.text().replaceAll("\\P{L}", " "));
					}
					iata_code = null;
				}
				if (ai.getFaaLid() == null || "".equals(ai.getFaaLid())) {
					Elements faa_lid = page.select("[href*=/wiki/Location_identifier] + b");
					logger.debug(faa_lid.text());
					if (faa_lid.text() != null && !faa_lid.text().isEmpty()) {
						ai.setFaaLid(faa_lid.text());
					}
					faa_lid = null;
				}
				if (ai.getIcaoCode() == null || "".equals(ai.getIcaoCode())) {
					Elements icao_code = page
							.select("[href*=/wiki/International_Civil_Aviation_Organization_airport_code] + b");
					if (icao_code != null && !icao_code.isEmpty()) {
						ai.setIcaoCode(icao_code.text().replaceAll("\\P{L}", " "));
					}
					icao_code = null;
				}
				// Get Lat and Long
				Elements coordinates = page.select("[href*=/tools.wmflabs.org/geohack/]");
				if (coordinates.attr("href") != null && !"".equals(coordinates.attr("href"))) {
					GeoHackScraper geoScrape = new GeoHackScraper();
					Elements latLong = geoScrape.parseGeoHackPageForLatLong("https:" + coordinates.attr("href"));
					ai.setLatitude(Double.parseDouble(latLong.select("[class*=latitude]").text()));
					ai.setLongitude(Double.parseDouble(latLong.select("[class*=longitude]").text()));
				} else {
					logger.debug(ai.getIataCode() + " " + ai.getName() + " has no coordiantes");
				}
				// Get Airlines
				Elements andTH = page.select("th:contains(Airlines) ~ th:contains(Destinations)");
				// check to see if we have any airlines and destinations
				if (andTH.parents().size() > 1) {
					Elements andTRs = andTH.parents().get(1).select("tr");
					// remove the header row
					andTRs.remove(0);
					// down to the meat and potatoes
					Element andTR = andTRs.get(0);
					Elements andTDs = andTR.select("td");
					Elements airlineAs = andTDs.get(0).select("a");
					for (int i = 0; i < airlineAs.size(); i++) {
						// for (int i = 0; i < 5; i++) {
						Airlines airline = new Airlines();
						airline.setName(airlineAs.get(i).text());
						airline.setWikiUrl(airlineAs.attr("href"));
						(new Thread(new AirlinePageForAirportPageParser(caller, this, i, airline))).start();
					}
					// Get airline destinations for this airport

					// Elements destinationAs = andTDs.get(1).select("a");

				} else {
					completed = true;
				}

				quit = true;
				page = null;
				logger.debug(ai.getIataCode() + " " + ai.getName() + " Airport Page Processed");
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
						"Failed to parse page " + "https://en.wikipedia.org" + ai.getWikiUrl() + ". Trying again.");
			}
		}
		caller.updateAirport(recordNumber, ai);
		if (!completed) {
			TimerTask timerTask = new TimerTask() {

				@Override
				public void run() {
					completed = true;
				}
			};
			timer = new Timer("AirlineTimeOut");// create a new Timer
			timer.schedule(timerTask, 30000);
		}
		while (!completed) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (timer != null) {
			timer.cancel();
			timer = null;
		}

	}

	public void airlineUpdateDone() {
		completed = true;
	}

}
