package com.aclipsa.aclipsasdkdemo.activities;

import java.util.ArrayList;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDK.AclipsaSDKMessageHandler;
import com.aclipsa.aclipsasdk.AclipsaSDKConstants.MESSAGE_FILTER;
import com.aclipsa.aclipsasdk.AclipsaSDKMessage;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaMessage;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.adapters.MessageAdapter;

public class MessageFragment extends Fragment implements OnTabChangeListener,
		AclipsaSDKMessageHandler {

	private final String TAG = AllVideosFragment.class.getName();
	private ProgressDialog progressdialog;

	private TabHost mTabHost;
	private int mCurrentTab;

	private ListView inboxListView;
	private ListView sentListView;

	private MessageAdapter inboxAdapter;
	private MessageAdapter sentAdapter;
	
	private ArrayList<AclipsaMessage> inboxList;
	private ArrayList<AclipsaMessage> sentList;
	
	private boolean isActive;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		progressdialog = new ProgressDialog(getActivity());
		progressdialog.setMessage("Loading...");
		progressdialog.setIndeterminate(true);

		isActive = true;
		
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		isActive = true;

		View v = inflater.inflate(R.layout.message_view, null);

		mTabHost = (TabHost) v.findViewById(android.R.id.tabhost);
		setupTabs();

		mTabHost.setOnTabChangedListener(this);
		mTabHost.setCurrentTab(mCurrentTab);

		inboxListView = (ListView) v.findViewById(R.id.inboxListView);
		sentListView = (ListView) v.findViewById(R.id.sentListView);
		
		inboxList = new ArrayList<>();
		sentList = new ArrayList<>();
		
		inboxAdapter = new MessageAdapter(getActivity(),R.layout.message_item,inboxList);
		sentAdapter = new MessageAdapter(getActivity(), R.layout.message_item, sentList);

		inboxListView.setAdapter(inboxAdapter);
		sentListView.setAdapter(sentAdapter);
		
		inboxListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				try {

					AclipsaMessage message = inboxAdapter.getItem(position);
					Intent intent = MessageDetailActivity.createIntent(
							v.getContext(), message.getMsg_guid());
					startActivity(intent);

				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}
		});

		sentListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {
				try {

					AclipsaMessage message = sentAdapter.getItem(position);
					Intent intent = MessageDetailActivity.createIntent(
							v.getContext(), message.getMsg_guid());
					startActivity(intent);

				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}
		});
		
		progressdialog.show();
		AclipsaSDK.getInstance(getActivity()).getMessages(this,
		 "INBOXMESSAGES", MESSAGE_FILTER.FILTER_RECEIVED_MESSAGES);

		return v;
	}

	@Override
	public void onResume() {
		isActive = true;
		super.onResume();
	}

	@Override
	public void onPause() {
		isActive = false;
		super.onPause();
	}

	private void setupTabs() {
		mTabHost.setup(); // you must call this before adding your tabs!
		mTabHost.addTab(newTab("Inbox", R.id.tab1));
		mTabHost.addTab(newTab("Sent", R.id.tab2));
	}

	private TabSpec newTab(String tag, int tabContentId) {

		TabSpec tabSpec = mTabHost.newTabSpec(tag);
		tabSpec.setIndicator(tag);
		tabSpec.setContent(tabContentId);
		return tabSpec;
	}

	@Override
	public void onTabChanged(String tabId) {

		progressdialog.show();

		if (tabId.equals("Inbox")) {
			mCurrentTab = 0;
			 AclipsaSDK.getInstance(getActivity()).getMessages(this,
			 "INBOXMESSAGES", MESSAGE_FILTER.FILTER_RECEIVED_MESSAGES);

		} else if (tabId.equals("Sent")) {
			mCurrentTab = 1;
			AclipsaSDK.getInstance(getActivity()).getMessages(this,
					"SENTMESSAGES", MESSAGE_FILTER.FILTER_SENT_MESSAGES);
		}

	}

	@Override
	public void apiCreateMessageSuccess(Object tag, int statusCode,
			String errorString, String guid) {

		progressdialog.dismiss();

	}

	@Override
	public void apiMessageRequestResponseSuccess(Object tag, int statusCode,
			String errorString, Object response) {
		
		ArrayList<AclipsaMessage> messages = (ArrayList<AclipsaMessage>)response;
		
		if (isActive == true) { // since the callback may happen even if the
								// fragment or activity is not foreground
			progressdialog.dismiss();

			if (tag == "INBOXMESSAGES") {
				inboxList.clear();
				inboxList.addAll(messages);
				inboxAdapter.notifyDataSetChanged();
				
			} else if (tag == "SENTMESSAGES") {

				sentList.clear();
				sentList.addAll(messages);
				sentAdapter.notifyDataSetChanged();
			}

		}
	}

	@Override
	public void apiMessageRequestResponseFailure(Object tag, int statusCode,
			String errorString) {

		if (isActive == true) {
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
}
