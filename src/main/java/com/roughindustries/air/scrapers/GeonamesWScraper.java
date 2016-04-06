package com.roughindustries.air.scrapers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
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
public class GeonamesWScraper{

	/**
	 * 
	 */
	final static Logger logger = Logger.getLogger(GeonamesWScraper.class);

	/**
	 * 
	 */
	GlobalProperties Props = GlobalProperties.getInstance();

	public boolean updateLocationsServed(Double Lat, Double Long, double radius) {
		boolean results = false;
		try {
			WebService.setUserName("travishdc");
			List<Toponym> searchResult;
			searchResult = WebService.findNearby(Lat.doubleValue(), Long.doubleValue(), radius, FeatureClass.P,
					new String[] { "PPLC" }, "en", 3);
			searchResult.addAll(WebService.findNearby(Lat.doubleValue(), Long.doubleValue(), radius, FeatureClass.P,
					new String[] { "PPLA" }, "en", 10));
			searchResult.addAll(WebService.findNearby(Lat.doubleValue(), Long.doubleValue(), radius, FeatureClass.P,
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
			if(completeSearchResults.size() >= 3){
				completeSearchResults = completeSearchResults.subList(0, 3);
			} else {
				completeSearchResults = completeSearchResults.subList(0, completeSearchResults.size());
			}
			for (Toponym topo_item : completeSearchResults) {
				LocationsServed loc = new LocationsServed();
				loc.setName(topo_item.getName());
				loc.setLatitude(topo_item.getLatitude());
				loc.setLongitude(topo_item.getLongitude());
				loc = updateLocationsServed(loc);
				logger.debug(topo_item.toString());
			}
			results = true;
		} catch (GeoNamesException e) {
			logger.error("Geonames limit probably exceeded!");
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
			if(locs.size() > 0){
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
}
