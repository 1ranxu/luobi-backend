package com.luoying.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class MyMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String exchange,String routingKey,String messgae){
        rabbitTemplate.convertAndSend(exchange,routingKey,messgae);
    }
}
