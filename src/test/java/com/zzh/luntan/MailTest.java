package com.zzh.luntan;

import com.zzh.luntan.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = LuntanApplication.class)
public class MailTest {
    @Autowired
    MailClient mailClient;

    @Autowired
    TemplateEngine templateEngine;

    @Test
    public void mailTest() {
        mailClient.sendMail("zzh1230@foxmail.com", "test", "test");
    }

    @Test
    public void htmlMailTest() {
        Context context = new Context();
        context.setVariable("fromName", "a935668820");
        // 此处即为Html结构体，发送到收件人浏览器会自动识别html
        String process = templateEngine.process("/mail/test", context);

        mailClient.sendMail("zzh1230@foxmail.com", "htmlTest", process);
    }
}
