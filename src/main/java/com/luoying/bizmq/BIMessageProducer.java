package com.luoying.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class BIMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String exchange,String routingKey,String messgae){
        rabbitTemplate.convertAndSend(exchange,routingKey,messgae);
    }
}
