package com.zzh.luntan.controller;

import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.zzh.luntan.controller.annotation.LoginRequired;
import com.zzh.luntan.entity.Comment;
import com.zzh.luntan.entity.PageHelper;
import com.zzh.luntan.entity.Post;
import com.zzh.luntan.entity.User;
import com.zzh.luntan.service.*;
import com.zzh.luntan.util.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;


@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {
    @Autowired
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @Value("${community.path.header}")
    private String headerPath;
    @Autowired
    private UserService userService;
    @Autowired
    private ThreadLocalVector threadLocalVector;
    @Autowired
    private LikeService likeService;
    @Autowired
    private FollowService followService;
    @Autowired
    private PostService postService;
    @Autowired
    private CommentService commentService;
    @Value("${zzh.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String contextPath;
    @Value("${qiniu.key.access}")
    private String accessKey;
    @Value("${qiniu.key.secret}")
    private String secretKey;
    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;
    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;
    @Value("${qiniu.bucket.profileimg.name}")
    private String profileImgBucketName;
    @Value("${qiniu.bucket.profileimg.url}")
    private String profileImgBucketUrl;
    @Value("${qiniu.bucket.profilemusic.name}")
    private String profileMusicBucketName;
    @Value("${qiniu.bucket.profilemusic.url}")
    private String profileMusicBucketUrl;
    @Value("${qiniu.bucket.profilevideo.name}")
    private String profileVideoBucketName;
    @Value("${qiniu.bucket.profilevideo.url}")
    private String profileVideoBucketUrl;
    @Value("${qiniu.bucket.profileback.name}")
    private String profileBackBucketName;
    @Value("${qiniu.bucket.profileback.url}")
    private String profileBackBucketUrl;
    @Autowired
    private RedisTemplate redisTemplate;


    // 返回更新头像页面时，将存储至七牛云所需的凭证等信息一并携带过去，在html中使用js发送ajax请求携带这些参数访问七牛云即可存储
    @LoginRequired
    @GetMapping("/getSettingPage")
    public String getSettingPage(Model model) {
        // 头像文件名
        String fileName = CommunityUtil.generateUUID();
        // 上传成功时规定七牛云响应给我们的Json数据
        StringMap stringMap = new StringMap();
        // returnBody是固定写法
        stringMap.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);
        return "/site/setting";
    }

    // 将数据库中用户头像更新为七牛云连接
    @PostMapping("/updateHeader")
    @ResponseBody
    public String updateHeader(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名为空");
        }
        userService.updateHeaderUrl(threadLocalVector.getUser().getId(), headerBucketUrl + "/" + fileName);
        return CommunityUtil.getJSONString(0);
    }

    // 废弃
    // 头像存储到本地
    @LoginRequired
    @PostMapping("/headerSetting")
    public String headerSetting(MultipartFile multipartFile, Model model) {
        if (multipartFile == null) {
            model.addAttribute("headerMsg", "未选择文件！");
            return "/site/setting";
        }
        String originalFilename = multipartFile.getOriginalFilename();
        // 得到文件名后缀，如果为null，说明传入的文件没有格式
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("headerMsg", "未识别的文件格式！");
            return "/site/setting";
        }
        // 随机一个文件名，拼接用户上传的文件后缀，组成新的文件名。
        String newFileName = CommunityUtil.generateUUID() + suffix;
        // 将文件写入到盘中
        // 由于路径是随时可以更改的，配置到properties文件中
        File file = new File(headerPath + "/" + newFileName);
        try {
            multipartFile.transferTo(file);
        } catch (IOException e) {
            logger.error("上传文件到服务器失败" + e.getMessage());
            throw new RuntimeException("上传文件到服务器失败", e);
        }
        // 更新数据库表中头像为Controller中的路径，显示头像的时候再次请求另一个Controller中的方法获取头像http://localhost:8080/community/user/header/xxx.png
        // 肯定是登陆状态，所以从本地线程容器取user
        User user = threadLocalVector.getUser();
        // 拼接url
        userService.updateHeaderUrl(user.getId(), domain + contextPath + "/user/getHeaderImage/" + newFileName);
        return "redirect:/user/getSettingPage";
    }

    // 废弃
    // 从本地输出流输出头像
    @GetMapping("/getHeaderImage/{newFileName}")
    public void getHeaderImage(@PathVariable("newFileName") String newFileName, HttpServletResponse response) {
        // 设置response格式
        String suffix = newFileName.substring(newFileName.lastIndexOf(".") + 1);
        response.setContentType("image/" + suffix);
        String path = headerPath + "/" + newFileName;
        try (
                OutputStream outputStream = response.getOutputStream();
                // 从硬盘中获取该用户头像
                FileInputStream inputStream = new FileInputStream(path);
        ) {
            byte[] buffer = new byte[1024];
            int sign = 0;
            // 读取用户头像，输出用户头像
            while ((sign = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, sign);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("读取用户头像失败" + e.getMessage());
        }
    }

    @LoginRequired
    @PostMapping("/passwordSetting")
    public String passwordSetting(String oldPassword, String newPassword, String againNewPassword, Model model, @CookieValue("ticket") String ticket) {
        if (StringUtils.isBlank(oldPassword)) {
            model.addAttribute("oldPasswordMsg", "请输入原密码！");
            return "/site/setting";
        }
        if (StringUtils.isBlank(newPassword)) {
            model.addAttribute("newPasswordMsg", "请输入新密码！");
            return "/site/setting";
        }
        if (StringUtils.isBlank(againNewPassword)) {
            model.addAttribute("againNewPasswordMsg", "请确认密码！");
            return "/site/setting";
        }
        if (!newPassword.equals(againNewPassword)) {
            model.addAttribute("againNewPasswordMsg", "两次输入的密码不一致!！");
            return "/site/setting";
        }

        User user = threadLocalVector.getUser();
        String salt = user.getSalt();
        if (!CommunityUtil.MD5(oldPassword + salt).equals(user.getPassword())) {
            model.addAttribute("oldPasswordMsg", "原密码不一致！");
            return "/site/setting";
        }
        newPassword = CommunityUtil.MD5(newPassword + salt);
        userService.updatePassword(user.getId(), newPassword);
        // 将凭证置为过期，重新登陆
        userService.logout(ticket);
        return "redirect:/login";
    }

    @GetMapping("/profile/{userId}")
    public String profile(@PathVariable("userId") int userId, Model model) {
        User user = userService.selectUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在");
        }
        // 查用户被赞数量
        int userLikeCount = likeService.userLikeCount(userId);
        // 获取用户的关注用户数，粉丝数，关注状态
        long followeeCount = followService.getFolloweeCount(userId, ENTITY_TYPE_USER);
        long followerCount = followService.getFollowerCount(ENTITY_TYPE_USER, userId);
        boolean followStatus = false;
        if (threadLocalVector.getUser() != null) {
            followStatus = followService.getFollowStatus(ENTITY_TYPE_USER, userId, threadLocalVector.getUser().getId());
        }
        // 查询用户相册
        String img = user.getImg();
        ArrayList<String> profileImageList = new ArrayList<>();
        if (!StringUtils.isEmpty(img)) {
            String[] split = img.split("q");
            for (String s : split) {
                profileImageList.add(profileImgBucketUrl + "/" + s);
            }
            model.addAttribute("profileImageList", profileImageList);
        }
        // 查询用户视频
        String video = user.getVideo();
        if (!StringUtils.isEmpty(video)) {
            model.addAttribute("profileVideo", profileVideoBucketUrl + "/" + video);
        }
        // 查询用户背景图
        String back = user.getBack();
        if (!StringUtils.isEmpty(back)) {
            model.addAttribute("profileBack", profileBackBucketUrl + "/" + back);
        }
        // 查询用户music
        String music = user.getMusic();
        if (!StringUtils.isEmpty(music)) {
            ArrayList<Map<String, String>> musicList = new ArrayList<>();
            // 先按照~分割歌曲为n首
            String[] everyMusic = music.split("~");
            for (String s : everyMusic) {
                // 按照：分割七牛云的歌曲名和封面图名
                String[] split = s.split(":");
                // 此时split长度为2，第一个是七牛云的歌曲和封面共用前缀，第二个是歌曲的原名
                HashMap<String, String> map = new HashMap<>();
                map.put("qiniuMusic", profileMusicBucketUrl + "/" + split[0] + "Music");
                map.put("qiniuMusicImg", profileMusicBucketUrl + "/" + split[0] + "MusicImg");
                // 歌曲原名带有.mp3后缀，去除,防止原歌曲中含有"."，取分割完前面的部分再次拼接为歌曲名
                String[] musicNameSplit = split[1].split("\\.");
                int length = musicNameSplit.length;
                String name = "";
                if (length > 2) {// 说明原歌曲有.
                    for (int i = 0; i < length - 1; i++) {
                        name += ((i == (length - 2)) ? musicNameSplit[i] + "." : musicNameSplit[i]);
                    }
                } else {
                    // 说明原歌曲没有"."直接取musicNameSplit[0]即可
                    name = musicNameSplit[0];
                }
                map.put("musicName", name);
                musicList.add(map);
            }
            model.addAttribute("musicList", musicList);
        }
        String moderator = null;
        switch (user.getModerator()) {
            case 1:
                moderator = "生活区版主";
                break;
            case 2:
                moderator = "问答区版主";
                break;
            case 3:
                moderator = "知识区版主";
                break;
            case 4:
                moderator = "交易区版主";
                break;
            case 5:
                moderator = "动漫区版主";
                break;
            case 6:
                moderator = "游戏区版主";
                break;
        }
        model.addAttribute("moderator", moderator);

        // 规定上传图片时如果成功，七牛云响应给我们的Json数据
        StringMap stringMap = new StringMap();
        // returnBody是固定写法
        stringMap.put("returnBody", CommunityUtil.getJSONString(0));
        // 生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String imgUploadToken = auth.uploadToken(profileImgBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("imgUploadToken", imgUploadToken);
        // 上传音乐成功时规定七牛云响应给我们的Json数据
        String musicUploadToken = auth.uploadToken(profileMusicBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("musicUploadToken", musicUploadToken);
        // 上传视频成功时规定七牛云响应给我们的Json数据
        String videoUploadToken = auth.uploadToken(profileVideoBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("videoUploadToken", videoUploadToken);
        // 上传背景图成功时规定七牛云响应给我们的Json数据
        String backUploadToken = auth.uploadToken(profileBackBucketName, null, 3600, stringMap);
        // 前端发送凭证给七牛云即可
        model.addAttribute("backUploadToken", backUploadToken);

        model.addAttribute("followerCount", followerCount);
        model.addAttribute("followeeCount", followeeCount);
        model.addAttribute("followStatus", followStatus);
        model.addAttribute("userLikeCount", userLikeCount);
        model.addAttribute("user", user);
        return "/site/profile";
    }

    @GetMapping("/profile/post/{userId}")
    public String myPosts(@PathVariable("userId") int userId, PageHelper pageHelper, Model model) {
        User user = userService.selectUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user", user);
        pageHelper.setUrlPath("/user/profile/post/" + userId);
        int counts = postService.postCounts(userId, 0);
        pageHelper.setRows(counts);
        pageHelper.setLimit(5);
        List<Post> posts = postService.selectPostByUserId(userId, pageHelper.getOffSet(), pageHelper.getLimit(), 0, 0);
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        if (!posts.isEmpty()) {
            for (Post post : posts) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("post", post);
                map.put("likeCount", likeService.likeCount(ENTITY_TYPE_POST, post.getId()));
                list.add(map);
            }
        }
        // 查询用户背景图
        String back = user.getBack();
        if (!StringUtils.isEmpty(back)) {
            model.addAttribute("profileBack", profileBackBucketUrl + "/" + back);
        }
        model.addAttribute("list", list);
        model.addAttribute("counts", counts);
        return "/site/my-post";
    }

    @GetMapping("/profile/reply/{userId}")
    public String myReply(@PathVariable("userId") int userId, PageHelper pageHelper, Model model) {
        User user = userService.selectUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user", user);
        pageHelper.setUrlPath("/user/profile/reply/" + userId);
        int counts = commentService.selectCommentCountByUserId(userId);
        pageHelper.setRows(counts);
        pageHelper.setLimit(10);
        List<Comment> comments = commentService.selectCommentByUserId(userId, pageHelper.getOffSet(), pageHelper.getLimit());
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        HashSet<Integer> set = new HashSet<>();
        if (!comments.isEmpty()) {
            for (Comment comment : comments) {
                if (comment.getEntityType() == 1) {
                    HashMap<String, Object> map = new HashMap<>();
                    Post post = postService.selectPostById(comment.getEntityId());
                    map.put("post", post);
                    map.put("reply", comment);
                    set.add(post.getId());
                    list.add(map);
                } else if (comment.getEntityType() == 2) {
                    Post post = findFatherPost(comment.getEntityType(), comment.getEntityId());
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("post", post);
                    map.put("reply", comment);
                    set.add(post.getId());
                    list.add(map);
                }
            }
        }
        // 查询用户背景图
        String back = user.getBack();
        if (!StringUtils.isEmpty(back)) {
            model.addAttribute("profileBack", profileBackBucketUrl + "/" + back);
        }
        model.addAttribute("list", list);
        model.addAttribute("postCount", set.size());
        return "/site/my-reply";
    }

    private Post findFatherPost(int entityType, int entityId) {
        Comment comment;
        while (entityType != 1) {
            comment = commentService.selectCommentById(entityId);
            entityType = comment.getEntityType();
            entityId = comment.getEntityId();
        }
        return postService.selectPostById(entityId);
    }

    @PostMapping("/profileSetting")
    public String profileSetting(Model model, int userId, String newUserName, String newContact, String newDescription, @CookieValue("ticket") String ticket) {
        User oldUser = userService.selectUserByName(newUserName);
        if (oldUser != null) {
            model.addAttribute("userNameMsg", "该用户名已被使用！");
            return "/site/setting";
        }
        userService.updateProfile(userId, newUserName, newContact, newDescription);
        userService.logout(ticket);
        // 清除用户数据从redis中，防止别人看到的是未刷新的资料
        userService.delRedisKey(RedisKeyUtil.getUserKey(userId));
        return "redirect:/login";
    }

    @ResponseBody
    @PostMapping("/updateProfileImg")
    public String updateProfileImg(int userId, String fileNames) {
        if (userId == 0) {
            return CommunityUtil.getJSONString(1, "未登录");
        }
        userService.updateProfileImg(userId, fileNames);

        return CommunityUtil.getJSONString(0, "上传成功,若无图片稍后刷新网页");
    }

    @ResponseBody
    @PostMapping("/updateProfileMusic")
    public String updateProfileMusic(int userId, String fileName) {
        if (userId == 0) {
            return CommunityUtil.getJSONString(1, "未登录");
        }
        userService.updateProfileMusic(userId, fileName);
        return CommunityUtil.getJSONString(0, "上传成功，若无音乐稍后刷新网页");
    }

    @ResponseBody
    @PostMapping("/updateProfileVideo")
    public String updateProfileVideo(int userId, String fileName) {
        if (userId == 0) {
            return CommunityUtil.getJSONString(1, "未登录");
        }
        userService.updateProfileVideo(userId, fileName);
        return CommunityUtil.getJSONString(0, "上传成功，若无视频稍后刷新网页");
    }

    @ResponseBody
    @PostMapping("/updateProfileBack")
    public String updateProfileBack(int userId, String fileName) {
        if (userId == 0) {
            return CommunityUtil.getJSONString(1, "未登录");
        }
        userService.updateProfileBack(userId, fileName);
        return CommunityUtil.getJSONString(0, "上传成功，若无背景稍后刷新网页");
    }
}

