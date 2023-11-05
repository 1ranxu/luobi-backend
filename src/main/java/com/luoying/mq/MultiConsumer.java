package com.luoying.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

public class MultiConsumer {

    private static final String TASK_QUEUE_NAME = "multi_queue";

    public static void main(String[] argv) throws Exception {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        // 建立连接，创建频道
        final Connection connection = factory.newConnection();
        for (int i = 0; i < 2; i++) {
            final Channel channel = connection.createChannel();
            // 创建消息队列
            channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
            // 控制每个消费者最多同时处理 1 个任务
            channel.basicQos(1);
            // 定义了如何处理消息
            int finalI = i;
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                try {
                    // 处理逻辑
                    System.out.println(" [x] Received 编号" + finalI + "'" + message + "'");
                    //睡20秒，模拟工人每20秒处理一条消息，处理能力有限
                    Thread.sleep(20000);
                    // 确认消息
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(),false);
                } catch (Exception e) {
                    e.printStackTrace();
                    // 拒绝消息
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false );
                } finally {
                    System.out.println(" [x] Done");
                }
            };
            // 消费消息，会持续阻塞
            channel.basicConsume(TASK_QUEUE_NAME, false, deliverCallback, consumerTag -> {
            });
        }
    }
}