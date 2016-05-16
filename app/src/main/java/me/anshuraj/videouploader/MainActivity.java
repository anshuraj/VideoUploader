package me.anshuraj.videouploader;

import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener {

    String TAG = "TAG";
    String MIME_VIDEO = "video/*";
    String MIME_PHOTO = "photo/*";
    private static final int REQUEST_CODE_OPENER = 1;
    protected static final int REQUEST_CODE_RESOLUTION = 1;
    protected static final int  RESOLVE_CONNECTION_REQUEST_CODE = 1;
    private static final int FILE_SELECT_CODE = 3;
    protected static final int REQUEST_CODE_CREATOR = 2;
    GoogleApiClient mGoogleApiClient;
    Button UploadBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usingdrivesdk);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        UploadBtn = (Button) findViewById(R.id.button);

        //FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        UploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                showFileChooser();
                //Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(driveContentsCallback);

            }
        });

        mGoogleApiClient = buildApiClient();
    }

    GoogleApiClient buildApiClient(){
        return new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }



    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CREATOR:
                if (resultCode == RESULT_OK) {
                    DriveId driveId = data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    showMessage("File created with ID: " + driveId);
                }
                finish();
                break;
            case REQUEST_CODE_OPENER:
                if (resultCode == RESULT_OK) {
                    DriveId driveId = data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    showMessage("Selected file's ID: " + driveId);
                }
                finish();
                break;
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d(TAG, "File Uri: " + uri.toString());
                    // Get the path
                    String path = null;
                    try {
                        path = FileUtils.getPath(this, uri);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "File Path: " + path);
                    // Get the file instance
                     File file = new File(path);
                    // Initiate the upload
                    saveFiletoDrive(file, "video/*");
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {

            // disconnect Google Android Drive API connection.
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }


    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

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



    final ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                            .setMimeType("text/html").build();
                    IntentSender intentSender = Drive.DriveApi
                            .newCreateFileActivityBuilder()
                            .setInitialMetadata(metadataChangeSet)
                            .setInitialDriveContents(result.getDriveContents())
                            .build(mGoogleApiClient);
                    try {
                        startIntentSenderForResult(
                                intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                    } catch (IntentSender.SendIntentException e) {
                        Log.w("TAG", "Unable to send intent", e);
                    }
                }


            };

    private void saveFiletoDrive(final File file, final String mime) {
        // Start by creating a new contents, and setting a callback.
        Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(
                new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        // If the operation was not successful, we cannot do
                        // anything
                        // and must
                        // fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i("TAG", "Failed to create new contents.");
                            return;
                        }
                        Log.i("TAG", "Connection successful, creating new contents...");
                        // Otherwise, we can write our data to the new contents.
                        // Get an output stream for the contents.
                        OutputStream outputStream = result.getDriveContents()
                                .getOutputStream();
                        FileInputStream fis;
                        try {
                            fis = new FileInputStream(file.getPath());
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buf = new byte[1024];
                            int n;
                            while (-1 != (n = fis.read(buf)))
                                baos.write(buf, 0, n);
                            byte[] photoBytes = baos.toByteArray();
                            outputStream.write(photoBytes);

                            outputStream.close();
                            outputStream = null;
                            fis.close();
                            fis = null;

                        } catch (FileNotFoundException e) {
                            Log.w("TAG", "FileNotFoundException: " + e.getMessage());
                        } catch (IOException e1) {
                            Log.w("TAG", "Unable to write file contents." + e1.getMessage());
                        } catch (OutOfMemoryError e2) {
                            Toast.makeText(getApplicationContext(), "Out of memory", Toast.LENGTH_SHORT).show();
                        }


                        String title = file.getName();
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setMimeType(mime).setTitle(title).build();

                        if (mime.equals(MIME_PHOTO)) {
                                Log.i("TAG", "Creating new photo on Drive (" + title
                                        + ")");
                            Drive.DriveApi.getRootFolder(mGoogleApiClient
                                    ).createFile(mGoogleApiClient,
                                    metadataChangeSet,
                                    result.getDriveContents());
                        } else if (mime.equals(MIME_VIDEO)) {
                            Log.i("TAG", "Creating new video on Drive (" + title
                                    + ")");
                            /*Drive.DriveApi.getRootFolder(mGoogleApiClient).createFile(mGoogleApiClient,
                                    metadataChangeSet,
                                    result.getDriveContents()); */

                            IntentSender intentSender = Drive.DriveApi
                                    .newCreateFileActivityBuilder()
                                    .setInitialMetadata(metadataChangeSet)
                                    .setInitialDriveContents(result.getDriveContents())
                                    .build(mGoogleApiClient);
                            try {
                                startIntentSenderForResult(
                                        intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                            } catch (IntentSender.SendIntentException e) {
                                Log.w(TAG, "Unable to send intent", e);
                            }

                        }
                    }
                });
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {

        Log.i("TAG", "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, connectionResult.getErrorCode());
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
                Log.i("TAG", "connectionFailed");
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }
}
