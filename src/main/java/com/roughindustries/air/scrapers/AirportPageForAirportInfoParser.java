package com.roughindustries.air.scrapers;

import java.io.IOException;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

import com.roughindustries.air.App;
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

	App app;

	private int airport_index;

	public AirportPageForAirportInfoParser(int i, App app) {
		this.airport_index = i;
		this.app = app;
		// this.ai.setInternalAirportId(0);
	}

	@Override
	public void run() {
		boolean quit = false;
		int max_attempts = 5;
		int attempts = 0;
		Airports ai = app.full_al.get(airport_index);
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

						// ai.setIsAd(true);
						Elements andTRs = andTH.parents().get(1).select("tr");
						// remove the header row
						andTRs.remove(0);
						// down to the meat and potatoes
						for (int i = 0; i < andTRs.size(); i++) {
							Element andTR = andTRs.get(i);
							Elements andTDs = andTR.select("td");
							Elements airlineAs = andTDs.get(0).select("a");
							Elements airlineDs = andTDs.get(1).select("a");
							if (airlineAs.size() > 0) {
								Airlines airline = new Airlines();
								airline.setName(airlineAs.get(0).text());
								if (airlineAs.get(0).getElementsByAttributeValueContaining("href", "redlink")
										.isEmpty()) {
									if (airlineAs.get(0).attr("href").contains("/wiki/")) {
										airline.setWikiUrl(airlineAs.get(0).attr("href"));
										if (airlineDs.size() > 0) {
											for (Element destination : airlineDs) {
												String name = "";
												String href = "";
												String iata = "";
												if (destination.attr("href").contains("/wiki/")) {
													href = destination.attr("href");
													name = destination.text();
													Document destpage = Jsoup.parse(new URL("https://en.wikipedia.org" + href),
															10000);
													Elements iata_code = destpage.select(
															"[href*=/wiki/International_Air_Transport_Association_airport_code] + b");
													if (iata_code != null & !iata_code.isEmpty()) {
														iata = iata_code.text().replaceAll("\\P{L}", " ");
														Airports airport = new Airports();
														airport.setIataCode(iata);
														airline.destinations.put(airport.getName(), airport);
														ai.destinations.put(iata, airport);
													}

												}
												
											}
										}
										if (airline.getIataCode() != null && !airline.getIataCode().isEmpty()) {
											ai.airlines.put(airline.getIataCode(), airline);
										}
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
						// ai = updateAirport(ai);

						AirportScraper as = new AirportScraper();
						as.parseAirportPageForLatLong(ai);

						page = null;

						app.al.put(ai.getIataCode(), ai);
						logger.debug(ai.getIataCode() + " " + ai.getName() + " Airport Page Processed");
					} else {
						// ai.setIsAd(false);
						// updateAirport(ai);
						page = null;
						logger.debug(ai.getIataCode() + " " + ai.getName()
								+ " Airport Page Not Processed. No Airline or Destinations.");
					}
				}
				quit = true;
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
