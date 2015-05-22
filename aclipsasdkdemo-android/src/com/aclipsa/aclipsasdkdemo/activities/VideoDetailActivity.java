package com.aclipsa.aclipsasdkdemo.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDK.AclipsaSDKHandler;
import com.aclipsa.aclipsasdk.AclipsaSDKConstants;
import com.aclipsa.aclipsasdk.AclipsaSDKException;
import com.aclipsa.aclipsasdk.AclipsaSDKVideo;
import com.aclipsa.aclipsasdk.AclipsaSDKVideoView;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaVideo;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.helpers.ToastHelper;

public class VideoDetailActivity extends Activity implements AclipsaSDKHandler {

	public final static String TAG = VideoDetailActivity.class.getName();

	private AclipsaVideo mVideo;
//	private MediaPlayer mMediaPlayer;
//	private StreamProxy proxy;
	private AclipsaSDKVideoView videoView;
	private ProgressDialog progressdialog;
	
	// handler for received Intents for the AclipsaSDK playback event 
	private BroadcastReceiver mMessageReceiver;
	
	private int tempCounter = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.video_detail_activity);

		videoView = (AclipsaSDKVideoView) findViewById(R.id.detailVideoView);
		
		
		Intent intent = getIntent();
		mVideo = intent.getParcelableExtra("VIDEO");
		
		//TODO: needs to be called once.  most likely after registration
	    AclipsaSDK.getInstance(this).enableProgressNotifications(true);
		
		mMessageReceiver = new BroadcastReceiver() {
			  @Override
			  public void onReceive(Context context, Intent intent) {
			    
				// Extract data included in the Intent
			    boolean isPaused = intent.getBooleanExtra(AclipsaSDKConstants.ACLIPSA_VIDEO_PLAYBACK_PAUSE, false);
			    boolean isResumed = intent.getBooleanExtra(AclipsaSDKConstants.ACLIPSA_VIDEO_PLAYBACK_RESUME, false);
			    boolean isStopped = intent.getBooleanExtra(AclipsaSDKConstants.ACLIPSA_VIDEO_PLAYBACK_STOP, false);
			    
			    if(isPaused)
			    	ToastHelper.show(context, "Playback is paused");
			    
			    if(isResumed)
			    	ToastHelper.show(context, "Playback is resumed");
			    
			    if(isStopped)
			    	ToastHelper.show(context, "Playback is stopped");
			    
			 // Extract data included in the Intent
	    		boolean hasStarted = intent.getBooleanExtra(AclipsaSDKConstants.ACLIPSA_VIDEO_PLAYBACK_START, false);

	    		if(hasStarted)
	    			tempCounter++;
	    		
	    		//TODO: Look for ways to get accurate playback trigger
//	    		if(tempCounter == 3) //It seems that playback start is triggered 3 times.  3rd time is the actual playback
////	    			ToastHelper.show(context, "Playback started");
//	    			progressdialog.dismiss();
			  }
		};
		
//		progressdialog = new ProgressDialog(this);
//		progressdialog.setMessage("Loading...");
//		progressdialog.setIndeterminate(true);
//		
//		progressdialog.show();
	}

	@Override
	public void onResume() {
		super.onResume();
		
		// Register mMessageReceiver to receive messages.
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,new IntentFilter(AclipsaSDKConstants.ACLIPSA_VIDEO_PLAYBACK_BROADCAST));
		
		try {
			AclipsaSDK.getInstance(this).playVideo(this, this, mVideo, videoView, null, null);
		} catch (AclipsaSDKException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void onPause() {
		
		// Unregister since the activity is not visible
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		
		super.onPause();
	}

	
	@Override
	public void apiRequestResponseFailure(Object tag, int statusCode,
			String errorString) {
		// TODO Auto-generated method stub
		
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				this);

		// set title
		alertDialogBuilder.setTitle("Error");

		// set dialog message
		alertDialogBuilder
				.setMessage(errorString)
				.setCancelable(false)
				.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int id) {
								// if this button is clicked, close
								// current activity
								dialog.cancel();
							}
						});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
		
	}

	@Override
	public void apiRequestResponseSuccess(Object tag, int statusCode,
			String errorString) {
		// TODO Auto-generated method stub
		
	}
	

}
