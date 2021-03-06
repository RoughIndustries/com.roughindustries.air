package com.roughindustries.air.client;

import com.roughindustries.air.model.Airlines;
import com.roughindustries.air.model.AirlinesExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AirlinesMapper {

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airlines
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	int countByExample(AirlinesExample example);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airlines
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	int deleteByExample(AirlinesExample example);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airlines
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	int insert(Airlines record);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airlines
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	int insertSelective(Airlines record);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airlines
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	List<Airlines> selectByExample(AirlinesExample example);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airlines
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	int updateByExampleSelective(@Param("record") Airlines record, @Param("example") AirlinesExample example);

	/**
	 * This method was generated by MyBatis Generator. This method corresponds to the database table public.airlines
	 * @mbggenerated  Sat Apr 02 15:30:07 CDT 2016
	 */
	int updateByExample(@Param("record") Airlines record, @Param("example") AirlinesExample example);
}