package com.zzh.luntan.entity;

import java.util.HashMap;
import java.util.Map;

public class Event {
    private String topic;
    // 事件触发人
    private int userId;
    // 事件作用实体
    private int entityType;
    // 作用实体id
    private int entityId;
    // 实体作者id
    private int entityOwnerId;
    // 其他属性
    private Map<String, Object> map=new HashMap<>();

    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }


    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityOwnerId() {
        return entityOwnerId;
    }

    public Event setEntityOwnerId(int entityOwnerId) {
        this.entityOwnerId = entityOwnerId;
        return this;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public Event setMap(String key, Object value) {
        this.map.put(key, value);
        return this;
    }
}
