package de.androidcrypto.easychat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import de.androidcrypto.easychat.adapter.ChatRecyclerAdapter;
import de.androidcrypto.easychat.model.ChatMessageModel;
import de.androidcrypto.easychat.model.ChatroomModel;
import de.androidcrypto.easychat.model.UserModel;
import de.androidcrypto.easychat.utils.AndroidUtil;
import de.androidcrypto.easychat.utils.FirebaseUtil;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    UserModel otherUser;
    String chatroomId;
    ChatroomModel chatroomModel;
    ChatRecyclerAdapter adapter;

    EditText messageInput;
    ImageButton sendMessageBtn;
    ImageButton backBtn;
    TextView otherUsername;
    RecyclerView recyclerView;
    ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //get UserModel
        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());

        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        imageView = findViewById(R.id.profile_pic_image_view);

        FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Uri uri = t.getResult();
                        AndroidUtil.setProfilePic(this, uri, imageView);
                    }
                });

        backBtn.setOnClickListener((v) -> {
            onBackPressed();
        });
        otherUsername.setText(otherUser.getUsername());

        sendMessageBtn.setOnClickListener((v -> {
            String message = messageInput.getText().toString().trim();
            if (message.isEmpty())
                return;
            sendMessageToUser(message);
        }));

        getOrCreateChatroomModel();
        setupChatRecyclerView();
    }

    void setupChatRecyclerView() {
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options, getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    void sendMessageToUser(String message) {

        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage(message);
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        ChatMessageModel chatMessageModel = new ChatMessageModel(message, FirebaseUtil.currentUserId(), Timestamp.now());
        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if (task.isSuccessful()) {
                            messageInput.setText("");
                            //sendNotification(message);
                            sendNotificationApiV1(message);
                        }
                    }
                });
    }

    void getOrCreateChatroomModel() {
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if (chatroomModel == null) {
                    //first time chat
                    chatroomModel = new ChatroomModel(
                            chatroomId,
                            Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()),
                            Timestamp.now(),
                            ""
                    );
                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
                }
            }
        });
    }

    void sendNotificationApiV1(String message) {
        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                UserModel currentUser = task.getResult().toObject(UserModel.class);
                try {
                    // get the JSON object
                    JSONObject jsonMessage = createJson(otherUser.getFcmToken(), "from " + currentUser.getUsername(), message);
                    String FCM_API = "https://fcm.googleapis.com/v1/projects/easychat-ce2c5/messages:send";
                    try {
                        String SERVER_KEY = getAccessToken();
                        URL url = new URL(FCM_API);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setRequestProperty("Authorization", "Bearer " + SERVER_KEY);
                        connection.setDoOutput(true);

                        // Send the request
                        try (OutputStream os = connection.getOutputStream()) {
                            byte[] input = jsonMessage.toString().getBytes("utf-8");
                            os.write(input, 0, input.length);
                        }

                        // Get the response
                        int responseCode = connection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            Log.d("FCM", "Message sent successfully");
                        } else {
                            Log.e("FCM", "Error sending message. Response Code: " + responseCode);
                        }
                    } catch (Exception e) {
                        Log.e("FCM", "send Exception: " + e.getMessage());
                    }
                } catch (Exception e) {
                System.out.println("*** Exception2: " + e.getMessage());
            }



        // get the JSO object

            }
        });


    }

    public JSONObject createJson(String token, String title, String messageBody) {
        JSONObject message = new JSONObject();
        JSONObject to = new JSONObject();
        JSONObject data = new JSONObject();
        try {
            data.put("title", title);
            data.put("body", messageBody);

            to.put("token", token);
            to.put("notification", data);

            message.put("message", to);
            if (token != null) {
                //Log.i(TAG, "createJson message: " + message);
                return message;
                //sentNotification(message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }



    void sendNotification(String message) {

        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                UserModel currentUser = task.getResult().toObject(UserModel.class);
                try {

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("to", otherUser.getFcmToken());
                    JSONObject notificationObj = new JSONObject();
                    notificationObj.put("title", currentUser.getUsername());
                    notificationObj.put("body", message);
                    notificationObj.put("icon", "icon_for_splash");
                    notificationObj.put("sound", "little_bell_14606.mp3");
                    notificationObj.put("android_channel_id",R.string.default_notification_channel_id);
                    jsonObject.put("notification", notificationObj);


/* old
                    JSONObject jsonObject = new JSONObject();
                    JSONObject notificationObj = new JSONObject();
                    notificationObj.put("title", currentUser.getUsername());
                    notificationObj.put("body", message);
                    JSONObject dataObj = new JSONObject();
                    dataObj.put("userId", currentUser.getUserId());
                    jsonObject.put("notification", notificationObj);
                    jsonObject.put("data", dataObj);
                    jsonObject.put("to", otherUser.getFcmToken());
                    */
/*
                    // example
                    String token = otherUser.getFcmToken();
                    JSONObject messageJs = new JSONObject();
                    JSONObject to = new JSONObject();
                    JSONObject data = new JSONObject();
                    try {
                        data.put("title", "Notification");
                        data.put("body", "you have 1 notification");

                        to.put("token", token);
                        to.put("data", data);

                        messageJs.put("message", to);
                        if (token != null) {
                            callApiHttpV1(messageJs);
                        }
                    } catch (Exception e) {
                        System.out.println("*** Exception1: " + e.getMessage());
                    }
*/
                    callApi(jsonObject);
                } catch (Exception e) {
                    System.out.println("*** Exception2: " + e.getMessage());
                }
            }
        });

    }

    void sendNotificationV1(String message) {

        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                UserModel currentUser = task.getResult().toObject(UserModel.class);

                try {
                    JSONObject jsonObject = new JSONObject();

                    jsonObject.put("to", otherUser.getFcmToken());

                JSONObject notificationObj = new JSONObject();
                    notificationObj.put("title", currentUser.getUsername());
                    notificationObj.put("body", message);
                    notificationObj.put("icon", "icon_for_splash");
                    notificationObj.put("sound", "little_bell_14606.mp3");
                    notificationObj.put("android_channel_id",R.string.default_notification_channel_id);
                    jsonObject.put("notification", notificationObj);
                    callApiHttpV1(jsonObject);
                } catch (JSONException e) {
                    //throw new RuntimeException(e);
                    System.out.println("*** JSONException: " + e.getMessage());
                }
/* old
                    JSONObject jsonObject = new JSONObject();
                    JSONObject notificationObj = new JSONObject();
                    notificationObj.put("title", currentUser.getUsername());
                    notificationObj.put("body", message);
                    JSONObject dataObj = new JSONObject();
                    dataObj.put("userId", currentUser.getUserId());
                    jsonObject.put("notification", notificationObj);
                    jsonObject.put("data", dataObj);
                    jsonObject.put("to", otherUser.getFcmToken());
                    */



            }
        });

    }

    void callApiHttpV1(JSONObject jsonObject) {
        System.out.println("*** callApiHttpV1: " + jsonObject);
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        //String url = "https://fcm.googleapis.com/fcm/send";
        String NOTIFICATION_URL = "https://fcm.googleapis.com/v1/projects/easychat-ce2c5/messages:send" ;
        String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
        String[] SCOPES = { MESSAGING_SCOPE };

        // todo remove after test
        System.out.println("*** callApiHttpV1 for notification ***");
        System.out.println("*** jsonObject: " + jsonObject.toString());
        String accessToken = "";
        try {
            accessToken = getAccessToken();
        } catch (IOException e) {
            //throw new RuntimeException(e);
            System.out.println("RunTime : " + e.getMessage());
        }
        System.out.println("*** access token: " + accessToken);

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = null;
        request = new Request.Builder()
                .url(NOTIFICATION_URL)
                .post(body)
                .header("Authorization", "Bearer " + accessToken)
                .build();
        System.out.println("*** request: " + request.toString());
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                System.out.println("*** callApi onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                System.out.println("*** callApi onResponse: " + response.toString());
            }

        });
        System.out.println("*** call api v1 ended");
    }

    private String getAccessToken() throws IOException {
        final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
        final String[] SCOPES = { MESSAGING_SCOPE };
        InputStream inputStream = getResources().openRawResource(R.raw.service_account);

        GoogleCredentials googleCredential = GoogleCredentials
                .fromStream(inputStream)
                .createScoped(Arrays.asList(SCOPES));
        googleCredential.refresh();

        //Log.i("TAGggg", "getAccessToken: " + googleCredential.toString());
        System.out.println("*** getAccessToken: " + googleCredential.toString());

        return googleCredential.getAccessToken().getTokenValue();
    }

/*
Firebase Admin SDK:
https://console.firebase.google.com/u/0/project/easychat-ce2c5/settings/serviceaccounts/adminsdk
FileInputStream serviceAccount =
new FileInputStream("path/to/serviceAccountKey.json");

FirebaseOptions options = new FirebaseOptions.Builder()
  .setCredentials(GoogleCredentials.fromStream(serviceAccount))
  .build();

FirebaseApp.initializeApp(options);
 */


    void callApi(JSONObject jsonObject) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        String url = "https://fcm.googleapis.com/fcm/send";

        // todo remove after test
        System.out.println("*** callApi for notification ***");
        System.out.println("*** jsonObject: " + jsonObject.toString());

        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "key=AAAAceXXnNk:APA91bGl1YFfTu8oJmiZrIzuc4uv9P2CZnLouAmBAx_vIKVXy3Gb8cQEFSv9yngFIU_fXOzcAP6B-img6MOJzUQ-aWTbsbBxbGbPEJ1mAaSNy-cfI9xqBt4p5LyFD90tDWAffaUx3Vkr")
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                System.out.println("*** callApi onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                System.out.println("*** callApi onResponse: " + response.toString());
            }
        });

    }

}