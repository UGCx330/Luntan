package com.zzh.luntan.controller;

import com.zzh.luntan.entity.Comment;
import com.zzh.luntan.entity.Event;
import com.zzh.luntan.event.EventProducer;
import com.zzh.luntan.quartz.PostScoreRefreshJob;
import com.zzh.luntan.service.CommentService;
import com.zzh.luntan.service.PostService;
import com.zzh.luntan.util.CommunityConstant;
import com.zzh.luntan.util.RedisKeyUtil;
import com.zzh.luntan.util.ThreadLocalVector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Date;

@Controller
public class CommentController implements CommunityConstant {
    @Autowired
    private CommentService commentService;
    @Autowired
    private ThreadLocalVector threadLocalVector;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private PostService postService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("/comment/addComment/{postId}")
    public String addComment(Comment comment, @PathVariable("postId") int postId) {
        // 需要前端隐藏输入框设置entityType和entityId和targetId
        comment.setCreateTime(new Date());
        comment.setUserId(threadLocalVector.getUser().getId());
        comment.setStatus(0);
        commentService.addComment(comment);

        // 发送事件
        Event event = new Event()
                .setUserId(threadLocalVector.getUser().getId())
                .setTopic(TOPIC_COMMENT)
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setMap("postId", postId);
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            // 如果评论的是帖子，那么查找帖子主人id
            event.setEntityOwnerId(postService.selectPostById(comment.getEntityId()).getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            // 如果回复的是评论/回复，那么查找其主人id
            event.setEntityOwnerId(commentService.selectCommentById(comment.getEntityId()).getUserId());
        }
        eventProducer.fireEvent(event);

        // 如果此评论是针对帖子发布的评论，则导致数据库中的帖子中的commentCount字段发生改变，所以要更新es中的此帖子
        // 发布帖子事件
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(postId);
            eventProducer.fireEvent(event);

            // 将该帖子id存入要计算分数的redis的set中
            redisTemplate.opsForSet().add(RedisKeyUtil.getScoreKey(), postId);
        }


        return "redirect:/post/detail/" + postId;
    }
}
