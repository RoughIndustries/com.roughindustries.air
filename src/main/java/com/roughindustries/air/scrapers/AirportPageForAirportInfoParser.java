package com.roughindustries.air.scrapers;

import java.net.URL;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.roughindustries.air.App;
import com.roughindustries.air.model.Airports;
import com.roughindustries.air.resources.GlobalProperties;

public class AirportPageForAirportInfoParser implements Runnable{

	/**
	 * 
	 */
	final static Logger logger = Logger.getLogger(AirportPageForAirportInfoParser.class);
	
	/**
	 * 
	 */
	GlobalProperties Props = GlobalProperties.getInstance();

	private App caller; int recordNumber; Airports ai;
	
	public AirportPageForAirportInfoParser(App caller, int recordNumber, Airports ai){
		this.caller = caller;
		this.recordNumber = recordNumber;
		this.ai = ai;
	}
	
	@Override
	public void run() {
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
			//Get Lat and Long
			//Elements summaryTable = page.select("table[class*=infobox vcard]");
			Elements coordinates = page.select("[href*=/tools.wmflabs.org/geohack/]");
			if(coordinates.attr("href") != null && !"".equals(coordinates.attr("href"))){
				GeoHackScraper geoScrape = new GeoHackScraper();
				Elements latLong = geoScrape.parseGeoHackPageForLatLong("https:"+coordinates.attr("href"));
				ai.setLatitude(Double.parseDouble(latLong.select("[class*=latitude]").text()));
				ai.setLongitude(Double.parseDouble(latLong.select("[class*=longitude]").text()));
			} else {
				logger.debug( ai.getIataCode() + " " + ai.getName() + " has no coordiantes");
			}
			page = null;
			logger.debug(ai.getIataCode() + " " + ai.getName() + " Airport Page Processed");
		} catch (Exception e) {
			e.printStackTrace();
		}
		caller.updateAirport(recordNumber, ai);
	}
}
