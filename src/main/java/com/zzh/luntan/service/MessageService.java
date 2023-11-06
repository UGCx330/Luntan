package com.zzh.luntan.service;

import com.zzh.luntan.entity.Message;
import com.zzh.luntan.entity.User;
import com.zzh.luntan.mapper.MessageMapper;
import com.zzh.luntan.util.SensitiveFilterUtil;
import com.zzh.luntan.util.ThreadLocalVector;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class MessageService {
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private SensitiveFilterUtil sensitiveFilterUtil;

    // 查询某用户与所有其他用户的所有会话(最新一条消息)，且分页
    public List<Message> selectLastOAllConversation(int userId, int offset, int limit) {
        return messageMapper.selectLastOAllConversation(userId, offset, limit);
    }

    // 查询某会话的所有私信，且分页
    public List<Message> selectLettersOfConversation(String conversationId, int offset, int limit) {
        return messageMapper.selectLettersOfConversation(conversationId, offset, limit);
    }

    // 查询某会话的私信条数,分页使用，且需要显示在页面
    public int lettersCountOfConversation(String conversationId) {
        return messageMapper.lettersCountOfConversation(conversationId);
    }

    // 查询某用户的所有会话数量，分页使用
    public int conversationCount(int userId) {
        return messageMapper.conversationCount(userId);
    }

    // 查询未读消息数量(所有未读或者某会话未读)
    public int unreadCount(int userId, String conversationId) {
        return messageMapper.unreadCount(userId, conversationId);
    }

    // 发送私信后插入私信，返回插入的私信id
    public int addMessage(Message message) {
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilterUtil.filter(message.getContent()));
        return messageMapper.addMessage(message);
    }

    // 更新私信未读状态为已读,返回更新条数
    public int updateStatusOfMessage(List<Integer> idList) {
        return messageMapper.updateStatusOfMessage(idList, 1);
    }

    // 查找某人某类通知的最新一条消息
    public Message selectLatestOfTopic(@Param("userId") int userId, @Param("topic") String topic) {
        return messageMapper.selectLatestOfTopic(userId, topic);
    }

    // 查看某人当前类通知消息总数
    public int selectTopicAllCount(@Param("userId") int userId, @Param("topic") String topic) {
        return messageMapper.selectTopicAllCount(userId, topic);
    }

    // 查看某人通知未读数量（传入topic-某topic未读，不传-所有topic未读）
    public int selectUnreadCountOfTopic(@Param("userId") int userId, @Param("topic") String topic) {
        return messageMapper.selectUnreadCountOfTopic(userId, topic);
    }

    // 查询某人某主题的通知列表,分页
    public List<Message> selectTopicList(@Param("userId") int userId, @Param("topic") String topic, @Param("offset") int offset, @Param("limit") int limit) {
        return messageMapper.selectTopicList(userId, topic, offset, limit);
    }
}
