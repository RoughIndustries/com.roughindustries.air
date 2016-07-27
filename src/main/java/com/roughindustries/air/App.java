package com.roughindustries.air;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.jsoup.select.Elements;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlWriter;
import com.roughindustries.air.client.AirlinesMapper;
import com.roughindustries.air.model.Airlines;
import com.roughindustries.air.model.AirlinesExample;
import com.roughindustries.air.model.Airports;
import com.roughindustries.air.resources.GlobalProperties;
import com.roughindustries.air.scrapers.AirportPageForAirportInfoParser;
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
	static GlobalProperties Props = GlobalProperties.getInstance();

	public CopyOnWriteArrayList<Airports> full_al = new CopyOnWriteArrayList<Airports>();
	public CopyOnWriteArrayList<Airports> al = new CopyOnWriteArrayList<Airports>();
	final static BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(25);
	AirportScraper as = new AirportScraper();

	public static void main(String[] args) {
		App app = new App();
		// This method is designed to get the yaml airports as quick as
		// possible. It is not really good for anything else.
		app.parseAirports();
		//app = null;
		System.gc();
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		while (queue.size() > 0) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//app.parseLatLong();
		app.parseLocationServed();
		app.writeYamlToFile("airports.yml");
		logger.debug(""+app.al.size());

	}

	public App() {

	}

	public synchronized void updateAirline(int recordNumber, Airlines al) {
		SqlSession ses = null;
		try {
			ses = Props.getSqlSessionFactory().openSession();
			AirlinesMapper mapper = ses.getMapper(AirlinesMapper.class);
			AirlinesExample example = new AirlinesExample();
			example.createCriteria().andIataCodeEqualTo(al.getIataCode());
			int updates = mapper.updateByExample(al, example);
			if (updates < 1) {
				ses.insert("com.roughindustries.air.client.AirlinesMapper.insertSelective", al);
			}
			ses.commit();
		} finally {
			if (ses != null) {
				ses.close();
				ses = null;
			}
		}
	}

	public void parseAirports() {
		try {
			logger.debug(Props.getAirportPage());
			Elements airports = as.parseIATAAlphaGroups(as.getIATAAlphaGroups(as.getAirportListPage()));
			full_al = as.parseAirportsElementList(airports);

			// Blow through the airports so that we can gwt them into yaml to
			// work on them
			ExecutorService executorService = new ThreadPoolExecutor(13, 25, 0L, TimeUnit.MILLISECONDS, queue);
			for (int i = 0; i < full_al.size(); i++) {
				boolean submitted = false;
				while (!submitted) {
					try {
						Airports airport = full_al.get(i);
						Runnable call = new AirportPageForAirportInfoParser(i, this);
						executorService.execute(call);
						submitted = true;
					} catch (RejectedExecutionException ree) {
						// logger.debug("Queue is full");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			executorService.shutdown();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void parseLatLong(){
		for(int i = 0; i < al.size(); i++){
			Airports new_ai = as.parseAirportPageForLatLong(al.get(i));
			al.set(i, new_ai);
		}
	}
	
	public void parseLocationServed(){
		//for(int i = 0; i < al.size(); i++){
		for(int i = 0; i < 25; i++){
			Airports new_ai = as.parseGeonamesWSLocServ(al.get(i));
			al.set(i, new_ai);
		}
	}

	public void writeYamlToFile(String filename) {
		try {
			YamlWriter writer = new YamlWriter(new FileWriter(filename));
			writer.getConfig().writeConfig.setEscapeUnicode(false);
			writer.write(al);
			writer.close();
		} catch (YamlException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
