package com.roughindustries.air.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;

public class GlobalProperties {

	/**
	 * 
	 */
	final static Logger logger = Logger.getLogger(GlobalProperties.class);

	/**
	 * 
	 */
	final static String globalPropertiesFileName = "global.properties";
	
	/**
	 * 
	 */
	final static String mybatisConfigFileName = "mybatis-config.xml";
	
	/**
	 * 
	 */
	private static GlobalProperties instance = null;

	/**
	 * 
	 */
	private String airportPage = null;
	
	/**
	 * 
	 */
	private SqlSessionFactory sqlSessionFactory;

	/**
	 * 
	 */
	protected GlobalProperties() {
		// Exists only to defeat instantiation.
	}

	/**
	 * @return
	 */
	public static GlobalProperties getInstance() {
		if (instance == null) {
			instance = new GlobalProperties();
			Properties prop = new Properties();
			

			InputStream inputStream = GlobalProperties.class.getClassLoader().getResourceAsStream(globalPropertiesFileName);

			if (inputStream != null) {
				try {
					prop.load(inputStream);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				try {
					throw new FileNotFoundException("property file '" + globalPropertiesFileName + "' not found in the classpath");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}

			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			Date time = new Date(System.currentTimeMillis());

			// get the property value and print it out
			instance.setAirportPage(prop.getProperty("airportPage"));

			try {
				inputStream = Resources.getResourceAsStream(mybatisConfigFileName);
				SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
				instance.setSqlSessionFactory(sqlSessionFactory);
			} catch (IOException e) {
				e.printStackTrace();
			}

			logger.debug("Props retrieved at " + time);
		}
		return instance;
	}

	/**
	 * @return the airportPage
	 */
	public String getAirportPage() {
		return airportPage;
	}

	/**
	 * @param airportPage
	 *            the airportPage to set
	 */
	protected void setAirportPage(String airportPage) {
		this.airportPage = airportPage;
	}

	/**
	 * @return the sqlSession
	 */
	public synchronized SqlSessionFactory getSqlSessionFactory() {
		return sqlSessionFactory;
	}

	/**
	 * @param sqlSession the sqlSession to set
	 */
	public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
	}

}
