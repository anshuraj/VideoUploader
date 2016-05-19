package me.anshuraj.videouploader;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

/**
 * Created by deadlydespo on 14-05-2016.
 */
public class UsingDriveSDK extends AppCompatActivity  {

    Button btn;
    Drive service ;
    GoogleAccountCredential credential;

    ProgressDialog pd;
    AlertDialog.Builder builder;
    AlertDialog alert ;

    String ACCOUNT_NAME;
    private static final String TAG = "TAG";
    private static final int FILE_SELECT_CODE = 3;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final int COMPLETE_AUTHORIZATION_REQUEST_CODE = 101;
    private static final int REQUEST_ACCOUNT_PICKER = 1001;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.GET_ACCOUNTS
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        btn = (Button) findViewById(R.id.uploadBtn);

        if(Build.VERSION.SDK_INT> Build.VERSION_CODES.LOLLIPOP_MR1) {
            int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(
                        this,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
            }
        }

        credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(DriveScopes.DRIVE));
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        ACCOUNT_NAME = settings.getString(PREF_ACCOUNT_NAME, null);
        //if(ACCOUNT_NAME != null) {

            credential.setSelectedAccountName(ACCOUNT_NAME);
            service = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
        chooseAccount();
/*
        }
        else{
            chooseAccount();
        }
*/

        Log.i(TAG," after credential");

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //startActivityForResult(getIntent() ,COMPLETE_AUTHORIZATION_REQUEST_CODE);
                //chooseAccount();
                showFileChooser();
            }
        });

    }

    public AsyncTask<Void, Void, Void> task;

    void upload(final String path) throws NullPointerException{
        task = new AsyncTask<Void, Void, Void>() {
            int error = 0;

            class CustomProgressListener implements MediaHttpUploaderProgressListener {
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    Log.i(TAG, " inside customprogressListener ");
                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            System.out.println("Initiation has started!");
                            //pd.setIndeterminate(true);
                            break;
                        case INITIATION_COMPLETE:
                            System.out.println("Initiation is complete!");
                            pd.setIndeterminate(false);
                            break;
                        case MEDIA_IN_PROGRESS:
                            System.out.println(uploader.getProgress() * 100);
                            pd.setProgress((int) (uploader.getProgress() * 100));
                            break;
                        case MEDIA_COMPLETE:
                            System.out.println("Upload is complete!");
                            pd.setProgress(100);
                    }
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                builder = new AlertDialog.Builder(UsingDriveSDK.this);
                pd = new ProgressDialog(UsingDriveSDK.this);
                pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pd.setCancelable(false);
                pd.setMessage("Uploading...");
                pd.setProgress(0);
                pd.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
                pd.show();
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (pd != null && pd.isShowing()) {
                    pd.dismiss();
                    pd.cancel();
                    pd = null;
                }

                if (error != 1 && error != 2) {
                    builder.setCancelable(false);
                    builder.setMessage("Uploaded!")
                            .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    finish();
                                }
                            });

                    alert = builder.create();
                    alert.show();
                } else if (error == 2) {
                    Toast.makeText(UsingDriveSDK.this, "Error, please try later",
                            Toast.LENGTH_SHORT).show();
                }
                else if (error == 2 && error == 1) {
                    Toast.makeText(UsingDriveSDK.this, "Error, please try later",
                            Toast.LENGTH_SHORT).show();
                    if (alert != null)
                        alert.dismiss();

                } else if (error == 1) {
                    if (alert != null)
                        alert.dismiss();
                }


            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                alert.dismiss();
                pd.dismiss();
            }

            @Override
            protected Void doInBackground(Void... params) {
                Log.d(TAG, " Inside upload thread ");
                try {
                    // Try to perform a Drive API request, for instance:
                    java.io.File mediaFile = new java.io.File(path);
                    InputStreamContent mediaContent =
                            new InputStreamContent("video/*",
                                    new BufferedInputStream(new FileInputStream(mediaFile)));
                    mediaContent.setLength(mediaFile.length());

                    File fileMetadata = new File();
                    fileMetadata.setTitle(mediaFile.getName());
                    fileMetadata.setMimeType("video/*");

                    Drive.Files.Insert request = null;

                    try {
                        request = service.files().insert(fileMetadata, mediaContent);
                        MediaHttpUploader uploader = request.getMediaHttpUploader();
                        uploader.setDirectUploadEnabled(false);
                        uploader.setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE);
                        uploader.setProgressListener(new CustomProgressListener());
                        request.execute();

                    } catch (UserRecoverableAuthIOException e) {

                        Log.i(TAG, "User Recoverable Exception ");
                        error = 1;
                        startActivityForResult(e.getIntent(), COMPLETE_AUTHORIZATION_REQUEST_CODE);
                    }

                } catch (IOException e) {
                    Log.i("Err", "IOException");
                    error = 2;
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();



    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {

            case COMPLETE_AUTHORIZATION_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    // App is authorized, you can go back to sending the API request
                    //upload();
                    Log.i(TAG," inside COMPLETE_AUTHORIZATION_REQUEST_CODE ");
                    Toast.makeText(this, "Application authorized, try again",
                            Toast.LENGTH_SHORT).show();
                    //showFileChooser();


                } else {
                    // User denied access, show him the account chooser again
                    Log.i(TAG," inside choose account in else COMPLETE_AUTHORIZATION_REQUEST_CODE ");
                    chooseAccount();

                }
                break;

            case REQUEST_ACCOUNT_PICKER:
                Log.i(TAG," result_acc_pick ");
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        credential.setSelectedAccountName(accountName);
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.commit();
                        //AsyncLoadTasks.run(this);
                        //upload();
                        Log.i(TAG," result_acc_pick ------------");
                        //showFileChooser();
                        //startActivityForResult(getIntent() ,COMPLETE_AUTHORIZATION_REQUEST_CODE);
                    }
                    else{
                        chooseAccount();
                    }
                }
                else{

                }
                break;
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    //Log.i(TAG, "File Uri: " + uri.toString());
                    // Get the path
                    String path = null;
                    try {
                        path = FileUtils.getPath(this, uri);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "File Path: " + path);
                    // Get the file instance
                    //java.io.File file = new java.io.File(path);
                    // Initiate the upload
                    Log.i(TAG," File_select_code ");
                    upload(path);
                }
                break;
        }
    }

    private void chooseAccount() {
        Log.i(TAG," inside choose account ");
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        Log.i(TAG," inside filechooser ");
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }





}
