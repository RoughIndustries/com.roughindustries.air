/**
 * 
 */
package com.roughindustries.air.scrapers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.roughindustries.air.model.Airports;
import com.roughindustries.air.resources.GlobalProperties;

/**
 * @author roughindustries
 *
 */
public class AirportScraper{

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
	public List<Airports> parseAirportsElementList(Elements airports) {
		List<Airports> al = new ArrayList<Airports>();
		for (Element airport : airports) {
			Elements td_list = airport.select("td");
			if (td_list.get(2).getElementsByAttributeValueContaining("href", "redlink").isEmpty()) {
				Airports ia = new Airports();
				ia.setIataCode(td_list.get(0).text().replaceAll("\\P{L}", " "));
				ia.setIcaoCode(td_list.get(1).text().replaceAll("\\P{L}", " "));
				ia.setName(td_list.get(2).getElementsByAttribute("href").text().replaceAll("\\P{L}", " "));
				ia.setWikiUrl(td_list.get(2).getElementsByAttribute("href").attr("href"));
				al.add(ia);
				logger.debug(ia.getIataCode() + " " + ia.getIcaoCode() + " " + ia.getName() + " " + ia.getWikiUrl());
			}
		}
		return al;
	}

	/**
	 * @return
	 * @throws IOException
	 */
	public Airports parseAirportPageForAirportInfo(Airports ai) throws IOException {
		try {
			Document page = Jsoup.parse("https://en.wikipedia.org" + ai.getWikiUrl());
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
		return ai;

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

	/**
	 * @param airports
	 * @return
	 */
	public boolean updateAirportsTable(List<Airports> airports) {
		return false;

	}
}
