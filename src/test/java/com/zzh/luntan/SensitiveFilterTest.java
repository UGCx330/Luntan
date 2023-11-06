package com.zzh.luntan;

import com.zzh.luntan.util.SensitiveFilterUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = LuntanApplication.class)
public class SensitiveFilterTest {
    @Autowired
    SensitiveFilterUtil sensitiveFilterUtil;

    @Test
    public void test() {
        String test = "嫖娼呀，赌博";
        System.out.printf(sensitiveFilterUtil.filter(test));
    }
}
