package de.androidcrypto.easychat.model;

public class FileInformation {
    private final String mimeType;
    private final String fileName;
    private final long fileSize;

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
}
