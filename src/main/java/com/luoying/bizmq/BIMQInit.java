package com.luoying.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * 用于创建测试用到的交换机和队列（只在主程序启动前执行一次）
 */
public class BIMQInit {
    public static void main(String[] args) throws Exception {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        // 建立连接，创建频道
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        // 创建交换机
        String EXCHANGE_NAME = BIMQConstant.BI_EXCHANGE_NAME;
        channel.exchangeDeclare(EXCHANGE_NAME,"direct");
        // 创建队列
        String queueName = BIMQConstant.BI_QUEUE_NAME;
        channel.queueDeclare(queueName, true, false, false, null);
        // 绑定交换机，指定路由键
        channel.queueBind(queueName, EXCHANGE_NAME, BIMQConstant.BI_ROUTING_KEY);
    }
}
