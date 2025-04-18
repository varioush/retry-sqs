package com.example.framework.model;

import java.io.Serializable;
import java.util.UUID;

public class Task implements Serializable {
    private String id;
    private String payload;
    private TaskType type;
    private int retryCount;
    private long lastRetryTimestamp;
    
    public enum TaskType {
        SYNC, ASYNC
    }
    
    public Task() {
        this.id = UUID.randomUUID().toString();
        this.retryCount = 0;
    }
    
    public Task(String payload, TaskType type) {
        this();
        this.payload = payload;
        this.type = type;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public void setPayload(String payload) {
        this.payload = payload;
    }
    
    public TaskType getType() {
        return type;
    }
    
    public void setType(TaskType type) {
        this.type = type;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public long getLastRetryTimestamp() {
        return lastRetryTimestamp;
    }
    
    public void setLastRetryTimestamp(long lastRetryTimestamp) {
        this.lastRetryTimestamp = lastRetryTimestamp;
    }
    
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryTimestamp = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", retryCount=" + retryCount +
                '}';
    }
}

