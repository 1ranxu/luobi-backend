package com.luoying.bizmq;

import com.luoying.common.ErrorCode;
import com.luoying.constant.CommonConstant;
import com.luoying.exception.BusinessException;
import com.luoying.manager.AIManager;
import com.luoying.model.entity.Chart;
import com.luoying.service.ChartService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.BindException;

@Component
@Slf4j
public class BIMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AIManager aiManager;

    //指定程序监听的消息队列和确认机制
    @RabbitListener(queues = {BIMQConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        if (StringUtils.isBlank(message)){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");

        }
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart==null){
            channel.basicNack(deliveryTag,false,false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"消息为空");
        }
        //先修改图表的任务状态为“执行中”；等执行成功后，修改为“已完成”，保存执行结果；执行失败后，修改为失败，记录失败信息
        //修改任务状态为执行中，减少重复执行的风险、同时让用户知晓执行状态
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        boolean result = chartService.updateById(updateChart);
        if (!result) {
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
            return;
        }
        // 调用鱼聪明SDK，得到响应

        String answer = aiManager.doChat(CommonConstant.BI_MODEL_ID, buildUserInput(chart));
        // 从AI响应结果中，取出需要的数据
        String[] splits = answer.split("【【【【【");
        if (splits.length < 3) {
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(), "AI生成错误");
            return;
        }
        String genChart = splits[1].trim();
        String genSummary = splits[2].trim();
        //执行成功
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        //todo 建议定义状态为枚举值
        updateChartResult.setStatus("succeeded");
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenSummary(genSummary);
        result = chartService.updateById(updateChartResult);
        if (!result) {
            channel.basicNack(deliveryTag,false,false);
            handleChartUpdateError(chart.getId(), "更新图表已完成状态失败");
            return;
        }
        //确认消息
        channel.basicAck(deliveryTag, false);
    }

    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus("failed");
        updateChart.setExecMessage(execMessage);
        boolean result = chartService.updateById(updateChart);
        if (!result) {
            log.error("更新图表失败状态失败，" + chartId + "，" + execMessage);
        }
    }

    /**
     * 构造用户请求
     * @param chart
     * @return
     */
    private String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String data = chart.getRawData();
        // 构造用户请求（分析目标，图表名称，图表类型，csv数据）
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        userInput.append(goal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(data).append("\n");
        return userInput.toString();
    }
}
