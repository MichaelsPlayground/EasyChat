package de.androidcrypto.easychat;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Button;
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
import androidx.core.app.ActivityCompat;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import de.androidcrypto.easychat.adapter.ImageAdapter;
import de.androidcrypto.easychat.adapter.StorageDirectoriesAdapter;
import de.androidcrypto.easychat.adapter.StorageFileAdapter;
import de.androidcrypto.easychat.adapter.StorageReferenceAdapter;
import de.androidcrypto.easychat.model.FileInformation;
import de.androidcrypto.easychat.model.ImageModel;
import de.androidcrypto.easychat.model.StorageFileModel;
import de.androidcrypto.easychat.model.UserModel;
import de.androidcrypto.easychat.utils.AndroidUtil;
import de.androidcrypto.easychat.utils.FirebaseUtil;

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

        // default UI settings
        defaultUiSettings();


        ActivityCompat.requestPermissions(getActivity(),
                new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);


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
            if (selectedImageUri != null) {
                uploadImage(selectedImageUri);
            }
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

            // this lists listing from the root
            StorageReference reference = FirebaseStorage.getInstance().getReference();
            String actualUserId = FirebaseAuth.getInstance().getUid();
            FirebaseStorage.getInstance().getReference().child(actualUserId).child("images").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            //FirebaseStorage.getInstance().getReference().child(actualUserId).child("files").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
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

                                    // set progressIndicator to 0
                                    progressIndicator.setProgress(0);
                                    DownloadManager.Request request = new DownloadManager.Request(uri);
                                    String title = null;
                                    try {
                                        title = URLUtil.guessFileName(new URL(uri.toString()).toString(), null, null);
                                    } catch (MalformedURLException e) {
                                        //throw new RuntimeException(e);
                                        System.out.println("malformed url");
                                    }
                                    System.out.println("*** title: " + title);
                                    request.setTitle(title);

                                    request.setDescription("Downloading file, please wait");
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                                    //request.setDestinationInExternalFilesDir(getContext(), Environment.DIRECTORY_DOWNLOADS, "");
                                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title);
                                    DownloadManager manager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                                    //Registering receiver in Download Manager
                                    getActivity().registerReceiver(onCompleted, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                                    long downloadId = manager.enqueue(request);
                                    System.out.println("reference: " + downloadId);
                                    Toast.makeText(getActivity(), "Downloading started, please wait...", Toast.LENGTH_SHORT).show();

                                    // progress
                                    new Thread(new Runnable() {
                                        @SuppressLint("Range")
                                        @Override
                                        public void run() {
                                            boolean downloading = true;
                                            while (downloading) {
                                                DownloadManager.Query q = new DownloadManager.Query();
                                                q.setFilterById(downloadId);
                                                Cursor cursor = manager.query(q);
                                                cursor.moveToFirst();
                                                @SuppressLint("Range") int bytes_downloaded = cursor.getInt(cursor
                                                        .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                                int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                                                if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                                                    downloading = false;
                                                }
                                                final int dl_progress = (int) ((bytes_downloaded * 100L) / bytes_total);
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        progressIndicator.setProgress(dl_progress);
                                                    }
                                                });
                                                //Log.d(Constants.MAIN_VIEW_ACTIVITY, statusMessage(cursor));
                                                cursor.close();
                                            }
                                        }
                                    }).start();

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
            uploadFileBtnClick();
        }));

        return view;
    }


    private void defaultUiSettings() {
        uploadFile.setEnabled(false);
        uploadImage.setEnabled(false);
    }

    // inform the user by Toast that the download is completed
    BroadcastReceiver onCompleted = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context.getApplicationContext(), "Download Finish", Toast.LENGTH_SHORT).show();
            defaultUiSettings(); // disable upload buttons
        }
    };

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
                            uploadFile.setEnabled(true);
                            // Perform operations on the document using its URI.
                            // todo get filename from uri
                            // https://developer.android.com/training/secure-file-sharing/retrieve-info
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
        if (selectedFileUri != null) {

            // todo prevent uploading of some file types, see https://firebase.google.com/support/faq#storage-exe-restrictions
            String fileExtension = selectedFileUri.getLastPathSegment(); // gives the extension
            // exe, apk, dll, bat, ipa are not allowed

            uploadFile(selectedFileUri);
        }
    }

    void uploadImageBtnClick() {
        // https://github.com/Everyday-Programmer/Upload-Image-to-Firebase-Android


    }

    private FileInformation getFileInformationFromUri(Uri uri) {
        /*
         * Get the file's content URI from the incoming Intent,
         * then query the server app to get the file's display name
         * and size.
         */
        Context context = getContext();
        if (context == null) return null;
        Cursor returnCursor = null;
        String mimeType = "";
        String fileName = "";
        long fileSize = 0;
        try {
            returnCursor = context.getContentResolver().query(uri, null, null, null, null);
            mimeType = context.getContentResolver().getType(uri);
            /*
             * Get the column indexes of the data in the Cursor,
             * move to the first row in the Cursor, get the data,
             * and display it.
             */
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
            returnCursor.moveToFirst();
            fileName = returnCursor.getString(nameIndex);
            fileSize = returnCursor.getLong(sizeIndex);
        } catch (NullPointerException e) {
            //
        } finally {
            returnCursor.close();
        }
        return new FileInformation(mimeType, fileName, fileSize);
    }

    private void uploadImage(Uri uri) {
        System.out.println("*** uploadImageUri: " + uri);
        String actualUserId = FirebaseAuth.getInstance().getUid();
        // trying to get the original filename from uri
        FileInformation fileInformation = getFileInformationFromUri(uri);
        StorageReference ref;
        if ( TextUtils.isEmpty(fileInformation.getFileName())) {
            ref = storageReference.child(actualUserId).child("images/" + UUID.randomUUID().toString() + ".jpg");
        } else {
            ref = storageReference.child(actualUserId).child("images/" + fileInformation.getFileName());
        }
        ref.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
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

    private void uploadFile(Uri uri) {
        System.out.println("*** uploadFileUri: " + uri);
        progressIndicator.setProgress(0);
        String actualUserId = FirebaseAuth.getInstance().getUid();
        // trying to get the original filename from uri
        FileInformation fileInformation = getFileInformationFromUri(uri);
        StorageReference ref;
        if ( TextUtils.isEmpty(fileInformation.getFileName())) {
            ref = storageReference.child(actualUserId).child("files/" + UUID.randomUUID().toString() + ".abc");
        } else {
            ref = storageReference.child(actualUserId).child("files/" + fileInformation.getFileName());
        }
        // deactivate upload button
        uploadFile.setEnabled(false);
        ref.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                System.out.println("*** upload onSuccess");
                Toast.makeText(getContext(), "File Uploaded!!", Toast.LENGTH_LONG).show();
                ref.getDownloadUrl();
                // get download url
                ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        fileInformation.setDownloadUrl(uri);
                        addFileInformationToUserCollection(fileInformation);
                    }
                });
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

    private void addFileInformationToUserCollection(FileInformation fileInformation) {
        FirebaseUtil.currentUserFilesCollectionReference().add(fileInformation)
                .addOnCompleteListener(task -> {
                    setInProgress(false);
                    if (task.isSuccessful()) {
                        AndroidUtil.showToast(getContext(), "Filestore entry added successfully");
                    } else {
                        AndroidUtil.showToast(getContext(), "Filestore entry adding failed");
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













