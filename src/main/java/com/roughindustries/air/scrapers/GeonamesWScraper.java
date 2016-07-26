package com.roughindustries.air.scrapers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.geonames.WebService;

import com.roughindustries.air.client.LocationsServedMapper;
import com.roughindustries.air.model.Airports;
import com.roughindustries.air.model.LocationsServed;
import com.roughindustries.air.model.LocationsServedExample;
import com.roughindustries.air.resources.GlobalProperties;

/**
 * @author roughindustries
 *
 */
public class GeonamesWScraper {

	private static long HourStartTime = 0;
	private static int currentHourCount = 0;

	/**
	 * 
	 */
	final static Logger logger = Logger.getLogger(GeonamesWScraper.class);

	/**
	 * 
	 */
	GlobalProperties Props = GlobalProperties.getInstance();

	public static synchronized void addOneToCount() {
		if (HourStartTime == 0) {
			HourStartTime = new Date().getTime();
		}
		Date current = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(HourStartTime);
		cal.add(Calendar.HOUR, 1);
		Date oneHourAhead = cal.getTime();
		if (oneHourAhead.getTime() < current.getTime()) {
			currentHourCount = 0;
			HourStartTime = new Date().getTime();
		} else {
			currentHourCount++;
		}
	}

	public boolean updateLocationsServed(Double Lat, Double Long, double radius) {
		boolean results = false;
		try {
			if (GeonamesWScraper.getCurrentHourCount() > 1500) {
				Date current = new Date();
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(GeonamesWScraper.getHourStartTime());
				cal.add(Calendar.HOUR, 1);
				Date oneHourAhead = cal.getTime();
				Thread.sleep(oneHourAhead.getTime() - current.getTime());
			} else {
				WebService.setUserName("travishdc");
				List<Toponym> searchResult;
				searchResult = WebService.findNearby(Lat.doubleValue(), Long.doubleValue(), radius, FeatureClass.P,
						new String[] { "PPLC" }, "en", 3);
				GeonamesWScraper.addOneToCount();
				searchResult.addAll(WebService.findNearby(Lat.doubleValue(), Long.doubleValue(), radius, FeatureClass.P,
						new String[] { "PPLA" }, "en", 10));
				GeonamesWScraper.addOneToCount();
				searchResult.addAll(WebService.findNearby(Lat.doubleValue(), Long.doubleValue(), radius, FeatureClass.P,
						new String[] { "PPLA2" }, "en", 10));
				GeonamesWScraper.addOneToCount();
				List<Toponym> completeSearchResults = new ArrayList<Toponym>();
				for (Toponym topo_item : searchResult) {
					Toponym toponym = WebService.get(topo_item.getGeoNameId(), "en", Style.LONG.name());
					GeonamesWScraper.addOneToCount();
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
				if (completeSearchResults.size() >= 3) {
					completeSearchResults = completeSearchResults.subList(0, 3);
				} else {
					completeSearchResults = completeSearchResults.subList(0, completeSearchResults.size());
				}
				for (Toponym topo_item : completeSearchResults) {
					LocationsServed loc = new LocationsServed();
					loc.setName(topo_item.getName());
					loc.setLatitude(topo_item.getLatitude());
					loc.setLongitude(topo_item.getLongitude());
					// loc = updateLocationsServed(loc);
					logger.debug(topo_item.toString());
				}
				results = true;
			}
		} catch (GeoNamesException e) {
			logger.error(
					"Geonames limit probably exceeded! Possible count is " + GeonamesWScraper.getCurrentHourCount());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return results;

	}

	private LocationsServed updateLocationsServed(LocationsServed loc) {
		SqlSession ses = null;
		LocationsServed result = null;
		try {
			ses = Props.getSqlSessionFactory().openSession();
			LocationsServedMapper mapper = ses.getMapper(LocationsServedMapper.class);
			LocationsServedExample example = new LocationsServedExample();
			example.createCriteria().andLatitudeEqualTo(loc.getLatitude());
			example.createCriteria().andLongitudeEqualTo(loc.getLongitude());

			List<LocationsServed> locs = mapper.selectByExample(example);
			if (locs.size() > 0) {
				loc.setInternalLocationServedId(locs.get(0).getInternalLocationServedId());
				mapper.updateByExample(loc, example);
				result = loc;
			} else {
				ses.insert("com.roughindustries.air.client.LocationsServedMapper.insertSelective", loc);
				locs = mapper.selectByExample(example);
				result = locs.get(0);
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

	public static synchronized long getHourStartTime() {
		return HourStartTime;
	}

	public static synchronized void setHourStartTime(long hourStartTime) {
		HourStartTime = hourStartTime;
	}

	public static synchronized int getCurrentHourCount() {
		return currentHourCount;
	}

	public static synchronized void setCurrentHourCount(int currentHourCount) {
		GeonamesWScraper.currentHourCount = currentHourCount;
	}
}
