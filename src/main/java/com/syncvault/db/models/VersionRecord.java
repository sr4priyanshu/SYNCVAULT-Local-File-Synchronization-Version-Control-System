package com.syncvault.db.models;
public class VersionRecord {
    private long versionId;
    private long fileId;
    private int versionNumber;
    private String contentHash;
    private long fileSize;
    private String createdAt;
    private String storageType;
    private String storagePath;
    private Long parentVersionId;
    public VersionRecord() {
    }
    public VersionRecord(
            long versionId,
            long fileId,
            int versionNumber,
            String contentHash,
            long fileSize,
            String createdAt,
            String storageType,
            String storagePath,
            Long parentVersionId
    ) {
        this.versionId = versionId;
        this.fileId = fileId;
        this.versionNumber = versionNumber;
        this.contentHash = contentHash;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
        this.storageType = storageType;
        this.storagePath = storagePath;
        this.parentVersionId = parentVersionId;
    }
    public long getVersionId() {
        return versionId;
    }
    public void setVersionId(long versionId) {
        this.versionId = versionId;
    }
    public long getFileId() {
        return fileId;
    }
    public void setFileId(long fileId) {
        this.fileId = fileId;
    }
    public int getVersionNumber() {
        return versionNumber;
    }
    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }
    public String getContentHash() {
        return contentHash;
    }
    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
    public long getFileSize() {
        return fileSize;
    }
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    public String getStorageType() {
        return storageType;
    }
    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }
    public String getStoragePath() {
        return storagePath;
    }
    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }
    public Long getParentVersionId() {
        return parentVersionId;
    }
    public void setParentVersionId(Long parentVersionId) {
        this.parentVersionId = parentVersionId;
    }
    @Override
    public String toString() {
        return "VersionRecord{" +
                "versionId=" + versionId +
                ", fileId=" + fileId +
                ", versionNumber=" + versionNumber +
                ", contentHash='" + contentHash + '\'' +
                ", fileSize=" + fileSize +
                ", createdAt='" + createdAt + '\'' +
                ", storageType='" + storageType + '\'' +
                ", storagePath='" + storagePath + '\'' +
                ", parentVersionId=" + parentVersionId +
                '}';
    }
}