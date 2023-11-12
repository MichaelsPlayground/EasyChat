package de.androidcrypto.easychat.utils;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.List;

public class FirebaseUtil {

    public static String currentUserId(){
        return FirebaseAuth.getInstance().getUid();
    }
    public static final String USERS_FOLDER_NAME = "users";
    public static final String CHATROOMS_FOLDER_NAME = "chatrooms";
    public static final String CHATS_FOLDER_NAME = "chats";
    public static final String FILES_FOLDER_NAME = "files";
    public static final String IMAGES_FOLDER_NAME = "images";
    public static final String PROFILE_PIC_FOLDER_NAME = "profile_pic";
    public static boolean isLoggedIn(){
        if(currentUserId() != null){
            return true;
        }
        return false;
    }

    public static DocumentReference currentUserDetails(){
        return FirebaseFirestore.getInstance().collection(USERS_FOLDER_NAME).document(currentUserId());
    }

    public static CollectionReference allUserCollectionReference(){
        return FirebaseFirestore.getInstance().collection(USERS_FOLDER_NAME);
    }

    public static DocumentReference getChatroomReference(String chatroomId){
        return FirebaseFirestore.getInstance().collection(CHATROOMS_FOLDER_NAME).document(chatroomId);
    }

    public static CollectionReference getChatroomMessageReference(String chatroomId){
        return getChatroomReference(chatroomId).collection(CHATS_FOLDER_NAME);
    }

    public static String getChatroomId(String userId1,String userId2){
        if(userId1.hashCode() < userId2.hashCode()){
            return userId1 + "_" + userId2;
        }else{
            return userId2 + "_" + userId1;
        }
    }

    public static CollectionReference allChatroomCollectionReference(){
        return FirebaseFirestore.getInstance().collection(CHATROOMS_FOLDER_NAME);
    }

    public static DocumentReference getOtherUserFromChatroom(List<String> userIds){
        if(userIds.get(0).equals(FirebaseUtil.currentUserId())){
            return allUserCollectionReference().document(userIds.get(1));
        }else{
            return allUserCollectionReference().document(userIds.get(0));
        }
    }

    // appended
    public static CollectionReference currentUserFilesCollectionReference(){
        return FirebaseFirestore.getInstance().collection(USERS_FOLDER_NAME).document(currentUserId()).collection(FILES_FOLDER_NAME);
    }

    public static StorageReference currentUserStorageFilesReference() {
        return FirebaseStorage.getInstance().getReference().child(currentUserId()).child(FILES_FOLDER_NAME);
    }

    public static StorageReference currentUserStorageImagesReference() {
        return FirebaseStorage.getInstance().getReference().child(currentUserId()).child(IMAGES_FOLDER_NAME);
    }

    public static String timestampToString(Timestamp timestamp){
        // todo error correction: changed SimpleDateFormat("HH:MM") to SimpleDateFormat("HH:mm")
        return new SimpleDateFormat("HH:mm").format(timestamp.toDate());
    }

    public static String timestampFullToString(Timestamp timestamp){
        return new SimpleDateFormat("dd.MM.yy HH:mm:ss").format(timestamp.toDate());
    }

    public static void logout(){
        FirebaseAuth.getInstance().signOut();
    }

    public static StorageReference getCurrentProfilePicStorageRef(){
        return FirebaseStorage.getInstance().getReference().child(PROFILE_PIC_FOLDER_NAME)
                .child(FirebaseUtil.currentUserId());
    }

    public static StorageReference getOtherProfilePicStorageRef(String otherUserId){
        return FirebaseStorage.getInstance().getReference().child(PROFILE_PIC_FOLDER_NAME)
                .child(otherUserId);
    }


}










