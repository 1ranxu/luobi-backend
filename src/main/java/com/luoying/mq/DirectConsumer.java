package com.luoying.mq;

import com.rabbitmq.client.*;

import java.util.HashMap;
import java.util.Map;

public class DirectConsumer {

    private static final String EXCHANGE_NAME = "direct-exchange";

    public static void main(String[] argv) throws Exception {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        // 建立连接，创建频道
        Connection connection = factory.newConnection();
        Channel channel1 = connection.createChannel();
        Channel channel2 = connection.createChannel();
        // 创建交换机
        channel1.exchangeDeclare(EXCHANGE_NAME, "direct");
        // 创建队列
        String queueName1 = "小落的工作队列";
        String queueName2 = "小樱的工作队列";
        channel1.queueDeclare(queueName1, true, false, false, null);
        channel2.queueDeclare(queueName2, true, false, false, null);
        // 绑定交换机，指定路由键
        channel1.queueBind(queueName1, EXCHANGE_NAME, "xiaoluo");
        channel2.queueBind(queueName2, EXCHANGE_NAME, "xiaoying");
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        // 定义了小落如何处理消息
        DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [小落] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        // 定义了小樱如何处理消息
        DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [小樱] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        // 消费消息
        channel1.basicConsume(queueName1, true, deliverCallback1, consumerTag -> {
        });
        channel2.basicConsume(queueName2, true, deliverCallback2, consumerTag -> {
        });
    }
}