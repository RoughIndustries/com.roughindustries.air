/**
 * 
 */
package com.roughindustries.air;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.geonames.FeatureClass;
import org.geonames.InsufficientStyleException;
import org.geonames.Style;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.WebService;

import com.roughindustries.air.client.AirportsMapper;
import com.roughindustries.air.client.LocationsServedMapper;
import com.roughindustries.air.model.Airports;
import com.roughindustries.air.model.AirportsExample;
import com.roughindustries.air.model.LocationsServed;
import com.roughindustries.air.model.LocationsServedExample;
import com.roughindustries.air.resources.GlobalProperties;

/**
 * @author roughindustries
 *
 */
public class App2 implements Runnable {

	/**
	 * 
	 */
	final static Logger logger = Logger.getLogger(App2.class);

	/**
	 * 
	 */
	GlobalProperties Props = GlobalProperties.getInstance();

	private Double Lat, Long;

	/**
	 * @param lat
	 * @param l
	 */
	public App2(Double Lat, Double Long) {
		super();
		this.Lat = Lat;
		this.Long = Long;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */

	@Override
	public void run() {
		try {
			WebService.setUserName("travishdc");
			List<Toponym> searchResult;
			searchResult = WebService.findNearby(Lat.doubleValue(), Long.doubleValue(), 150.0, FeatureClass.P,
					new String[] { "PPLC" }, "en", 3);
			searchResult.addAll(WebService.findNearby(Lat.doubleValue(), Long.doubleValue(), 150.0, FeatureClass.P,
					new String[] { "PPLA" }, "en", 10));
			searchResult.addAll(WebService.findNearby(Lat.doubleValue(), Long.doubleValue(), 150.0, FeatureClass.P,
					new String[] { "PPLA2" }, "en", 10));
			List<Toponym> completeSearchResults = new ArrayList<Toponym>();
			for (Toponym topo_item : searchResult) {
				Toponym toponym = WebService.get(topo_item.getGeoNameId(), "en", Style.LONG.name());
				completeSearchResults.add(toponym);
				logger.debug(topo_item.toString());

			}
			completeSearchResults.sort(new Comparator<Toponym>() {
				@Override
				public int compare(Toponym o1, Toponym o2) {
					try {
						if (o1.getPopulation() == null) {
							return 1;
						} else if (o2.getPopulation() == null) {
							return -1;
						} else {
							int result = Double.compare(o1.getPopulation(), o2.getPopulation());
							return -1 * result;
						}
					} catch (InsufficientStyleException e) {
						e.printStackTrace();
					}
					return 0;
				}
			});
			completeSearchResults = completeSearchResults.subList(0, 3);
			for (Toponym topo_item : completeSearchResults) {
				LocationsServed loc = new LocationsServed();
				loc.setName(topo_item.getName());
				loc.setLatitude(topo_item.getLatitude());
				loc.setLongitude(topo_item.getLongitude());
				updateLocationsServed(loc);
				logger.debug(topo_item.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void updateLocationsServed(LocationsServed loc) {
		SqlSession ses = null;
		try {
			ses = Props.getSqlSessionFactory().openSession();
			LocationsServedMapper mapper = ses.getMapper(LocationsServedMapper.class);
			LocationsServedExample example = new LocationsServedExample();
			example.createCriteria().andLatitudeEqualTo(loc.getLatitude());
			example.createCriteria().andLongitudeEqualTo(loc.getLongitude());
			int updates = mapper.updateByExample(loc, example);
			if (updates < 1) {
				ses.insert("com.roughindustries.air.client.LocationsServedMapper.insertSelective", loc);
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
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// BWI
		// App2 app = new App2(39.175278, -76.668333);
		// PHL
		// App2 app = new App2(39.871944, -75.241111);
		// DCA
		// App2 app = new App2(38.852222, -77.037778);
		// IAD
		// App2 app = new App2(38.944444, -77.455833);
		// RIC
		//App2 app = new App2(37.505, -77.319444);
		// NRT
		App2 app = new App2(35.765278, 140.385556);
		// FMM
		// App2 app = new App2(47.9925, 10.243611);
		app.run();
	}

}
