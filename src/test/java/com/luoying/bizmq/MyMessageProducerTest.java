package com.luoying.bizmq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MyMessageProducerTest {
    @Resource
    private MyMessageProducer myMessageProducer;

    /**
     * 先启动SpringBoot项目，再执行该测试
     */
    @Test
    void sendMessage() {
        myMessageProducer.sendMessage("code-exchange", "code-key", "你好");
    }
}