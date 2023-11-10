package de.androidcrypto.easychat.model;

import android.net.Uri;
public class StorageFileModel {
    private String name;
    private Uri uri;
    private String downloadUrl;

    public StorageFileModel() {
    }
    public StorageFileModel(String name, Uri uri, String downloadUrl) {
        this.name = name;
        this.uri = uri;
        this.downloadUrl = downloadUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
