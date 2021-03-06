package com.roughindustries.air.client;

import com.roughindustries.air.model.Airports;
import com.roughindustries.air.model.AirportsExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AirportsMapper {

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airports
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	int countByExample(AirportsExample example);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airports
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	int deleteByExample(AirportsExample example);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airports
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	int insert(Airports record);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airports
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	int insertSelective(Airports record);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airports
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	List<Airports> selectByExample(AirportsExample example);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airports
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	int updateByExampleSelective(@Param("record") Airports record, @Param("example") AirportsExample example);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airports
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	int updateByExample(@Param("record") Airports record, @Param("example") AirportsExample example);
}