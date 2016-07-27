/**
 * 
 */
package com.roughindustries.air.scrapers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
	public List<Airports> parseAirportsElementList(CopyOnWriteArrayList<Airports> al, Elements airports) {
		int i = 0;
		for (Element airport : airports) {
//			if(i > 25){
//				break;
//			} else {
//				i++;
//			}
			Elements td_list = airport.select("td");
			if (td_list.get(2).getElementsByAttributeValueContaining("href", "redlink").isEmpty()) {
				Airports ia = new Airports();
				ia.setIataCode(td_list.get(0).text().replaceAll("\\P{L}", " "));
				ia.setIcaoCode(td_list.get(1).text().replaceAll("\\P{L}", " "));
				ia.setName(td_list.get(2).getElementsByAttribute("href").text().replaceAll("\\P{L}", " "));
				if (td_list.get(2).getElementsByAttributeValueContaining("href", "redlink").isEmpty()) {
					if (!td_list.get(2).getElementsByAttributeValueContaining("href", "/wiki/").isEmpty()) {
						ia.setWikiUrl(td_list.get(2).getElementsByAttribute("href").attr("href"));
					} else {
						logger.debug("Parsing Exception: Bad wiki link for "+ia.getIataCode());
					}
				} else {
					logger.debug("Parsing Exception: RedLine wiki link for "+ia.getIataCode());
				}
				al.add(ia);
				logger.debug(ia.getIataCode() + " " + ia.getIcaoCode() + " " + ia.getName() + " " + ia.getWikiUrl());
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

	/**
	 * @param airports
	 * @return
	 */
	public boolean updateAirportsTable(List<Airports> airports) {
		return false;

	}
}
