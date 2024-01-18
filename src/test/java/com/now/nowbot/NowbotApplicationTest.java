package com.now.nowbot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotEquals;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NowbotApplicationTest {

    @Test
    void get() {
        // 保证ioc容器起来就行, 其他测试以后再写
        assertNotEquals(0, "17064371L");
    }
}