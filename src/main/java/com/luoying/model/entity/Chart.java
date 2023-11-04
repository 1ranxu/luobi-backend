package com.luoying.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 图表信息表
 * @TableName chart
 */
@TableName(value ="chart")
@Data
public class Chart implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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

    /**
     * AI生成的图表数据
     */
    private String genChart;

    /**
     * AI生成的分析总结
     */
    private String genSummary;

    /**
     * 创建者id
     */
    private Long userId;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 执行信息
     */
    private String execMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}