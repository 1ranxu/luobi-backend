package com.luoying.model.vo;

import lombok.Data;

/**
 * BI的返回结果
 */
@Data
public class BIResponse {
    private String genChart;

    private String genSummary;

    private Long chartId;
}
