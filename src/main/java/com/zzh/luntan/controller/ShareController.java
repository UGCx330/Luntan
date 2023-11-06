package com.zzh.luntan.controller;

import com.zzh.luntan.entity.Event;
import com.zzh.luntan.event.EventProducer;
import com.zzh.luntan.util.CommunityConstant;
import com.zzh.luntan.util.CommunityUtil;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.*;
import java.util.HashMap;

@Controller
public class ShareController implements CommunityConstant {
    @Value("${zzh.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String contextPath;
    @Value("${wk.image.storage}")
    private String wkImageStorage;
    @Autowired
    private EventProducer eventProducer;
    @Value("${qiniu.bucket.share.url}")
    private String shareBucketUrl;
    private static final Logger logger = LoggerFactory.getLogger(ShareController.class);


    @PostMapping("/share")
    @ResponseBody
    public String share(String url) {
        String fileName = CommunityUtil.generateUUID();
        // 生成文件操作发给消息队列，因为耗时较长
        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setMap("url", url)
                .setMap("fileName", fileName)
                .setMap("suffix", ".png");
        eventProducer.fireEvent(event);
        HashMap<String, Object> map = new HashMap<>();
        // 返回给前端一个七牛云取出图片的路径，类似于取用户头像
        map.put("url", shareBucketUrl + "/" + fileName);
        return CommunityUtil.getJSONString(0, null, map);
    }

    //废弃
    // 此方法仅从本地返回图片
    @GetMapping("/takeShareImage/{fileName}")
    public void takeShareImage(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("文件名不能为空!");
        }
        response.setContentType("image/png");
        File file = new File(wkImageStorage + "/" + fileName + ".png");
        try {
            // 因为传输图片所以用字节流
            FileInputStream fileInputStream = new FileInputStream(file);
            ServletOutputStream outputStream = response.getOutputStream();
            byte[] bytes = new byte[1024];
            int b = 0;
            while ((b = fileInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, b);
            }
        } catch (IOException e) {
            logger.error("传输失败！" + e.getMessage());
        }
    }
}
