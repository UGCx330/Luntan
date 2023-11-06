package com.zzh.luntan.controller;

import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.zzh.luntan.entity.PageHelper;
import com.zzh.luntan.entity.Post;
import com.zzh.luntan.service.LikeService;
import com.zzh.luntan.service.PostService;
import com.zzh.luntan.service.UserService;
import com.zzh.luntan.util.CommunityConstant;
import com.zzh.luntan.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
public class HomeController implements CommunityConstant {
    @Autowired
    private PostService postService;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;
    @Value("${qiniu.key.access}")
    private String accessKey;
    @Value("${qiniu.key.secret}")
    private String secretKey;
    @Value("${qiniu.bucket.postImage.name}")
    private String postImageBucketName;
    @Value("${qiniu.bucket.postvideo.name}")
    private String postVideoBucketName;
    @Value("${luntan.admin.username}")
    private String admin;

    @GetMapping("/")
    public String root() {
        return "forward:/index";
    }

    // 路径中的参数名current与方法中的pageHelper中的属性current名字一致，可以自动注入
    @GetMapping(path = "/index")
    public String index(PageHelper pageHelper, Model model, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        pageHelper.setUrlPath("/index?orderMode=" + orderMode);
        // 设置总页数
        pageHelper.setRows(postService.postCounts(0, 0));
        // 传入user_id为0说明搜索全部用户帖子,orderMode如果为0则默认置顶和时间排序，为1则置顶，热度，时间排序
        List<Post> postList = postService.selectPostByUserId(0, pageHelper.getOffSet(), pageHelper.getLimit(), orderMode, 0);
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        // 存入map，渲染index
        for (Post post : postList) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("user", userService.selectUserById(post.getUserId()));
            map.put("post", post);
            // 查每个帖子的点赞数量
            map.put("likeCount", likeService.likeCount(ENTITY_TYPE_POST, post.getId()));
            list.add(map);
        }
        model.addAttribute("list", list);
        model.addAttribute("orderMode", orderMode);

