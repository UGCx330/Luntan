package com.zzh.luntan.service;

import com.zzh.luntan.entity.User;
import com.zzh.luntan.mapper.UserMapper;
import com.zzh.luntan.util.RedisKeyUtil;
import com.zzh.luntan.util.ThreadLocalVector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;
    @Autowired
    private ThreadLocalVector threadLocalVector;

    // 关注操作，同时向redis中插入两条数据，该用户关注了这个实体，该实体被这个用户关注。
    public void follow(int userId, int entityId, int entityType) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                operations.multi();
                // 有序集合，时间戳作为分数
                redisTemplate.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                redisTemplate.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                return operations.exec();
            }
        });
    }

    // 取关操作,同时删除两条数据
    public void deFollow(int userId, int entityId, int entityType) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                operations.multi();
                redisTemplate.opsForZSet().remove(followeeKey, entityId);
                redisTemplate.opsForZSet().remove(followerKey, userId);
                return operations.exec();
            }
        });
    }

    // 获取某类型的实体粉丝数量
    public long getFollowerCount(int entityType, int entityId) {
        return redisTemplate.opsForZSet().zCard(RedisKeyUtil.getFollowerKey(entityType, entityId));
    }

    // 获取某用户关注了某类型实体的数量
    public long getFolloweeCount(int userId, int entityType) {
        return redisTemplate.opsForZSet().zCard(RedisKeyUtil.getFolloweeKey(userId, entityType));
    }

    // 获取某用户对某类型的实体的关注状态(分数不为0即为关注)
    public boolean getFollowStatus(int entityType, int entityId, int userId) {
        return redisTemplate.opsForZSet().score(RedisKeyUtil.getFolloweeKey(userId, entityType), entityId) != null;
    }

    // 某用户关注的所有用户的列表,带分页
    public List<Map<String, Object>> getFolloweeList(int userId, int offset, int limit) {
        String key = RedisKeyUtil.getFolloweeKey(userId, 3);
        Set<Integer> followeeSet = redisTemplate.opsForZSet().reverseRange(key, offset, offset + limit - 1);
        if (followeeSet == null) {
            return null;
        }
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (int uid : followeeSet) {
            HashMap<String, Object> map = new HashMap<>();
            Double score = redisTemplate.opsForZSet().score(key, uid);
            map.put("followeeTime", new Date(score.longValue()));
            map.put("user", userService.selectUserById(uid));
            list.add(map);
        }
        return list;
    }

    // 某用户的所有粉丝列表，带分页
    public List<Map<String, Object>> getFollowerList(int userId, int offset, int limit) {
        String key = RedisKeyUtil.getFollowerKey(3, userId);
        Set<Integer> followerSet = redisTemplate.opsForZSet().reverseRange(key, offset, offset + limit - 1);
        if (followerSet == null) {
            return null;
        }
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (int uid : followerSet) {
            HashMap<String, Object> map = new HashMap<>();
            Double score = redisTemplate.opsForZSet().score(key, uid);
            map.put("followerTime", new Date(score.longValue()));
            map.put("user", userService.selectUserById(uid));
            list.add(map);
        }
        return list;
    }

}
