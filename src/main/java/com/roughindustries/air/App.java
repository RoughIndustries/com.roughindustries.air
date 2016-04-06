package com.roughindustries.air;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.jsoup.select.Elements;

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
public class App implements Runnable {

	/**
	 * 
	 */
	final static Logger logger = Logger.getLogger(App.class);

	/**
	 * 
	 */
	static GlobalProperties Props = GlobalProperties.getInstance();

	static List<Airports> al = null;

	public static void main(String[] args) {
		App app = new App();
		app.run();
		app = null;
		System.gc();
		while (true) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
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

	@Override
	public void run() {
		try {
			logger.debug(Props.getAirportPage());
			AirportScraper as = new AirportScraper();
			Elements airports = as.parseIATAAlphaGroups(as.getIATAAlphaGroups(as.getAirportListPage()));
			al = as.parseAirportsElementList(airports);
			//for (int i = 0; i < al.size(); i++) {
				// for (int i = 0; i < 5; i++) {
				//Airports airport = al.get(i);
				// (new Thread(new AirportPageForAirportInfoParser(this, i,
				// airport))).start();
				//(new AirportPageForAirportInfoParser(this, i, airport)).run();
			//}
			final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(25);
			ExecutorService executorService = new ThreadPoolExecutor(13, 25, 0L, TimeUnit.MILLISECONDS, queue);
			for (int i = 0; i < al.size(); i++) {
				boolean submitted = false;
				while (!submitted) {
					try {
						//if (al.get(i).getWikiUrl() != null && !al.get(i).getWikiUrl().isEmpty()) {
							Airports airport = al.get(i);
							//Runnable call = new DocumentRunnable(al.get(i).getWikiUrl());
							Runnable call = new AirportPageForAirportInfoParser(airport);
							executorService.execute(call);
						//}
						submitted = true;
					} catch (RejectedExecutionException ree) {
						//logger.debug("Queue is full");
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
}
