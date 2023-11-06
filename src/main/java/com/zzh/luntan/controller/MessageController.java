package com.zzh.luntan.controller;

import com.alibaba.fastjson2.JSONObject;
import com.zzh.luntan.entity.Event;
import com.zzh.luntan.entity.Message;
import com.zzh.luntan.entity.PageHelper;
import com.zzh.luntan.entity.User;
import com.zzh.luntan.service.MessageService;
import com.zzh.luntan.service.UserService;
import com.zzh.luntan.util.CommunityConstant;
import com.zzh.luntan.util.CommunityUtil;
import com.zzh.luntan.util.SensitiveFilterUtil;
import com.zzh.luntan.util.ThreadLocalVector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {
    @Autowired
    private ThreadLocalVector threadLocalVector;
    @Autowired
    private UserService userService;
    @Autowired
    private MessageService messageService;


    @GetMapping("/letter")
    public String selectLastOAllConversation(Model model, PageHelper pageHelper) {
        User user = threadLocalVector.getUser();
        pageHelper.setLimit(5);
        pageHelper.setUrlPath("/letter");
        pageHelper.setRows(messageService.conversationCount(user.getId()));
        List<Message> messageList = messageService.selectLastOAllConversation(user.getId(), pageHelper.getOffSet(), pageHelper.getLimit());
        ArrayList<HashMap<String, Object>> list = new ArrayList<>();
        if (messageList != null) {
            for (Message message : messageList) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("message", message);
                map.put("messageCount", messageService.lettersCountOfConversation(message.getConversationId()));
                map.put("conversationUnreadCount", messageService.unreadCount(user.getId(), message.getConversationId()));
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                map.put("targetUser", userService.selectUserById(targetId));
                list.add(map);
            }
        }
        int allUnreadCount = messageService.unreadCount(user.getId(), null);
        model.addAttribute("list", list);
        model.addAttribute("unreadCount", allUnreadCount);
        model.addAttribute("unreadCountOfTopic", messageService.selectUnreadCountOfTopic(user.getId(), null));
        return "site/letter";
    }

    @GetMapping("/letter/detail/{conversationId}")
    public String selectLettersOfConversation(Model model, PageHelper pageHelper, @PathVariable String conversationId) {
        pageHelper.setUrlPath("/letter/detail/" + conversationId);
        pageHelper.setLimit(5);
        pageHelper.setRows(messageService.lettersCountOfConversation(conversationId));
        List<Message> messageList = messageService.selectLettersOfConversation(conversationId, pageHelper.getOffSet(), pageHelper.getLimit());
        ArrayList<HashMap> list = new ArrayList<>();
        if (messageList != null) {
            for (Message message : messageList) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("message", message);
                map.put("fromUser", userService.selectUserById(message.getFromId()));
                list.add(map);
            }
        }
        List<Integer> idsList = getUnreadLetterIds(messageList);
        if (!idsList.isEmpty()) {
            messageService.updateStatusOfMessage(idsList);
        }
        model.addAttribute("list", list);
        model.addAttribute("targetUser", getTargetUser(conversationId));
        return "site/letter-detail";
    }

    // 将消息列表中未读消息的id提取出来
    private List<Integer> getUnreadLetterIds(List<Message> list) {
        ArrayList<Integer> idsList = new ArrayList<>();
        if (list != null) {
            for (Message message : list) {
                if (message.getToId() == threadLocalVector.getUser().getId() && message.getStatus() == 0) {
                    idsList.add(message.getId());
                }
            }
        }
        return idsList;
    }

    private User getTargetUser(String conversationId) {
        String[] split = conversationId.split("_");
        int id0 = Integer.parseInt(split[0]);
        int id1 = Integer.parseInt(split[1]);
        if (threadLocalVector.getUser().getId() == id0) {
            return userService.selectUserById(id1);
        } else {
            return userService.selectUserById(id0);
        }
    }

    @ResponseBody
    @PostMapping("/letter/send")
    public String addMessage(String targetName, String content) {
        User targetUser = userService.selectUserByName(targetName);
        if (targetUser == null) {
            return CommunityUtil.getJSONString(1, "该用户不存在！");
        }
        int fromId = threadLocalVector.getUser().getId();
        Message message = new Message();
        message.setCreateTime(new Date());
        message.setFromId(fromId);
        message.setToId(targetUser.getId());
        message.setContent(content);
        String conversationId = fromId < targetUser.getId() ? fromId + "_" + targetUser.getId() : targetUser.getId() + "_" + fromId;
        message.setConversationId(conversationId);
        messageService.addMessage(message);
        return CommunityUtil.getJSONString(0);
    }

    @GetMapping("/notice")
    public String getNotice(Model model) {
        User user = threadLocalVector.getUser();

        Message commentMessage = messageService.selectLatestOfTopic(user.getId(), TOPIC_COMMENT);
        if (commentMessage != null) {
            // model的map
            HashMap<String, Object> commentMap = new HashMap<>();
            // 将message表中的content转为map,以获取值
            String content = HtmlUtils.htmlUnescape(commentMessage.getContent());
            HashMap<String, Object> map = JSONObject.parseObject(content, HashMap.class);
            commentMap.put("message", commentMessage);
            commentMap.put("user", userService.selectUserById((Integer) map.get("userId")));
            commentMap.put("entityType", map.get("entityType"));
            commentMap.put("entityId", map.get("entityId"));
            commentMap.put("postId", map.get("postId"));
            // 数量
            commentMap.put("topicCount", messageService.selectTopicAllCount(user.getId(), TOPIC_COMMENT));
            commentMap.put("unreadCount", messageService.selectUnreadCountOfTopic(user.getId(), TOPIC_COMMENT));

            model.addAttribute("commentMap", commentMap);
        }

        Message likeMessage = messageService.selectLatestOfTopic(user.getId(), TOPIC_LIKE);
        if (likeMessage != null) {
            // model的map
            HashMap<String, Object> likeMap = new HashMap<>();
            // 将message表中的content转为map,以获取值
            String content = HtmlUtils.htmlUnescape(likeMessage.getContent());
            HashMap<String, Object> map = JSONObject.parseObject(content, HashMap.class);
            likeMap.put("message", likeMessage);
            likeMap.put("user", userService.selectUserById((Integer) map.get("userId")));
            likeMap.put("entityType", map.get("entityType"));
            likeMap.put("entityId", map.get("entityId"));
            likeMap.put("postId", map.get("postId"));
            // 数量
            likeMap.put("topicCount", messageService.selectTopicAllCount(user.getId(), TOPIC_LIKE));
            likeMap.put("unreadCount", messageService.selectUnreadCountOfTopic(user.getId(), TOPIC_LIKE));

            model.addAttribute("likeMap", likeMap);
        }

        Message followMessage = messageService.selectLatestOfTopic(user.getId(), TOPIC_FOLLOW);
        if (followMessage != null) {
            // model的map
            HashMap<String, Object> followMap = new HashMap<>();
            // 将message表中的content转为map,以获取值
            String content = HtmlUtils.htmlUnescape(followMessage.getContent());
            HashMap<String, Object> map = JSONObject.parseObject(content, HashMap.class);
            followMap.put("message", followMessage);
            followMap.put("user", userService.selectUserById((Integer) map.get("userId")));
            followMap.put("entityType", map.get("entityType"));
            followMap.put("entityId", map.get("entityId"));
            // 数量
            followMap.put("topicCount", messageService.selectTopicAllCount(user.getId(), TOPIC_FOLLOW));
            followMap.put("unreadCount", messageService.selectUnreadCountOfTopic(user.getId(), TOPIC_FOLLOW));

            model.addAttribute("followMap", followMap);
        }

        // 查询朋友私信未读数量
        model.addAttribute("unreadCount", messageService.unreadCount(user.getId(), null));
        // 查询未读通知数量
        model.addAttribute("unreadCountOfTopic", messageService.selectUnreadCountOfTopic(user.getId(), null));
        return "/site/notice";
    }

    @GetMapping("/notice/detail/{topic}")
    public String getNoticeDetail(Model model, @PathVariable("topic") String topic, PageHelper pageHelper) {
        User user = threadLocalVector.getUser();
        pageHelper.setLimit(5);
        pageHelper.setRows(messageService.selectTopicAllCount(user.getId(), topic));
        pageHelper.setUrlPath("/notice/detail/" + topic);
        List<Message> list = messageService.selectTopicList(user.getId(), topic, pageHelper.getOffSet(), pageHelper.getLimit());
        ArrayList<HashMap<String, Object>> messageList = new ArrayList<>();
        if (list != null) {
            for (Message message : list) {
                HashMap<String, Object> map = new HashMap<>();
                String content = HtmlUtils.htmlUnescape(message.getContent());
                HashMap map1 = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userService.selectUserById((Integer) map1.get("userId")));
                map.put("fromUser", userService.selectUserById(message.getFromId()));
                map.put("message", message);
                map.put("postId", map1.get("postId"));
                map.put("entityType", map1.get("entityType"));
                map.put("entityId", map1.get("entityId"));
                messageList.add(map);
            }
        }
        model.addAttribute("messageList", messageList);

        // 设置此页通知message为已读状态
        List<Integer> unreadLetterIds = getUnreadLetterIds(list);
        if (!unreadLetterIds.isEmpty()) {
            messageService.updateStatusOfMessage(unreadLetterIds);
        }

        return "/site/notice-detail";
    }


}
