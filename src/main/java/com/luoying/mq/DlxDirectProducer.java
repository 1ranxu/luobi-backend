package com.luoying.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.Scanner;

public class DlxDirectProducer {

    private static final String DEAD_EXCHANGE_NAME = "dlx-direct-exchange";
    private static final String WORK_EXCHANGE_NAME = "direct-exchange2";

    public static void main(String[] argv) throws Exception {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        // 建立连接，创建频道
        try (Connection connection = factory.newConnection();
             Channel channel1 = connection.createChannel();
             Channel channel2 = connection.createChannel();
        ) {
            // 创建死信交换机
            channel1.exchangeDeclare(DEAD_EXCHANGE_NAME, "direct");
            // 创建死信队列
            String queueName1 = "老板的死信队列";
            String queueName2 = "外包的死信队列";
            channel1.queueDeclare(queueName1, true, false, false, null);
            channel2.queueDeclare(queueName2, true, false, false, null);
            // 绑定死信交换机，指定路由键
            channel1.queueBind(queueName1, DEAD_EXCHANGE_NAME, "老板");
            channel2.queueBind(queueName2, DEAD_EXCHANGE_NAME, "外包");
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
            // 定义了老板如何处理死信
            DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [老板] Received 死信 '" +
                        delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
                channel1.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };
            // 定义了外包如何处理死信
            DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [外包] Received 死信 '" +
                        delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
                channel2.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };
            // 消费消息
            channel1.basicConsume(queueName1, false, deliverCallback1, consumerTag -> {
            });
            channel2.basicConsume(queueName2, false, deliverCallback2, consumerTag -> {
            });

            Scanner sc = new Scanner(System.in);
            while (sc.hasNext()) {
                String userInput = sc.nextLine();
                String[] strings = userInput.split(" ");
                if (strings.length < 1) {
                    continue;
                }
                String message = strings[0];
                // 从输入获取路由键
                String routingKey = strings[1];
                // 老板和外包向工作交换机发消息
                channel1.basicPublish(WORK_EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));
                System.out.println(" [x] Sent '" + message + "' with routing:" + routingKey);
            }
        }
    }
}