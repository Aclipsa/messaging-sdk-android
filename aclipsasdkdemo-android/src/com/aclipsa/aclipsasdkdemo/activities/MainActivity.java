package com.aclipsa.aclipsasdkdemo.activities;

import java.util.UUID;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDK.AclipsaSDKHandler;
import com.aclipsa.aclipsasdk.AclipsaSDKConstants;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants;
import com.aclipsa.aclipsasdkdemo.helpers.ToastHelper;

public class MainActivity extends Activity implements AclipsaSDKHandler{

	private final String TAG = MainActivity.class.getName();
	private ProgressDialog progressdialog;
	
	// handler for received Intents for the AclipsaSDK upload event 
	private BroadcastReceiver mMessageReceiver;
	private Button resumeButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_menu_activity);
		
		
		//Depending in the server, this may take some time to finish	
		
		SharedPreferences pref = getSharedPreferences("com.aclipsa.aclipsasdkdemo", Context.MODE_PRIVATE);
		String deviceUuid = pref.getString("UUID", null);
		if (deviceUuid == null)
		{
			deviceUuid = UUID.randomUUID().toString();
			SharedPreferences.Editor prefEditor = pref.edit();
			prefEditor.putString("UUID", deviceUuid).commit();
		}

        AclipsaSDK.getInstance(this).register(DemoAppConstants.CLIENT_KEY, deviceUuid, DemoAppConstants.DEMO_PASSPHRASE, DemoAppConstants.CONTENTPROVIDER_AUTHORITY,
                DemoAppConstants.SCHEME, DemoAppConstants.SAAS_URL, this, "REGISTER");


        AclipsaSDK.getInstance(getApplicationContext()).enableProgressNotifications(true);

		//So we show a dialog to to let the user know
		progressdialog = ProgressDialog.show(this, null, "Initialize SDK...", true);
		
	    mMessageReceiver = new BroadcastReceiver() {
			  @Override
			  public void onReceive(Context context, Intent intent) {
				  if(intent != null){
					  // Extract data included in the Intent
					  boolean isPaused = intent.getBooleanExtra(AclipsaSDKConstants.ACLIPSA_VIDEO_UPLOAD_PAUSED, false);
					  if(isPaused == true){
						  resumeButton.setVisibility(View.VISIBLE);
					  }
				  }
			  }
		};
		
	}
	
	private void createActionbars(){
		final ActionBar actionBar = getActionBar();
		// Specify that tabs should be displayed in the action bar.
	    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	    
	 // Create a tab listener that is called when the user changes tabs.
	    ActionBar.TabListener tabListener = new ActionBar.TabListener() {
	        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
	        	switch(tab.getPosition()){
	        		case 0:
	        			Fragment allVideosFrag = new AllVideosFragment();
	        			replaceFragment(ft, allVideosFrag);
	        			break;
	        		case 1:
	        			Fragment createVideoFrag = new CreateVideoFragment();
	        			replaceFragment(ft, createVideoFrag);
	        			break;
	        		case 2:
	        			Fragment messageFragment = new MessageFragment();
	        			replaceFragment(ft, messageFragment);
	        			break;
	        		case 3:
	        			Fragment createMessage = new CreateMessageFragment();
	        			replaceFragment(ft, createMessage);
	        			break;	
	        		case 4:
	        			Fragment loginFrag = new LoginFragment();
	        			replaceFragment(ft, loginFrag);
	        			break;
                    case 5:
                        Fragment conversationFrag = new ConversationFragment();
                        replaceFragment(ft, conversationFrag);
                        break;
	        	}
	        	
	        }

	        public void onTabUnselected(ActionBar.Tab tab,
	                FragmentTransaction ft) { }

	        public void onTabReselected(ActionBar.Tab tab,
	                FragmentTransaction ft) { }
	    };
	    
	    actionBar.addTab( actionBar.newTab()
        		.setText("All Videos")
        		.setTabListener(tabListener));
	    
	    actionBar.addTab( actionBar.newTab()
        		.setText("Create Video")
        		.setTabListener(tabListener));
	    
	    actionBar.addTab( actionBar.newTab()
        		.setText("Messages")
        		.setTabListener(tabListener));
	    
	    actionBar.addTab( actionBar.newTab()
        		.setText("Create Message")
        		.setTabListener(tabListener));
	    
	    actionBar.addTab( actionBar.newTab()
        		.setText("Account")
        		.setTabListener(tabListener));

        actionBar.addTab( actionBar.newTab()
                .setText("Conversations")
                .setTabListener(tabListener));
	    
	    actionBar.setDisplayShowCustomEnabled(true);
	    
	    resumeButton = new Button(this);
	    resumeButton.setText("Resume upload");
	    resumeButton.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
	    resumeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				resumeButton.setVisibility(View.GONE);
				
				//resume the upload
				AclipsaSDK.getInstance(arg0.getContext()).resumeCreateVideo(null);
			}
		});
	    
	    actionBar.setCustomView(resumeButton);
	    
	    boolean isUploadPaused = AclipsaSDK.getInstance(this).isVideoUploadPaused(null);
	    if(isUploadPaused == true)
	    	resumeButton.setVisibility(View.VISIBLE);
	    else
	    	resumeButton.setVisibility(View.GONE);
	    
	}
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Register mMessageReceiver to receive messages.
		LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,new IntentFilter(AclipsaSDKConstants.ACLIPSA_VIDEO_UPLOAD_BROADCAST));
		
	}
	
	@Override
	public void onPause() {
		
		// Unregister since the activity is not visible
		  LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		
		super.onPause();
	}
	
	private void replaceFragment(FragmentTransaction ft, Fragment frag){
		ft.replace(R.id.fragmentContainer, frag);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
//		ft.commit();
	}
	
	@Override
	public void apiRequestResponseSuccess(Object tag, int statusCode, String errorString){
		if(progressdialog != null)
			progressdialog.dismiss();
		
		Log.i(TAG, "apiRequestResponse");
		if (tag == "REGISTER")
		{
			ToastHelper.show(this, "Register instance successful");
			
			//continue creating the action bars
			createActionbars();
		}
	}

	@Override
	public void apiRequestResponseFailure(Object tag, int statusCode,
			String errorString) {	
		if(progressdialog != null)
			progressdialog.dismiss();
		
		ToastHelper.show(this, "Register not successful");
		
		//Its an error but we need to show something...
		createActionbars();
	}

}
