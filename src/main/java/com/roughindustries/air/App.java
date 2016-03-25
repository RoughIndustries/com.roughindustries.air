package com.roughindustries.air;

import java.io.IOException;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.jsoup.select.Elements;

import com.roughindustries.air.client.AirportsMapper;
import com.roughindustries.air.model.Airports;
import com.roughindustries.air.model.AirportsExample;
import com.roughindustries.air.resources.GlobalProperties;
import com.roughindustries.air.scrapers.AirportScraper;

/**
 * Hello world!
 *
 */
public class App {

	/**
	 * 
	 */
	final static Logger logger = Logger.getLogger(App.class);

	/**
	 * 
	 */
	static GlobalProperties Props = GlobalProperties.getInstance();;

	public static void main(String[] args) {
		SqlSession ses = null;
		try {
			logger.debug(Props.getAirportPage());
			AirportScraper as = new AirportScraper();
			Elements airports = as.parseIATAAlphaGroups(as.getIATAAlphaGroups(as.getAirportListPage()));
			List<Airports> al = as.parseAirportsElementList(airports);
			//for (int i = 0; i < al.size(); i++) {
			for (int i = 0; i < 2; i++) {
				Airports airport = al.get(i);
				al.set(i, as.parseAirportPageForAirportInfo(airport));
				airport = null;
				if (i % 1000 == 0) {
					logger.debug("GARBAGE COLLECT!!!!");
					System.gc();
				}
			}
			ses = Props.getSqlSessionFactory().openSession();
			for (Airports ai : al) {
				AirportsMapper mapper = ses.getMapper(AirportsMapper.class);
				AirportsExample example = new AirportsExample();
				example.createCriteria().andIataCodeEqualTo(ai.getIataCode());
				int updates = mapper.updateByExample(ai, example);
				if (updates < 1) {
					ses.insert("insert", ai);
				}
			}
			ses.commit();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			ses.close();
		}
	}
}
