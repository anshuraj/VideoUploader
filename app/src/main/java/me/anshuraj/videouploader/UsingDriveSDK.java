package me.anshuraj.videouploader;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

/**
 * Created by deadlydespo on 14-05-2016.
 */
public class UsingDriveSDK extends AppCompatActivity  {

    Button btn;

    GoogleAccountCredential credential;
    Drive service ;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String TAG = "Log TAG";
    private static final int FILE_SELECT_CODE = 3;
    private static final int REQUEST_AUTHORIZATION = 1;
    private  static final int COMPLETE_AUTHORIZATION_REQUEST_CODE = 101;
    private  static final int REQUEST_ACCOUNT_PICKER = 1001;

    final HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();

    final JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = (Button) findViewById(R.id.button);


        credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(DriveScopes.DRIVE));
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        credential.setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));
        service = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();

        Log.i(TAG," after credential");
        startActivityForResult(getIntent() ,COMPLETE_AUTHORIZATION_REQUEST_CODE);
        Log.i(TAG,"startActivityForResult(getIntent() ,COMPLETE_AUTHORIZATION_REQUEST_CODE);");

        //upload();
        //showFileChooser();
        //Log.i(TAG," after showFileChooser()");
        /*btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileChooser();
            }
        });*/
    }

    void upload(final String path){
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                Log.d(TAG," Inside upload thread ");
                try {
                    // Try to perform a Drive API request, for instance:
                    java.io.File mediaFile = new java.io.File(path);
                    InputStreamContent mediaContent =
                            new InputStreamContent("image/jpeg",
                                    new BufferedInputStream(new FileInputStream(mediaFile)));

                    mediaContent.setLength(mediaFile.length());

                    File fileMetadata = new File();
                    fileMetadata.setTitle(mediaFile.getName());
                    fileMetadata.setMimeType("video/*");

                    Drive.Files.Insert request = service.files().insert(fileMetadata, mediaContent);
                    request.getMediaHttpUploader().setProgressListener(new CustomProgressListener());
                    request.execute();

                    //File file = service.files().create(file, mediaContent).execute();

                } catch (UserRecoverableAuthIOException e) {
                    Log.i(TAG,"User Recoverable Exception ");
                    startActivityForResult(e.getIntent(), COMPLETE_AUTHORIZATION_REQUEST_CODE);
                } catch (IOException e) {
                    Log.i("Err", "IOException");
                }
            }
        });

        thread.start();

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {

            case COMPLETE_AUTHORIZATION_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    // App is authorized, you can go back to sending the API request
                    //upload();
                    showFileChooser();
                    Log.i(TAG," COMPLETE_AUTHORIZATION_REQUEST_CODE ");
                } else {
                    // User denied access, show him the account chooser again
                    chooseAccount();
                    Log.i(TAG," choose account in COMPLETE_AUTHORIZATION_REQUEST_CODE ");
                }
                break;

            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    //AsyncLoadTasks.run(this);
                    //upload();
                    //showFileChooser();
                    Log.i(TAG," REQUEST_AUTHORIZATION ");
                } else {
                    chooseAccount();
                    Log.i(TAG," chooseAccount request_auth ");
                }
                break;

            case REQUEST_ACCOUNT_PICKER:
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
                        Log.i(TAG," result_acc_pick ");
                        showFileChooser();
                    }
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
                    //Log.i(TAG, "File Path: " + path);
                    // Get the file instance
                    //java.io.File file = new java.io.File(path);
                    // Initiate the upload
                    //saveFiletoDrive(file, "video/*");
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
        Log.i(TAG," Cinside filechooser ");
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

    class CustomProgressListener implements MediaHttpUploaderProgressListener {
        public void progressChanged(MediaHttpUploader uploader) throws IOException {
            Log.i(TAG," inside customprogressListener ");
            switch (uploader.getUploadState()) {
                case INITIATION_STARTED:
                    System.out.println("Initiation has started!");
                    break;
                case INITIATION_COMPLETE:
                    System.out.println("Initiation is complete!");
                    break;
                case MEDIA_IN_PROGRESS:
                    System.out.println(uploader.getProgress()*100);
                    break;
                case MEDIA_COMPLETE:
                    System.out.println("Upload is complete!");
            }
        }
    }

}
