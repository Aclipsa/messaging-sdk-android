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
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDK.AclipsaSDKVideoHandler;
import com.aclipsa.aclipsasdk.AclipsaSDKConstants;
import com.aclipsa.aclipsasdk.AclipsaSDKVideo;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaVideo;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.helpers.ToastHelper;

public class CreateVideoFragment extends Fragment implements AclipsaSDKVideoHandler {

	private static final int VIDEO_SELECTED = 100;
	private ImageView previewImageView;
	private EditText titleEditText;
	private ProgressDialog progressdialog;
	private String uploadingGuid;
	private CheckBox encodingCheckbox;

	// handler for received Intents for the AclipsaSDK upload event 
	private BroadcastReceiver mMessageReceiver;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.create_video_activity, null);
		
		previewImageView = (ImageView)v.findViewById(R.id.previewImageView);
		
		titleEditText = (EditText)v.findViewById(R.id.titleEditText);
		
		encodingCheckbox = (CheckBox)v.findViewById(R.id.bypassEncodingCheckbox);
		
		Button pickVideoButton = (Button)v.findViewById(R.id.pickVideoButton);
		
		pickVideoButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("video/*");
				startActivityForResult(intent, VIDEO_SELECTED);
			}
		});
		
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
		
		AclipsaSDK.getInstance(getActivity()).enableProgressNotifications(true);
		
		mMessageReceiver = new BroadcastReceiver() {
			  @Override
			public void onReceive(Context context, Intent intent) {
				if (progressdialog != null) {

					// Extract data included in the Intent
					boolean isResumed = intent.getBooleanExtra(
							AclipsaSDKConstants.ACLIPSA_VIDEO_UPLOAD_RESUMED, false);
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
	public void onPause() {
		// Unregister since the activity is not visible
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageReceiver);
		
		//Hide the keyboard if its showing
		InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(titleEditText.getWindowToken(), 0); 
		
		super.onPause();
	}



	@Override
	public void onResume() {
		super.onResume();
		
		// Register mMessageReceiver to receive messages.
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageReceiver,new IntentFilter(AclipsaSDKConstants.ACLIPSA_VIDEO_UPLOAD_BROADCAST));
	}



	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == VIDEO_SELECTED && resultCode == Activity.RESULT_OK){
			
			progressdialog.show();
			
			Uri selectedvideo = data.getData();
			
			Log.i("Arthur", "selectedvideo = "+ selectedvideo);
			
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
					uploadingGuid = AclipsaSDK.getInstance(getActivity()).createVideo(this, selectedvideo, titleEditText.getEditableText().toString(), "CREATEVIDEO", null, encodingCheckbox.isChecked());
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

				uploadingGuid = AclipsaSDK.getInstance(getActivity()).createVideo(this, selectedvideo, titleEditText.getEditableText().toString(), "CREATEVIDEO", null, encodingCheckbox.isChecked());
			}
		}	
	}


	@Override
	public void apiCreateVideoSuccess(Object tag, int statusCode,
			String errorString, String guid) {
		
		progressdialog.dismiss();
		
		if (tag == "CREATEVIDEO")
		{
			ToastHelper.show(getActivity(), "Create Video Success");			
		}
	}

	
	
	@Override
	public void apiVideoRequestResponseSuccess(Object tag, int statusCode,
			String errorString, ArrayList<AclipsaVideo> videos) {
		
		progressdialog.dismiss();
		
	}


	@Override
	public void apiVideoRequestResponseFailure(Object tag, int statusCode,
			String errorString) {

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
