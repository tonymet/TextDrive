package us.tonym.TextDrive;

import java.io.IOException;
import java.io.OutputStream;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;
import com.google.android.gms.drive.DriveApi.ContentsResult;
import com.google.android.gms.drive.DriveApi.OnNewContentsCallback;

public class MainActivity  extends Activity implements
	GoogleApiClient.ConnectionCallbacks,
	GoogleApiClient.OnConnectionFailedListener {
	
	// google's code
	  private static final String TAG = "TextDriveActivity";
	  protected static final int REQUEST_CODE_CREATOR = 1;
	  private static final DriveId sFolderId =
	            DriveId.createFromResourceId("0BzsgwfF2CVNUM0pDM0EwRVJ5aFk");

	    /**
	     * Extra for account name.
	     */
	    protected static final String EXTRA_ACCOUNT_NAME = "account_name";

	    /**
	     * Request code for auto Google Play Services error resolution.
	     */
	    protected static final int REQUEST_CODE_RESOLUTION = 1;

	    /**
	     * Next available request code.
	     */
	    protected static final int NEXT_AVAILABLE_REQUEST_CODE = 2;

	    /**
	     * Google API client.
	     */
	    private GoogleApiClient mGoogleApiClient;

	    /**
	     * Called when activity gets visible. A connection to Drive services need to
	     * be initiated as soon as the activity is visible. Registers
	     * {@code ConnectionCallbacks} and {@code OnConnectionFailedListener} on the
	     * activities itself.
	     */
	    @Override
	    protected void onResume() {
	        super.onResume();
	        if (mGoogleApiClient == null) {
	            mGoogleApiClient = new GoogleApiClient.Builder(this)
	                    .addApi(Drive.API)
	                    .addScope(Drive.SCOPE_FILE)
	                    .addConnectionCallbacks(this)
	                    .addOnConnectionFailedListener(this)
	                    .build();
	        }
	        mGoogleApiClient.connect();
	        Log.i(TAG, "GoogleApiClient connected onResume()");
	    }

	    /**
	     * Handles resolution callbacks.
	     */
	    /*
	    @Override
	    protected void onActivityResult(int requestCode, int resultCode,
	            Intent data) {
	        super.onActivityResult(requestCode, resultCode, data);
	        if (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
	            mGoogleApiClient.connect();
	        }
	    }
	    */

	    /**
	     * Called when activity gets invisible. Connection to Drive service needs to
	     * be disconnected as soon as an activity is invisible.
	     */
	    @Override
	    protected void onPause() {
	        if (mGoogleApiClient != null) {
	            mGoogleApiClient.disconnect();
	            Log.i(TAG, "GoogleApiClient disconnect onPause()");
	        }
	        super.onPause();
	    }

	    /**
	     * Called when {@code mGoogleApiClient} is connected.
	     */
	    @Override
	    public void onConnected(Bundle connectionHint) {
	        Log.i(TAG, "GoogleApiClient connected");
	    }

	    /**
	     * Called when {@code mGoogleApiClient} is disconnected.
	     */
	    @Override
	    public void onDisconnected() {
	        Log.i(TAG, "GoogleApiClient disconnected");
	    }

	    /**
	     * Called when {@code mGoogleApiClient} is trying to connect but failed.
	     * Handle {@code result.getResolution()} if there is a resolution is
	     * available.
	     */
	    @Override
	    public void onConnectionFailed(ConnectionResult result) {
	        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
	        if (!result.hasResolution()) {
	            // show the localized error dialog.
	            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
	            return;
	        }
	        try {
	            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
	        } catch (SendIntentException e) {
	            Log.e(TAG, "Exception while starting resolution activity", e);
	        }
	    }

	    /**
	     * Shows a toast message.
	     */
	    public void showMessage(String message) {
	        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	    }

	    /**
	     * Getter for the {@code GoogleApiClient}.
	     */
	    public GoogleApiClient getGoogleApiClient() {
	      return mGoogleApiClient;
	    }
	
	
	// my code

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void onSaveClick(View v){
		
        OnNewContentsCallback onContentsCallback = new OnNewContentsCallback() {

            @Override
            public void onNewContents(ContentsResult result) {
            	TextView tv = (TextView)findViewById(R.id.editText1);
            	OutputStream outputStream = result.getContents().getOutputStream();
            	 try {
                     outputStream.write(tv.getText().toString().getBytes());
                 } catch (IOException e1) {
                     Log.i(TAG, "Unable to write file contents.");
                 }
                MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                        .setMimeType("text/plain").build();
                IntentSender intentSender = Drive.DriveApi
                        .newCreateFileActivityBuilder()
                        .setInitialMetadata(metadataChangeSet)
                        .setInitialContents(result.getContents())
                        //.setActivityStartFolder(sFolderId)
                        .build(getGoogleApiClient());
                try {
                    startIntentSenderForResult(
                            intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                } catch (SendIntentException e) {
                  Log.w(TAG, "Unable to send intent", e);
                }
            }
        };
        Drive.DriveApi.newContents(getGoogleApiClient()).addResultCallback(onContentsCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CODE_CREATOR:
            if (resultCode == RESULT_OK) {
                DriveId driveId = (DriveId) data.getParcelableExtra(
                        OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                showMessage("File created with ID: " + driveId);
            }
            finish();
            break;
        default:
            super.onActivityResult(requestCode, resultCode, data);
            break;
        }
    }
	
	
	// end google code
}
