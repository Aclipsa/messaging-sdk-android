package com.aclipsa.aclipsasdkdemo.activities;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDK.AclipsaSDKHandler;
import com.aclipsa.aclipsasdk.AclipsaSDK.AclipsaSDKMessageHandler;
import com.aclipsa.aclipsasdk.AclipsaSDKConstants;
import com.aclipsa.aclipsasdk.AclipsaSDKException;
import com.aclipsa.aclipsasdk.AclipsaSDKMessage;
import com.aclipsa.aclipsasdk.AclipsaSDKVideoView;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaMessage;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaVideo;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.helpers.DrawableFromUrlTask;
import com.aclipsa.aclipsasdkdemo.helpers.ToastHelper;

public class MessageDetailActivity extends Activity implements AclipsaSDKHandler{
	
	private static final String M_GUID = "mguid";
	private static final String TAG = MessageDetailActivity.class.getName();
	
	private String message_guid;
	private AclipsaMessage message;
	private ProgressDialog progressdialog;
	private TextView titleTextView;
	private TextView bodyTextView;
	private AclipsaSDKVideoView videoView;
	
	private int tempCounter = 0;
	
	// handler for received Intents for the AclipsaSDK playback event 
	private BroadcastReceiver mMessageReceiver;
	
	public static Intent createIntent(Context context, String message_guid){
		Intent intent = new Intent(context, MessageDetailActivity.class);
		intent.putExtra(M_GUID, message_guid);
		
		return intent;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.message_detail_activity);
	 
	    message_guid = getIntent().getStringExtra(M_GUID);
	    
	    message = AclipsaSDK.getInstance(this).getMessageWithGuid(message_guid);
	    
	    videoView = (AclipsaSDKVideoView)findViewById(R.id.messageVideoView);	
	    	
	    titleTextView = (TextView)findViewById(R.id.titleTextView);
	    bodyTextView  = (TextView)findViewById(R.id.bodyTextView);
	    
	    titleTextView.setText("Title: ");
	    bodyTextView.setText("Message: ");	

	    if(message.getTitle() != null)
	    	titleTextView.setText("Title: "+message.getTitle());
	    
	    if(message.getCaption() != null)
	    	bodyTextView.setText("Message: "+message.getCaption());	
		
	    Button playButton = (Button)findViewById(R.id.playButton);
	    playButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				try {
					videoView.setZOrderOnTop(false);
					
					AclipsaSDK.getInstance(arg0.getContext()).playVideo(arg0.getContext(), 
							MessageDetailActivity.this, message.getVideo(), videoView, null, null);
					
					
				} catch (AclipsaSDKException e) {
					e.printStackTrace();
				}
				
			}
		});
	    
	    //TODO: needs to be called once.  most likely after registration
	    AclipsaSDK.getInstance(this).enableProgressNotifications(true);
	    
	    mMessageReceiver = new BroadcastReceiver() {
	    	@Override
	    	public void onReceive(Context context, Intent intent) {

	    		// Extract data included in the Intent
	    		boolean isPaused = intent.getBooleanExtra(AclipsaSDKConstants.ACLIPSA_VIDEO_PLAYBACK_PAUSE, false);
				boolean isResumed = intent.getBooleanExtra(AclipsaSDKConstants.ACLIPSA_VIDEO_PLAYBACK_RESUME, false);
				boolean isStopped = intent.getBooleanExtra(AclipsaSDKConstants.ACLIPSA_VIDEO_PLAYBACK_STOP, false);
	    		boolean hasStarted = intent.getBooleanExtra(AclipsaSDKConstants.ACLIPSA_VIDEO_PLAYBACK_START, false);
	    		
	    		if(isPaused)
			    	ToastHelper.show(context, "Playback is paused");
			    
			    if(isResumed)
			    	ToastHelper.show(context, "Playback is resumed");
			    
			    if(isStopped)
			    	ToastHelper.show(context, "Playback is stopped");
	    		
	    		if(hasStarted)
	    			tempCounter++;

	    		//TODO: Look for ways to get accurate playback trigger
	    		if(tempCounter == 3) //It seems that playback start is triggered 3 times.  3rd time is the actual playback
	    			progressdialog.dismiss();

	    	}
	    };
	    
	    if(message.getVideo() != null){
	    	//TODO: may need to move this on the sdk
	    	new DrawableFromUrlTask(videoView).execute(message.getVideo().getThumbnail_medium());
	    
	    	videoView.setZOrderOnTop(true);
	    }
	    else{
	    	videoView.setVisibility(View.GONE);
	    	playButton.setVisibility(View.GONE);
	    }
	    
	    progressdialog = new ProgressDialog(this);
		progressdialog.setMessage("Loading...");
		progressdialog.setIndeterminate(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Register mMessageReceiver to receive messages.
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,new IntentFilter(AclipsaSDKConstants.ACLIPSA_VIDEO_PLAYBACK_BROADCAST));
	}
	
	@Override
	public void onPause() {
		
		// Unregister since the activity is not visible
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		
		super.onPause();
	}
	
	//Playback handler
	@Override
	public void apiRequestResponseFailure(Object arg0, int statusCode, String errorString) {
		
		progressdialog.dismiss();
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
	public void apiRequestResponseSuccess(Object arg0, int statusCode, String errorString) {
		//Success means that our parameter was validated correctly
		progressdialog.show();
		
	}
	
	
}
