package com.aclipsa.aclipsasdkdemo.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDKException;
import com.aclipsa.aclipsasdk.AclipsaSDKVideoView;
import com.aclipsa.aclipsasdk.SdkDataManager;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaMessage;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaRecipient;
import com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.helpers.ZipAClipUtils;
import com.mobileaze.common.helpers.ToastHelper;
import com.aclipsa.aclipsasdk.AclipsaSDK.AclipsaSDKMessageHandler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by evetorres on 12/7/13.
 */
public class MessageSendActivity extends Activity implements AclipsaSDKMessageHandler, AclipsaSDK.AclipsaSDKHandler
{
    ImageView videocapturedImageView;
    ImageButton playVideoButton;
    EditText recipientEditText;
    EditText messageTitleEditText;
    EditText messageBodyEditText;
    Button trimVideoButton;
    Button sendMessageButton;
    Button saveMessageButton;
    Button cancelMessageButton;
    ProgressDialog progressdialog;
    private AclipsaSDKVideoView videoView;

    File videoFile;
    String videoFilePath, message_guid;
    Uri videoUri;
    Context context;
    boolean isForwarded;
    AclipsaMessage forwardedMessage;
    private ArrayList<AclipsaRecipient> aclipsaRecipients;

    final private ArrayList<String> recipientList = new ArrayList<String>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_send_activity);

        context = this;

        isForwarded = getIntent().getExtras().getBoolean(DemoAppConstants.FORWARDED_VIDEO, false);
        message_guid = getIntent().getExtras().getString("messageGuid");
        aclipsaRecipients = getIntent().getExtras().getParcelableArrayList(DemoAppConstants.EXTRA_RECIPIENTS);

        videoFilePath = getIntent().getStringExtra(DemoAppConstants.EXTRA_VIDEO_PATH);
        if(videoFilePath != null)
        {
            videoUri = Uri.parse(videoFilePath);
            videoFile = new File(videoUri.getPath());
        }

        videoView = (AclipsaSDKVideoView) findViewById(R.id.videoView);

        playVideoButton = (ImageButton) findViewById(R.id.playVideoButton);
        playVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //play the video from disk
                if (videoFile.exists()) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse("file://" + videoFile.getPath()), "video/*");
                    startActivity(intent);
                }
            }
        });

        videocapturedImageView = (ImageView) findViewById(R.id.videoCaptureImageView);
        if(videoFilePath != null)
        {
            Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoFile.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
            videocapturedImageView.setImageBitmap(thumbnail);
        }

        recipientEditText = (EditText) findViewById(R.id.recipientEditText);
        String recipients = "";
        AclipsaRecipient rec;
        if (aclipsaRecipients != null)
        {
            for (int x=0; x < aclipsaRecipients.size(); x++)
            {
                rec = aclipsaRecipients.get(x);
                recipients = recipients + rec.getTsui();

                if (x < aclipsaRecipients.size() - 1)
                {
                    recipients = recipients + ",";
                }
            }

            recipientEditText.setText(recipients);
        }

        messageTitleEditText = (EditText) findViewById(R.id.messageTitleEditText);
        messageBodyEditText = (EditText) findViewById(R.id.messageBodyEditText);

        trimVideoButton = (Button) findViewById(R.id.trimButton);
        trimVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TrimVideoActivity trimVideoActivity = new TrimVideoActivity();
                //trimVideoActivity.setVideoFile(videoFile);

                Intent intent = new Intent(context, trimVideoActivity.getClass());
                intent.putExtra(DemoAppConstants.EXTRA_VIDEO_PATH, videoFilePath);
                startActivity(intent);
            }
        });

        sendMessageButton = (Button) findViewById(R.id.sendMessageButton);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //        showProgress("Sending message...");
                String[] toList = recipientEditText.getText().toString().split(",");
                ArrayList<String> recipients = new ArrayList<String>();
                for (int x = 0; x < toList.length; x++)
                {
                    recipients.add(toList[x]);
                }

                progressdialog = ProgressDialog.show(MessageSendActivity.this, "", "Sending Message...", true, false);

                String mGuid = SdkDataManager.SOL_createSecureUUID();
                String uploadingGuid;

                if (isForwarded)
                {
                    Log.d("demo app", "isforwarded:" + mGuid);
                    String newMessageUUID = SdkDataManager.getInstance(MessageSendActivity.this).SOL_createSecureUUID();
                    uploadingGuid = AclipsaSDK.getInstance(context).forwardMessage(MessageSendActivity.this, MessageSendActivity.this, DemoAppConstants.SENDER_TAG, messageTitleEditText.getEditableText().toString() ,
                            messageBodyEditText.getEditableText().toString(), newMessageUUID ,forwardedMessage, recipients);
                }
                else
                {
                    //TODO: Harcoding orientation to portrait for now
                    String orientation = "protrait";
                    Log.d("demo app", "1isforwarded:" + mGuid);
                    uploadingGuid = AclipsaSDK.getInstance(MessageSendActivity.this).sendMessageForThread(MessageSendActivity.this, MessageSendActivity.this, "VIDEO_MESSAGE",
                            messageTitleEditText.getEditableText().toString(), messageBodyEditText.getEditableText().toString(),
                            videoUri, recipients, false, mGuid, orientation,true);
                }
            }
        });

        saveMessageButton = (Button) findViewById(R.id.saveMessageButton);
        saveMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

                // set title
                alertDialogBuilder.setTitle("Save to Library");

                // set dialog message
                alertDialogBuilder
                        .setMessage("Are you sure you want to save this video to your library? Items on your library are not secure and can be viewed by anyone with access to your device.")
                        .setCancelable(true)
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.cancel();
                                    }
                                })
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialogInterface,
                                                        int id) {

                                        File albumPath = getAlbumStorageDir();
                                        if (albumPath.exists()) {
                                            Calendar c = Calendar.getInstance();
                                            String dateName = fromInt(c.get(Calendar.MONTH))
                                                    + "-" + fromInt(c.get(Calendar.DAY_OF_MONTH))
                                                    + "-" + fromInt(c.get(Calendar.YEAR))
                                                    + "-" + fromInt(c.get(Calendar.HOUR_OF_DAY))
                                                    + fromInt(c.get(Calendar.MINUTE))
                                                    + fromInt(c.get(Calendar.SECOND));
                                            File outputFile = new File(albumPath.getPath(), "video-" + dateName + ".mp4");

                                            try {
                                                ZipAClipUtils.copyFile(videoFile, outputFile);
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }

                                            Toast toast = Toast.makeText(getApplication(), "File Saved to Library", Toast.LENGTH_SHORT);
                                            toast.setGravity(Gravity.TOP, 0, 100);
                                            toast.show();

                                            //Trigger a media scan
                                            MediaScannerConnection.scanFile(
                                                    context,
                                                    new String[]{outputFile.getAbsolutePath()}, null,
                                                    new MediaScannerConnection.OnScanCompletedListener() {
                                                        @Override
                                                        public void onScanCompleted(String path, Uri uri) {

                                                        }
                                                    });
                                        }
                                        dialogInterface.cancel();
                                    }
                                });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        cancelMessageButton = (Button) findViewById(R.id.cancelMessageButton);
        cancelMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isForwarded)
        {
            forwardedMessage = AclipsaSDK.getInstance(context).getMessageWithGuid(message_guid);

            if (forwardedMessage != null) {
                messageBodyEditText.setText(forwardedMessage.getCaption());

                if(forwardedMessage.getVideo() != null){
                    AclipsaSDK.getInstance(this).putImageUrlInImageView(videocapturedImageView, forwardedMessage.getVideo().getThumbnail_low(), getMyIdentifier());

                    playVideoButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mediaPlayer) {
                                    progressdialog.dismiss();
                                }
                            };

                            try {
                                AclipsaSDK.getInstance(MessageSendActivity.this).playVideoFromMessage(MessageSendActivity.this, MessageSendActivity.this, forwardedMessage, videoView, preparedListener, null, null);
                            } catch (AclipsaSDKException e) {
                                e.printStackTrace();
                            }

                            progressdialog = ProgressDialog.show(MessageSendActivity.this, "", "Loading video...", true, false);
                        }
                    });

                }
            }
        }
    }

    private File getAlbumStorageDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), DemoAppConstants.ZIPACLIP_ALBUM);
        if (!file.mkdirs()) {
            Log.e("demo app", "Album Directory not created");
        }

        return file;
    }

    private String fromInt(int val)
    {
        return String.valueOf(val);
    }

    @Override
    public void apiCreateMessageSuccess(Object tag, int statusCode, String errorString, String guid) {

        progressdialog.dismiss();

        if (tag == "VIDEOLESS_MESSAGE")
        {
            Log.i("demo app", "apiCreateMessageSuccess VIDEOLESS_MESSAGE success");
        }
        else if (tag == "VIDEO_MESSAGE")
        {
            Log.i("demo app", "apiCreateMessageSuccess VIDEO_MESSAGE success");
        }

        ToastHelper.show(this, "Message has been successfully sent.");
        finish();
    }


    @Override
    public void apiMessageRequestResponseSuccess(Object tag, int statusCode, String errorString, Object response) {

        progressdialog.dismiss();

//		ArrayList<AclipsaSDKMessage> messages

        if (tag == "VIDEOLESS_MESSAGE")
        {
            Log.i("demo app", "apiMessageRequestResponseSuccess VIDEOLESS_MESSAGE success");
        }
        else if (tag == "VIDEO_MESSAGE")
        {
            Log.i("demo app", "apiMessageRequestResponseSuccess VIDEO_MESSAGE success");
        }

        ToastHelper.show(this, "Message has been successfully sent.");
        finish();
    }

    @Override
    public void apiMessageRequestResponseFailure(Object tag, int statusCode, String errorString) {
        // TODO Auto-generated method stub

        progressdialog.dismiss();

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

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

        //finish();
    }

    @Override
    public void apiRequestResponseSuccess(Object tag, int statusCode, String error) {
        Log.d("demo app", "DataManager zipApiRequestResponseSuccess");

        //we have successfully registered
        dataManagerResponseSuccess(tag, statusCode,error, null, null);
    }

    @Override
    public void apiRequestResponseFailure(Object tag, int statusCode, String error) {
        Log.d("demo app", "DataManager zipApiRequestResponseFailure");
        dataManagerResponseFail(tag, statusCode, error);
    }

    public String getMyIdentifier(){
        return AclipsaSDK.getInstance(context).getCurrentUserIdentifier();
    }

    public void dataManagerResponseSuccess(Object tag, int statusCode, String errorErrorResponse, String messageGuid, Object data) {

    }

    public void dataManagerResponseFail(Object tag, int statusCode, String errorErrorResponse) {

    }
}