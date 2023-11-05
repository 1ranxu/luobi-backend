package com.luoying.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.util.HashMap;
import java.util.Map;

public class DlxDirectConsumer {

    private static final String DEAD_EXCHANGE_NAME = "dlx-direct-exchange";
    private static final String WORK_EXCHANGE_NAME = "direct-exchange2";

    public static void main(String[] argv) throws Exception {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        // 建立连接，创建频道
        Connection connection = factory.newConnection();
        Channel channel1 = connection.createChannel();
        Channel channel2 = connection.createChannel();
        // 指定死信参数
        Map<String, Object> args1 = new HashMap<String, Object>();
        Map<String, Object> args2= new HashMap<String, Object>();
        // 指定死信交换机
        args1.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
        args2.put("x-dead-letter-exchange", DEAD_EXCHANGE_NAME);
        // 指定死信要转发到哪个队列
        args1.put("x-dead-letter-routing-key", "老板");
        args2.put("x-dead-letter-routing-key", "外包");

        // 创建工作交换机
        channel1.exchangeDeclare(WORK_EXCHANGE_NAME, "direct");
        // 创建员工队列
        String queueName1 = "小猫的工作队列";
        String queueName2 = "小狗的工作队列";
        channel1.queueDeclare(queueName1, true, false, false, args1);
        channel2.queueDeclare(queueName2, true, false, false, args2);
        // 绑定死信交换机，指定路由键
        channel1.queueBind(queueName1, WORK_EXCHANGE_NAME, "小猫");
        channel2.queueBind(queueName2, WORK_EXCHANGE_NAME, "小狗");
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        // 定义了小猫如何处理消息
        DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            //拒绝消息
            System.out.println(" [小猫] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
            channel1.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false);
        };
        // 定义了小狗如何处理消息
        DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            //拒绝消息
            System.out.println(" [小狗] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
            channel2.basicNack(delivery.getEnvelope().getDeliveryTag(),false,false);
        };
        // 消费消息
        channel1.basicConsume(queueName1, false, deliverCallback1, consumerTag -> {
        });
        channel2.basicConsume(queueName2, false, deliverCallback2, consumerTag -> {
        });
    }
}