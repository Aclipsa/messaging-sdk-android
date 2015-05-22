package com.aclipsa.aclipsasdkdemo.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDKConstants;
import com.aclipsa.aclipsasdk.AclipsaSDKException;
import com.aclipsa.aclipsasdk.AclipsaSDKVideoView;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaMessage;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaRecipient;
import com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.adapters.RecipientsAdapter;
import com.aclipsa.aclipsasdkdemo.constants.ZipAClipSettingsHelper;
import com.mobileaze.common.helpers.DialogHelper;

import java.util.ArrayList;

/**
 * Created by evetorres on 12/16/13.
 */
public class ConversationMessageActivity extends Activity implements AclipsaSDK.AclipsaSDKHandler, AclipsaSDK.AclipsaSDKMessageHandler {

    private AclipsaMessage message;

    private TextView captionTextView;
    private TextView titleTextView;
    private TextView timeStampTextView;

    private AclipsaSDKVideoView videoView;
    private ImageView thumbnailImageView;
    private ImageButton playImageButton;
    private ViewGroup videoContainerLayout;
    private Button replyButton;
    private Button forwardButton;
    private Button deleteButton;
    private Button yankButton;
    private RecipientsAdapter recipientsAdapter;
    private ArrayList<AclipsaRecipient> recipients;
    private ListView recipientsListView;

    boolean isFromSendersView;

    private final static String ZIPBACK_TAG = "zipback";
    private final static String UNZIPBACK_TAG = "unzipback";
    private final static String MESSAGE_TAG = "message";
    private final static String DELETE_MESSAGE_TAG = "delete_message";
    private final static String MARK_MESSAGE_READ_TAG = "mark_message_read";

    private ProgressDialog progressDialog;
    private boolean isYanked = false;

    private BroadcastReceiver updateRecipientReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean shouldRefresh = intent.getBooleanExtra(DemoAppConstants.ZIPACLIP_PERSON_UPDATED, false);

