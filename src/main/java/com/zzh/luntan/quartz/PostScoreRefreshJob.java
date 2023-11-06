package com.zzh.luntan.quartz;

import com.zzh.luntan.entity.Post;
import com.zzh.luntan.service.ElasticsearchService;
import com.zzh.luntan.service.LikeService;
import com.zzh.luntan.service.PostService;
import com.zzh.luntan.util.CommunityConstant;
import com.zzh.luntan.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class PostScoreRefreshJob implements Job, CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ElasticsearchService elasticsearchService;
    @Autowired
    private PostService postService;
    @Autowired
    private LikeService likeService;

    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2018-8-5 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化论坛纪元失败", e);
        }
    }

    // 任务逻辑
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String scoreKey = RedisKeyUtil.getScoreKey();
        // 获取列表
        BoundSetOperations setOperations = redisTemplate.boundSetOps(scoreKey);
        if (setOperations.size() == 0) {
            logger.info("此时间段内需重新计算帖子分数的数量为0,停止计算");
            return;
        }
        logger.info("[任务开始] 正在刷新帖子分数: " + setOperations.size());
        // 从列表中不断弹出帖子id
        while (setOperations.size() > 0) {
            refresh((Integer) setOperations.pop());
        }
        logger.info("[任务结束] 帖子分数刷新完毕!");
    }

    private void refresh(int postId) {
        Post post = postService.selectPostById(postId);
        if (post == null || post.getStatus() == 2) {
            logger.info("帖子不存在,id=" + postId);
            return;
        }
        // 重新计算分数
        boolean wonderful = post.getStatus() == 1;
        double weight = (wonderful ? 75 : 0) + post.getCommentCount() * 10 + likeService.likeCount(ENTITY_TYPE_POST, postId) * 2;
        double score = Math.log10(Math.max(weight, 1)) + (post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);
        postService.updateScore(postId, score);
        // 更新es
        post.setScore(score);
        elasticsearchService.addPostToES(post);
    }
}