        // 上传图片成功时规定七牛云响应给我们的Json数据
        StringMap stringMap = new StringMap();
        // returnBody是固定写法
        stringMap.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(postImageBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("uploadToken", uploadToken);

        // 上传视频成功时规定七牛云响应给我们的Json数据
        String videoUploadToken = auth.uploadToken(postVideoBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("videoUploadToken", videoUploadToken);
        model.addAttribute("plate", "板块分区");
        return "index";
    }

    @GetMapping(path = "/life")
    public String life(PageHelper pageHelper, Model model, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        pageHelper.setUrlPath("/life?orderMode=" + orderMode);
        // 设置总页数
        pageHelper.setRows(postService.postCounts(0, 1));
        // 传入user_id为0说明搜索全部用户帖子,orderMode如果为0则默认置顶和时间排序，为1则置顶，热度，时间排序
        List<Post> postList = postService.selectPostByUserId(0, pageHelper.getOffSet(), pageHelper.getLimit(), orderMode, 1);
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        // 存入map，渲染index
        for (Post post : postList) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("user", userService.selectUserById(post.getUserId()));
            map.put("post", post);
            // 查每个帖子的点赞数量
            map.put("likeCount", likeService.likeCount(ENTITY_TYPE_POST, post.getId()));
            list.add(map);
        }
        model.addAttribute("list", list);
        model.addAttribute("orderMode", orderMode);

        // 上传图片成功时规定七牛云响应给我们的Json数据
        StringMap stringMap = new StringMap();
        // returnBody是固定写法
        stringMap.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(postImageBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("uploadToken", uploadToken);

        // 上传视频成功时规定七牛云响应给我们的Json数据
        String videoUploadToken = auth.uploadToken(postVideoBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("videoUploadToken", videoUploadToken);
        model.addAttribute("plate", "生活区");

        return "site/plate/life";
    }

    @GetMapping(path = "/answer")
    public String answer(PageHelper pageHelper, Model model, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        pageHelper.setUrlPath("/answer?orderMode=" + orderMode);
        // 设置总页数
        pageHelper.setRows(postService.postCounts(0, 2));
        // 传入user_id为0说明搜索全部用户帖子,orderMode如果为0则默认置顶和时间排序，为1则置顶，热度，时间排序
        List<Post> postList = postService.selectPostByUserId(0, pageHelper.getOffSet(), pageHelper.getLimit(), orderMode, 2);
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        // 存入map，渲染index
        for (Post post : postList) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("user", userService.selectUserById(post.getUserId()));
            map.put("post", post);
            // 查每个帖子的点赞数量
            map.put("likeCount", likeService.likeCount(ENTITY_TYPE_POST, post.getId()));
            list.add(map);
        }
        model.addAttribute("list", list);
        model.addAttribute("orderMode", orderMode);

        // 上传图片成功时规定七牛云响应给我们的Json数据
        StringMap stringMap = new StringMap();
        // returnBody是固定写法
        stringMap.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(postImageBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("uploadToken", uploadToken);

        // 上传视频成功时规定七牛云响应给我们的Json数据
        String videoUploadToken = auth.uploadToken(postVideoBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("videoUploadToken", videoUploadToken);
        model.addAttribute("plate", "问答区");

        return "site/plate/answer";
    }

    @GetMapping(path = "/knowledge")
    public String knowledge(PageHelper pageHelper, Model model, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        pageHelper.setUrlPath("/knowledge?orderMode=" + orderMode);
        // 设置总页数
        pageHelper.setRows(postService.postCounts(0, 3));
        // 传入user_id为0说明搜索全部用户帖子,orderMode如果为0则默认置顶和时间排序，为1则置顶，热度，时间排序
        List<Post> postList = postService.selectPostByUserId(0, pageHelper.getOffSet(), pageHelper.getLimit(), orderMode, 3);
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        // 存入map，渲染index
        for (Post post : postList) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("user", userService.selectUserById(post.getUserId()));
            map.put("post", post);
            // 查每个帖子的点赞数量
            map.put("likeCount", likeService.likeCount(ENTITY_TYPE_POST, post.getId()));
            list.add(map);
        }
        model.addAttribute("list", list);
        model.addAttribute("orderMode", orderMode);

        // 上传图片成功时规定七牛云响应给我们的Json数据
        StringMap stringMap = new StringMap();
        // returnBody是固定写法
        stringMap.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(postImageBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("uploadToken", uploadToken);

        // 上传视频成功时规定七牛云响应给我们的Json数据
        String videoUploadToken = auth.uploadToken(postVideoBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("videoUploadToken", videoUploadToken);
        model.addAttribute("plate", "知识区");
        return "site/plate/knowledge";
    }

    @GetMapping(path = "/deal")
    public String deal(PageHelper pageHelper, Model model, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        pageHelper.setUrlPath("/deal?orderMode=" + orderMode);
        // 设置总页数
        pageHelper.setRows(postService.postCounts(0, 4));
        // 传入user_id为0说明搜索全部用户帖子,orderMode如果为0则默认置顶和时间排序，为1则置顶，热度，时间排序
        List<Post> postList = postService.selectPostByUserId(0, pageHelper.getOffSet(), pageHelper.getLimit(), orderMode, 4);
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        // 存入map，渲染index
        for (Post post : postList) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("user", userService.selectUserById(post.getUserId()));
            map.put("post", post);
            // 查每个帖子的点赞数量
            map.put("likeCount", likeService.likeCount(ENTITY_TYPE_POST, post.getId()));
            list.add(map);
        }
        model.addAttribute("list", list);
        model.addAttribute("orderMode", orderMode);

        // 上传图片成功时规定七牛云响应给我们的Json数据
        StringMap stringMap = new StringMap();
        // returnBody是固定写法
        stringMap.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(postImageBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("uploadToken", uploadToken);

        // 上传视频成功时规定七牛云响应给我们的Json数据
        String videoUploadToken = auth.uploadToken(postVideoBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("videoUploadToken", videoUploadToken);
        model.addAttribute("plate", "交易区");

        return "site/plate/deal";
    }

    @GetMapping(path = "/anime")
    public String anime(PageHelper pageHelper, Model model, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        pageHelper.setUrlPath("/anime?orderMode=" + orderMode);
        // 设置总页数
        pageHelper.setRows(postService.postCounts(0, 5));
        // 传入user_id为0说明搜索全部用户帖子,orderMode如果为0则默认置顶和时间排序，为1则置顶，热度，时间排序
        List<Post> postList = postService.selectPostByUserId(0, pageHelper.getOffSet(), pageHelper.getLimit(), orderMode, 5);
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        // 存入map，渲染index
        for (Post post : postList) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("user", userService.selectUserById(post.getUserId()));
            map.put("post", post);
            // 查每个帖子的点赞数量
            map.put("likeCount", likeService.likeCount(ENTITY_TYPE_POST, post.getId()));
            list.add(map);
        }
        model.addAttribute("list", list);
        model.addAttribute("orderMode", orderMode);

        // 上传图片成功时规定七牛云响应给我们的Json数据
        StringMap stringMap = new StringMap();
        // returnBody是固定写法
        stringMap.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(postImageBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("uploadToken", uploadToken);

        // 上传视频成功时规定七牛云响应给我们的Json数据
        String videoUploadToken = auth.uploadToken(postVideoBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("videoUploadToken", videoUploadToken);
        model.addAttribute("plate", "动漫区");

        return "site/plate/anime";
    }

    @GetMapping(path = "/game")
    public String game(PageHelper pageHelper, Model model, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        pageHelper.setUrlPath("/game?orderMode=" + orderMode);
        // 设置总页数
        pageHelper.setRows(postService.postCounts(0, 6));
        // 传入user_id为0说明搜索全部用户帖子,orderMode如果为0则默认置顶和时间排序，为1则置顶，热度，时间排序
        List<Post> postList = postService.selectPostByUserId(0, pageHelper.getOffSet(), pageHelper.getLimit(), orderMode, 6);
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        // 存入map，渲染index
        for (Post post : postList) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("user", userService.selectUserById(post.getUserId()));
            map.put("post", post);
            // 查每个帖子的点赞数量
            map.put("likeCount", likeService.likeCount(ENTITY_TYPE_POST, post.getId()));
            list.add(map);
        }
        model.addAttribute("list", list);
        model.addAttribute("orderMode", orderMode);

        // 上传图片成功时规定七牛云响应给我们的Json数据
        StringMap stringMap = new StringMap();
        // returnBody是固定写法
        stringMap.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(postImageBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("uploadToken", uploadToken);

        // 上传视频成功时规定七牛云响应给我们的Json数据
        String videoUploadToken = auth.uploadToken(postVideoBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("videoUploadToken", videoUploadToken);
        model.addAttribute("plate", "游戏区");

        return "site/plate/game";
    }

    @GetMapping("/500Error")
    public String error() {
        return "/error/500";
    }

    @GetMapping("/denied")
    public String getDeniedPage() {
        return "/error/404";
    }

    @GetMapping("/sharePage")
    public String getSharePage() {
        return "/site/share";
    }

    @GetMapping("/applyAdmin")
    public String applyAdmin(Model model) {
        model.addAttribute("admin", admin);
        return "/site/applyAdmin";
    }
}
