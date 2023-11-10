package de.androidcrypto.easychat.model;

import android.net.Uri;

public class FileInformation {
    private final String mimeType;
    private final String fileName;
    private final long fileSize;
    private Uri downloadUrl;

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

    public Uri getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(Uri downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
