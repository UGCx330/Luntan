package com.zzh.luntan.event;

import com.alibaba.fastjson2.JSONObject;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.zzh.luntan.entity.Event;
import com.zzh.luntan.entity.Message;
import com.zzh.luntan.entity.Post;
import com.zzh.luntan.service.ElasticsearchService;
import com.zzh.luntan.service.MessageService;
import com.zzh.luntan.service.PostService;
import com.zzh.luntan.util.CommunityConstant;
import com.zzh.luntan.util.CommunityUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

@Component
public class EventConsumer implements CommunityConstant {
    @Autowired
    private MessageService messageService;
    @Autowired
    private PostService postService;
    @Autowired
    private ElasticsearchService elasticsearchService;
    @Value("${wk.image.storage}")
    private String wkImageStorage;
    @Value("${wk.command}")
    private String wkCommand;
    @Value("${qiniu.key.access}")
    private String accessKey;
    @Value("${qiniu.key.secret}")
    private String secretKey;
    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;
    // spring提供的定时线程任务
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    // 消费者，将event转变为message插入mysql
    @KafkaListener(topics = {TOPIC_LIKE, TOPIC_FOLLOW, TOPIC_COMMENT})
    public void handleCommentMessage(ConsumerRecord consumerRecord) {
        if (consumerRecord == null || consumerRecord.value() == null) {
            logger.error("消息为空！");
            return;
        }
        Event event = JSONObject.parseObject(consumerRecord.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式不正确！");
            return;
        }

        // 将event转为message
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityOwnerId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        HashMap<String, Object> map = new HashMap<>();
        map.put("userId", event.getUserId());
        map.put("entityType", event.getEntityType());
        map.put("entityId", event.getEntityId());

        if (!event.getMap().isEmpty()) {
            for (Map.Entry<String, Object> entry : event.getMap().entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(map));

        messageService.addMessage(message);
    }

    // 增加/更新es中帖子
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord consumerRecord) {
        if (consumerRecord == null || consumerRecord.value() == null) {
            logger.error("消息为空！");
            return;
        }
        Event event = JSONObject.parseObject(consumerRecord.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式不正确！");
            return;
        }

        // 数据库中查询增加/更新后的帖子
        Post post = postService.selectPostById(event.getEntityId());
        // 将此帖子增加/更新到es中
        elasticsearchService.addPostToES(post);
    }

    // 删除es中帖子
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord consumerRecord) {
        if (consumerRecord == null || consumerRecord.value() == null) {
            logger.error("消息为空！");
            return;
        }
        Event event = JSONObject.parseObject(consumerRecord.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式不正确！");
            return;
        }

        // 将此帖子从es中删除
        elasticsearchService.delPostFromES(event.getEntityId());
    }

    @KafkaListener(topics = {TOPIC_SHARE})
    public void handleShareMessage(ConsumerRecord consumerRecord) {
        if (consumerRecord == null || consumerRecord.value() == null) {
            logger.error("消息为空！");
            return;
        }
        Event event = JSONObject.parseObject(consumerRecord.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式不正确！");
            return;
        }
        // 截取此路径的网站的长图
        String url = (String) event.getMap().get("url");
        String fileName = (String) event.getMap().get("fileName");
        String suffix = (String) event.getMap().get("suffix");
        // 拼接cmd命令
        String cmd = wkCommand + " -n --enable-local-file-access" + " --quality 75 " + url + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("已在生成长图" + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败!" + e.getMessage());
        }
        // 因为生成长图是新的cmd线程，所以我们并不知道什么时候生成完，只有生成完毕才能上传到七牛云
        // 所以需要定义一个线程任务，当监听者方法启用时，每隔0.5上传到七牛。
        UpLoad upLoad = new UpLoad(fileName, suffix);
        // future对象可以用来停止定时器，我们需要在run方法中当满足条件时使用来停止。
        // 此处返回的future对象是正常返回的，只是spring的定时器500ms后才执行，也就是run方法500ms之后执行
        Future future = threadPoolTaskScheduler.scheduleAtFixedRate(upLoad, 500);
        // 500ms之前就会执行到此处，我们将future对象设置进upload即可。
        upLoad.setFuture(future);
        // 主线程就直接走完了，然后500ms后线程任务启动，future对象也在upload里面了。
    }

    class UpLoad implements Runnable {

        // 文件名称
        private String fileName;
        // 文件后缀
        private String suffix;
        // 启动任务的返回值
        private Future future;
        // 开始时间
        private long startTime;
        // 上传次数
        private int uploadTimes;

        public UpLoad(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            if (uploadTimes >= 3) {
                logger.error("上传次数过多，终止");
                future.cancel(true);
                return;
            }
            if (System.currentTimeMillis() - startTime > 30000) {
                logger.error("上传超时，终止");
                future.cancel(true);
                return;
            }
            File file = new File(wkImageStorage + "/" + fileName + suffix);
            // 如果已在本地生成长图，则尝试上传到七牛
            if (file.exists()) {
                logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));
                // 设置如果上传成功七牛云返回的响应信息
                StringMap stringMap = new StringMap();
                stringMap.put("returnBody", CommunityUtil.getJSONString(0));
                // 设置凭证
                Auth auth = Auth.create(accessKey, secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, stringMap);
                // 指定上传机房(华北1),manager可以发送请求
                UploadManager manager = new UploadManager(new Configuration(Zone.zone1()));
                // 开始上传,image/表示类型时图片
                try {
                    // 返回的response含有七牛返回的信息，不要混淆
                    Response response = manager.put(file, fileName, uploadToken, null, "image/" + suffix, false);
                    // 返回信息是json格式，转为json对象，来获取我们当初要求响应的code等
                    JSONObject jsonObject = JSONObject.parseObject(response.bodyString());
                    if (jsonObject == null || jsonObject.get("code") == null || !jsonObject.get("code").toString().equals("0")) {
                        logger.error(String.format("第%d次上传失败[%s]", uploadTimes, fileName));
                    } else {
                        logger.info(String.format("第%d次上传成功[%s]", uploadTimes, fileName));
                        // 成功了就停止此线程任务
                        future.cancel(true);
                    }
                } catch (QiniuException e) {
                    logger.error(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                }
            } else {
                // 还没有生成长图，跳过
                logger.info("等待图片生成[" + fileName + "].");
            }
        }
    }

}
