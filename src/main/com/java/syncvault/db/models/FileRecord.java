package com.syncvault.db.models;

public class FileRecord {
    private long fileId;
    private long folderId;
    private String fileName;
    private String filePath;
    private String extension;
    private String currentHash;
    private long currentSize;
    private String createdAt;
    private String updatedAt;
    private boolean isMissing;

    public FileRecord() {
    }

    public FileRecord(
            long fileId,
            long folderId,
            String fileName,
            String filePath,
            String extension,
            String currentHash,
            long currentSize,
            String createdAt,
            String updatedAt,
            boolean isMissing
    ) {
        this.fileId = fileId;
        this.folderId = folderId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.extension = extension;
        this.currentHash = currentHash;
        this.currentSize = currentSize;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isMissing = isMissing;
    }

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public long getFolderId() {
        return folderId;
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
    }

    public long getCurrentSize() {
        return currentSize;
    }

    public void setCurrentSize(long currentSize) {
        this.currentSize = currentSize;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isMissing() {
        return isMissing;
    }

    public void setMissing(boolean isMissing) {
        this.isMissing = isMissing;
    }

    @Override
    public String toString() {
        return "FileRecord{" +
                "fileId=" + fileId +
                ", folderId=" + folderId +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", extension='" + extension + '\'' +
                ", currentHash='" + currentHash + '\'' +
                ", currentSize=" + currentSize +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", isMissing=" + isMissing +
                '}';
    }
}