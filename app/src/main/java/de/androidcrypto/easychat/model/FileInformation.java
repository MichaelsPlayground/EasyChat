package de.androidcrypto.easychat.model;

import android.net.Uri;

public class FileInformation {
    private final String mimeType;
    private final String fileName;
    private final long fileSize;
    private String fileStorage;
    private Uri downloadUrl;
    private String timestamp;
    public FileInformation(String mimeType, String fileName, Long fileSize) {
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }
    public String getMimeType() {
        return mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getFileStorage() {
        return fileStorage;
    }

    public void setFileStorage(String fileStorage) {
        this.fileStorage = fileStorage;
    }

    public Uri getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(Uri downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
