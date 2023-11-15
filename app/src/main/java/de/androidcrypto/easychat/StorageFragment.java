package de.androidcrypto.easychat;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import de.androidcrypto.easychat.adapter.ImageAdapter;
import de.androidcrypto.easychat.adapter.StorageDirectoriesAdapter;
import de.androidcrypto.easychat.adapter.StorageFileAdapter;
import de.androidcrypto.easychat.adapter.StorageReferenceAdapter;
import de.androidcrypto.easychat.adapter.SwipeController;
import de.androidcrypto.easychat.adapter.SwipeToDeleteCallback;
import de.androidcrypto.easychat.model.FileInformation;
import de.androidcrypto.easychat.model.ImageModel;
import de.androidcrypto.easychat.model.StorageFileModel;
import de.androidcrypto.easychat.model.UserModel;
import de.androidcrypto.easychat.utils.AndroidUtil;
import de.androidcrypto.easychat.utils.FirebaseUtil;

public class StorageFragment extends Fragment {

    // upload section
    private RadioGroup rgUpload;
    private RadioButton rbUploadFile, rbUploadImage, rbUploadFileEncrypted, rbUploadImageEncrypted;
    private Button uploadUnencryptedFile, uploadUnencryptedImage;
    private Button uploadEncryptedFile, uploadEncryptedImage;
    private com.google.android.material.textfield.TextInputLayout uploadPassphraseLayout;
    private com.google.android.material.textfield.TextInputEditText uploadPassphrase;

    // download section
    private RadioGroup rgDownload;
    private RadioButton rbDownloadFile, rbDownloadImage, rbDownloadFileEncrypted, rbDownloadImageEncrypted;
    private Button downloadUnencryptedFile, downloadUnencryptedImage;
    private Button downloadEncryptedFile, downloadEncryptedImage;
    private com.google.android.material.textfield.TextInputLayout downloadPassphraseLayout;
    private com.google.android.material.textfield.TextInputEditText downloadPassphrase;
    private String downloadSelector;
    
    


    private Button storageListDirectories, selectImage, uploadImage, listImages, listFilesForDownload, selectFile, uploadFile;

    private Button listFilesForDownload2;


    private Button encryptFile, decryptFile, downloadFile, downloadDecryptFile;

    private Button deleteFile, deleteImage;
    private TextView storageWarning;
    private RecyclerView storageRecyclerView;

    //ImageView profilePic;
    //EditText usernameInput;
    //EditText phoneInput;
    private Button updateProfileBtn;

    private UserModel currentUserModel;
    private ActivityResultLauncher<Intent> imagePickLauncher;
    private ImageView selectedImageView;
    private Uri selectedFileUri, selectedImageUri;

    private StorageReference storageReference;
    private LinearProgressIndicator progressIndicator;
    private String encryptedFilename = "";
    private static final int NUMBER_OF_PBKDF2_ITERATIONS = 10000;
    private String fileStorageReference; // it filled when sending the Intent(Intent.ACTION_OPEN_DOCUMENT), data from FirebaseUtil e.g. ENCRYPTED_FILES_FOLDER_NAME
    private SwipeController swipeController = null;
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

        // upload section
        rgUpload = view.findViewById(R.id.rgStorageUpload);
        rbUploadFile = view.findViewById(R.id.rbStorageUploadFile);
        rbUploadImage = view.findViewById(R.id.rbStorageUploadImage);
        rbUploadFileEncrypted = view.findViewById(R.id.rbStorageUploadFileEnc);
        rbUploadImageEncrypted = view.findViewById(R.id.rbStorageUploadImageEnc);
        uploadUnencryptedFile = view.findViewById(R.id.btnStorageUploadUnencryptedFile);
        uploadUnencryptedImage = view.findViewById(R.id.btnStorageUploadUnencryptedImage);
        uploadEncryptedFile = view.findViewById(R.id.btnStorageUploadEncryptedFile);
        uploadEncryptedImage = view.findViewById(R.id.btnStorageUploadEncryptedImage);
        uploadPassphraseLayout = view.findViewById(R.id.etStorageUploadPassphraseLayout);
        uploadPassphrase = view.findViewById(R.id.etStorageUploadPassphrase);
        // download section
        rgDownload = view.findViewById(R.id.rgStorageDownload);
        rbDownloadFile = view.findViewById(R.id.rbStorageDownloadFile);
        rbDownloadImage = view.findViewById(R.id.rbStorageDownloadImage);
        rbDownloadFileEncrypted = view.findViewById(R.id.rbStorageDownloadFileEnc);
        rbDownloadImageEncrypted = view.findViewById(R.id.rbStorageDownloadImageEnc);
        downloadUnencryptedFile = view.findViewById(R.id.btnStorageDownloadUnencryptedFile);
        downloadUnencryptedImage = view.findViewById(R.id.btnStorageDownloadUnencryptedImage);
        downloadEncryptedFile = view.findViewById(R.id.btnStorageDownloadEncryptedFile);
        downloadEncryptedImage = view.findViewById(R.id.btnStorageDownloadEncryptedImage);
        downloadPassphraseLayout = view.findViewById(R.id.etStorageDownloadPassphraseLayout);
        downloadPassphrase = view.findViewById(R.id.etStorageDownloadPassphrase);
        
        
        //uploadSectionVisibilityOff();

