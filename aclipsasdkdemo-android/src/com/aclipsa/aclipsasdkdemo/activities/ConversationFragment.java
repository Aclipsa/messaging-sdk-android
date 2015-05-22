package com.aclipsa.aclipsasdkdemo.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDKConstants;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaMessage;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaRecipient;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaThread;
import com.aclipsa.aclipsasdkdemo.adapters.ThreadAdapter;
import com.aclipsa.aclipsasdkdemo.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.aclipsa.aclipsasdk.AclipsaSDK.AclipsaSDKMessageHandler;

/**
 * Created by evetorres on 12/4/13.
 */
public class ConversationFragment extends Fragment implements AclipsaSDKMessageHandler {

    private static final String threadsTag = "threadstag";

    Button recordButton;
    ListView conversationListView;
    ThreadAdapter threadAdapter;
    ProgressDialog progressDialog;

    Boolean isBackground;

    ArrayList<AclipsaThread> threads;
    ArrayList<AclipsaRecipient> senders;

    private Activity attachedActivity;

    private BroadcastReceiver mMessageRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            boolean shouldPullMessage = intent.getBooleanExtra(AclipsaSDKConstants.ACLIPSA_NEWMESSAGE, false);
            Log.i("demo app", "ConversationList shouldPullMessage = "+ shouldPullMessage);
            if(shouldPullMessage == true){
                getThreads();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.conversations_activity, null);

        isBackground = false;

        recordButton = (Button) v.findViewById(R.id.recordButton2);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRecordActivity();
            }
        });

        threads = new ArrayList<AclipsaThread>();
        senders = new ArrayList<AclipsaRecipient>();

        threadAdapter = new ThreadAdapter(getActivity(), R.layout.thread_item, threads);

        conversationListView = (ListView) v.findViewById(R.id.conversationListView);
        conversationListView.setAdapter(threadAdapter);
        conversationListView.setOnItemClickListener(threadClickListener);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(attachedActivity != null)
            progressDialog = ProgressDialog.show(attachedActivity, "", "Retrieving Messages...", true, false);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mMessageRefreshReceiver, new IntentFilter(AclipsaSDKConstants.ACLIPSA_MESSAGE_PULL_BROADCAST));

        isBackground = false;

        getThreads();
    }

    @Override
    public void onPause() {
        isBackground = true;
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mMessageRefreshReceiver);
        super.onPause();
    }

    @Override
    public void onAttach(Activity activity) {

        attachedActivity =  activity;
        super.onAttach(activity);
    }

    public void showRecordActivity() {
        Intent intent = new Intent(getActivity(), RecordVideoActivity.class);
        startActivity(intent);
    }

    private void getThreads(){
        Log.d("demo app", "getThreads");
        if(!isBackground)
        {
            AclipsaSDK.getInstance(getActivity()).getMessageThreads(this, threadsTag);
        }
    }

    private void getSenders(){
        if(!isBackground){
            AclipsaSDK.getInstance(getActivity()).getMessages(this, "getSendersFromMessages_tag_internal", AclipsaSDKConstants.MESSAGE_FILTER.FILTER_ALL_MESSAGES);
        }
    }

    @Override
    public void apiCreateMessageSuccess(Object tag, int statusCode, String errorString, String messageGuid)
    {
        dataManagerResponseSuccess(tag, statusCode, errorString, messageGuid, null);
        Log.d("demo app", "Success occurred in the SDK: " + errorString);
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

    public void dataManagerResponseSuccess(Object tag, int statusCode, String errorErrorResponse, String messageGuid, Object data)
    {

        progressDialog.dismiss();

        if(tag != null){
            if(tag.equals(threadsTag)){
                if(data != null){
                    if (((List)data).size() > 0){
                        if(((List)data).get(0).getClass() == AclipsaThread.class ){
                            threads.clear(); //remove the old data
                            threads.addAll((ArrayList<AclipsaThread>)data);

                            Collections.sort(threads,
                                    Collections.reverseOrder(new AclipsaThread.AclipsaThreadComparator()));

                            threadAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        }
    }

    public void dataManagerResponseFail(Object tag, int statusCode, String errorErrorResponse) {
        progressDialog.dismiss();
    }

    private AdapterView.OnItemClickListener threadClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l)
        {
            final String threadID = ((AclipsaThread)adapterView.getAdapter().getItem(position)).getThread_id();

            Intent intent = new Intent(getActivity(), ConversationDetailsActivity.class);
            intent.putExtra("threadId", threadID);
            startActivity(intent);

            onPause();
        }
    };
}