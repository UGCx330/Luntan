package com.zzh.luntan.mapper;

import com.zzh.luntan.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    public int selectCommentCount(@Param("entityType") int entityType, @Param("entityId") int entityId);

    List<Comment> selectComment(@Param("entityType") int entityType, @Param("entityId") int entityId, @Param("offSet") int offSet, @Param("limit") int limit);

    int addComment(Comment comment);

     Comment selectCommentById(@Param("commentId") int commentId);

    List<Comment> selectCommentByUserId(@Param("userId") int userId, @Param("offSet") int offSet, @Param("limit") int limit);

    int selectCommentCountByUserId(@Param("userId") int userId);

}
