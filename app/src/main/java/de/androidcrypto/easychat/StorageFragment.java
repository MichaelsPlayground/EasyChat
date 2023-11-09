package de.androidcrypto.easychat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

import de.androidcrypto.easychat.adapter.StorageDirectoriesAdapter;
import de.androidcrypto.easychat.adapter.StorageFileAdapter;
import de.androidcrypto.easychat.model.StorageFileModel;
import de.androidcrypto.easychat.model.UserModel;
import de.androidcrypto.easychat.utils.AndroidUtil;
import de.androidcrypto.easychat.utils.FirebaseUtil;

public class StorageFragment extends Fragment {

    Button storageListDirectories;
    RecyclerView storageRecyclerView;

    //ImageView profilePic;
    EditText usernameInput;
    EditText phoneInput;
    Button updateProfileBtn;
    ProgressBar progressBar;
    TextView logoutBtn;

    UserModel currentUserModel;
    ActivityResultLauncher<Intent> imagePickLauncher;
    Uri selectedImageUri;

    public StorageFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        imagePickLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        if(data!=null && data.getData()!=null){
                            selectedImageUri = data.getData();
                            AndroidUtil.setProfilePic(getContext(),selectedImageUri,profilePic);
                        }
                    }
                }
                );
         */
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_storage, container, false);

        storageListDirectories = view.findViewById(R.id.storage_list_directories_btn);
        storageRecyclerView = view.findViewById(R.id.storage_recyclerview);

        //profilePic = view.findViewById(R.id.profile_image_view);
        usernameInput = view.findViewById(R.id.profile_username);
        phoneInput = view.findViewById(R.id.profile_phone);
        updateProfileBtn = view.findViewById(R.id.profle_update_btn);
        progressBar = view.findViewById(R.id.profile_progress_bar);
        logoutBtn = view.findViewById(R.id.logout_btn);

        // see https://github.com/Everyday-Programmer/Firebase-Directory-Listing-Android/tree/main/app/src/main/java/com/example/firebasefileslisting
        storageListDirectories.setOnClickListener((v -> {

            // this is listing the profile_pic directory
            /*
            String startDirectory = "profile_pic";
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference reference = storage.getReference();
            StorageReference reference1 = reference.child("profile_pic2");

             */
            //StorageReference reference = FirebaseStorage.getInstance().getReference().child(Objects.requireNonNull(startDirectory));

            // this lis listing from the root
            StorageReference reference = FirebaseStorage.getInstance().getReference();

            reference.child("profile_pic").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                @Override
                public void onSuccess(ListResult listResult) {
                    ArrayList<String> arrayList = new ArrayList<>();
                    Iterator<StorageReference> i = listResult.getItems().iterator();
                    StorageReference ref;
                    while (i.hasNext()) {
                        ref = i.next();
                        System.out.println("onSuccess() File name: " + ref.getName());
                        arrayList.add(ref.getName());
                    }
                    /*
                    ArrayList<String> arrayList = new ArrayList<>();
                    for (StorageReference prefix : listResult.getPrefixes()) {
                        arrayList.add(prefix.getName());
                        System.out.println("*** added: " + prefix.getName());
                    }

                     */
                    StorageDirectoriesAdapter adapter = new StorageDirectoriesAdapter(getActivity(), arrayList);
                    storageRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    storageRecyclerView.setAdapter(adapter);

                    adapter.setOnItemClickListener(new StorageDirectoriesAdapter.OnItemClickListener() {
                        @Override
                        public void onClick(String directory) {
                            //startActivity(new Intent(getActivity(), LoginEmailPasswordActivity.class).putExtra("directory", directory));

                            StorageReference reference = FirebaseStorage.getInstance().getReference().child(Objects.requireNonNull(directory));
                            reference.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                                @Override
                                public void onSuccess(ListResult listResult) {
                                    ArrayList<StorageFileModel> arrayList = new ArrayList<>();
                                    StorageFileAdapter adapter = new StorageFileAdapter(getActivity(), arrayList);
                                    for (StorageReference storageReference : listResult.getItems()) {
                                        StorageFileModel storageFile = new StorageFileModel();
                                        storageFile.setName(storageReference.getName());
                                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                storageFile.setUri(uri);
                                                arrayList.add(storageFile);
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                    }
                                    storageRecyclerView.setAdapter(adapter);

                                    adapter.setOnItemClickListener(new StorageFileAdapter.OnItemClickListener() {
                                        @Override
                                        public void onClick(Uri uri) {
                                            Toast.makeText(getActivity(), "You clicked on file name: ", Toast.LENGTH_SHORT).show();
                                            /*
                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            intent.setDataAndType(uri, "video/mp4");
                                            startActivity(intent);
                                             */
                                        }
                                    });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getActivity(), "There was an error while listing videos", Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getActivity(), "There was an error while listing directories", Toast.LENGTH_SHORT).show();
                }
            });
        }));


        getUserData();

        updateProfileBtn.setOnClickListener((v -> {
            updateBtnClick();
        }));

        logoutBtn.setOnClickListener((v)->{
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        FirebaseUtil.logout();
                        Intent intent = new Intent(getContext(),SplashActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                }
            });



        });

        /*
        profilePic.setOnClickListener((v)->{
            ImagePicker.with(this).cropSquare().compress(512).maxResultSize(512,512)
                    .createIntent(new Function1<Intent, Unit>() {
                        @Override
                        public Unit invoke(Intent intent) {
                            imagePickLauncher.launch(intent);
                            return null;
                        }
                    });
        });
        */

        return view;
    }

    void updateBtnClick(){
        String newUsername = usernameInput.getText().toString();
        if(newUsername.isEmpty() || newUsername.length()<3){
            usernameInput.setError("Username length should be at least 3 chars");
            return;
        }
        currentUserModel.setUsername(newUsername);
        setInProgress(true);


        if(selectedImageUri!=null){
            FirebaseUtil.getCurrentProfilePicStorageRef().putFile(selectedImageUri)
                    .addOnCompleteListener(task -> {
                        updateToFirestore();
                    });
        }else{
            updateToFirestore();
        }





    }

    void updateToFirestore(){
        FirebaseUtil.currentUserDetails().set(currentUserModel)
                .addOnCompleteListener(task -> {
                    setInProgress(false);
                    if(task.isSuccessful()){
                        AndroidUtil.showToast(getContext(),"Updated successfully");
                    }else{
                        AndroidUtil.showToast(getContext(),"Updated failed");
                    }
                });
    }



    void getUserData(){
        setInProgress(true);

        /*
        FirebaseUtil.getCurrentProfilePicStorageRef().getDownloadUrl()
                        .addOnCompleteListener(task -> {
                                if(task.isSuccessful()){
                                    Uri uri  = task.getResult();
                                    AndroidUtil.setProfilePic(getContext(),uri,profilePic);
                                }
                        });

         */

        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            setInProgress(false);
            currentUserModel = task.getResult().toObject(UserModel.class);
            usernameInput.setText(currentUserModel.getUsername());
            // todo change name phoneInput to emailInput
            phoneInput.setText(currentUserModel.getEmail());
        });
    }


    void setInProgress(boolean inProgress){
        if(inProgress){
            progressBar.setVisibility(View.VISIBLE);
            updateProfileBtn.setVisibility(View.GONE);
        }else{
            progressBar.setVisibility(View.GONE);
            updateProfileBtn.setVisibility(View.VISIBLE);
        }
    }
}













