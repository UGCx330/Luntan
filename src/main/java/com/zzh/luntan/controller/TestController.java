package com.zzh.luntan.controller;

import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.zzh.luntan.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {
    @Value("${qiniu.key.access}")
    private String accessKey;
    @Value("${qiniu.key.secret}")
    private String secretKey;
    @Value("${qiniu.bucket.postImage.name}")
    private String postImageBucketName;
    @Value("${qiniu.bucket.postImage.url}")
    private String postImageBucketUrl;
    @Value("${qiniu.bucket.postvideo.name}")
    private String postVideoBucketName;
    @Value("${qiniu.bucket.postvideo.url}")
    private String postVideoBucketUrl;

    @GetMapping("/test/imageUpload")
    public String imageTest(Model model) {
        // 文件名
        String fileName = CommunityUtil.generateUUID();
        // 上传成功时规定七牛云响应给我们的Json数据
        StringMap stringMap = new StringMap();
        // returnBody是固定写法
        stringMap.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(postImageBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);
        return "/site/test";
    }

    @GetMapping("/test/videoUpload")
    public String videoTest(Model model) {
        // 上传成功时规定七牛云响应给我们的Json数据
        StringMap stringMap = new StringMap();
        // returnBody是固定写法
        stringMap.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(postVideoBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("uploadToken", uploadToken);
        return "/site/postVideoTest";
    }

}
