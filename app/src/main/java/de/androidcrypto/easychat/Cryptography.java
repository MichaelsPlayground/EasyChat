package de.androidcrypto.easychat;

import android.content.Context;
import android.net.Uri;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class Cryptography {
    // AesGcmEncryptionInlineIvPbkdf2BufferedCipherInputStream
    public static void main(String[] args) throws NoSuchPaddingException, NoSuchAlgorithmException, IOException,
            NoSuchProviderException, InvalidKeyException, InvalidKeySpecException, InvalidAlgorithmParameterException {
        System.out.println("Aes Gcm Encryption Inline Iv Pbkdf2 Buffered CipherInputStream");

        char[] password = "123456".toCharArray();
        int iterations = 10000;
        String uncryptedFilename = "uncrypted.txt";
        String encryptedFilename = "encrypted.enc";
        String decryptedFilename = "decrypted.txt";
        boolean result;
        result = encryptGcmFileBufferedCipherOutputStream(uncryptedFilename, encryptedFilename, password, iterations);
        System.out.println("result encryption: " + result);
        result = decryptGcmFileBufferedCipherInputStream(encryptedFilename, decryptedFilename, password, iterations);
        System.out.println("result decryption: " + result);

    }

    public static boolean encryptGcmFileBufferedCipherOutputStream(String inputFilename, String outputFilename, char[] password, int iterations) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[32];
        secureRandom.nextBytes(salt);
        byte[] nonce = new byte[12];
        secureRandom.nextBytes(nonce);
        Cipher cipher = Cipher.getInstance("AES/GCM/NOPadding");
        try (FileInputStream in = new FileInputStream(inputFilename);
             FileOutputStream out = new FileOutputStream(outputFilename);
             CipherOutputStream encryptedOutputStream = new CipherOutputStream(out, cipher);) {
            out.write(nonce);
            out.write(salt);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec keySpec = new PBEKeySpec(password, salt, iterations, 32 * 8); // 128 - 192 - 256
            byte[] key = secretKeyFactory.generateSecret(keySpec).getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);
            byte[] buffer = new byte[8096];
            int nread;
            while ((nread = in.read(buffer)) > 0) {
                encryptedOutputStream.write(buffer, 0, nread);
            }
            encryptedOutputStream.flush();
        }
        if (new File(outputFilename).exists()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean decryptGcmFileBufferedCipherInputStream(String inputFilename, String outputFilename, char[] password, int iterations) throws
            IOException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] salt = new byte[32];
        byte[] nonce = new byte[12];
        Cipher cipher = Cipher.getInstance("AES/GCM/NOPadding");

        try (FileInputStream in = new FileInputStream(inputFilename); // i don't care about the path as all is local
             CipherInputStream cipherInputStream = new CipherInputStream(in, cipher);
             FileOutputStream out = new FileOutputStream(outputFilename)) // i don't care about the path as all is local
        {
            byte[] buffer = new byte[8192];
            in.read(nonce);
            in.read(salt);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec keySpec = new PBEKeySpec(password, salt, iterations, 32 * 8); // 128 - 192 - 256
            byte[] key = secretKeyFactory.generateSecret(keySpec).getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
            int nread;
            while ((nread = cipherInputStream.read(buffer)) > 0) {
                out.write(buffer, 0, nread);
            }
            out.flush();
        }
        if (new File(outputFilename).exists()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * section for working with uri data
     */

    public static boolean encryptGcmFileBufferedCipherOutputStreamToCacheFile(Context context, Uri inputUri, String outputFilename, char[] password, int iterations) {

        //String inputFilename = "";
        //String outputFilename = "";

        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[32];
        secureRandom.nextBytes(salt);
        byte[] nonce = new byte[12];
        secureRandom.nextBytes(nonce);
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/GCM/NOPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            return false;
        }
        try (InputStream in = context.getContentResolver().openInputStream(inputUri);
             FileOutputStream out = new FileOutputStream(outputFilename);
             CipherOutputStream encryptedOutputStream = new CipherOutputStream(out, cipher)) {
            out.write(nonce);
            out.write(salt);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec keySpec = new PBEKeySpec(password, salt, iterations, 32 * 8); // 128 - 192 - 256
            byte[] key = secretKeyFactory.generateSecret(keySpec).getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);
            byte[] buffer = new byte[8096];
            int nread;
            while ((nread = in.read(buffer)) > 0) {
                encryptedOutputStream.write(buffer, 0, nread);
            }
            encryptedOutputStream.flush();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean decryptGcmFileBufferedCipherInputStreamFromCache(Context context, String inputFilename, Uri outputUri, char[] password, int iterations) {
        byte[] salt = new byte[32];
        byte[] nonce = new byte[12];
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/GCM/NOPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            return false;
        }

        try (FileInputStream in = new FileInputStream(inputFilename); // i don't care about the path as all is local
             CipherInputStream cipherInputStream = new CipherInputStream(in, cipher);
             OutputStream out = context.getContentResolver().openOutputStream(outputUri)) {
            byte[] buffer = new byte[8192];
            in.read(nonce);
            in.read(salt);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec keySpec = new PBEKeySpec(password, salt, iterations, 32 * 8); // 128 - 192 - 256
            byte[] key = secretKeyFactory.generateSecret(keySpec).getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
            int nread;
            while ((nread = cipherInputStream.read(buffer)) > 0) {
                out.write(buffer, 0, nread);
            }
            out.flush();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean decryptGcmFileBufferedCipherInputStreamFromInputStream(Context context, InputStream inputStream, Uri outputUri, char[] password, int iterations) {
        byte[] salt = new byte[32];
        byte[] nonce = new byte[12];
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/GCM/NOPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            return false;
        }

        try (BufferedInputStream in = new BufferedInputStream(inputStream);
             CipherInputStream cipherInputStream = new CipherInputStream(in, cipher);
             OutputStream out = context.getContentResolver().openOutputStream(outputUri)) {
            byte[] buffer = new byte[8192];
            in.read(nonce);
            in.read(salt);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec keySpec = new PBEKeySpec(password, salt, iterations, 32 * 8); // 128 - 192 - 256
            byte[] key = secretKeyFactory.generateSecret(keySpec).getEncoded();
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(16 * 8, nonce);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
            int nread;
            while ((nread = cipherInputStream.read(buffer)) > 0) {
                out.write(buffer, 0, nread);
            }
            out.flush();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean copyInputStreamFromInputStream(Context context, InputStream inputStream, Uri outputUri) {

        try (BufferedInputStream in = new BufferedInputStream(inputStream);
             OutputStream out = context.getContentResolver().openOutputStream(outputUri)) {
            byte[] buffer = new byte[8192];
            int nread;
            while ((nread = in.read(buffer)) > 0) {
                out.write(buffer, 0, nread);
            }
            out.flush();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * section for internal converter
     */

    private static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

}
