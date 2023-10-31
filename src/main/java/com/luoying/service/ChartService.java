package com.luoying.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.luoying.model.dto.chart.ChartQueryRequest;
import com.luoying.model.entity.Chart;

/**
* @author 落樱的悔恨
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2023-10-31 14:16:29
*/
public interface ChartService extends IService<Chart> {
    /**
     * 获取查询条件
     *
     * @param chartQueryRequest
     * @return
     */
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

}
