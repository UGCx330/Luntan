package com.zzh.luntan.controller;

import com.zzh.luntan.entity.Event;
import com.zzh.luntan.entity.PageHelper;
import com.zzh.luntan.entity.User;
import com.zzh.luntan.event.EventProducer;
import com.zzh.luntan.service.FollowService;
import com.zzh.luntan.service.UserService;
import com.zzh.luntan.util.CommunityConstant;
import com.zzh.luntan.util.CommunityUtil;
import com.zzh.luntan.util.ThreadLocalVector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {
    @Autowired
    private ThreadLocalVector threadLocalVector;
    @Autowired
    private FollowService followService;
    @Autowired
    private UserService userService;
    @Autowired
    private EventProducer eventProducer;


    @PostMapping("/follow")
    @ResponseBody
    public String follow(int entityId, int entityType) {
        User user = threadLocalVector.getUser();
        if (user == null) {
            throw new RuntimeException();
        }
        followService.follow(user.getId(), entityId, entityType);

        //发送事件
        Event event = new Event()
                .setUserId(threadLocalVector.getUser().getId())
                .setTopic(TOPIC_FOLLOW)
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityOwnerId(entityId);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0, "已关注");
    }

    @PostMapping("/deFollow")
    @ResponseBody
    public String deFollow(int entityId, int entityType) {
        User user = threadLocalVector.getUser();
        if (user == null) {
            throw new RuntimeException();
        }
        followService.deFollow(user.getId(), entityId, entityType);
        return CommunityUtil.getJSONString(0, "已取消关注");
    }

    // 用户的关注列表
    @GetMapping("/followeeList/{userId}")
    public String followeeList(@PathVariable("userId") int userId, Model model, PageHelper pageHelper) {
        User user = userService.selectUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user", user);

        pageHelper.setUrlPath("/followeeList/" + userId);
        pageHelper.setLimit(5);
        pageHelper.setRows((int) followService.getFolloweeCount(userId, ENTITY_TYPE_USER));

        // 获取list
        List<Map<String, Object>> followeeList = followService.getFolloweeList(userId, pageHelper.getOffSet(), pageHelper.getLimit());
        for (Map<String, Object> map : followeeList) {
            User u = (User) map.get("user");
            // 获取登录用户对每个用户的关注状态
            map.put("followStatus", followStatus(u.getId()));
        }
        model.addAttribute("followeeList", followeeList);
        return "/site/followee";
    }

    // 用户的粉丝列表
    @GetMapping("/followerList/{userId}")
    public String followerList(@PathVariable("userId") int userId, Model model, PageHelper pageHelper) {
        User user = userService.selectUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user", user);

        pageHelper.setUrlPath("/followerList/" + userId);
        pageHelper.setLimit(5);
        pageHelper.setRows((int) followService.getFollowerCount(3, userId));

        // 获取list
        List<Map<String, Object>> followerList = followService.getFollowerList(userId, pageHelper.getOffSet(), pageHelper.getLimit());
        if (followerList != null) {
            for (Map<String, Object> map : followerList) {
                User u = (User) map.get("user");
                // 获取登录用户对每个用户的关注状态
                map.put("followStatus", followStatus(u.getId()));
            }
        }
        model.addAttribute("followerList", followerList);
        return "/site/follower";
    }

    private boolean followStatus(int entityId) {
        if (threadLocalVector.getUser() == null) {
            return false;
        }
        return followService.getFollowStatus(ENTITY_TYPE_USER,entityId,threadLocalVector.getUser().getId());
    }


}
