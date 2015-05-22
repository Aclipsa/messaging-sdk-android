package com.aclipsa.aclipsasdkdemo.activities;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDK.AclipsaSDKHandler;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.helpers.ToastHelper;

public class LoginFragment extends Fragment implements AclipsaSDKHandler {
	
	private Integer LOGIN_TAG = 1;
	private Integer LOGOUT_TAG = 2;
	
	private final String TAG = AllVideosFragment.class.getName();
	private ProgressDialog progressdialog;
	private EditText userIdentifierEditText;
	private Button loginButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.login_activity, null);
	
		userIdentifierEditText = (EditText) v.findViewById(R.id.userIdentifier);
		loginButton = (Button) v.findViewById(R.id.loginButton);
		loginButton.setTag(LOGIN_TAG);
		
		loginButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				//Hide the keyboard if its showing
				InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(userIdentifierEditText.getWindowToken(), 0);  
				
				Integer mode = (Integer)loginButton.getTag();
				if (mode == LOGIN_TAG)
				{
					progressdialog = ProgressDialog.show(getActivity(), null, "Logging in...", true);
					AclipsaSDK.getInstance(getActivity()).loginUser(userIdentifierEditText.getEditableText().toString(), LoginFragment.this, LOGIN_TAG);
				} else if (mode == LOGOUT_TAG)
				{
					progressdialog = ProgressDialog.show(getActivity(), null, "Logging out...", true);
					AclipsaSDK.getInstance(getActivity()).logoutUser(LoginFragment.this, LOGOUT_TAG);		
				}
			}
		});
		
		String identifier = AclipsaSDK.getInstance(getActivity()).getCurrentUserIdentifier();
		if (identifier != null)
		{
			loginButton.setText("Logout");
			loginButton.setTag(LOGOUT_TAG);
			userIdentifierEditText.setText(identifier);
			userIdentifierEditText.setEnabled(false);
		}
		return v;
		
	}

	@Override
	public void apiRequestResponseSuccess(Object tag, int statusCode,
			String errorString) {
		progressdialog.dismiss();

		// TODO Auto-generated method stub
		if (tag == LOGIN_TAG)
		{
			ToastHelper.show(getActivity(), "Login Successful");
			loginButton.setText("Logout");
			loginButton.setTag(LOGOUT_TAG);
			userIdentifierEditText.setEnabled(false);
		} else if (tag == LOGOUT_TAG)
		{
			ToastHelper.show(getActivity(), "Logout Successful");
			loginButton.setText("Login");
			loginButton.setTag(LOGIN_TAG);
			loginButton.setEnabled(true);
			userIdentifierEditText.setText("");
			userIdentifierEditText.setEnabled(true);
		}
	}
	

	@Override
	public void onPause() {
		
		//Hide the keyboard if its showing
		InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(userIdentifierEditText.getWindowToken(), 0);  

		
		super.onPause();
	}

	@Override
	public void apiRequestResponseFailure(Object tag, int statusCode,
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
