package com.now.nowbot;

import com.now.nowbot.service.OsuGetService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertNotEquals;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NowbotApplicationTest {
    @MockBean
    OsuGetService osuGetService;

    @Test
    void get() {
        assertNotEquals(0, "17064371L");
    }
}