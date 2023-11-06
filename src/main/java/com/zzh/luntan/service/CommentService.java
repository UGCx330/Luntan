package com.zzh.luntan.service;

import com.zzh.luntan.entity.Comment;
import com.zzh.luntan.mapper.CommentMapper;
import com.zzh.luntan.util.CommunityConstant;
import com.zzh.luntan.util.SensitiveFilterUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstant {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private SensitiveFilterUtil sensitiveFilterUtil;
    @Autowired
    private PostService postService;

    public int selectCommentCount(int entityType, int entityId) {
        return commentMapper.selectCommentCount(entityType, entityId);
    }

    public List<Comment> selectComment(int entityType, int entityId, int offSet, int limit) {
        return commentMapper.selectComment(entityType, entityId, offSet, limit);
    }

    public List<Comment> selectCommentByUserId(int userId, int offSet, int limit) {
        return commentMapper.selectCommentByUserId(userId, offSet, limit);
    }
    public int selectCommentCountByUserId(int userId) {
        return commentMapper.selectCommentCountByUserId(userId);
    }

    public Comment selectCommentById(int commentId) {
        return commentMapper.selectCommentById(commentId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {
        if (comment.getContent() == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilterUtil.filter(comment.getContent()));
        int rows = commentMapper.addComment(comment);
        // 如果是帖子的评论，因为详情页是直接用的post表的comment_count字段，所以评论后需要重新在comment表统计该帖子的评论数量，并更新到post表中
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            int count = commentMapper.selectCommentCount(comment.getEntityType(), comment.getEntityId());
            postService.updateCommentCount(comment.getEntityId(), count);
        }
        return rows;
    }
}
