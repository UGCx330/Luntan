package com.zzh.luntan.mapper.elasticsearch;

import com.zzh.luntan.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends ElasticsearchRepository<Post, Integer> {
    //反射自动赋予实现类
}
