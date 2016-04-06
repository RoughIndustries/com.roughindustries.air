package com.roughindustries.air.scrapers;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.geonames.FeatureClass;
import org.geonames.GeoNamesException;
import org.geonames.InsufficientStyleException;
import org.geonames.Style;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.ToponymSearchResult;
import org.geonames.WebService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.roughindustries.air.client.AirportsMapper;
import com.roughindustries.air.client.LocationsServedMapper;
import com.roughindustries.air.model.Airlines;
import com.roughindustries.air.model.Airports;
import com.roughindustries.air.model.AirportsExample;
import com.roughindustries.air.model.LocationsServed;
import com.roughindustries.air.model.LocationsServedExample;
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

	Airports ai;

	public AirportPageForAirportInfoParser(Airports ai) {
		this.ai = ai;
		// this.ai.setInternalAirportId(0);
	}

	@Override
	public void run() {
		boolean quit = false;
		int max_attempts = 5;
		int attempts = 0;
		while (!quit) {
			try {
				Document page = null;
				if (ai.getWikiUrl() != null && !ai.getWikiUrl().isEmpty()) {
					page = Jsoup.parse(new URL("https://en.wikipedia.org" + ai.getWikiUrl()), 10000);
					// Get Airlines
					Elements andTH = page.select("th:contains(Airlines) ~ th:contains(Destinations)");
					// check to see if we have any airlines and destinations
					if (andTH.parents().size() > 1) {
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
							Elements latLong = geoScrape
									.parseGeoHackPageForLatLong("https:" + coordinates.attr("href"));
							if (latLong != null) {
								ai.setLatitude(Double.parseDouble(latLong.select("[class*=latitude]").text()));
								ai.setLongitude(Double.parseDouble(latLong.select("[class*=longitude]").text()));
							}
						} else {
							logger.debug(ai.getIataCode() + " " + ai.getName() + " has no coordiantes from wiki");

							try {
								WebService.setUserName("travishdc");
								ToponymSearchCriteria searchCriteria = new ToponymSearchCriteria();
								// searchCriteria.setQ(java.net.URLEncoder.encode(ai.getName(),"UTF8"));
								searchCriteria.setQ(ai.getName());
								ToponymSearchResult searchResult = WebService.search(searchCriteria);
								if (searchResult.getToponyms().size() > 0) {
									Toponym toponym = searchResult.getToponyms().get(0);
									ai.setLatitude(toponym.getLatitude());
									ai.setLongitude(toponym.getLongitude());
									logger.debug("Geonames search results " + toponym.getName() + " "
											+ toponym.getCountryName());
								}
								searchCriteria = null;
								searchResult = null;
							} catch (GeoNamesException e) {
								logger.error("Geonames limit probably exceeded! Unable to try to get airport coordinates.");
							}
						}

						// ai.setIsAd(true);
						Elements andTRs = andTH.parents().get(1).select("tr");
						// remove the header row
						andTRs.remove(0);
						// down to the meat and potatoes
						for (int i = 0; i < andTRs.size(); i++) {
							Element andTR = andTRs.get(i);
							Elements andTDs = andTR.select("td");
							Elements airlineAs = andTDs.get(0).select("a");
							if (airlineAs.size() > 0) {
								Airlines airline = new Airlines();
								airline.setName(airlineAs.get(0).text());
								if (airlineAs.get(0).getElementsByAttributeValueContaining("href", "redlink")
										.isEmpty()) {
									if (airlineAs.get(0).attr("href").contains("/wiki/")) {
										airline.setWikiUrl(airlineAs.get(0).attr("href"));
									} else {
										logger.debug("Exception: Bad wiki link for " + airline.getName() + " from "
												+ ai.getName());
									}
								} else {
									logger.debug("Exception: RedLine wiki link for " + airline.getName() + " from "
											+ ai.getName());
								}
							}
						}

						// Elements destinationAs = andTDs.get(1).select("a");
						ai = updateAirport(ai);

						if ((ai.getLatitude() != null && !ai.getLatitude().isNaN())
								&& (ai.getLongitude() != null && !ai.getLongitude().isNaN())) {
							if (ai.getLocationsServedLastUpdate() == null
									|| (ai.getLocationsServedLastUpdate().getTime() + 432000000 > (new Date())
											.getTime())) {
								GeonamesWScraper geonames = new GeonamesWScraper();
								boolean updated = geonames.updateLocationsServed(ai.getLatitude(), ai.getLongitude(),
										150.0);
								if (updated) {
									ai.setLocationsServedLastUpdate(new Date());
								}
								geonames = null;
								ai = updateAirport(ai);
							}
						}

						page = null;
					} else {
						// ai.setIsAd(false);
						// updateAirport(ai);
						page = null;
					}

				}

				quit = true;
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
				e.printStackTrace();
				logger.debug(
						"Failed to parse page " + "https://en.wikipedia.org" + ai.getWikiUrl() + ". Trying again.");
			}
		}

	}

	private Airports updateAirport(Airports ai) {
		SqlSession ses = null;
		Airports result = null;
		try {
			ses = Props.getSqlSessionFactory().openSession();
			AirportsMapper mapper = ses.getMapper(AirportsMapper.class);
			AirportsExample example = new AirportsExample();
			example.createCriteria().andIataCodeEqualTo(ai.getIataCode());
			List<Airports> airports = mapper.selectByExample(example);
			if (airports.size() > 0) {
				ai.setInternalAirportId(airports.get(0).getInternalAirportId());
				mapper.updateByExample(ai, example);
				result = ai;
			} else {
				ses.insert("com.roughindustries.air.client.AirportsMapper.insertSelective", ai);
				airports = mapper.selectByExample(example);
				result = airports.get(0);
			}
			ses.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (ses != null) {
				ses.close();
				ses = null;
			}
		}
		return result;

	}
}