        // download section

        storageListDirectories = view.findViewById(R.id.storage_list_directories_btn);
        storageRecyclerView = view.findViewById(R.id.storage_recyclerview);
        selectImage = view.findViewById(R.id.storage_select_image_btn);
        uploadImage = view.findViewById(R.id.storage_upload_image_btn);
        listImages = view.findViewById(R.id.storage_list_images_btn);
        listFilesForDownload = view.findViewById(R.id.storage_list_files_for_download_btn);
        listFilesForDownload2 = view.findViewById(R.id.storage_list_files_for_download_2_btn);
        selectedImageView = view.findViewById(R.id.storage_image_view);
        progressIndicator = view.findViewById(R.id.storage_progress);

        // preset is the unencrypted files selection
        downloadSelector = FirebaseUtil.FILES_FOLDER_NAME;



        // old ones

        selectFile = view.findViewById(R.id.storage_select_file_btn);
        uploadFile = view.findViewById(R.id.storage_upload_file_btn);

        encryptFile = view.findViewById(R.id.storage_encrypt_file_btn);
        decryptFile = view.findViewById(R.id.storage_decrypt_file_btn);


        downloadFile = view.findViewById(R.id.storage_download_file_btn);
        downloadDecryptFile = view.findViewById(R.id.storage_download_decrypt_file_btn);

        deleteFile = view.findViewById(R.id.storage_delete_file_btn);
        deleteImage = view.findViewById(R.id.storage_delete_image_btn);
        storageWarning = view.findViewById(R.id.storage_warning);


        //profilePic = view.findViewById(R.id.profile_image_view);
        //usernameInput = view.findViewById(R.id.profile_username);
        //phoneInput = view.findViewById(R.id.profile_phone);
        updateProfileBtn = view.findViewById(R.id.profle_update_btn);

        storageRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        storageReference = FirebaseStorage.getInstance().getReference();

        // default UI settings
        defaultUiSettings();


        ActivityCompat.requestPermissions(getActivity(),
                new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);

        /**
         * section for uploads
         */

        View.OnClickListener rbUploadFileListener = null;
        rbUploadFileListener = new View.OnClickListener() {
            public void onClick(View v) {
                uploadSectionVisibilityOff();
                uploadUnencryptedFile.setVisibility(View.VISIBLE);
            }
        };
        rbUploadFile.setOnClickListener(rbUploadFileListener);

        View.OnClickListener rbUploadImageListener = null;
        rbUploadImageListener = new View.OnClickListener() {
            public void onClick(View v) {
                uploadSectionVisibilityOff();
                uploadUnencryptedImage.setVisibility(View.VISIBLE);
            }
        };
        rbUploadImage.setOnClickListener(rbUploadImageListener);

        View.OnClickListener rbUploadFileEncListener = null;
        rbUploadFileEncListener = new View.OnClickListener() {
            public void onClick(View v) {
                uploadSectionVisibilityOff();
                uploadEncryptedFile.setVisibility(View.VISIBLE);
                uploadPassphraseLayout.setVisibility(View.VISIBLE);
            }
        };
        rbUploadFileEncrypted.setOnClickListener(rbUploadFileEncListener);

        View.OnClickListener rbUploadImageEncListener = null;
        rbUploadImageEncListener = new View.OnClickListener() {
            public void onClick(View v) {
                uploadSectionVisibilityOff();
                uploadEncryptedImage.setVisibility(View.VISIBLE);
                uploadPassphraseLayout.setVisibility(View.VISIBLE);
            }
        };
        rbUploadImageEncrypted.setOnClickListener(rbUploadImageEncListener);


        uploadUnencryptedFile.setOnClickListener((v -> {
            uploadUnencryptedFileBtnClick();
        }));

        uploadUnencryptedImage.setOnClickListener((v -> {
            uploadUnencryptedImageBtnClick();
        }));

        uploadEncryptedFile.setOnClickListener((v -> {
            uploadEncryptedFileBtnClick();
        }));

        uploadEncryptedImage.setOnClickListener((v -> {
            uploadEncryptedImageBtnClick();
        }));


        /**
         * section for downloads
         */