            if(shouldRefresh){
                refreshRecipientAndSenderView();
            }
        }
    };

    private BroadcastReceiver mMessageRefreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            boolean shouldPullMessage = intent.getBooleanExtra(AclipsaSDKConstants.ACLIPSA_NEWMESSAGE, false);
            if(shouldPullMessage == true){
                AclipsaMessage mess = AclipsaSDK.getInstance(context).getMessageWithGuid(message.getMsg_guid());
                recipients.clear();
                recipients.addAll(mess.getRecipients());


                recipientsAdapter.notifyDataSetChanged();
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_detail_activity);

        Log.d("demo app", "onCreate");
        recipientsListView = (ListView) findViewById(R.id.recipientsList);
        captionTextView = (TextView) findViewById(R.id.captionTextView);
        titleTextView = (TextView) findViewById(R.id.titleTextView);
        timeStampTextView = (TextView) findViewById(R.id.timeStampTextView);

        if(recipients == null) {
            recipients = new ArrayList<>();
        }

        videoContainerLayout = (ViewGroup) findViewById(R.id.videoContainerLayout);
        videoView = (AclipsaSDKVideoView) findViewById(R.id.videoView);

        thumbnailImageView = (ImageView) findViewById(R.id.thumbnailView);

        String messageGUID = getIntent().getStringExtra("messageGUID");
        AclipsaMessage mess = AclipsaSDK.getInstance(this).getMessageWithGuid(messageGUID);

        setMessage(mess, getIntent().getBooleanExtra("isFromSenders", false));

        if (message.getVideo() == null)
        {
            thumbnailImageView.setVisibility(View.GONE);
        } else {
            AclipsaSDK.getInstance(this).putImageUrlInImageView(thumbnailImageView, message.getVideo().getThumbnail_medium(), getMyIdentifier());
        }

        recipientsAdapter = new RecipientsAdapter(this, R.layout.recipients_list_item, recipients);
        recipientsListView.setAdapter(recipientsAdapter);

        playImageButton = (ImageButton) findViewById(R.id.playImageButton);
        playImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("demo app", "message.getVideo() = " + message.getVideo());
                if (message.getVideo() != null){

                    MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            progressDialog.hide();
                        }
                    };

                    try {
                        videoView.setZOrderOnTop(false);
                        AclipsaSDK.getInstance(view.getContext()).playVideoFromMessage(view.getContext(), ConversationMessageActivity.this, message, videoView, preparedListener, null, null);
                    }
                    catch (AclipsaSDKException e) {
                        e.printStackTrace();
                    }
                    progressDialog.show();
                }
            }
        });

        replyButton = (Button) findViewById(R.id.replyButton);
        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                showRecordVideoFragment(message.getRecipients(), message.getMsg_guid());

                ArrayList<AclipsaRecipient> toForward = new ArrayList<AclipsaRecipient>();
                toForward.add(message.getFrom());
                showRecordVideoFragment(toForward, message.getMsg_guid());
            }
        });

        forwardButton = (Button) findViewById(R.id.forwardButton);
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showComposeFragment(DemoAppConstants.DISPLAY_RECORD_VIDEO_READY_FRAGMENT, DemoAppConstants.FORWARDED_VIDEO, null, message.getMsg_guid());
            }
        });

        deleteButton = (Button) findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogHelper.displayDialog(ConversationMessageActivity.this, "Delete", "Deleting this message will make it unavailable to you and the recipients forever. Are you sure you want to delete it?", "Delete", onDeleteConfirmed);
            }
        });

        yankButton = (Button) findViewById(R.id.yankButton);
        yankButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isYanked == false){
                    displayYankConfirmation();
                }
                else{
                    showProgress("Processing...");
                    AclipsaSDK.getInstance(ConversationMessageActivity.this).unyankMessage(ConversationMessageActivity.this, UNZIPBACK_TAG, message);
                }
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Loading video...");
    }

    DialogInterface.OnClickListener onDeleteConfirmed = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog,int id) {
            showProgress("Deleting message...");
            AclipsaSDK.getInstance(ConversationMessageActivity.this).deleteMessage(ConversationMessageActivity.this, DELETE_MESSAGE_TAG, message);
        }
    };

    private void displayYankConfirmation()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(null);
        builder.setMessage("Do you really want to take this message away from all recipients?");
        builder.setPositiveButton("Yank", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showProgress("Processing...");
                AclipsaSDK.getInstance(ConversationMessageActivity.this).yankMessage(ConversationMessageActivity.this, ZIPBACK_TAG, message);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        //show the dialog
        builder.create().show();
    }

    public void showProgress(String progressMessage) {
        progressDialog = ProgressDialog.show(this, "", progressMessage, true, false);
    }

    public void hideProgress() {
        if (progressDialog != null)
        {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d("demo app", "onResume:caption:(" + message.getCaption() + ") title:(" + message.getTitle() + ")");
        captionTextView.setText(message.getCaption());
        titleTextView.setText(message.getTitle());
        timeStampTextView.setText(message.getCreated_at().toString());

        recipientsAdapter.notifyDataSetChanged();

        String myTsui = ZipAClipSettingsHelper.getString(this, DemoAppConstants.USER_TSUI, null);

        if (myTsui != null)
        {
            if (myTsui.compareToIgnoreCase(message.getFrom().getTsui()) != 0)
            {
                // Means current user is not the owner of this message
                forwardButton.setVisibility(View.GONE);
            }
        }

        if (message.getVideo() == null){
            videoContainerLayout.setVisibility(View.GONE);
        }


        for (int i = 0; i < message.getRecipients().size(); i++) {
            if (message.getRecipients().get(i).isYanked()) {
                isYanked = true;
            }
        }

        if (isYanked)
            yankButton.setText("Unyank");
        else
            yankButton.setText("Yank");

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageRefreshReceiver, new IntentFilter(AclipsaSDKConstants.ACLIPSA_MESSAGE_PULL_BROADCAST));
        LocalBroadcastManager.getInstance(this).registerReceiver(updateRecipientReceiver, new IntentFilter(com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.ZIPACLIP_PERSON_UPDATE_BROADCAST));

        //Send a message read if we are not the sender of the message
        if(!message.getFrom().getTsui().equals(AclipsaSDK.getInstance(this).getCurrentUserIdentifier())){
            for(AclipsaRecipient recip: message.getRecipients()){
                if(recip.getTsui().equals(AclipsaSDK.getInstance(this).getCurrentUserIdentifier())){
                    if(recip.isUnread() == true)
                        AclipsaSDK.getInstance(this).toggleMessageReadFlag(this, message.getMsg_guid(), MARK_MESSAGE_READ_TAG, true);
                }
            }
        }

        refreshRecipientAndSenderView();
    }

    @Override
    public void onPause() {

        // Unregister since the fragment is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageRefreshReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateRecipientReceiver);

        super.onPause();
    }

    public void setMessage(AclipsaMessage mess, boolean isFromSenders){
        Log.d("demo app", "setMessage");
        this.message = new AclipsaMessage();
        this.message = mess;
        this.isFromSendersView = isFromSenders;
        if(message == null) {
            this.recipients = new ArrayList<>();
        } else {
            this.recipients = message.getRecipients();
        }
    }

    public void showComposeFragment(int whichFragment, String videoPath, ArrayList<AclipsaRecipient> recipients, String messageGuid) {
        Intent intent = new Intent(this, MessageSendActivity.class);
        intent.putExtra(com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.FORWARDED_VIDEO, true);
        intent.putExtra("messageGuid", messageGuid);
        this.startActivity(intent);
        this.finish();
    }

    public void showRecordVideoFragment(ArrayList<AclipsaRecipient> recipients, String messageGuid){
        Intent intent = new Intent(this, RecordVideoActivity.class);
        intent.putExtra("isReply", true);
        intent.putExtra("messageGuid", messageGuid);
        intent.putParcelableArrayListExtra(com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.EXTRA_RECIPIENTS, recipients);
        this.startActivity(intent);
        this.finish();
    }

    private void refreshRecipientAndSenderView(){
        recipientsAdapter.notifyDataSetChanged();

    }

    public void dataManagerResponseSuccess(Object tag, int statusCode, String errorErrorResponse, String messageGuid, Object data)
    {
        hideProgress();

        if(tag != null){
            Log.d("demo app", "tag:" + tag);
            if (tag.equals(ZIPBACK_TAG)){
                hideProgress();
                isYanked = true;
                yankButton.setText("Unyank");
            }
            else if(tag.equals(UNZIPBACK_TAG)){
                hideProgress();
                isYanked = false;
                yankButton.setText("Yank");
                showMessage(this,"Yank undone.","This message can now be viewed by the original recipients again.");
            }
            else if(tag.equals(DELETE_MESSAGE_TAG)){
                finish();
            }
        }
    }

    public void dataManagerResponseFail(Object tag, int statusCode, String errorErrorResponse)
    {
        hideProgress();
    }

    @Override
    public void apiRequestResponseSuccess(Object tag, int statusCode, String error) {
        Log.d("demo app", "DataManager zipApiRequestResponseSuccess");

        //we have successfully registered
        dataManagerResponseSuccess(tag, statusCode, error, null, null);
    }

    @Override
    public void apiRequestResponseFailure(Object tag, int statusCode, String error) {
        Log.d("demo app", "DataManager zipApiRequestResponseFailure");
        dataManagerResponseFail(tag, statusCode, error);
    }

    @Override
    public void apiCreateMessageSuccess(Object tag, int statusCode, String errorString, String messageGuid) {
        dataManagerResponseSuccess(tag, statusCode, errorString, messageGuid, null);
        Log.d("demo app", "Success occurred in the SDK: " + errorString);
    }

    @Override
    public void apiMessageRequestResponseSuccess(Object tag, int statusCode, String errorErrorResponse, Object data) {

        Log.d("demo app", "apiMessageRequestResponseSuccess " + errorErrorResponse);

        dataManagerResponseSuccess(tag, statusCode, errorErrorResponse, null, data);
    }

    public String getMyIdentifier(){
        return AclipsaSDK.getInstance(this).getCurrentUserIdentifier();
    }

    @Override
    public void apiMessageRequestResponseFailure(Object tag, int statusCode, String errorString) {
        Log.d("demo app", "Failure occurred in the SDK: " + errorString);
        dataManagerResponseFail(tag, statusCode, errorString);
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
}