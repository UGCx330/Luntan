package com.zzh.luntan.controller;

import com.zzh.luntan.entity.Event;
import com.zzh.luntan.event.EventProducer;
import com.zzh.luntan.service.LikeService;
import com.zzh.luntan.util.CommunityConstant;
import com.zzh.luntan.util.CommunityUtil;
import com.zzh.luntan.util.RedisKeyUtil;
import com.zzh.luntan.util.ThreadLocalVector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.HashMap;

@Controller
public class LikeController implements CommunityConstant {
    @Autowired
    private ThreadLocalVector threadLocalVector;
    @Autowired
    private LikeService likeService;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate redisTemplate;

    // 点赞/取消点赞
    @PostMapping("/like")
    @ResponseBody
    public String like(int entityType, int entityId, int targetUserId, int postId) {
        int userId = threadLocalVector.getUser().getId();
        likeService.likeAndUnLike(userId, entityType, entityId, targetUserId);
        int likeStatus = likeService.isLike(userId, entityType, entityId);
        long likeCount = likeService.likeCount(entityType, entityId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("likeStatus", likeStatus);
        map.put("likeCount", likeCount);

        // 是点赞事件
        if (likeStatus == 1) {
            // 发送事件
            Event event = new Event()
                    .setUserId(threadLocalVector.getUser().getId())
                    .setTopic(TOPIC_LIKE)
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityOwnerId(targetUserId)
                    .setMap("postId", postId);
            eventProducer.fireEvent(event);
        }

        // 点赞或取消点赞都会重新计算分数
        if (entityType == ENTITY_TYPE_POST) {
            redisTemplate.opsForSet().add(RedisKeyUtil.getScoreKey(), postId);
        }

        return CommunityUtil.getJSONString(0, null, map);
    }


}
