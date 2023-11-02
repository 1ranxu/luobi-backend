package com.luoying.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AIManagerTest {
    @Resource
    private AIManager aiManager;

    @Test
    void doChat() {
        String answer = aiManager.doChat(1719916921023344642L,"");
        System.out.println("answer = " + answer);
    }
}