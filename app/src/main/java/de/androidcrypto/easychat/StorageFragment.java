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
    private LinearProgressIndicator uploadProgressIndicator;

    // download section
    private RadioGroup rgDownload;
    private RadioButton rbDownloadFile, rbDownloadImage, rbDownloadFileEncrypted, rbDownloadImageEncrypted;
    private Button downloadListFiles;
    private Button downloadUnencryptedFile, downloadUnencryptedImage;
    private Button downloadEncryptedFile, downloadEncryptedImage;
    private com.google.android.material.textfield.TextInputLayout downloadPassphraseLayout;
    private com.google.android.material.textfield.TextInputEditText downloadPassphrase;
    private String downloadSelector, downloadSelectedDownloadUrl;
    private LinearProgressIndicator downloadProgressIndicator;
    
    
    // old ones

    private Button deleteFile, deleteImage;
    private TextView storageWarning;
    private RecyclerView storageRecyclerView;

    private Uri selectedFileUri, selectedImageUri;

    private StorageReference storageReference;

    private String encryptedFilename = "";
    private static final int NUMBER_OF_PBKDF2_ITERATIONS = 10000;
    private String fileStorageReference; // it filled when sending the Intent(Intent.ACTION_OPEN_DOCUMENT), data from FirebaseUtil e.g. ENCRYPTED_FILES_FOLDER_NAME
    private SwipeController swipeController = null;
    public StorageFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        uploadProgressIndicator = view.findViewById(R.id.lpiStorageUploadProgress);

        // download section
        rgDownload = view.findViewById(R.id.rgStorageDownload);
        rbDownloadFile = view.findViewById(R.id.rbStorageDownloadFile);
        rbDownloadImage = view.findViewById(R.id.rbStorageDownloadImage);
        rbDownloadFileEncrypted = view.findViewById(R.id.rbStorageDownloadFileEnc);
        rbDownloadImageEncrypted = view.findViewById(R.id.rbStorageDownloadImageEnc);
        downloadListFiles = view.findViewById(R.id.btnStorageDownloadListFiles);
        downloadProgressIndicator = view.findViewById(R.id.lpiStorageDownloadProgress);

        downloadUnencryptedFile = view.findViewById(R.id.btnStorageDownloadUnencryptedFile);
        downloadUnencryptedImage = view.findViewById(R.id.btnStorageDownloadUnencryptedImage);
        downloadEncryptedFile = view.findViewById(R.id.btnStorageDownloadEncryptedFile);
        downloadEncryptedImage = view.findViewById(R.id.btnStorageDownloadEncryptedImage);
        downloadPassphraseLayout = view.findViewById(R.id.etStorageDownloadPassphraseLayout);
        downloadPassphrase = view.findViewById(R.id.etStorageDownloadPassphrase);
        storageRecyclerView = view.findViewById(R.id.storage_recyclerview);

        // preset is the unencrypted files selection
        downloadSelector = FirebaseUtil.FILES_FOLDER_NAME;
        downloadListFiles.setText(FirebaseUtil.STORAGE_LIST_BUTTON_UNENCRYPTED_FILES);

        deleteFile = view.findViewById(R.id.storage_delete_file_btn);
        deleteImage = view.findViewById(R.id.storage_delete_image_btn);
        storageWarning = view.findViewById(R.id.storage_warning);



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
                //downloadUnencryptedFile.setVisibility(View.VISIBLE);
                downloadSelector = FirebaseUtil.FILES_FOLDER_NAME;
                downloadListFiles.setText(FirebaseUtil.STORAGE_LIST_BUTTON_UNENCRYPTED_FILES);
            }
        };
        rbDownloadFile.setOnClickListener(rbDownloadFileListener);

        View.OnClickListener rbDownloadImageListener = null;
        rbDownloadImageListener = new View.OnClickListener() {
            public void onClick(View v) {
                downloadSectionVisibilityOff();
                //downloadUnencryptedImage.setVisibility(View.VISIBLE);
                downloadSelector = FirebaseUtil.IMAGES_FOLDER_NAME;
                downloadListFiles.setText(FirebaseUtil.STORAGE_LIST_BUTTON_UNENCRYPTED_IMAGES);
            }
        };
        rbDownloadImage.setOnClickListener(rbDownloadImageListener);

        View.OnClickListener rbDownloadFileEncListener = null;
        rbDownloadFileEncListener = new View.OnClickListener() {
            public void onClick(View v) {
                downloadSectionVisibilityOff();
                //downloadEncryptedFile.setVisibility(View.VISIBLE);
                downloadPassphraseLayout.setVisibility(View.VISIBLE);
                downloadSelector = FirebaseUtil.ENCRYPTED_FILES_FOLDER_NAME;
                downloadListFiles.setText(FirebaseUtil.STORAGE_LIST_BUTTON_ENCRYPTED_FILES);
            }
        };
        rbDownloadFileEncrypted.setOnClickListener(rbDownloadFileEncListener);

        View.OnClickListener rbDownloadImageEncListener = null;
        rbDownloadImageEncListener = new View.OnClickListener() {
            public void onClick(View v) {
                downloadSectionVisibilityOff();
                //downloadEncryptedImage.setVisibility(View.VISIBLE);
                downloadPassphraseLayout.setVisibility(View.VISIBLE);
                downloadSelector = FirebaseUtil.ENCRYPTED_IMAGES_FOLDER_NAME;
                downloadListFiles.setText(FirebaseUtil.STORAGE_LIST_BUTTON_ENCRYPTED_IMAGES);
            }
        };
        rbDownloadImageEncrypted.setOnClickListener(rbDownloadImageEncListener);

        downloadListFiles.setOnClickListener((v -> {
            downloadListFilesBtnClick();
            //downloadUnencryptedFileBtnClick();
        }));

        downloadUnencryptedFile.setOnClickListener((v -> {
            //downloadUnencryptedFileBtnClick();
        }));

        downloadUnencryptedImage.setOnClickListener((v -> {
            //downloadUnencryptedImageBtnClick();
        }));

        downloadEncryptedFile.setOnClickListener((v -> {
            //downloadEncryptedFileBtnClick();
        }));

        downloadEncryptedImage.setOnClickListener((v -> {
            //downloadEncryptedImageBtnClick();
        }));


        /**
         * old methods
         */

        deleteFile.setOnClickListener((v -> {
            deleteFileBtnClick();
        }));

        deleteImage.setOnClickListener((v -> {
            deleteImageBtnClick();
        }));

        return view;
    }


    private void defaultUiSettings() {
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
                                    uploadProgressIndicator.setMax(Math.toIntExact(snapshot.getTotalByteCount()));
                                    uploadProgressIndicator.setProgress(Math.toIntExact(snapshot.getBytesTransferred()));
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
                                        uploadProgressIndicator.setMax(Math.toIntExact(snapshot.getTotalByteCount()));
                                        uploadProgressIndicator.setProgress(Math.toIntExact(snapshot.getBytesTransferred()));
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

    private void downloadListFilesBtnClick() {
        // downloadSelector contains the folder name on storage like FirebaseUtil.FILES_FOLDER_NAME = 'files_une'
        // first we list the available files in this folder on Firebase Cloud Storage for selection by click

        StorageReference ref;
        AndroidUtil.showToast(getContext(), "downloadSelector: " + downloadSelector);

        if (downloadSelector.equals(FirebaseUtil.FILES_FOLDER_NAME)) {
            ref = FirebaseUtil.currentUserStorageUnencryptedFilesReference();
        } else if(downloadSelector.equals(FirebaseUtil.IMAGES_FOLDER_NAME)) {
            ref = FirebaseUtil.currentUserStorageUnencryptedImagesReference();
        } else if(downloadSelector.equals(FirebaseUtil.ENCRYPTED_FILES_FOLDER_NAME)) {
            ref = FirebaseUtil.currentUserStorageEncryptedFilesReference();
        } else if(downloadSelector.equals(FirebaseUtil.ENCRYPTED_IMAGES_FOLDER_NAME)) {
            ref = FirebaseUtil.currentUserStorageEncryptedImagesReference();
        } else {
            // some data are wrong
            AndroidUtil.showToast(getContext(), "something got wrong, aborted");
            return;
        }

        ref.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            //String actualUserId = FirebaseAuth.getInstance().getUid();
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
                    ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
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
                            AndroidUtil.showToast(getContext(), "Can not retrieve a DownloadUrl, aborted");
                            return;
                        }
                    });

                }

                StorageReferenceAdapter adapterSR = new StorageReferenceAdapter(getContext(), arrayListSR);
                storageRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                storageRecyclerView.setAdapter(adapterSR);
                storageRecyclerView.setVisibility(View.VISIBLE);

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
                        //System.out.println("*** clicked on name: " + storageReference.getName());

                        // get the download url from task
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                // Got the download URL for 'users/me/profile.png'
                                //System.out.println("*** uri: " + uri + " ***");

                                // set progressIndicator to 0
                                downloadProgressIndicator.setProgress(0);
                                //DownloadManager.Request request = new DownloadManager.Request(uri);
                                String title = null;
                                try {
                                    title = URLUtil.guessFileName(new URL(uri.toString()).toString(), null, null);
                                    downloadSelectedDownloadUrl = new URL(uri.toString()).toString();
                                } catch (MalformedURLException e) {
                                    //throw new RuntimeException(e);
                                    AndroidUtil.showToast(getContext(), "Malformed DownloadUrl, aborted");
                                    return;
                                }
                                // now select the folder and filename on device, we are using the file chooser of Android
                                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("*/*");
                                //intent.setType("image/*/*"); // for image

                                // Optionally, specify a URI for the file that should appear in the
                                // system file picker when it loads.
                                //boolean pickerInitialUri = false;
                                //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
                                String storeFilename = title;
                                //String storeFilename = "file3a.txt";
                                intent.putExtra(Intent.EXTRA_TITLE, storeFilename);
                                // is it an unencrypted or encrypted file ?
                                if ((downloadSelector.equals(FirebaseUtil.ENCRYPTED_FILES_FOLDER_NAME)) ||
                                        (downloadSelector.equals(FirebaseUtil.ENCRYPTED_IMAGES_FOLDER_NAME))) {
                                    // encrypted stuff
                                    intent.putExtra(Intent.EXTRA_TITLE, removeExtension(storeFilename));
                                    fileDownloadDecryptSaverActivityResultLauncherXX.launch(intent);
                                } else {
                                    // unencrypted stuff
                                    intent.putExtra(Intent.EXTRA_TITLE, storeFilename);
                                    fileDownloadSaverActivityResultLauncherXX.launch(intent);
                                }

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle any errors
                                AndroidUtil.showToast(getContext(), "Error on retrieving the DownloadUrl, aborted");
                                return;
                            }
                        });
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                AndroidUtil.showToast(getContext(), "There was an error while listing files, aborted");
            }
        });
    }

    // removes the last file extension (after last dot)
    private static String removeExtension(String fname) {
        int pos = fname.lastIndexOf('.');
        if(pos > -1)
            return fname.substring(0, pos);
        else
            return fname;
    }

    private void downloadDecryptFileBtnClickXX() {
        // this will download a file, decrypt it on loading and save it in the download folder
        // todo work on this, filename should be given from the real one

        // todo QQQ

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //intent.setType("*/*");
        intent.setType("image/*"); // for image

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        //boolean pickerInitialUri = false;
        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        // todo strip the last extension with ".enc off
        String storeFilename = "mtc2.jpg";
        //String storeFilename = "file3a.txt";
        intent.putExtra(Intent.EXTRA_TITLE, storeFilename);
        fileDownloadDecryptSaverActivityResultLauncherXX.launch(intent);
    }

    ActivityResultLauncher<Intent> fileDownloadDecryptSaverActivityResultLauncherXX = registerForActivityResult(
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

                            if (!downloadPassphrasePreCheck()) return;
                            int passphraseLength = downloadPassphrase.length();
                            // get the passphrase as char[]
                            char[] passphraseChars = new char[passphraseLength];
                            downloadPassphrase.getText().getChars(0, passphraseLength, passphraseChars, 0);

                            //char[] passphrase = "123456".toCharArray();
                            //int iterations = 1000;
                            //String cacheFilename = fileInformation.getFileName() + ".enc";
                            //String encryptedFilename = new File(getContext().getCacheDir(), encryptedFilename;
                            try {
                                //Okhttp3Progress.main();
                                //String cacheFilename = "mt_cook.jpg";
                                //String storageFilename = new File(getContext().getCacheDir(), cacheFilename).getAbsolutePath();
                                //System.out.println("*** storageFilename: " + storageFilename);
                                downloadProgressIndicator.setMax(Math.toIntExact(100));

                                //String downloadUrl = "https://firebasestorage.googleapis.com/v0/b/easychat-ce2c5.appspot.com/o/bgC77aBfeYZzX5deM6PqCUe0iMr1%2Ffiles%2Fmtc.bin.enc?alt=media&token=a10a55af-109d-4fa5-a5fe-ceab69004436";
                                //String downloadUrl = "https://firebasestorage.googleapis.com/v0/b/easychat-ce2c5.appspot.com/o/bgC77aBfeYZzX5deM6PqCUe0iMr1%2Ffiles%2Ffile03.txt.enc?alt=media&token=a0ff7051-0f76-4aba-ad51-f1b321a01b30";
                                //String downloadUrl = "https://firebasestorage.googleapis.com/v0/b/easychat-ce2c5.appspot.com/o/bgC77aBfeYZzX5deM6PqCUe0iMr1%2Ffiles%2FMt_Cook_LC0247.jpg.enc?alt=media&token=a904a1f8-4a4b-4553-a307-1949b987d148";
                                Okhttp3ProgressDownloaderDecrypt downloader = new Okhttp3ProgressDownloaderDecrypt(downloadSelectedDownloadUrl, downloadProgressIndicator, getContext(), selectedUri, passphraseChars, 10000);
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

    ActivityResultLauncher<Intent> fileDownloadSaverActivityResultLauncherXX = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        System.out.println("fileDownloadSaverActivityResultLauncher");
                        // There are no request codes
                        Intent resultData = result.getData();
                        // The result data contains a URI for the document or directory that
                        // the user selected.
                        Uri selectedUri = null;
                        if (resultData != null) {
                            selectedUri = resultData.getData();
                            // Perform operations on the document using its URI.
                            Toast.makeText(getContext(), "You selected this file for download: " + selectedUri.toString(), Toast.LENGTH_SHORT).show();

                            //String cacheFilename = fileInformation.getFileName() + ".enc";
                            //String encryptedFilename = new File(getContext().getCacheDir(), encryptedFilename;
                            try {
                                //Okhttp3Progress.main();
                                //String cacheFilename = "mt_cook.jpg";
                                //String storageFilename = new File(getContext().getCacheDir(), cacheFilename).getAbsolutePath();
                                //System.out.println("*** storageFilename: " + storageFilename);
                                downloadProgressIndicator.setMax(Math.toIntExact(100));

                                //String downloadUrl = "https://firebasestorage.googleapis.com/v0/b/easychat-ce2c5.appspot.com/o/bgC77aBfeYZzX5deM6PqCUe0iMr1%2Ffiles%2Fmtc.bin.enc?alt=media&token=a10a55af-109d-4fa5-a5fe-ceab69004436";
                                //String downloadUrl = "https://firebasestorage.googleapis.com/v0/b/easychat-ce2c5.appspot.com/o/bgC77aBfeYZzX5deM6PqCUe0iMr1%2Ffiles%2Ffile03.txt.enc?alt=media&token=a0ff7051-0f76-4aba-ad51-f1b321a01b30";
                                //String downloadUrl = "https://firebasestorage.googleapis.com/v0/b/easychat-ce2c5.appspot.com/o/bgC77aBfeYZzX5deM6PqCUe0iMr1%2Ffiles%2FMt_Cook_LC0247.jpg.enc?alt=media&token=a904a1f8-4a4b-4553-a307-1949b987d148";
                                //Okhttp3ProgressDownloaderDecrypt downloader = new Okhttp3ProgressDownloaderDecrypt(downloadUrl, progressIndicator, getContext(), selectedUri, passphrase, iterations);
                                Okhttp3ProgressDownloaderNoDecrypt downloader = new Okhttp3ProgressDownloaderNoDecrypt(downloadSelectedDownloadUrl, downloadProgressIndicator, getContext(), selectedUri);
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


    /**
     * section for old methods
     */

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

    /**
     * section for encryption and decryption of a file
     */


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

    private boolean downloadPassphrasePreCheck() {
        if (TextUtils.isEmpty(downloadPassphrase.getText().toString())) {
            downloadPassphraseLayout.setError("please enter a passphrase, minimum is 3 characters");
            return false;
        } else {
            downloadPassphraseLayout.setError("");
        }
        if (downloadPassphrase.length() < 3) {
            downloadPassphraseLayout.setError("please enter a passphrase, minimum is 3 characters");
            return false;
        } else {
            downloadPassphraseLayout.setError("");
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
        storageRecyclerView.setVisibility(View.GONE);
    }
    
}













