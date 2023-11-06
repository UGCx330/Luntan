package com.zzh.luntan.service;

import com.zzh.luntan.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 点赞与取消点赞,对于帖子和评论，向redis中set集合存储userId。统计赞的数量时，统计userId数量即可
    // 对于用户收到的赞，向redis中直接存储键值对，键即为userId，值就是数量
    public void likeAndUnLike(int userId, int entityType, int entityId, int targetUserId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String likeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userKey = RedisKeyUtil.getUserLikeKey(targetUserId);
                Boolean isMember = redisTemplate.opsForSet().isMember(likeKey, userId);
                // 开启事务
                operations.multi();
                if (isMember) {
                    redisTemplate.opsForSet().remove(likeKey, userId);
                    redisTemplate.opsForValue().decrement(userKey);
                } else {
                    redisTemplate.opsForSet().add(likeKey, userId);
                    redisTemplate.opsForValue().increment(userKey);
                }
                return operations.exec();
            }
        });
    }

    // 查看用户是否已点赞,1已点赞，0未点赞
    public int isLike(int userId, int entityType, int entityId) {
        String key = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(key, userId) ? 1 : 0;
    }

    // 查看实体的点赞数量
    public long likeCount(int entityType, int entityId) {
        return redisTemplate.opsForSet().size(RedisKeyUtil.getEntityLikeKey(entityType, entityId));
    }

    // 查看用户获得的赞数量
    public int userLikeCount(int userId) {
        // 没有赞就没有Object，Integer也就是null
        Integer count = (Integer) redisTemplate.opsForValue().get(RedisKeyUtil.getUserLikeKey(userId));
        return count == null ? 0 : count.intValue();
    }



}
