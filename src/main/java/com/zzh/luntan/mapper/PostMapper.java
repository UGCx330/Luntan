package com.zzh.luntan.mapper;

import com.zzh.luntan.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMapper {
    // orderMode为1，按照置顶，分数，时间排序
    // orderMode为0，按照置顶，时间排序
    List<Post> selectPost(@Param("userId") int userId, @Param("offset") int offset, @Param("limit") int limit, @Param("orderMode") int orderMode, @Param("plate") int plate);

    int postCounts(@Param("userId") int userId, @Param("plate") int plate);

    int addPost(Post post);

    Post selectPostById(@Param("postId") int postId);

    int updateCommentCount(@Param("entityId") int entityId, @Param("count") int count);

    int updateType(@Param("postId") int postId, @Param("type") int type);

    int updateStatus(@Param("postId") int postId, @Param("status") int status);

    int updateScore(@Param("postId") int postId, @Param("score") double score);
}
