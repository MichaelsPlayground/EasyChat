package de.androidcrypto.easychat;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import de.androidcrypto.easychat.adapter.ImageAdapter;
import de.androidcrypto.easychat.adapter.StorageDirectoriesAdapter;
import de.androidcrypto.easychat.adapter.StorageFileAdapter;
import de.androidcrypto.easychat.adapter.StorageReferenceAdapter;
import de.androidcrypto.easychat.model.ImageModel;
import de.androidcrypto.easychat.model.StorageFileModel;
import de.androidcrypto.easychat.model.UserModel;
import de.androidcrypto.easychat.utils.AndroidUtil;
import de.androidcrypto.easychat.utils.FirebaseUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StorageFragment extends Fragment {

    Button storageListDirectories, selectImage, uploadImage, listImages, listFilesForDownload, selectFile, uploadFile;
    RecyclerView storageRecyclerView;

    //ImageView profilePic;
    //EditText usernameInput;
    //EditText phoneInput;
    Button updateProfileBtn;
    ProgressBar progressBar;
    TextView logoutBtn;

    UserModel currentUserModel;
    ActivityResultLauncher<Intent> imagePickLauncher;
    ImageView selectedImageView;
    Uri selectedFileUri, selectedImageUri;

    StorageReference storageReference;
    LinearProgressIndicator progressIndicator;

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
        View view = inflater.inflate(R.layout.fragment_storage, container, false);

        storageListDirectories = view.findViewById(R.id.storage_list_directories_btn);
        storageRecyclerView = view.findViewById(R.id.storage_recyclerview);
        selectImage = view.findViewById(R.id.storage_select_image_btn);
        uploadImage = view.findViewById(R.id.storage_upload_image_btn);
        listImages = view.findViewById(R.id.storage_list_images_btn);
        listFilesForDownload = view.findViewById(R.id.storage_list_files_for_download_btn);
        selectedImageView = view.findViewById(R.id.storage_image_view);
        progressIndicator = view.findViewById(R.id.storage_progress);

        selectFile = view.findViewById(R.id.storage_select_file_btn);
        uploadFile = view.findViewById(R.id.storage_upload_file_btn);


        //profilePic = view.findViewById(R.id.profile_image_view);
        //usernameInput = view.findViewById(R.id.profile_username);
        //phoneInput = view.findViewById(R.id.profile_phone);
        updateProfileBtn = view.findViewById(R.id.profle_update_btn);
        progressBar = view.findViewById(R.id.profile_progress_bar);
        logoutBtn = view.findViewById(R.id.logout_btn);

        storageRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        storageReference = FirebaseStorage.getInstance().getReference();

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

            reference.child("images").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                @Override
                public void onSuccess(ListResult listResult) {
                    ArrayList<String> arrayList = new ArrayList<>();
                    Iterator<StorageReference> i = listResult.getItems().iterator();
                    StorageReference ref;
                    while (i.hasNext()) {
                        ref = i.next();
                        System.out.println("onSuccess() File name: " + ref.getName());
                        System.out.println("*** ref.downloadUrl: " + ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                // Got the download URL for 'users/me/profile.png'
                                System.out.println("*** uri: " + uri + " ***");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle any errors
                                System.out.println("### Error: " + exception.getMessage() + " ###");
                            }
                        }));
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

                            System.out.println("directory: " + directory + " ***");

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

        selectImage.setOnClickListener((v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            activityResultLauncher.launch(intent);
        }));

        uploadImage.setOnClickListener((v -> {
            uploadImage(selectedImageUri);
        }));

        listImages.setOnClickListener((v -> {
            String actualUserId = FirebaseAuth.getInstance().getUid();
            FirebaseStorage.getInstance().getReference().child(actualUserId).child("images").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                //FirebaseStorage.getInstance().getReference().child("images").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                @Override
                public void onSuccess(ListResult listResult) {
                    ArrayList<ImageModel> arrayList = new ArrayList<>();
                    ImageAdapter adapter = new ImageAdapter(getContext(), arrayList);
                    adapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
                        @Override
                        public void onClick(ImageModel image) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(image.getUrl()));
                            intent.setDataAndType(Uri.parse(image.getUrl()), "image/*");
                            startActivity(intent);
                        }
                    });
                    storageRecyclerView.setAdapter(adapter);
                    listResult.getItems().forEach(new Consumer<StorageReference>() {
                        @Override
                        public void accept(StorageReference storageReference) {
                            ImageModel image = new ImageModel();
                            image.setName(storageReference.getName());
                            storageReference.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    String url = "https://" + task.getResult().getEncodedAuthority() + task.getResult().getEncodedPath() + "?alt=media&token=" + task.getResult().getQueryParameters("token").get(0);
                                    image.setUrl(url);
                                    arrayList.add(image);
                                    adapter.notifyDataSetChanged();
                                }
                            });

                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(), "Failed to retrieve images", Toast.LENGTH_SHORT).show();
                }
            });

        }));


        updateProfileBtn.setOnClickListener((v -> {

        }));

        logoutBtn.setOnClickListener((v) -> {
            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        FirebaseUtil.logout();
                        Intent intent = new Intent(getContext(), SplashActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                }
            });


        });

        listFilesForDownload.setOnClickListener((v -> {

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
            String actualUserId = FirebaseAuth.getInstance().getUid();
            FirebaseStorage.getInstance().getReference().child(actualUserId).child("images").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
                @Override
                public void onSuccess(ListResult listResult) {
                    ArrayList<StorageFileModel> arrayList = new ArrayList<>();
                    ArrayList<StorageReference> arrayListSR = new ArrayList<>();
                    Iterator<StorageReference> i = listResult.getItems().iterator();
                    StorageReference ref;
                    while (i.hasNext()) {
                        ref = i.next();
                        arrayListSR.add(ref);
                        StorageFileModel sfm = new StorageFileModel();
                        sfm.setName(ref.getName());
                        System.out.println("onSuccess() File name: " + ref.getName());
                        System.out.println("*** ref.downloadUrl: " + ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                // Got the download URL for 'users/me/profile.png'
                                System.out.println("*** uri: " + uri + " ***");
                                sfm.setUri(uri);
                                arrayList.add(sfm);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle any errors
                                System.out.println("### Error: " + exception.getMessage() + " ###");
                            }
                        }));

                    }
                    /*
                    ArrayList<String> arrayList = new ArrayList<>();
                    for (StorageReference prefix : listResult.getPrefixes()) {
                        arrayList.add(prefix.getName());
                        System.out.println("*** added: " + prefix.getName());
                    }

                     */

                    System.out.println("*** arrayList has entries: " + arrayList.size() + " ***");

                    StorageReferenceAdapter adapterSR = new StorageReferenceAdapter(getContext(), arrayListSR);
                    storageRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    storageRecyclerView.setAdapter(adapterSR);
                    adapterSR.setOnItemClickListener(new StorageReferenceAdapter.OnItemClickListener() {
                        @Override
                        public void onClick(StorageReference storageReference) {
                            System.out.println("*** clicked on name: " + storageReference.getName());

                            // get the download url from task
                            storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    // Got the download URL for 'users/me/profile.png'
                                    System.out.println("*** uri: " + uri + " ***");
/*
                                    OkHttpClient client = new OkHttpClient();

                                    Request getRequest = new Request.Builder()
                                            .url(uri.toString())
                                            //.url("https://mytodoserver.com/todolist")
                                            .build();

                                    client.newCall(getRequest).enqueue(new Callback() {
                                        @Override
                                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                                            e.printStackTrace();
                                        }

                                        @Override
                                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                                            System.out.println("onResponse");
                                            System.out.println(response.body().string());
                                            InputStream is = response.body().byteStream();
                                        }
                                    });
*/
                                    DownloadManager.Request request = new DownloadManager.Request(uri);
                                    String title = URLUtil.guessFileName(uri.toString(), null, null);
                                    request.setTitle(title);
                                    request.setDescription("Downloading file, please wait");
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                                    request.setDestinationInExternalFilesDir(getContext(), Environment.DIRECTORY_DOWNLOADS, "");
                                    //request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title);
                                    DownloadManager manager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                                    long reference = manager.enqueue(request);
                                    System.out.println("reference: " + reference);
                                    Toast.makeText(getActivity(), "Downloading started, please wait...", Toast.LENGTH_LONG).show();

/*
                                    ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                                    Handler handler = new Handler(Looper.getMainLooper());

                                    executor.execute(new Runnable() {

                                        int count;

                                        @Override
                                        public void run() {

                                            //Background work here
                                            try {

                                                // put your url.this is sample url.
                                                URL url = new URL(uri.toString());
                                                URLConnection conection = url.openConnection();
                                                conection.connect();

                                                int lengthOfFile = conection.getContentLength();
                                                System.out.println("*** lengthOfFile: " + lengthOfFile);

                                                // download the file

                                                InputStream input = conection.getInputStream();

                                                //catalogfile is your destenition folder
                                                OutputStream output = Files.newOutputStream(Paths.get("video.mp4"));


                                                byte[] data = new byte[1024];

                                                long total = 0;

                                                while ((count = input.read(data)) != -1) {
                                                    total += count;
                                                    // publishing the progress....


                                                    //publishProgress(Integer.valueOf("" + (int) ((total * 100) / lenghtOfFile)));

                                                    // writing data to file
                                                    output.write(data, 0, count);
                                                }

                                                // flushing output
                                                output.flush();

                                                // closing streams
                                                output.close();
                                                input.close();


                                                handler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        //UI Thread work here
                                                        progressBar.setVisibility(View.GONE);

                                                    }
                                                });
                                            } catch (Exception e) {

                                            }
                                        }
                                    });

*/

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle any errors
                                    System.out.println("### Error: " + exception.getMessage() + " ###");
                                }
                            });
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getActivity(), "There was an error while listing files", Toast.LENGTH_SHORT).show();
                }
            });
        }));

        selectFile.setOnClickListener((v -> {
            selectFileBtnClick();
        }));


        uploadFile.setOnClickListener((v -> {
            //uploadImage(selectedImageUri);


        }));

        return view;
    }

    void selectFileBtnClick() {
        // https://developer.android.com/training/data-storage/shared/documents-files
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        boolean pickerInitialUri = false;
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        fileChooserActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> fileChooserActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent resultData = result.getData();
                        // The result data contains a URI for the document or directory that
                        // the user selected.
                        if (resultData != null) {
                            selectedFileUri = resultData.getData();
                            // Perform operations on the document using its URI.
                            try {
                                String fileContent = readTextFromUri(selectedFileUri);
                                Toast.makeText(getContext(), "Content: " + fileContent, Toast.LENGTH_LONG).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(getContext(), "Error on reading the file", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
            });

    private String readTextFromUri(Uri uri) throws IOException {
        if (uri != null) {
            StringBuilder stringBuilder = new StringBuilder();
            //try (InputStream inputStream = getContentResolver().openInputStream(uri);
            try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(Objects.requireNonNull(inputStream)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
            }
            return stringBuilder.toString();
        } else {
            return "";
        }
    }

    void uploadFileBtnClick() {


    }

    void uploadImageBtnClick() {
        // https://github.com/Everyday-Programmer/Upload-Image-to-Firebase-Android


    }

    private void uploadImage(Uri file) {
        System.out.println("*** uploadImageUri: " + file);
        String actualUserId = FirebaseAuth.getInstance().getUid();
        StorageReference ref = storageReference.child(actualUserId).child("images/" + UUID.randomUUID().toString());
        ref.putFile(file).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                System.out.println("*** upload onSuccess");
                Toast.makeText(getContext(), "Image Uploaded!!", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("*** upload onFailure");
                Toast.makeText(getContext(), "Failed!" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                progressIndicator.setMax(Math.toIntExact(snapshot.getTotalByteCount()));
                progressIndicator.setProgress(Math.toIntExact(snapshot.getBytesTransferred()));
            }
        });

    }


    void updateBtnClick() {

    }


    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                if (result.getData() != null) {
                    uploadImage.setEnabled(true);
                    selectedImageUri = result.getData().getData();
                    System.out.println("*** selectedImageUri: " + selectedImageUri);
                    Glide.with(requireContext()).load(selectedImageUri).into(selectedImageView);
                }
            } else {
                Toast.makeText(getActivity(), "Please select an image", Toast.LENGTH_SHORT).show();
            }
        }
    });


    void updateToFirestore() {
        FirebaseUtil.currentUserDetails().set(currentUserModel)
                .addOnCompleteListener(task -> {
                    setInProgress(false);
                    if (task.isSuccessful()) {
                        AndroidUtil.showToast(getContext(), "Updated successfully");
                    } else {
                        AndroidUtil.showToast(getContext(), "Updated failed");
                    }
                });
    }


    void getUserData() {
        setInProgress(true);
    }


    void setInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            updateProfileBtn.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            updateProfileBtn.setVisibility(View.VISIBLE);
        }
    }
}













