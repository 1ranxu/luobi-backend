package com.luoying.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BIMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     * @param messgae
     */
    public void sendMessage(String messgae){
        rabbitTemplate.convertAndSend(BIMQConstant.BI_EXCHANGE_NAME,BIMQConstant.BI_ROUTING_KEY,messgae);
    }
}
