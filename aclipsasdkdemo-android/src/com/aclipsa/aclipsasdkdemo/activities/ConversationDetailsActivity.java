package com.aclipsa.aclipsasdkdemo.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDKConstants;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaMessage;
import com.aclipsa.aclipsasdk.AclipsaSDK.AclipsaSDKMessageHandler;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.adapters.MessageAdapter2;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by evetorres on 12/13/13.
 */
public class ConversationDetailsActivity extends Activity implements AclipsaSDKMessageHandler {

    private String THREAD_TAG = com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.THREAD_TAG + UUID.randomUUID();

    private ListView conversationListView;

    private ArrayList<AclipsaMessage> messageList;
    private MessageAdapter2 messageAdapter;
    private boolean isForeground;
    private String threadID;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            float progress = intent.getFloatExtra(
                    AclipsaSDKConstants.ACLIPSA_VIDEO_UPLOAD_PROGRESS, 0);

            String uploadID = intent.getStringExtra(AclipsaSDKConstants.ACLIPSA_VIDEO_UPLOAD_ID);

//            Log.i(TAG, "ConversationDetailFragment " + uploadID + " progress = " + progress);

            //TODO: show upload status??  Or handle it in adapter?
        }
    };

    private BroadcastReceiver mMessageRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            boolean shouldPullMessage = intent.getBooleanExtra(AclipsaSDKConstants.ACLIPSA_NEWMESSAGE, false);
            Log.d("demo app", "ConversationDetail shouldPullMessage = "+ shouldPullMessage);
            if(shouldPullMessage == true){
                AclipsaSDK.getInstance(ConversationDetailsActivity.this).getMessageThreadFromID(ConversationDetailsActivity.this, THREAD_TAG, threadID);
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.conversation_list_activity);

        threadID = getIntent().getStringExtra("threadId");

        messageList = new ArrayList<AclipsaMessage>();
        messageAdapter = new MessageAdapter2(this, R.layout.message_cell, messageList);

        conversationListView = (ListView) findViewById(R.id.messagelistView);
        conversationListView.setAdapter(messageAdapter);
        conversationListView.setOnItemClickListener(clickListener);
    }

    private AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

            AclipsaMessage mess = messageList.get(position);
            Log.d("demo app", "onResume1:caption:(" + mess.getCaption() + ") title:(" + mess.getTitle() + ")");

            Intent intent = new Intent(ConversationDetailsActivity.this, ConversationMessageActivity.class);
            intent.putExtra("messageGUID", mess.getMsg_guid());
            intent.putExtra("isFromSenders", false);
            startActivity(intent);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        Log.i("demo app", "ConversationDetailFragment onResume()");

        isForeground = true;

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(AclipsaSDKConstants.ACLIPSA_VIDEO_UPLOAD_BROADCAST));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageRefreshReceiver, new IntentFilter(AclipsaSDKConstants.ACLIPSA_MESSAGE_PULL_BROADCAST));

        AclipsaSDK.getInstance(this).getMessageThreadFromID(this, THREAD_TAG, threadID);
    }

    @Override
    public void onPause() {

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageRefreshReceiver);

        isForeground = false;

        super.onPause();
    }

    @Override
    public void apiCreateMessageSuccess(Object tag, int statusCode, String errorString, String messageGuid) {
        Log.d("demo app", "Success occurred in the SDK: " + errorString);
        dataManagerResponseSuccess(tag, statusCode, errorString, messageGuid, null);
    }

    @Override
    public void apiMessageRequestResponseSuccess(Object tag, int statusCode, String errorErrorResponse, Object data) {

        Log.d("demo app", "apiMessageRequestResponseSuccess " + errorErrorResponse);
        dataManagerResponseSuccess(tag, statusCode, errorErrorResponse, null, data);
    }

    @Override
    public void apiMessageRequestResponseFailure(Object tag, int statusCode, String errorString) {
        Log.d("demo app", "Failure occurred in the SDK: " + errorString);
        dataManagerResponseFail(tag, statusCode, errorString);
    }

    public void dataManagerResponseSuccess(Object tag, int statusCode, String errorErrorResponse, String messageGuid, Object data) {

        Log.i("demo app", "ConversationDetailFragment dataManagerResponseSuccess");
        if (tag != null) {

            if (tag.equals(THREAD_TAG))
                populateListView(data);
        }
    }

    public void dataManagerResponseFail(Object tag, int statusCode, String errorErrorResponse) {
        //hideProgress();
        showMessage(this, "Error", errorErrorResponse);
    }

    protected void showMessage(Context context, String title, String message) {

        if(context != null){
            //Don't show message if screen is locked
            KeyguardManager kgMgr =
                    (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            boolean showing = kgMgr.inKeyguardRestrictedInputMode();

            if(showing == false){
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(title);
                builder.setMessage(message);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                //show the dialog
                builder.create().show();
            }
        }
    }

    private void populateListView(Object data)
    {
        if (data != null) {
            if (data.getClass().equals(ArrayList.class)) {
                if (((List) data).size() > 0)
                {
                    ArrayList<AclipsaMessage> aclipsaSDKMessages = null;

                    if (((List) data).get(0).getClass() == AclipsaMessage.class)
                    {
                        aclipsaSDKMessages = (ArrayList<AclipsaMessage>) data;

                        messageList.clear();

                        //We filter out yanked message
                        for (AclipsaMessage mess : aclipsaSDKMessages) {
                            messageList.add(mess);
                        }

                        messageAdapter.notifyDataSetChanged();
                    }
                }
                else
                {
                    if(isForeground == true)
                        finish();

                    Log.d("demo app", "Exiting ConversationDetailFragment since message is empty");
                }
            }
        }
    }
}