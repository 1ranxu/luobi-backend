package com.luoying.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建请求
 */
@Data
public class ChartAddRequest implements Serializable {

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 原始数据
     */
    private String rawData;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 图表名称
     */
    private String chartName;

    private static final long serialVersionUID = 1L;
}