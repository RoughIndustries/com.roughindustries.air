package com.roughindustries.air;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.jsoup.select.Elements;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
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

	public ConcurrentHashMap<String, Airports> full_al = new ConcurrentHashMap<String, Airports>();
	public ConcurrentHashMap<String, Airports> apl = new ConcurrentHashMap<String, Airports>();
	public ConcurrentHashMap<String, Airlines> all = new ConcurrentHashMap<String, Airlines>();
	final static BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(25);
	AirportScraper as = new AirportScraper();
	LocationServedTimerTask timerTask;

	public static void main(String[] args) {
		App app = new App();
		// read the yaml in
		app.readYamlToFile("airports.yml");

		// This method is designed to get the airports as quick as
		// possible. It is not really good for anything else. The
		// idea is blow through these so that things that need to
		// be throttled can be.
		// app.parseAirports();
		// app = null;
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

		// write the results out so we don't have to scrape them in the future
		// app.writeYamlToFile("airports.yml");

		// Load the yaml here instead of scraping

		// app.parseLatLong();
		// app.parseLocationServed();
		app.processLocationServed();
		logger.debug("" + app.apl.size());

		while (!app.timerTask.isFinished()) {
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

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
			// int i = 0;
			for (String key : full_al.keySet()) {
				// if(i >= 50){
				// break;
				// } else {
				// i++;
				// }
				boolean submitted = false;
				while (!submitted) {
					try {
						// String key = it.next();
						Airports airport = full_al.get(key);
						Runnable call = new AirportPageForAirportInfoParser(airport, this);
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

	public void parseLatLong() {
		for (int i = 0; i < apl.size(); i++) {
			Airports new_ai = as.parseAirportPageForLatLong(apl.get(i));
			apl.put(new_ai.getIataCode(), new_ai);
		}
	}

	class LocationServedTimerTask extends TimerTask {

		App app;
		int i = 0;
		boolean finished = false;
		private Iterator<String> keySet;

		public LocationServedTimerTask(App app) {
			this.app = app;
			this.keySet = app.apl.keySet().iterator();
		}

		@Override
		public void run() {
			if (keySet.hasNext()) {
				String key = keySet.next();
				Airports ai = app.apl.get(key);
				Airports new_ai = app.as.parseGeonamesWSLocServ(ai);
				apl.put(new_ai.getIataCode(), new_ai);
				i++;
			} else {
				finished = true;
			}
			 if(i >= 5){
			 finished = true;
			 }

		}

		public boolean isFinished() {
			return finished;
		}
	}

	public void processLocationServed() {
		// run this task as a background/daemon thread
		timerTask = new LocationServedTimerTask(this);
		Timer timer = new Timer(true);
		// ?? seconds
		timer.scheduleAtFixedRate(timerTask, 0, 10 * 1000);
		// 5 minutes
		// timer.scheduleAtFixedRate(timerTask, 0, 5*60*1000);
	}

	public void parseLocationServed() {
		Iterator<Entry<String, Airports>> it = apl.entrySet().iterator();
		int i = 0;
		while (it.hasNext()) {
			if (i >= 25) {
				break;
			}
			i++;
			Entry<String, Airports> pair = it.next();
			Airports new_ai = as.parseGeonamesWSLocServ(pair.getValue());
			apl.put(new_ai.getIataCode(), new_ai);
		}
	}

	public void writeYamlToFile(String filename) {
		try {
			YamlWriter writer = new YamlWriter(new FileWriter(filename));
			writer.getConfig().writeConfig.setEscapeUnicode(false);
			writer.getConfig().writeConfig.setAutoAnchor(false);
			writer.getConfig().writeConfig.setUseVerbatimTags(true);
			// writer.getConfig().writeConfig.setAlwaysWriteClassname(false);
			// writer.getConfig().writeConfig.setWriteRootElementTags(false);
			writer.write(apl);
			writer.close();
		} catch (YamlException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void readYamlToFile(String filename) {
		try {
			FileReader fr = null;

			File locatedFile = new File(filename);
			if (locatedFile.exists()) {
				fr = new FileReader(locatedFile);
				YamlReader reader = new YamlReader(fr);
				apl = (ConcurrentHashMap<String, Airports>) reader.read();
				if (apl == null) {
					apl = new ConcurrentHashMap<String, Airports>();
				}
				reader.close();
			}
		} catch (YamlException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
