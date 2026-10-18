package com.syncvault.db.models;
public class ActivityLog {
    private long logId;
    private Long fileId;
    private String action;
    private String details;
    private String createdAt;
    public ActivityLog() {
    }
    public ActivityLog(
            long logId,
            Long fileId,
            String action,
            String details,
            String createdAt
    ) {
        this.logId = logId;
        this.fileId = fileId;
        this.action = action;
        this.details = details;
        this.createdAt = createdAt;
    }
    public long getLogId() {
        return logId;
    }
    public void setLogId(long logId) {
        this.logId = logId;
    }
    public Long getFileId() {
        return fileId;
    }
    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }
    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public String getDetails() {
        return details;
    }
    public void setDetails(String details) {
        this.details = details;
    }
    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    @Override
    public String toString() {
        return "ActivityLog{" +
                "logId=" + logId +
                ", fileId=" + fileId +
                ", action='" + action + '\'' +
                ", details='" + details + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}