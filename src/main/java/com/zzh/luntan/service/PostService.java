package com.zzh.luntan.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.zzh.luntan.entity.Post;
import com.zzh.luntan.mapper.PostMapper;
import com.zzh.luntan.util.SensitiveFilterUtil;
import jakarta.annotation.PostConstruct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PostService {
    private static final Logger logger = LoggerFactory.getLogger(PostService.class);
    @Autowired
    PostMapper postMapper;
    @Autowired
    SensitiveFilterUtil sensitiveFilterUtil;
    // 如果caffeine的v是List集合，则说明一个caffeine对象能存储15个集合
    @Value("${caffeine.posts.max-size}")
    private Integer cacheSize;
    @Value("${caffeine.posts.expire-seconds}")
    private Integer expireSeconds;

    // Caffeine核心接口: Cache, LoadingCache, AsyncLoadingCache,通常我们使用第二个的实例
    // 如果只定义一个缓存对象，容易出现缓存中全是主页帖子的情况，不同分区的帖子进来会淘汰主页帖子，各种淘汰，所以每个分区都要使用一个缓存对象
    // 我们规定只有在userId为0并且orderMode为1的时候使用帖子缓存，键使用offset:limit组合为每页帖子缓存的唯一标识
    // 缓存未分区主页热度排行帖子
    private LoadingCache<String, List<Post>> indexPostLoadingCache;

    // 缓存生活区按照热度排行帖子
    private LoadingCache<String, List<Post>> lifePostLoadingCache;

    // 缓存问答区按照热度排行帖子
    private LoadingCache<String, List<Post>> answerPostLoadingCache;

    // 缓存知识区按照热度排行帖子
    private LoadingCache<String, List<Post>> knowledgePostLoadingCache;

    // 缓存交易区按照热度排行帖子
    private LoadingCache<String, List<Post>> dealPostLoadingCache;

    // 缓存动漫区按照热度排行帖子
    private LoadingCache<String, List<Post>> animeLoadingCache;

    // 缓存游戏区按照热度排行帖子
    private LoadingCache<String, List<Post>> gamePostLoadingCache;

    // 行数缓存，规定userId为0时候使用，由于此缓存一个键值对即可存储，那么键我们设置为0即可，且分区只有6个，所以15容量是够用的，所有分区共用此缓存对象即可
    private LoadingCache<String, Integer> rowsLoadingCache;

    // 缓存只需要实例化一次即可，所以当Spring实例化service的时候实例化即可。
    @PostConstruct
    private void init() {
        // 设置大小和过期时间
        indexPostLoadingCache = Caffeine.newBuilder().maximumSize(cacheSize).expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
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
        lifePostLoadingCache = Caffeine.newBuilder().maximumSize(cacheSize).expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
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
                        return postMapper.selectPost(0, offset, limit, 1, 1);
                    }
                });
        answerPostLoadingCache = Caffeine.newBuilder().maximumSize(cacheSize).expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
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
                        return postMapper.selectPost(0, offset, limit, 1, 2);
                    }
                });
        knowledgePostLoadingCache = Caffeine.newBuilder().maximumSize(cacheSize).expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
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
                        return postMapper.selectPost(0, offset, limit, 1, 3);
                    }
                });
        dealPostLoadingCache = Caffeine.newBuilder().maximumSize(cacheSize).expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
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
                        return postMapper.selectPost(0, offset, limit, 1, 4);
                    }
                });
        animeLoadingCache = Caffeine.newBuilder().maximumSize(cacheSize).expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
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
                        return postMapper.selectPost(0, offset, limit, 1, 5);
                    }
                });
        gamePostLoadingCache = Caffeine.newBuilder().maximumSize(cacheSize).expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
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
                        return postMapper.selectPost(0, offset, limit, 1, 6);
                    }
                });
        rowsLoadingCache = Caffeine.newBuilder().maximumSize(cacheSize).expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, Integer>() {
                    @Override
                    public @Nullable Integer load(String key) throws Exception {
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误!");
                        }
                        String[] split = key.split(":");
                        if (split == null || split.length != 2) {
                            throw new IllegalArgumentException("参数错误!");
                        }
                        int userId = Integer.parseInt(split[0]);
                        int plate = Integer.parseInt(split[1]);
                        logger.debug("缓存未命中，从数据库中取出数据");
                        return postMapper.postCounts(userId, plate);
                    }
                });
    }

    public List<Post> selectPostByUserId(int userId, int offset, int limit, int orderMode, int plate) {
        // 仅缓存热度排序，因为热度一时半会变不了
        if (userId == 0 && orderMode == 1) {
            if (plate == 0) {
                // 主页
                return indexPostLoadingCache.get(offset + ":" + limit);
            } else if (plate == 1) {
                // 生活
                return lifePostLoadingCache.get(offset + ":" + limit);
            } else if (plate == 2) {
                // 问答
                return answerPostLoadingCache.get(offset + ":" + limit);
            } else if (plate == 3) {
                // 知识
                return knowledgePostLoadingCache.get(offset + ":" + limit);
            } else if (plate == 4) {
                // 交易
                return dealPostLoadingCache.get(offset + ":" + limit);
            } else if (plate == 5) {
                // 动漫
                return animeLoadingCache.get(offset + ":" + limit);
            } else if (plate == 6) {
                // 游戏
                return gamePostLoadingCache.get(offset + ":" + limit);
            }
        }
        logger.debug("正在从数据库中获取帖子");
        return postMapper.selectPost(userId, offset, limit, orderMode, plate);
    }

    public int postCounts(int userId, int plate) {
        if (userId == 0) {
            return rowsLoadingCache.get(userId + ":" + plate);
        }
        logger.debug("正在从数据库中获取行数");
        return postMapper.postCounts(userId, plate);
    }

    public int addPost(Post post) {
        if (post == null) {
            // 抛出运行时异常
            throw new IllegalArgumentException("Controller传递Post参数为null");
        }
        // 转义Html标记语言
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        // 过滤敏感词
        post.setTitle(sensitiveFilterUtil.filter(post.getTitle()));
        post.setContent(sensitiveFilterUtil.filter(post.getContent()));
        return postMapper.addPost(post);
    }

    public Post selectPostById(int postId) {
        return postMapper.selectPostById(postId);
    }

    public int updateCommentCount(int entityId, int count) {
        return postMapper.updateCommentCount(entityId, count);
    }

    public int updateType(int postId, int type) {
        return postMapper.updateType(postId, type);
    }

    public int updateStatus(int postId, int status) {
        return postMapper.updateStatus(postId, status);
    }

    public int updateScore(int postId, double score) {
        return postMapper.updateScore(postId, score);
    }
}
