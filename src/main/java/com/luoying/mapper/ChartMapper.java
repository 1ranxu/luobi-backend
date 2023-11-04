package com.luoying.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.luoying.model.entity.Chart;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
* @author 落樱的悔恨
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2023-10-31 14:16:29
* @Entity generator.domain.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {
    List<Map<String, Object>> queryChartData(@Param("querySql") String querySql);
}




