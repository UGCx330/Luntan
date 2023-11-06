package com.zzh.luntan;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.zzh.luntan.entity.Post;
import com.zzh.luntan.mapper.PostMapper;
import com.zzh.luntan.service.PostService;
import com.zzh.luntan.util.SensitiveFilterUtil;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = LuntanApplication.class)
public class CaffeineTest {
    private static final Logger logger = LoggerFactory.getLogger(PostService.class);
    @Autowired
    PostMapper postMapper;
    // 如果caffeine的v是List集合，则说明一个caffeine对象能存储15个集合
    @Value("${caffeine.posts.max-size}")
    private Integer cacheSize;
    @Value("${caffeine.posts.expire-seconds}")
    private Integer expireSeconds;

    @Test
    public void test() {
        LoadingCache<String, List<Post>> indexPostLoadingCache = Caffeine.newBuilder().maximumSize(cacheSize).expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<Post>>() {
                    @Override
                    public @Nullable List<Post> load(String key) throws Exception {
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误!");
                        }
                        // 此处写当我们从缓存中取数据但是没有数据的时候，应该怎么从数据库中取出数据保存到缓存并返回
                        // s就是我们传入的key，也就是offset和limit的组合，所以我们需要拆分，从数据库取值
                        String[] split = key.split(":");
                        if (split == null || split.length != 2) {
                            throw new IllegalArgumentException("参数错误!");
                        }
                        int offset = Integer.parseInt(split[0]);
                        int limit = Integer.parseInt(split[1]);
                        logger.debug("缓存未命中，从数据库中取出数据");
                        return postMapper.selectPost(0, offset, limit, 1, 0);
                    }
                });
        indexPostLoadingCache.get(0 + ":" + 10);
        indexPostLoadingCache.get(10 + ":" + 10);
        indexPostLoadingCache.get(20 + ":" + 10);
        indexPostLoadingCache.get(30 + ":" + 10);

        System.out.println(indexPostLoadingCache.estimatedSize());

    }
}
