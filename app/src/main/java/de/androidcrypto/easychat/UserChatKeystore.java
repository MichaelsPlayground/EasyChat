package de.androidcrypto.easychat;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import de.androidcrypto.easychat.model.UserChatKeystoreModel;

/**
 * The class is responsible for secret storage of user keys that were used to encrypt chat messages.
 * As we are using a modern, "Post Quantum Cryptography" secured key exchange the regular keystore
 * are not capable for storing those keys.
 * The well known RSA- and EC- based methods are using (private and public) keys with a length of
 * less than 100 bytes length, the PQC ones use much longer keys.
 * This keystore will save the keys in an encrypted way using:
 * AES-256 encryption in GCM mode with random nonce. The key is derived by a PBKDF2 key derivation,
 * the algorithm is 'PBKDF2WithHmacSHA256' with 10000 iterations and random salt.
 * <p>
 * The keystore is accepting these keys:
 * a) private key of user with key number
 * b) public key of user with key number
 * c) public key of other parties with key number
 * <p>
 * The key data is stored in these files:
 * user_private_keys.ucks
 * user_public_keys.ucks
 * other_public_keys.ucks
 */

public class UserChatKeystore {
    private Context context;
    private char[] passwordChar;
    private final int PASSWORD_CHAR_MINIMUM_LENGTH = 3;
    private byte[] nonce;
    private final int NONCE_LENGTH = 12;
    private byte[] salt;
    private final int SALT_LENGTH = 32;
    private final int NUMBER_OF_PBKDF2_ITERATIONS = 10000;
    private final int DERIVED_KEY_LENGTH = 256; // 256 bit = 32 byte
    private final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private byte[] derivedKey;
    private boolean isUserChatKeystoreValid = false;
    private final String USER_PRIVATE_KEYS_FILENAME = "user_private_keys.ucks";
    private final String USER_PUBLIC_KEYS_FILENAME = "user_public_keys.ucks";
    private final String OTHER_PUBLIC_KEYS_FILENAME = "other_public_keys.ucks";
    private List<UserChatKeystoreModel> ucksListUserPrivateKeys;
    private List<UserChatKeystoreModel> ucksListUserPublicKeys;
    private List<UserChatKeystoreModel> ucksListOtherPublicKeys;

    public UserChatKeystore(Context context, char[] passwordChar) {
        // sanity checks
        if (context == null) {
            isUserChatKeystoreValid = false;
            return;
        }
        if ((passwordChar == null) || (passwordChar.length < PASSWORD_CHAR_MINIMUM_LENGTH)) {
            isUserChatKeystoreValid = false;
            return;
        }
        this.passwordChar = passwordChar;
        this.context = context;
        try {
            SecureRandom secureRandom = new SecureRandom();
            salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            SecretKeyFactory secretKeyFactory = null;
            secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            KeySpec keySpec = new PBEKeySpec(passwordChar, salt, NUMBER_OF_PBKDF2_ITERATIONS, DERIVED_KEY_LENGTH);
            derivedKey = secretKeyFactory.generateSecret(keySpec).getEncoded();

            // todo create the empty files if not exist, otherwise load them
            ucksListUserPrivateKeys = new ArrayList<>();
            ucksListUserPublicKeys = new ArrayList<>();
            ucksListOtherPublicKeys = new ArrayList<>();

            boolean loadUserPrivateKeyList = loadUserChatKeystoreUserPrivateKeys();
            System.out.println(("loadUserPrivateKeyList" + loadUserChatKeystoreUserPrivateKeys()));
            if  (!loadUserPrivateKeyList) {
                boolean saveUserPrivateKeyList = saveUserChatKeystoreUserPrivateKeys();
                System.out.println(("saveloadUserPrivateKeyList: " + saveUserPrivateKeyList));
            }



            isUserChatKeystoreValid = true;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            //throw new RuntimeException(e);
            isUserChatKeystoreValid = false;
            return;
        }
    }

    public boolean saveUserPrivateKey(String userId, int keyNumber, byte[] privateKey) {
        // sanity checks
        if (TextUtils.isEmpty(userId)) return false;
        if ((privateKey == null) || (privateKey.length < 1)) {
            return false;
        }

        // todo load list of objects
        loadUserChatKeystoreUserPrivateKeys();
        UserChatKeystoreModel ucks = new UserChatKeystoreModel(userId, keyNumber, privateKey, true);
        ucksListUserPrivateKeys.add(ucks);

        // to save list of objects
        return true;
    }

    /**
     * section for reading and writing the list of objects
     */

    private boolean loadUserChatKeystoreUserPrivateKeys() {
        ucksListUserPrivateKeys = new ArrayList<>();

        InputStream fis = null;
        ObjectInputStream ois = null;
        Object loadedObj = null;
        try {
            fis = context.openFileInput(USER_PRIVATE_KEYS_FILENAME);
            ois = new ObjectInputStream(fis);

            while ((loadedObj = ois.readObject()) != null) {
                ucksListUserPrivateKeys.add((UserChatKeystoreModel) loadedObj);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (null != ois) {
                try {
                    ois.close();
                } catch (IOException e) {
                    return false;
                }
            }
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean saveUserChatKeystoreUserPrivateKeys() {
        ObjectOutputStream oos = null;
        try {
           /* File file = new File(this.getFilesDir().toString(), fileName);
            file.createNewFile();
            if(!file.mkdir()){ //just to check whats the reason behind the failed attempt
                Toast.makeText(this, "Security Issue", Toast.LENGTH_SHORT).show();
            }*/
            FileOutputStream fos = context.openFileOutput(USER_PRIVATE_KEYS_FILENAME, context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            for (int i = 0; i < ucksListUserPrivateKeys.size(); i++) {
                oos.writeObject(ucksListUserPrivateKeys.get(i));
            }
            oos.close();
            fos.close();
            return true;
        } catch (FileNotFoundException err) {
            Toast.makeText(context, "Something went wrong while saving", Toast.LENGTH_SHORT).show();
            return false;
        } catch (Exception abcd) {
            Toast.makeText(context, "Ooops, I don't know what's the problem. Sorry about that!", Toast.LENGTH_SHORT).show();
            return false;
        } finally {//makes sure to close the ObjectOutputStream
            if (oos != null) {
                try {
                    oos.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
    }
}
