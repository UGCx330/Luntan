package com.zzh.luntan.controller;

import com.zzh.luntan.entity.PageHelper;
import com.zzh.luntan.entity.Post;
import com.zzh.luntan.service.ElasticsearchService;
import com.zzh.luntan.service.LikeService;
import com.zzh.luntan.service.UserService;
import com.zzh.luntan.util.CommunityConstant;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;

@Controller
public class SearchController implements CommunityConstant {
    @Autowired
    private ElasticsearchService elasticsearchService;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;


    @GetMapping("/search")
    public String searchPost(String searchWord, Model model, PageHelper pageHelper) {
        pageHelper.setUrlPath("/search?searchWord="+searchWord);
        pageHelper.setLimit(10);
        Page<Post> result = elasticsearchService.searchFromES(searchWord, pageHelper.getCurrent() - 1, pageHelper.getLimit());

        // 构建显示结果List,在service的搜索中我们定义了如果搜索结果为空则返回null
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        if (result != null) {
            for (Post post : result) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("post", post);
                map.put("user", userService.selectUserById(post.getUserId()));
                map.put("likeCount", likeService.likeCount(ENTITY_TYPE_POST, post.getId()));
                list.add(map);
            }
        }
        pageHelper.setRows(result == null ? 0 : (int) result.getTotalElements());

        model.addAttribute("list", list);
        model.addAttribute("searchWord", searchWord);

        return "site/search";
    }
}
