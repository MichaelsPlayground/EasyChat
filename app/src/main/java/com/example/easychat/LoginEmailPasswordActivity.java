package com.example.easychat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.easychat.model.UserModel;
import com.example.easychat.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginEmailPasswordActivity extends AppCompatActivity {

    private static final String TAG = "EmailPassword";
    EditText usernameInput, passwordInput; // todo change username to emailAddress
    Button letMeInBtn, signupBtn;
    ProgressBar progressBar, signupProgressbar;
    String phoneNumber;
    UserModel userModel;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_email_password);

        usernameInput = findViewById(R.id.login_username);
        letMeInBtn = findViewById(R.id.login_let_me_in_btn);
        progressBar =findViewById(R.id.login_progress_bar);

        // new
        passwordInput = findViewById(R.id.login_password);
        signupBtn = findViewById(R.id.signup_btn);
        signupProgressbar = findViewById(R.id.signup_progress_bar);

        //phoneNumber = getIntent().getExtras().getString("phone");
        //getUsername();
        // todo new method to get username = email-address

        letMeInBtn.setOnClickListener((v -> {
            setUsername();
        }));

        signupBtn.setOnClickListener((v -> {
            signupUser(v);
        }));

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

    }

    void signupUser(View view) {
        // todo this is NEW
        String email = usernameInput.getText().toString();
        if(email.isEmpty() || email.length()<3){
            usernameInput.setError("Email length should be at least 3 chars");
            return;
        }
        String password = passwordInput.getText().toString();
        if(password.isEmpty() || password.length()<6){
            passwordInput.setError("Password length should be at least 6 chars");
            return;
        }
        // todo validate email address, see EmailPasswordFragment.java validateForm
        // todo use progressbar
        progressBar.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(LoginEmailPasswordActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            //updateUI(user);
                            userModel = new UserModel(email,email, Timestamp.now(),FirebaseUtil.currentUserId());
                            Intent intent = new Intent(LoginEmailPasswordActivity.this,MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
                            startActivity(intent);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(view.getContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            //updateUI(null);
                        }

                        // todo hideProgressBar();
                        progressBar.setVisibility(View.GONE);
                    }
                });

    }

    void setUsername(){

        String username = usernameInput.getText().toString();
        if(username.isEmpty() || username.length()<3){
            usernameInput.setError("Username length should be at least 3 chars");
            return;
        }
        setInProgress(true);
        if(userModel!=null){
            userModel.setUsername(username);
        }else{
            userModel = new UserModel(phoneNumber,username, Timestamp.now(),FirebaseUtil.currentUserId());
        }

        FirebaseUtil.currentUserDetails().set(userModel).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                setInProgress(false);
                if(task.isSuccessful()){
                   Intent intent = new Intent(LoginEmailPasswordActivity.this,MainActivity.class);
                   intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
                   startActivity(intent);
                }
            }
        });

    }

    void getUsername(){
        setInProgress(true);
        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                setInProgress(false);
                if(task.isSuccessful()){
                  userModel =    task.getResult().toObject(UserModel.class);
                 if(userModel!=null){
                     usernameInput.setText(userModel.getUsername());
                 }
                }
            }
        });
    }

    void setInProgress(boolean inProgress){
        // todo work on this
        if(inProgress){
            progressBar.setVisibility(View.VISIBLE);
            letMeInBtn.setVisibility(View.GONE);
        }else{
            progressBar.setVisibility(View.GONE);
            letMeInBtn.setVisibility(View.VISIBLE);
        }
    }
}