        View.OnClickListener rbDownloadFileListener = null;
        rbDownloadFileListener = new View.OnClickListener() {
            public void onClick(View v) {
                downloadSectionVisibilityOff();
                downloadUnencryptedFile.setVisibility(View.VISIBLE);
                downloadSelector = FirebaseUtil.FILES_FOLDER_NAME;
            }
        };
        rbDownloadFile.setOnClickListener(rbDownloadFileListener);

        View.OnClickListener rbDownloadImageListener = null;
        rbDownloadImageListener = new View.OnClickListener() {
            public void onClick(View v) {
                downloadSectionVisibilityOff();
                downloadUnencryptedImage.setVisibility(View.VISIBLE);
                downloadSelector = FirebaseUtil.IMAGES_FOLDER_NAME;
            }
        };
        rbDownloadImage.setOnClickListener(rbDownloadImageListener);

        View.OnClickListener rbDownloadFileEncListener = null;
        rbDownloadFileEncListener = new View.OnClickListener() {
            public void onClick(View v) {
                downloadSectionVisibilityOff();
                downloadEncryptedFile.setVisibility(View.VISIBLE);
                downloadPassphraseLayout.setVisibility(View.VISIBLE);
                downloadSelector = FirebaseUtil.ENCRYPTED_FILES_FOLDER_NAME;
            }
        };
        rbDownloadFileEncrypted.setOnClickListener(rbDownloadFileEncListener);

        View.OnClickListener rbDownloadImageEncListener = null;
        rbDownloadImageEncListener = new View.OnClickListener() {
            public void onClick(View v) {
                downloadSectionVisibilityOff();
                downloadEncryptedImage.setVisibility(View.VISIBLE);
                downloadPassphraseLayout.setVisibility(View.VISIBLE);
                downloadSelector = FirebaseUtil.ENCRYPTED_IMAGES_FOLDER_NAME;
            }
        };
        rbDownloadImageEncrypted.setOnClickListener(rbDownloadImageEncListener);


        downloadUnencryptedFile.setOnClickListener((v -> {
            downloadFromSelectorBtnClick();
            //downloadUnencryptedFileBtnClick();
        }));

        downloadUnencryptedImage.setOnClickListener((v -> {
            downloadUnencryptedImageBtnClick();
        }));

        downloadEncryptedFile.setOnClickListener((v -> {
            downloadEncryptedFileBtnClick();
        }));

        downloadEncryptedImage.setOnClickListener((v -> {
            downloadEncryptedImageBtnClick();
        }));


        /**
         * old methods
         */

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
            //FirebaseStorage.getInstance().getReference().child(actualUserId).child("images").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            FirebaseStorage.getInstance().getReference().child(actualUserId).child("files").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
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

