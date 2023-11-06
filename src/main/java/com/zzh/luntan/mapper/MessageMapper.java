package com.zzh.luntan.mapper;

import com.zzh.luntan.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {
    // 查询某用户与所有其他用户的所有会话(最新一条消息)，且分页
    List<Message> selectLastOAllConversation(@Param("userId") int userId, @Param("offset") int offset, @Param("limit") int limit);

    // 查询某会话的所有私信，且分页
    List<Message> selectLettersOfConversation(@Param("conversationId") String conversationId, @Param("offset") int offset, @Param("limit") int limit);

    // 查询某会话的私信条数,分页使用，且需要显示在页面
    int lettersCountOfConversation(@Param("conversationId") String conversationId);

    // 查询某用户的所有会话数量，分页使用
    int conversationCount(@Param("userId") int userId);

    // 查询未读消息数量(所有未读或者某会话未读)
    int unreadCount(@Param("userId") int userId, @Param("conversationId") String conversationId);

    // 发送私信后插入私信，返回插入的私信id
    int addMessage(Message message);

    // 更新私信状态,返回更新条数
    int updateStatusOfMessage(List<Integer> idList, @Param("status") int status);

    // 查找某人某类通知的最新一条消息
    Message selectLatestOfTopic(@Param("userId") int userId, @Param("topic") String topic);

    // 查看某人当前类通知消息总数
    int selectTopicAllCount(@Param("userId") int userId, @Param("topic") String topic);

    // 查看某人通知未读数量（传入topic-某topic未读，不传-所有topic未读）
    int selectUnreadCountOfTopic(@Param("userId") int userId, @Param("topic") String topic);

    //查询某人某主题的通知列表,分页
    List<Message> selectTopicList (@Param("userId") int userId, @Param("topic") String topic, @Param("offset") int offset, @Param("limit") int limit);


}
