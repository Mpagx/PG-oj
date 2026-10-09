package com.poj.poj;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 主类测试
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@SpringBootTest
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "poj.external.tests", matches = "true")
class MainApplicationTests {

    @Test
    void contextLoads() {
    }

}
