package de.androidcrypto.easychat.model;

public class UserChatKeystoreModel {

    private final String userId;
    private final int keyNumber;
    private byte[] keyNumberBytes;
    private final byte[] key;
    private final boolean isKey;

    public UserChatKeystoreModel(String userId, int keyNumber, byte[] key, boolean isKey) {
        this.userId = userId;
        this.keyNumber = keyNumber;
        this.key = key;
        this.isKey = isKey;
    }

    public String getUserId() {
        return userId;
    }

    public int getKeyNumber() {
        return keyNumber;
    }

    public byte[] getKeyNumberBytes() {
        return keyNumberBytes;
    }

    public byte[] getKey() {
        return key;
    }

    public boolean isKey() {
        return isKey;
    }
}
