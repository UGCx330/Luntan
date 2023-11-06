package com.zzh.luntan.controller;

import com.alibaba.fastjson2.JSONObject;
import com.zzh.luntan.entity.*;
import com.zzh.luntan.event.EventProducer;
import com.zzh.luntan.service.*;
import com.zzh.luntan.util.CommunityConstant;
import com.zzh.luntan.util.CommunityUtil;
import com.zzh.luntan.util.RedisKeyUtil;
import com.zzh.luntan.util.ThreadLocalVector;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/post")
public class PostController implements CommunityConstant {
    @Autowired
    private ThreadLocalVector threadLocalVector;
    @Autowired
    private PostService postService;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate redisTemplate;
    @Value("${qiniu.bucket.postImage.url}")
    private String postImageBucketUrl;
    @Value("${qiniu.bucket.postvideo.url}")
    private String postVideoBucketUrl;

    @PostMapping("/add")
    @ResponseBody
    public String addPost(String title, String content, String fileNames, String videoUrl, String iframe, String plate) {
        User user = threadLocalVector.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(403, "请登陆后再发帖");
        }

        // String image = null;
        // for (String name : fileNames) {
        //     image += postImageBucketUrl + "/" + name + "q";
        // }
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setUserId(user.getId());
        post.setCreateTime(new Date());
        if (!StringUtils.isEmpty(fileNames)) {
            post.setImage(fileNames);
        }
        if (!StringUtils.isEmpty(videoUrl)) {
            post.setVideoUrl(videoUrl);
        }
        if (!StringUtils.isEmpty(iframe)) {
            post.setIframe(iframe);
        }
        if (!StringUtils.isEmpty(plate)) {
            post.setPlate(plate);
        }
        postService.addPost(post);

        // 发布帖子事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setEntityType(ENTITY_TYPE_POST)
                .setUserId(threadLocalVector.getUser().getId())
                .setEntityId(post.getId());
        eventProducer.fireEvent(event);

        // 计算分数
        redisTemplate.opsForSet().add(RedisKeyUtil.getScoreKey(), post.getId());

        return CommunityUtil.getJSONString(0, "发表成功");
    }

    @GetMapping("/detail/{postId}")
    public String getPost(@PathVariable("postId") int postId, Model model, PageHelper pageHelper) {
        // 帖子标题,内容,图片
        Post post = postService.selectPostById(postId);
        User user = userService.selectUserById(post.getUserId());
        model.addAttribute("post", post);
        String image = post.getImage();
        ArrayList<String> postImageList = new ArrayList<>();
        if (image != null) {
            String[] split = image.split("q");
            for (String s : split) {
                postImageList.add(postImageBucketUrl + "/" + s);
            }
            model.addAttribute("postImageList", postImageList);
        }

        model.addAttribute("videoUrl", postVideoBucketUrl + "/" + post.getVideoUrl());
        model.addAttribute("user", user);
        // 帖子点赞数量及用户是否点赞该帖子
        model.addAttribute("postLikeCount", likeService.likeCount(ENTITY_TYPE_POST, postId));
        model.addAttribute("postLikeStatus", threadLocalVector.getUser() != null ? likeService.isLike(threadLocalVector.getUser().getId(), ENTITY_TYPE_POST, postId) : 0);
        // 设置针对某一帖子所有评论的页码的pageHelper
        pageHelper.setLimit(5);
        pageHelper.setUrlPath("/post/detail/" + postId);
        pageHelper.setRows(post.getCommentCount());
        // 针对某个帖子查询出所有的评论,并且针对某个评论查询出该评论的所有回复
        // 新建一个Map链表，每个map中存放评论，发表评论的用户，该评论的回复数量，该评论的回复Map链表
        // 该评论的回复链表中每个map存放回复，发表回复的用户，回复的目标用户
        List<Comment> commentList = commentService.selectComment(ENTITY_TYPE_POST, post.getId(), pageHelper.getOffSet(), pageHelper.getLimit());
        ArrayList<Map<String, Object>> commentMapList = new ArrayList<>();
        for (Comment comment : commentList) {
            HashMap<String, Object> commentMap = new HashMap<>();
            commentMap.put("comment", comment);
            commentMap.put("user", userService.selectUserById(comment.getUserId()));
            // 评论点赞数
            commentMap.put("commentLikeCount", likeService.likeCount(ENTITY_TYPE_COMMENT, comment.getId()));
            // 用户是否已点赞,未登录一律视为未赞
            commentMap.put("commentLikeStatus", threadLocalVector.getUser() != null ? likeService.isLike(threadLocalVector.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId()) : 0);
            // 每个评论的回复map链表
            List<Comment> replyList = commentService.selectComment(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
            ArrayList<Map<String, Object>> replyMapList = new ArrayList<>();
            if (replyList != null) {
                for (Comment reply : replyList) {
                    HashMap<String, Object> replyMap = new HashMap<>();
                    User targetUser = reply.getTargetId() == 0 ? null : userService.selectUserById(reply.getTargetId());
                    replyMap.put("user", userService.selectUserById(reply.getUserId()));
                    replyMap.put("reply", reply);
                    replyMap.put("targetUser", targetUser);
                    replyMap.put("replyLikeCount", likeService.likeCount(ENTITY_TYPE_COMMENT, reply.getId()));
                    replyMap.put("replyLikeStatus", threadLocalVector.getUser() != null ? likeService.isLike(threadLocalVector.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId()) : 0);
                    replyMapList.add(replyMap);
                }
            }
            commentMap.put("replyCount", commentService.selectCommentCount(ENTITY_TYPE_COMMENT, comment.getId()));
            commentMap.put("replyMapList", replyMapList);
            commentMapList.add(commentMap);
        }

        model.addAttribute("commentMapList", commentMapList);
        return "site/discuss-detail";
    }

    @PostMapping("/setTop")
    @ResponseBody
    public String setTop(int postId) {
        postService.updateType(postId, 1);
        // 发送事件，覆盖ES中此帖子
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(threadLocalVector.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(postId);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    @PostMapping("/setWonderful")
    @ResponseBody
    public String setWonderful(int postId) {
        postService.updateStatus(postId, 1);
        // 发送事件，覆盖ES中此帖子
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(threadLocalVector.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(postId);
        eventProducer.fireEvent(event);

        // 重新计算分数
        redisTemplate.opsForSet().add(RedisKeyUtil.getScoreKey(), postId);

        return CommunityUtil.getJSONString(0);
    }

    @PostMapping("delete")
    @ResponseBody
    public String delete(int postId) {
        postService.updateStatus(postId, 2);
        // 发送事件，删除ES中此帖子
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(threadLocalVector.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(postId);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

}