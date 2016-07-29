/**
 * 
 */
package com.roughindustries.air.scrapers;

import java.io.IOException;
import java.net.URL;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.roughindustries.air.model.Airports;
import com.roughindustries.air.model.LocationsServed;
import com.roughindustries.air.resources.GlobalProperties;

/**
 * @author roughindustries
 *
 */
public class AirportScraper {

	/**
	 * 
	 */
	final static Logger logger = Logger.getLogger(AirportScraper.class);

	/**
	 * 
	 */
	GlobalProperties Props = GlobalProperties.getInstance();

	/**
	 * @return
	 * @throws IOException
	 */
	public Document getAirportListPage() throws IOException {
		return Jsoup.connect(Props.getAirportPage()).get();
	}

	/**
	 * @param airportPage
	 * @return
	 */
	public Elements getIATAAlphaGroups(Document airportPage) {
		return airportPage.select("p > [href*=/wiki/List_of_airports_by_IATA_code]");
	}

	/**
	 * @param iataAlphaGroups
	 * @return
	 * @throws IOException
	 */
	public Elements parseIATAAlphaGroups(Elements iataAlphaGroups) throws IOException {
		Elements tr_list = new Elements();
		for (Element link : iataAlphaGroups) {
			Document iata_code_list_doc = Jsoup.connect(link.attr("abs:href")).get();
			Elements temp = iata_code_list_doc.select("[class*=wikitable]").select("tbody")
					.select("tr:not([class*=sortbottom])");
			temp.remove(0);
			tr_list.addAll(temp);
		}
		return tr_list;
	}

	/**
	 * @param airports
	 * @return
	 */
	public ConcurrentHashMap<String, Airports> parseAirportsElementList(Elements airports) {
		ConcurrentHashMap<String, Airports> al = new ConcurrentHashMap<String, Airports>();
		int i = 0;
		for (Element airport : airports) {
			Elements td_list = airport.select("td");
			if (td_list.get(2).getElementsByAttributeValueContaining("href", "redlink").isEmpty()) {
				Airports ia = new Airports();
				String iata = td_list.get(0).text().replaceAll("\\P{L}", "").trim();
				String name = td_list.get(2).getElementsByAttribute("href").text().replaceAll("\\P{L}", " ");
				if (iata != null) {
					ia.setIataCode(iata);
					ia.setIcaoCode(td_list.get(1).text().replaceAll("\\P{L}", " "));
					ia.setName(name);
					if (td_list.get(2).getElementsByAttributeValueContaining("href", "redlink").isEmpty()) {
						if (!td_list.get(2).getElementsByAttributeValueContaining("href", "/wiki/").isEmpty()) {
							ia.setWikiUrl(td_list.get(2).getElementsByAttribute("href").attr("href"));
						} else {
							logger.debug("Parsing Exception: Bad wiki link for " + ia.getIataCode());
						}
					} else {
						logger.debug("Parsing Exception: RedLine wiki link for " + ia.getIataCode());
					}
					al.put(iata, ia);
					logger.debug(
							ia.getIataCode() + " " + ia.getIcaoCode() + " " + ia.getName() + " " + ia.getWikiUrl());
				} else {
					logger.debug("Parsing Exception: Bad IATA " + name);
				}
			}
		}
		return al;
	}

	/**
	 * @return
	 */
	public Airports parseAirportPageForAirlines(String wiki_page) {
		return null;
	}

	/**
	 * @return
	 */
	public Airports parseAirportPageForAirlineDestinations() {
		return null;
	}

	public synchronized Airports parseAirportPageForLatLong(Airports ai) {
		try {
			Document page = null;
			if (ai.getWikiUrl() != null && !ai.getWikiUrl().isEmpty()) {
				page = Jsoup.parse(new URL("https://en.wikipedia.org" + ai.getWikiUrl()), 10000);
				// Get Lat and Long
				if (ai.getIataCode().equalsIgnoreCase("AAH")) {
					logger.debug("");
				}
				Elements coordinates = page.select("[href*=/tools.wmflabs.org/geohack]");
				for (Element element : coordinates) {
					logger.debug(ai.getName() + " coordinates page " + "https:" + element.attr("href"));
				}
				if (coordinates.attr("href") != null && !"".equals(coordinates.attr("href"))) {
					GeoHackScraper geoScrape = new GeoHackScraper();
					Elements latLong = geoScrape.parseGeoHackPageForLatLong("https:" + coordinates.attr("href"));
					if (latLong != null) {
						ai.setLatitude(Double.parseDouble(latLong.select("[class*=latitude]").text()));
						ai.setLongitude(Double.parseDouble(latLong.select("[class*=longitude]").text()));
						logger.debug(ai.getName() + " lat=" + ai.getLatitude() + " long=" + ai.getLongitude());
					}
				} else {
					logger.debug(ai.getIataCode() + " " + ai.getName() + " has no coordiantes from wiki");

					// try {
					// WebService.setUserName("travishdc");
					// ToponymSearchCriteria searchCriteria = new
					// ToponymSearchCriteria();
					// //
					// searchCriteria.setQ(java.net.URLEncoder.encode(ai.getName(),"UTF8"));
					// searchCriteria.setQ(ai.getName());

					// ToponymSearchResult searchResult =
					// WebService.search(searchCriteria);
					// GeonamesWScraper.addOneToCount();
					//
					// if (searchResult.getToponyms().size() > 0) {
					// Toponym toponym = searchResult.getToponyms().get(0);
					// ai.setLatitude(toponym.getLatitude());
					// ai.setLongitude(toponym.getLongitude());
					// logger.debug("Geonames search results " +
					// toponym.getName() + " "
					// + toponym.getCountryName());
					// }
					// searchCriteria = null;
					// searchResult = null;

					// } catch (GeoNamesException e) {
					// logger.error("Geonames limit probably exceeded! Unable to
					// try to get airport coordinates.");
					// }
				}
			}
		} catch (Exception e) {
		}
		return ai;
	}

	public synchronized Airports parseGeonamesWSLocServ(Airports ai) {
		if (ai.getLocationsServedLastUpdate() == null) {
			Clock clock = Clock.systemUTC();
			long time = clock.millis() - 432000000;
			logger.debug("Time is " + time);
			ai.setLocationsServedLastUpdate(time);
		}
		if ((ai.getLatitude() != null && !ai.getLatitude().isNaN())
				&& (ai.getLongitude() != null && !ai.getLongitude().isNaN())) {
			Clock clock = Clock.systemUTC();
			long time = clock.millis() - 432000000;
			if (ai.getLocationsServedLastUpdate() == null || (ai.getLocationsServedLastUpdate() <= time)) {
				Clock instant_now = Clock.systemUTC();
				logger.debug(ai.getLocationsServedLastUpdate() + " > " + instant_now.millis());
				GeonamesWScraper geonames = new GeonamesWScraper();
				List<LocationsServed> updated = geonames.updateLocationsServed(ai.getLatitude(), ai.getLongitude(),
						150.0);
				ai.locationsServed.addAll(updated);
				ai.setLocationsServedLastUpdate(instant_now.millis());
				geonames = null;
			}
		}
		return ai;
	}

	/**
	 * @param airports
	 * @return
	 */
	public boolean updateAirportsTable(List<Airports> airports) {
		return false;

	}
}