        listFilesForDownload2.setOnClickListener((v -> {

            // this lists listing from the root
            //StorageReference reference = FirebaseStorage.getInstance().getReference();
            String actualUserId = FirebaseAuth.getInstance().getUid();
            //FirebaseStorage.getInstance().getReference().child(actualUserId).child("images").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            FirebaseStorage.getInstance().getReference().child(actualUserId).child("files").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
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
                                System.out.println("*** uri: " + uri + " ***");
                                sfm.setUri(uri);
                                arrayList.add(sfm);
                                System.out.println("arrayList added");
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

                    // this is using class SwipeToDeleteCallback
                    // see: https://www.digitalocean.com/community/tutorials/android-recyclerview-swipe-to-delete-undo
                    SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(getContext()) {
                        @Override
                        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {


                            //final int position = viewHolder.getAdapterPosition(); // getAdapterPosition is deprecated
                            final int position = viewHolder.getBindingAdapterPosition();
                            final StorageReference item = adapterSR.getArrayList().get(position);
                            //final String item = mAdapter.getData().get(position);

                            adapterSR.removeItem(position);
                            // todo remove from Storage and Firestore


                            System.out.println("actual contents of the arraylist");
                        }
                    };

                    ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
                    itemTouchhelper.attachToRecyclerView(storageRecyclerView);

                    /*
                    RecyclerSwipeHelper mRecyclerSwipeHelper = new RecyclerSwipeHelper(ContextCompat.getColor(getContext(), R.color.swipe_right_color),
                            ContextCompat.getColor(getContext(), R.color.swipe_left_color),
                            R.drawable.outline_edit_24, R.drawable.outline_delete_forever_24, getContext()) {
                        @Override
                        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

                            //calling the notifyItemChanged() method is absolutely essential to redraw the RecyclerView item and remove the icons we had drawn.
                            adapterSR.notifyItemChanged(viewHolder.getBindingAdapterPosition());

                            if (direction == ItemTouchHelper.LEFT) {
                                //handle left swipe
                                Toast.makeText(getActivity(), "swiped left", Toast.LENGTH_SHORT).show();
                            } else {
                                //handle right swipe
                                Toast.makeText(getActivity(), "swiped right", Toast.LENGTH_SHORT).show();
                            }
                        }
                    };
                    ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(mRecyclerSwipeHelper);
                    mItemTouchHelper.attachToRecyclerView(storageRecyclerView);
*/

/*
                    swipeController = new SwipeController(new SwipeControllerActions() {
                        @Override
                        public void onRightClicked(int position) {
                            //adapterSR.players.remove(position);
                            adapterSR.notifyItemRemoved(position);
                            adapterSR.notifyItemRangeChanged(position, adapterSR.getItemCount());
                        }
                    });

                    ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);
                    itemTouchhelper.attachToRecyclerView(storageRecyclerView);

                    storageRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                        @Override
                        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
                            swipeController.onDraw(c);
                        }
                    });
*/

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

        encryptFile.setOnClickListener((v -> {
            encryptFileBtnClick();
        }));

        decryptFile.setOnClickListener((v -> {
            decryptFileBtnClick();
        }));


        downloadFile.setOnClickListener((v -> {
            downloadFileBtnClick();
        }));

        downloadDecryptFile.setOnClickListener((v -> {
            downloadDecryptFileBtnClick();
        }));

        deleteFile.setOnClickListener((v -> {
            deleteFileBtnClick();
        }));

        deleteImage.setOnClickListener((v -> {
            deleteImageBtnClick();
        }));

        return view;
    }


    private void defaultUiSettings() {
        uploadFile.setEnabled(false);
        uploadImage.setEnabled(false);
        storageWarning.setVisibility(View.GONE);
    }

    // inform the user by Toast that the download is completed
    BroadcastReceiver onCompleted = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context.getApplicationContext(), "Download Finish", Toast.LENGTH_SHORT).show();
            defaultUiSettings(); // disable upload buttons
        }
    };

    /**
     * section for file and image uploads
     */

    // unencrypted uploads
    private void uploadUnencryptedFileBtnClick() {
        // select a file in download folder and upload it to firebase cloud storage
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        boolean pickerInitialUri = false;
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        fileStorageReference = FirebaseUtil.FILES_FOLDER_NAME;
        fileUploadUnencryptedChooserActivityResultLauncher.launch(intent);
    }

    private void uploadUnencryptedImageBtnClick() {
        // select an image in download folder and upload it to firebase cloud storage
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        boolean pickerInitialUri = false;
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        fileStorageReference = FirebaseUtil.IMAGES_FOLDER_NAME;
        fileUploadUnencryptedChooserActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> fileUploadUnencryptedChooserActivityResultLauncher = registerForActivityResult(
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
                            String fileStorageReferenceLocal = fileStorageReference;
                            fileStorageReference = ""; // clear after usage

                            FileInformation fileInformation = getFileInformationFromUri(selectedFileUri);
                            StorageReference ref;
                            String selectedFolder;
                            if (fileStorageReferenceLocal.equals(FirebaseUtil.FILES_FOLDER_NAME)) {
                                System.out.println("*** fileStorageReferenceLocal.equals(FirebaseUtil.FILES_FOLDER_NAME");
                                ref = FirebaseUtil.currentUserStorageUnencryptedFilesReference(fileInformation.getFileName());
                                selectedFolder = FirebaseUtil.FILES_FOLDER_NAME;
                                fileInformation.setFileStorage(selectedFolder);
                            } else {
                                System.out.println("*** fileStorageReferenceLocal. NOT equals(FirebaseUtil.FILES_FOLDER_NAME");
                                ref = FirebaseUtil.currentUserStorageUnencryptedImagesReference(fileInformation.getFileName());
                                selectedFolder = FirebaseUtil.IMAGES_FOLDER_NAME;
                                fileInformation.setFileStorage(selectedFolder);
                            }
                            // now upload the  file / image
                            ref.putFile(selectedFileUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    Toast.makeText(getContext(), "File uploaded with SUCCESS", Toast.LENGTH_SHORT).show();
                                    ref.getDownloadUrl();
                                    // get download url
                                    ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            fileInformation.setDownloadUrl(uri);
                                            fileInformation.setTimestamp(AndroidUtil.getTimestamp());
                                            System.out.println("*** selectedFolder: " + selectedFolder);
                                            addFileInformationToUserCollection(selectedFolder, fileInformation.getFileName(), fileInformation);
                                            //addFileInformationToUserCollection(fileInformation);
                                        }
                                    });
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getContext(), "File upload error: " + e.getMessage(), Toast.LENGTH_LONG).show();

                                }
                            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                                    progressIndicator.setMax(Math.toIntExact(snapshot.getTotalByteCount()));
                                    progressIndicator.setProgress(Math.toIntExact(snapshot.getBytesTransferred()));
                                }
                            });
                        }
                    }
                }
            });

    // encrypted uploads

    private void uploadEncryptedFileBtnClick() {

        //askPassphrase();
        // just a pre check
        if (!uploadPassphrasePreCheck()) return;

        // select a file in download folder, encrypt it and upload it to firebase cloud storage
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        boolean pickerInitialUri = false;
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        fileStorageReference = FirebaseUtil.ENCRYPTED_FILES_FOLDER_NAME;
        fileUploadEncryptedChooserActivityResultLauncher.launch(intent);
    }

    private void uploadEncryptedImageBtnClick() {

        // just a pre check
        if (!uploadPassphrasePreCheck()) return;

        // select an image in download folder, encrypt it and upload it to firebase cloud storage
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        boolean pickerInitialUri = false;
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        fileStorageReference = FirebaseUtil.ENCRYPTED_IMAGES_FOLDER_NAME;
        fileUploadEncryptedChooserActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> fileUploadEncryptedChooserActivityResultLauncher = registerForActivityResult(
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
                            String fileStorageReferenceLocal = fileStorageReference;
                            fileStorageReference = ""; // clear after usage

                            FileInformation fileInformation = getFileInformationFromUri(selectedFileUri);
                            StorageReference ref;
                            // internal cache
                            String cacheFilename = fileInformation.getFileName() + ".enc";
                            File encryptedFile = new File(getContext().getCacheDir(), cacheFilename);
                            encryptedFilename = encryptedFile.getAbsolutePath();
                            String selectedFolder;
                            if (fileStorageReferenceLocal.equals(FirebaseUtil.ENCRYPTED_FILES_FOLDER_NAME)) {
                                ref = FirebaseUtil.currentUserStorageEncryptedFilesReference(cacheFilename);
                                selectedFolder = FirebaseUtil.ENCRYPTED_FILES_FOLDER_NAME;
                                fileInformation.setFileStorage(selectedFolder);
                            } else {
                                ref = FirebaseUtil.currentUserStorageEncryptedImagesReference(cacheFilename);
                                selectedFolder = FirebaseUtil.ENCRYPTED_IMAGES_FOLDER_NAME;
                                fileInformation.setFileStorage(selectedFolder);
                            }

                            if (!uploadPassphrasePreCheck()) return;
                            int passphraseLength = uploadPassphrase.length();
                            // get the passphrase as char[]
                            char[] passphraseChars = new char[passphraseLength];
                            uploadPassphrase.getText().getChars(0, passphraseLength, passphraseChars, 0);

                            boolean success = Cryptography.encryptGcmFileBufferedCipherOutputStreamToCacheFile(getContext(), selectedFileUri, encryptedFilename, passphraseChars, NUMBER_OF_PBKDF2_ITERATIONS);
                            Toast.makeText(getActivity(), "encryptGcmFileBufferedCipherOutputStreamToCacheFile: " + success, Toast.LENGTH_SHORT).show();
                            // now upload the encrypted file / image
                            if (success) {
                                Uri file = Uri.fromFile(encryptedFile);
                                StorageMetadata storageMetadata = new StorageMetadata.Builder()
                                        .setContentType(getContext().getContentResolver().getType(file))
                                        .build();
                                ref.putFile(file, storageMetadata).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        Toast.makeText(getContext(), "File uploaded with SUCCESS", Toast.LENGTH_SHORT).show();
                                        ref.getDownloadUrl();
                                        // get download url
                                        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                fileInformation.setDownloadUrl(uri);
                                                fileInformation.setTimestamp(AndroidUtil.getTimestamp());
                                                //addFileInformationToUserCollection(fileInformation);
                                                addFileInformationToUserCollection(selectedFolder, fileInformation.getFileName(), fileInformation);
                                                // delete file in cache folder
                                                boolean success = deleteCacheFile(encryptedFile);
                                                Toast.makeText(getContext(), "delete file in cache folder success: " + success, Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getContext(), "File upload error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                                        progressIndicator.setMax(Math.toIntExact(snapshot.getTotalByteCount()));
                                        progressIndicator.setProgress(Math.toIntExact(snapshot.getBytesTransferred()));
                                    }
                                });
                            }
                        }
                    }
                }
            });

    private void addFileInformationToUserCollection(String subfolder, String filename, FileInformation fileInformation) {
        FirebaseUtil.currentUserFilesCollectionReference(subfolder, filename)
                .set(fileInformation)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        AndroidUtil.showToast(getContext(), "Filestore entry added successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        AndroidUtil.showToast(getContext(), "Filestore entry adding failed");
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

    /**
     * section for download files and images
     */

    private void downloadFromSelectorBtnClick() {
        // downloadSelector contains the folder name on storage like FirebaseUtil.FILES_FOLDER_NAME = 'files_une'
        // first we list the available files in this folder on Firebase Cloud Storage for selection by click



        String actualUserId = FirebaseAuth.getInstance().getUid();
        FirebaseStorage.getInstance().getReference().child(actualUserId).child("files").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
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
                            System.out.println("*** uri: " + uri + " ***");
                            sfm.setUri(uri);
                            arrayList.add(sfm);
                            System.out.println("arrayList added");
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

                // this is using class SwipeToDeleteCallback
                // see: https://www.digitalocean.com/community/tutorials/android-recyclerview-swipe-to-delete-undo
                SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(getContext()) {
                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {


                        //final int position = viewHolder.getAdapterPosition(); // getAdapterPosition is deprecated
                        final int position = viewHolder.getBindingAdapterPosition();
                        final StorageReference item = adapterSR.getArrayList().get(position);
                        //final String item = mAdapter.getData().get(position);

                        adapterSR.removeItem(position);
                        // todo remove from Storage and Firestore


                        System.out.println("actual contents of the arraylist");
                    }
                };

                ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeToDeleteCallback);
                itemTouchhelper.attachToRecyclerView(storageRecyclerView);

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

    }

    // unencrypted download of files and images
    private void downloadUnencryptedFileBtnClick() {

    }

    private void downloadUnencryptedImageBtnClick() {

    }

    // encrypted download of files and images
    private void downloadEncryptedFileBtnClick() {

    }

    private void downloadEncryptedImageBtnClick() {

    }

    /**
     * section for old methods
     */

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
        fileInformation.setFileStorage(FirebaseUtil.IMAGES_FOLDER_NAME);
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
        fileInformation.setFileStorage(FirebaseUtil.FILES_FOLDER_NAME);
        StorageReference ref;
        if ( TextUtils.isEmpty(fileInformation.getFileName())) {
            ref = storageReference.child(actualUserId).child("files/" + UUID.randomUUID().toString() + ".abc");
        } else {
            ref = storageReference.child(actualUserId).child("files/" + fileInformation.getFileName());
        }
        // deactivate upload button
        uploadFile.setEnabled(false);

        // todo upload files directly encrypting using inputstream and outputstream
        //ref.putStream(inputStream, storageMetadata)

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
                        fileInformation.setTimestamp(AndroidUtil.getTimestamp());
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

    /**
     * section for encryption and decryption of a file
     */

    private void encryptFileBtnClick() {
        // select a file from download folder and encrypt it to internal storage, both by using uris
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        boolean pickerInitialUri = false;
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        fileEncryptChooserActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> fileEncryptChooserActivityResultLauncher = registerForActivityResult(
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
                            FileInformation fileInformation = getFileInformationFromUri(selectedFileUri);
                            Toast.makeText(getContext(), "You selected this file: " + fileInformation.getFileName()
                                    + " with size: " + fileInformation.getFileSize(), Toast.LENGTH_SHORT).show();
                            char[] passphrase = "123456".toCharArray();
                            int iterations = 1000;
                            String cacheFilename = fileInformation.getFileName() + ".enc";
                            encryptedFilename = new File(getContext().getCacheDir(), cacheFilename).getAbsolutePath();

                            boolean success = Cryptography.encryptGcmFileBufferedCipherOutputStreamToCacheFile(getContext(), selectedFileUri, encryptedFilename, passphrase, iterations);
                            Toast.makeText(getActivity(), "encryptGcmFileBufferedCipherOutputStreamToCacheFile: " + success, Toast.LENGTH_SHORT).show();

                            //uploadFile.setEnabled(true);
                            // Perform operations on the document using its URI.
                            // todo get filename from uri
                            // https://developer.android.com/training/secure-file-sharing/retrieve-info

                        }
                    }
                }
            });

    private void decryptFileBtnClick() {
        // select a file from internal storage and decrypt it to download folder, both by using uris

        // todo work on this, filename should be given from the real one

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        //boolean pickerInitialUri = false;
        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        // todo strip the last extension with ".enc off
        intent.putExtra(Intent.EXTRA_TITLE, encryptedFilename);
        fileDecryptSaverActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> fileDecryptSaverActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent resultData = result.getData();
                        // The result data contains a URI for the document or directory that
                        // the user selected.
                        Uri selectedUri = null;
                        if (resultData != null) {
                            selectedUri = resultData.getData();
                            // Perform operations on the document using its URI.
                            Toast.makeText(getContext(), "You selected this file for decryption: " + selectedUri.toString(), Toast.LENGTH_SHORT).show();

                            char[] passphrase = "123456".toCharArray();
                            int iterations = 1000;
                            //String cacheFilename = fileInformation.getFileName() + ".enc";
                            //String encryptedFilename = new File(getContext().getCacheDir(), encryptedFilename;

                            boolean success = Cryptography.decryptGcmFileBufferedCipherInputStreamFromCache(getContext(), encryptedFilename, selectedUri, passphrase, iterations);
                            Toast.makeText(getActivity(), "decryptGcmFileBufferedCipherOutputStreamFromCache: " + success, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });


    /**
     * section for service methods
     */

    private boolean deleteCacheFile(File file) {
        if (file == null) return true; // if there is not file it returns true
        if (!file.exists()) return true;
        return file.delete();
    }

    private boolean uploadPassphrasePreCheck() {
        if (TextUtils.isEmpty(uploadPassphrase.getText().toString())) {
            uploadPassphraseLayout.setError("please enter a passphrase, minimum is 3 characters");
            return false;
        } else {
            uploadPassphraseLayout.setError("");
        }
        if (uploadPassphrase.length() < 3) {
            uploadPassphraseLayout.setError("please enter a passphrase, minimum is 3 characters");
            return false;
        } else {
            uploadPassphraseLayout.setError("");
            return true;
        }
    }

    private void askPassphrase() {
        final EditText taskEditText = new EditText(getActivity());
        FrameLayout layout = new FrameLayout(getContext());
        layout.setPaddingRelative(45,15,45,0);
        layout.addView(taskEditText);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Enter your passphrase")
                .setMessage("Enter a passphrase (minimum 6 characters):")
                //.setView(taskEditText)
                .setView(layout)
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String task = taskEditText.getText().toString();
                        Toast.makeText(getContext(), "Passphrase: " + task, Toast.LENGTH_SHORT).show();
                        // do whatever you want to do
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    private void downloadFileBtnClick() {
        // select a file from internal storage and decrypt it to download folder, both by using uris

        // todo work on this, filename should be given from the real one

        try {
            //Okhttp3Progress.main();
            String cacheFilename = "mt_cook.jpg";
            String storageFilename = new File(getContext().getCacheDir(), cacheFilename).getAbsolutePath();
            System.out.println("*** storageFilename: " + storageFilename);
            progressIndicator.setMax(Math.toIntExact(100));
            String downloadUrl = "https://firebasestorage.googleapis.com/v0/b/easychat-ce2c5.appspot.com/o/bgC77aBfeYZzX5deM6PqCUe0iMr1%2Ffiles%2FMt_Cook_LC0247.jpg?alt=media&token=4db57606-0285-4ea6-b957-aaaf9e120d27";
            Okhttp3ProgressDownloader downloader = new Okhttp3ProgressDownloader(downloadUrl, storageFilename, progressIndicator);
            downloader.run();
            //Okhttp3ProgressCallback.main(storageFilename);
        } catch (Exception e) {
            //throw new RuntimeException(e);
            Toast.makeText(getActivity(), "Exception on download: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadDecryptFileBtnClick() {
        // this will download a file, decrypt it on loading and save it in the download folder
        // todo work on this, filename should be given from the real one

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //intent.setType("*/*");
        intent.setType("image/*/*"); // for image

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        //boolean pickerInitialUri = false;
        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        // todo strip the last extension with ".enc off
        String storeFilename = "mtc2.jpg";
        //String storeFilename = "file3a.txt";
        intent.putExtra(Intent.EXTRA_TITLE, storeFilename);
        fileDownloadDecryptSaverActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> fileDownloadDecryptSaverActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        System.out.println("fileDownloadDecryptSaverActivityResultLauncher");
                        // There are no request codes
                        Intent resultData = result.getData();
                        // The result data contains a URI for the document or directory that
                        // the user selected.
                        Uri selectedUri = null;
                        if (resultData != null) {
                            selectedUri = resultData.getData();
                            // Perform operations on the document using its URI.
                            Toast.makeText(getContext(), "You selected this file for download and decryption: " + selectedUri.toString(), Toast.LENGTH_SHORT).show();

                            char[] passphrase = "123456".toCharArray();
                            int iterations = 1000;
                            //String cacheFilename = fileInformation.getFileName() + ".enc";
                            //String encryptedFilename = new File(getContext().getCacheDir(), encryptedFilename;
                            try {
                                //Okhttp3Progress.main();
                                //String cacheFilename = "mt_cook.jpg";
                                //String storageFilename = new File(getContext().getCacheDir(), cacheFilename).getAbsolutePath();
                                //System.out.println("*** storageFilename: " + storageFilename);
                                progressIndicator.setMax(Math.toIntExact(100));

                                //String downloadUrl = "https://firebasestorage.googleapis.com/v0/b/easychat-ce2c5.appspot.com/o/bgC77aBfeYZzX5deM6PqCUe0iMr1%2Ffiles%2Fmtc.bin.enc?alt=media&token=a10a55af-109d-4fa5-a5fe-ceab69004436";
                                //String downloadUrl = "https://firebasestorage.googleapis.com/v0/b/easychat-ce2c5.appspot.com/o/bgC77aBfeYZzX5deM6PqCUe0iMr1%2Ffiles%2Ffile03.txt.enc?alt=media&token=a0ff7051-0f76-4aba-ad51-f1b321a01b30";
                                String downloadUrl = "https://firebasestorage.googleapis.com/v0/b/easychat-ce2c5.appspot.com/o/bgC77aBfeYZzX5deM6PqCUe0iMr1%2Ffiles%2FMt_Cook_LC0247.jpg.enc?alt=media&token=a904a1f8-4a4b-4553-a307-1949b987d148";
                                Okhttp3ProgressDownloaderDecrypt downloader = new Okhttp3ProgressDownloaderDecrypt(downloadUrl, progressIndicator, getContext(), selectedUri, passphrase, iterations);
                                downloader.run();
                                Toast.makeText(getActivity(), "fileDownloadDecryptSaverActivityResultLauncher success", Toast.LENGTH_SHORT).show();
                                //Okhttp3ProgressCallback.main(storageFilename);
                            } catch (Exception e) {
                                //throw new RuntimeException(e);
                                Toast.makeText(getActivity(), "Exception on download: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            });


    private void deleteFileBtnClick() {
        // deletes a file in Firebase Cloud Storage in files folder
        String actualUserId = FirebaseAuth.getInstance().getUid();
        cloudStorageFileDeletion(FirebaseStorage.getInstance().getReference().child(actualUserId).child("files"));
    }

    private void deleteImageBtnClick() {
        // deletes an image in Firebase Cloud Storage in images folder
        String actualUserId = FirebaseAuth.getInstance().getUid();
        cloudStorageFileDeletion(FirebaseStorage.getInstance().getReference().child(actualUserId).child("images"));
    }

    private void cloudStorageFileDeletion(StorageReference storageReference) {
        storageReference.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                storageWarning.setVisibility(View.VISIBLE);
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

                    arrayList.add(sfm);
                }

                System.out.println("*** arrayList has entries: " + arrayList.size() + " ***");

                StorageReferenceAdapter adapterSR = new StorageReferenceAdapter(getContext(), arrayListSR);
                storageRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                storageRecyclerView.setAdapter(adapterSR);
                adapterSR.setOnItemClickListener(new StorageReferenceAdapter.OnItemClickListener() {
                    @Override
                    public void onClick(StorageReference storageReference) {
                        System.out.println("*** clicked on name: " + storageReference.getName());

                        // todo use confirmation dialog before deleting
                        // get metadata
                        storageReference.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                            @Override
                            public void onSuccess(StorageMetadata storageMetadata) {
                                // Metadata now contains the metadata for 'images/forest.jpg'
                                String filename = storageMetadata.getName();
                                long size = storageMetadata.getSizeBytes();
                                Toast.makeText(getActivity(), "Deleting the file " + filename +
                                        " size: " + size, Toast.LENGTH_SHORT).show();
                                storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // File deleted successfully
                                        Toast.makeText(getActivity(), "The file was deleted", Toast.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Uh-oh, an error occurred!
                                        Toast.makeText(getActivity(), "Error on file deletion", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Uh-oh, an error occurred!
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
    }

    void setInProgress(boolean inProgress) {
        if (inProgress) {
            //progressBar.setVisibility(View.VISIBLE);
            updateProfileBtn.setVisibility(View.GONE);
        } else {
            //progressBar.setVisibility(View.GONE);
            updateProfileBtn.setVisibility(View.VISIBLE);
        }
    }

    private void uploadSectionVisibilityOff() {
        uploadUnencryptedFile.setVisibility(View.GONE);
        uploadUnencryptedImage.setVisibility(View.GONE);
        uploadEncryptedFile.setVisibility(View.GONE);
        uploadEncryptedImage.setVisibility(View.GONE);
        uploadPassphraseLayout.setVisibility(View.GONE);
    }

    private void downloadSectionVisibilityOff() {
        downloadUnencryptedFile.setVisibility(View.GONE);
        downloadUnencryptedImage.setVisibility(View.GONE);
        downloadEncryptedFile.setVisibility(View.GONE);
        downloadEncryptedImage.setVisibility(View.GONE);
        downloadPassphraseLayout.setVisibility(View.GONE);
    }
    
}













