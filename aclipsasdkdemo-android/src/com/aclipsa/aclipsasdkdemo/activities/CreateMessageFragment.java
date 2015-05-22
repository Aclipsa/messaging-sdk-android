package com.aclipsa.aclipsasdkdemo.activities;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDK.AclipsaSDKMessageHandler;
import com.aclipsa.aclipsasdk.AclipsaSDKConstants;
import com.aclipsa.aclipsasdk.SdkDataManager;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.helpers.ToastHelper;

public class CreateMessageFragment extends Fragment implements AclipsaSDKMessageHandler{
	
	private static final int VIDEO_SELECTED = 100;
	private ImageView previewImageView;
	private TextView progressTextView;
	private EditText titleEditText;
	private EditText messageEditText;
	private EditText recipient1EditText;
	private EditText recipient2EditText;
	private Button sendButton;
	private ProgressDialog progressdialog;
	private Uri selectedvideo;
	private CheckBox encodingCheckbox;
	
	private final static String TAG = CreateMessageFragment.class.getName();
	private String uploadingGuid;
	
	// handler for received Intents for the AclipsaSDK upload event 
	private BroadcastReceiver mMessageReceiver;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.create_message, null);
		
		previewImageView = (ImageView)v.findViewById(R.id.previewImageView);
		progressTextView = (TextView)v.findViewById(R.id.progressTextView);
		progressTextView.setText("");
		
		titleEditText = (EditText)v.findViewById(R.id.titleEditText);
		messageEditText = (EditText)v.findViewById(R.id.messageEditText);
		recipient1EditText = (EditText)v.findViewById(R.id.recipient1EditText);
		recipient2EditText = (EditText)v.findViewById(R.id.recipient2EditText);
		encodingCheckbox = (CheckBox)v.findViewById(R.id.bypassEncodingCheckbox);

		sendButton = (Button)v.findViewById(R.id.sendButton);
		
		Button pickVideoButton = (Button)v.findViewById(R.id.pickVideoButton);
		
		pickVideoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("video/*");
				startActivityForResult(intent, VIDEO_SELECTED);
			}
		});
		
		sendButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				sendMessage();
			}
		});
		
		
		AclipsaSDK.getInstance(getActivity()).enableProgressNotifications(true);
		
		mMessageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				
				if (progressdialog != null) {
					// Extract data included in the Intent
					boolean isResumed = intent.getBooleanExtra(
							AclipsaSDKConstants.ACLIPSA_VIDEO_UPLOAD_RESUMED,
							false);
					if (isResumed == true && progressdialog.isShowing() == false)
						progressdialog.show();

					float progress = intent.getFloatExtra(
							AclipsaSDKConstants.ACLIPSA_VIDEO_UPLOAD_PROGRESS, 0);
					progressdialog.setProgress((int) progress);

				}
			}
		};
		
		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Register mMessageReceiver to receive messages.
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,new IntentFilter(AclipsaSDKConstants.ACLIPSA_VIDEO_UPLOAD_BROADCAST));
	}
	
	@Override
	public void onPause() {

		// Unregister since the activity is not visible
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);

		//Hide the keyboard if its showing
		InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(titleEditText.getWindowToken(), 0);  
		imm.hideSoftInputFromWindow(messageEditText.getWindowToken(), 0);  
		imm.hideSoftInputFromWindow(recipient1EditText.getWindowToken(), 0);  
		imm.hideSoftInputFromWindow(recipient2EditText.getWindowToken(), 0);  
		

		super.onPause();
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == VIDEO_SELECTED && resultCode == Activity.RESULT_OK){
			selectedvideo = data.getData();
			
			//For preview
			String[] filePathColumn = { MediaStore.Video.Media.DATA };
			Cursor cursor =  getActivity().getContentResolver().query(selectedvideo, filePathColumn, null, null, null);

			if (cursor == null)
			{
				//try if we could access the uri directly	
				File testFile = new File(selectedvideo.getPath());
				if(testFile.exists()){
					Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(selectedvideo.getPath(),
				        MediaStore.Images.Thumbnails.MINI_KIND);
				
					previewImageView.setImageBitmap(thumbnail);
				}
				else{
					progressdialog.dismiss();
					ToastHelper.show(getActivity(), "Unable to obtain selected video");
					return;
				}
			}
			else{
				cursor.moveToFirst();

				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				String videoFile = cursor.getString(columnIndex);
				cursor.close();

				Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoFile,
						MediaStore.Images.Thumbnails.MINI_KIND);

				previewImageView.setImageBitmap(thumbnail);
			}
		}	
	}

	private void sendMessage()
	{
		
		if(recipient1EditText.getText().toString().length() < 1 && recipient2EditText.getText().toString().length() < 1){
			ToastHelper.show(getActivity(), "There needs to be at least one recipient");
			return;
		}
		
		ArrayList<String> recipients = new ArrayList<String>();
		
		if(recipient1EditText.getText().toString().length() > 1)
			recipients.add(recipient1EditText.getText().toString());
		
		if(recipient2EditText.getText().toString().length() > 1)
			recipients.add(recipient2EditText.getText().toString());
		
		if(selectedvideo != null){
			
			progressdialog = new ProgressDialog(getActivity()); 
			progressdialog.setMessage("Sending Message...");
			progressdialog.setCancelable(false);
			progressdialog.setIndeterminate(false);
			progressdialog.setMax(100);
			progressdialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressdialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Pause", new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			    	AclipsaSDK.getInstance(getActivity()).pauseCreateVideo(uploadingGuid);
			    }
			});
			progressdialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			       AclipsaSDK.getInstance(getActivity()).cancelVideoUpload(uploadingGuid);
			    }
			});
			
			progressdialog.show();
			
			uploadingGuid = AclipsaSDK.getInstance(getActivity()).sendMessage(getActivity(), this, 
					"VIDEO_MESSAGE", titleEditText.getEditableText().toString(), 
					messageEditText.getEditableText().toString(), selectedvideo, recipients, encodingCheckbox.isChecked(),SdkDataManager.SOL_createSecureUUID(), "portrait",true);
		}	
		else{	
			
			progressdialog = ProgressDialog.show(getActivity(), null, "Sending message...");
			
			uploadingGuid = AclipsaSDK.getInstance(getActivity()).sendMessage(getActivity(), this, 
					"VIDEOLESS_MESSAGE", titleEditText.getEditableText().toString(), 
					messageEditText.getEditableText().toString(), null, recipients, encodingCheckbox.isChecked(), SdkDataManager.SOL_createSecureUUID(), "portrait", true);
		}	
	}
	
	@Override
	public void apiCreateMessageSuccess(Object tag, int statusCode,
			String errorString, String guid) {

		progressdialog.dismiss();

		if (tag == "VIDEOLESS_MESSAGE")
		{
			Log.i(TAG, "apiCreateMessageSuccess VIDEOLESS_MESSAGE success");
		} 
		else if (tag == "VIDEO_MESSAGE")
		{
			Log.i(TAG, "apiCreateMessageSuccess VIDEO_MESSAGE success");
		}
	}


	@Override
	public void apiMessageRequestResponseSuccess(Object tag, int statusCode,
			String errorString, Object response) {
		
		progressdialog.dismiss();

//		ArrayList<AclipsaSDKMessage> messages
		
		if (tag == "VIDEOLESS_MESSAGE")
		{
			Log.i(TAG, "apiMessageRequestResponseSuccess VIDEOLESS_MESSAGE success");
		} 
		else if (tag == "VIDEO_MESSAGE")
		{
			Log.i(TAG, "apiMessageRequestResponseSuccess VIDEO_MESSAGE success");
		}
	}


	@Override
	public void apiMessageRequestResponseFailure(Object tag, int statusCode,
			String errorString) {
		// TODO Auto-generated method stub
		
		progressdialog.dismiss();

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				getActivity());

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
}
