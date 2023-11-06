package com.zzh.luntan.service;

import com.zzh.luntan.entity.Post;
import com.zzh.luntan.mapper.elasticsearch.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.CriteriaQueryBuilder;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ElasticsearchService {
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;
    @Autowired
    private PostRepository postRepository;

    // 增加/更新一条帖子到es服务器
    public void addPostToES(Post post) {
        postRepository.save(post);
    }

    // 删除一条帖子从es
    public void delPostFromES(int postId) {
        postRepository.deleteById(postId);
    }

    // 搜索帖子，从ES,分页
    public Page<Post> searchFromES(String searchWord, int current, int limit) {
        // 高亮字段构造，将title和content中匹配的字段前后加入em标签用于高亮显示
        ArrayList<HighlightField> highlightFieldList = new ArrayList<>();
        HighlightField highlightField = new HighlightField("title", HighlightFieldParameters.builder().withPreTags("<em>").withPostTags("</em>").build());
        highlightFieldList.add(highlightField);
        // 复用
        highlightField = new HighlightField("content", HighlightFieldParameters.builder().withPreTags("<em>").withPostTags("</em>").build());
        highlightFieldList.add(highlightField);
        // 构造高亮条件对象
        Highlight highlight = new Highlight(highlightFieldList);
        HighlightQuery highlightQuery = new HighlightQuery(highlight, Post.class);

        // 构造搜索条件对象
        Criteria criteria = new Criteria("title").matches(searchWord).or(new Criteria("content").matches(searchWord));
        // 被搜索字段条件
        CriteriaQueryBuilder criteriaQueryBuilder = new CriteriaQueryBuilder(criteria)
                // 排序条件
                .withSort(Sort.by(Sort.Direction.DESC, "type"))
                .withSort(Sort.by(Sort.Direction.DESC, "score"))
                .withSort(Sort.by(Sort.Direction.DESC, "createTime"))
                // 分页条件
                .withPageable(PageRequest.of(current, limit))
                // 高亮条件
                .withHighlightQuery(highlightQuery);
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteriaQueryBuilder);

        // 根据搜索条件使用elasticsearchTemplate得到结果集(本质是一个集合)
        SearchHits<Post> postSearchHits = elasticsearchTemplate.search(criteriaQuery, Post.class);
        List<SearchHit<Post>> searchHits = postSearchHits.getSearchHits();
        if (searchHits.isEmpty()) {
            return null;
        }
        ArrayList<Post> postList = new ArrayList<>();
        // 结果集中的post的title和content是不带em标签的，但是由于我们构造搜索条件的时候加入了高亮条件
        // 所以可以从结果集中获取高亮后的属性值，替换结果集中的原本属性值
        for (SearchHit<Post> searchHit : searchHits) {
            // 获取结果集中post
            Post post = searchHit.getContent();
            // 因为标题中可能出现多个不连续的高亮字词，所以是一个集合，集合中每个位置都是一个title，
            // 但是我们只需要将第一个匹配的字词高亮显示即可
            List<String> highlightTitle = searchHit.getHighlightField("title");
            if (highlightTitle.size() != 0) {
                post.setTitle(highlightTitle.get(0));
            }
            // 同理content
            List<String> highlightContent = searchHit.getHighlightField("content");
            if (highlightContent.size() != 0) {
                post.setContent(highlightContent.get(0));
            }
            postList.add(post);
        }

        // 因为需要用到分页信息，所以不光要Post结果集，还要要构建es中的Page对象。
        return new PageImpl<>(postList, PageRequest.of(current, limit), postSearchHits.getTotalHits());
    }
}
