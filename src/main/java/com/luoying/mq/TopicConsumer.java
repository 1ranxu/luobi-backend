package com.luoying.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class TopicConsumer {

    private static final String EXCHANGE_NAME = "topic-exchange";

    public static void main(String[] argv) throws Exception {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        // 建立连接，创建频道
        Connection connection = factory.newConnection();
        Channel channel1 = connection.createChannel();
        Channel channel2 = connection.createChannel();
        Channel channel3 = connection.createChannel();
        // 创建交换机
        channel1.exchangeDeclare(EXCHANGE_NAME, "topic");
        // 创建队列
        String queueName1 = "前端的工作队列";
        String queueName2 = "后端的工作队列";
        String queueName3 = "产品的工作队列";
        channel1.queueDeclare(queueName1, true, false, false, null);
        channel2.queueDeclare(queueName2, true, false, false, null);
        channel3.queueDeclare(queueName3, true, false, false, null);
        // 绑定交换机，指定路由键
        channel1.queueBind(queueName1, EXCHANGE_NAME, "#.前端.#");
        channel2.queueBind(queueName2, EXCHANGE_NAME, "#.后端.#");
        channel3.queueBind(queueName3, EXCHANGE_NAME, "#.产品.#");
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        // 定义了前端如何处理消息
        DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [前端] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        // 定义了后端如何处理消息
        DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [后端] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        // 定义了产品如何处理消息
        DeliverCallback deliverCallback3 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [产品] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };
        // 消费消息
        channel1.basicConsume(queueName1, true, deliverCallback1, consumerTag -> {
        });
        channel2.basicConsume(queueName2, true, deliverCallback2, consumerTag -> {
        });
        channel3.basicConsume(queueName3, true, deliverCallback3, consumerTag -> {
        });
    }
}