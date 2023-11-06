package com.zzh.luntan.service;

import com.zzh.luntan.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

@Service
public class DataService {
    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");

    // 将ip地址放入hyperloglog
    public void setUV(String ip) {
        String uvKey = RedisKeyUtil.getUVKey(simpleDateFormat.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(uvKey, ip);
    }

    // 将userId放入bitmap
    // userId即为bitmap中的第几位,如userId=123，则bitmap中的第123位置为1
    public void setDAU(int userId) {
        String dauKey = RedisKeyUtil.getDAUKey(simpleDateFormat.format(new Date()));
        redisTemplate.opsForValue().setBit(dauKey, userId, true);
    }

    // 获取日期范围内的不重复访客量,hyperloglog取每个日期范围作为key的ip集合去重即可
    public long calculateUV(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("日期不能为空");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        ArrayList<String> keyList = new ArrayList<>();
        while (!calendar.getTime().after(end)) {
            // 按照每个日期获得一个key
            keyList.add(RedisKeyUtil.getUVKey(simpleDateFormat.format(calendar.getTime())));
            // 延后一天
            calendar.add(Calendar.DATE, 1);
        }
        // 合并ip成为新的hyperloglog
        String uvKey = RedisKeyUtil.getUVKey(simpleDateFormat.format(start), simpleDateFormat.format(end));
        // 要求传入key的数组
        redisTemplate.opsForHyperLogLog().union(uvKey, keyList.toArray());
        return redisTemplate.opsForHyperLogLog().size(uvKey);
    }

    // 获取日期范围内的活跃用户量，只要用户访问过一次即算作活跃用户
    // 取每个日期的bitmap使用或运算
    public long calculateDAU(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("日期有误");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        ArrayList<byte[]> keyList = new ArrayList<>();
        while (!calendar.getTime().after(end)) {
            // 按照每个日期获得一个key
            keyList.add(RedisKeyUtil.getDAUKey(simpleDateFormat.format(calendar.getTime())).getBytes());
            // 延后一天
            calendar.add(Calendar.DATE, 1);
        }
        // 或运算合并bitmap成为新的bitmap
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String dauKey = RedisKeyUtil.getDAUKey(simpleDateFormat.format(start), simpleDateFormat.format(end));
                // byte[0][0]代表new一个二维数组长度为0
                // 回调函数内进行或运算，存储新的键值对
                connection.bitOp(RedisStringCommands.BitOperation.OR, dauKey.getBytes(), keyList.toArray(new byte[0][0]));
                // 获取bitmap为1的个数，要求传入的key是数组形式
                return connection.bitCount(dauKey.getBytes());
            }
        });

    }
}
