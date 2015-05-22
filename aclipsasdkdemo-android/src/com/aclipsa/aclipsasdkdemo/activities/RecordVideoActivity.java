package com.aclipsa.aclipsasdkdemo.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.media.MediaRecorder.OutputFormat;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.aclipsa.aclipsasdk.externalmodels.AclipsaRecipient;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.constants.ZipAClipSettingsHelper;
import com.mobileaze.common.helpers.CameraHelper;
import com.mobileaze.common.helpers.ToastHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecordVideoActivity extends Activity implements SensorEventListener{

    public static final String VIDEO_PATH = "video_path";

    private static final String TAG = "Demo app";

    private SurfaceView videoSurfaceView;
    private Camera camera;
    private SurfaceHolder previewHolder;
    private boolean inPreview = false;
    private boolean isRecording = false;
    private boolean isCountingDown = false;
    private boolean isReply = false;

    private Uri selectedvideo;
    private MediaRecorder recorder;
    private File recordFile;
    private ArrayList<AclipsaRecipient> recipients;
    private String message_guid;

    //    private static boolean isUsingBackCamera = false;
    private static boolean isFlashOn = false;
    private int cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;

    private ImageButton flashImageButton;
    private ImageButton switchCameraButton;
    private Button recordButton;
    private Chronometer recordLimitChronometer;

    private Point screenSizePoint;
    private DisplayMetrics outMetrics;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];


    private static int lastOrientation = 0;
    private static final int ORIENTATION_PORTRAIT = 0;
    private static final int ORIENTATION_LANDSCAPE_LEFT = 1;
    private static final int ORIENTATION_LANDSCAPE_RIGHT = 2;

    private ImageView countDownImageView;
    private CountDownTimer startRecordingCountDownTimer;
    private CountDownTimer recordLimitTimer;

    private int orientation = ORIENTATION_PORTRAIT;

    private ArrayList<AclipsaRecipient> aclipsaRecipients;
    private String threadID;

    public void showRecordFragment(int whichCamera) {

        RecordVideoActivity recordActivity = new RecordVideoActivity();
        recordActivity.setCameraFace(whichCamera);
        Intent intent = new Intent(this, recordActivity.getClass());
        startActivity(intent);
        finish();
    }

    public void changeAndStartRecordButton() {
        isRecording = true;
        //showButtonPulse(recordButton);
    }

    public void startChronometer() {
        recordLimitChronometer.setVisibility(View.VISIBLE);
        recordLimitChronometer.setBase(SystemClock.elapsedRealtime() + 1);
        recordLimitChronometer.start();
    }

    public void resetRecordButton() {
        recordButton.setBackgroundResource(R.drawable.tabbarrecordbuttonreadyhighlighted);
        isRecording = false;

        recordButton.setEnabled(true);

        if (recordLimitChronometer != null) {
            recordLimitChronometer.stop();
            recordLimitChronometer.setVisibility(View.INVISIBLE);
        }
    }

    public void showComposeFragment(int whichFragment, String videoPath, ArrayList<AclipsaRecipient> recipients, String messageGuid) {
        Intent intent = new Intent(this, MessageSendActivity.class);
        intent.putExtra(com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.EXTRA_VIDEO_PATH, videoPath);
        intent.putExtra("messageGuid", messageGuid);
        intent.putParcelableArrayListExtra(com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.EXTRA_RECIPIENTS, recipients);
        this.startActivity(intent);
        this.finish();
    }

    public void rotateButtons(int value) {
        recordButton.animate().rotation(value);
    }

    public float getScreenDensity() {
        return getResources().getDisplayMetrics().density;

    }

    public float getScreenWidth() {
        return (float) screenSizePoint.x;
    }

    public float getScreenHeight() {
        return (float) screenSizePoint.y;
    }

    public SurfaceView getSurfaceView() {
        return new SurfaceView(this);
    }

    public void enableRecordButton() {
        recordButton.setEnabled(true);
    }

    public void disableRecordButton() {
        recordButton.setEnabled(false);
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        //first saving my state, so the bundle wont be empty.
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }

    public void setRecipients(ArrayList<AclipsaRecipient> recips) {
        aclipsaRecipients = recips;
    }

    public void setThreadID(String id){
        threadID = id;
    }

    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                if(CameraHelper.hasBackCamera(getApplicationContext())){
                    cameraID = ZipAClipSettingsHelper.getInt(getApplicationContext(), com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.PREFERENCE_CURRENT_CAMERA, CameraInfo.CAMERA_FACING_BACK);

                } else {
                    cameraID = ZipAClipSettingsHelper.getInt(getApplicationContext(), com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.PREFERENCE_CURRENT_CAMERA, CameraInfo.CAMERA_FACING_FRONT);
                }

                if (camera == null) {

                    camera = Camera.open(cameraID);

                    if (cameraID == CameraInfo.CAMERA_FACING_BACK) {
                        Camera.Parameters param = camera.getParameters();
                        // http://stackoverflow.com/questions/7225571/camcorderprofile-quality-high-resolution-produces-green-flickering-video
                        param.set("cam_mode", 1);
                        param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                        camera.setParameters(param);
                    }

                }

                if (camera != null) {
                    //Hide the flash button if there is no flash
                    List<String> flashMode = camera.getParameters().getSupportedFlashModes();

                    if (flashMode != null && cameraID == Camera.CameraInfo.CAMERA_FACING_BACK) //null means there is no flash
                        flashImageButton.setVisibility(View.VISIBLE);
                    else
                        flashImageButton.setVisibility(View.GONE);
                }

                if (camera != null && previewHolder != null)
                    camera.setPreviewDisplay(previewHolder);

                if (android.os.Build.VERSION.SDK_INT >= 8) {   // If API >= 8 -> rotate display...
                    camera.setDisplayOrientation(90);
                }
                camera.startPreview();
                inPreview = true;

            } catch (Throwable t) {
                Log.e("PreviewDemo-surfaceCallback",
                        "Exception in setPreviewDisplay()", t);
            }
        }

        public void surfaceChanged(SurfaceHolder holder,
                                   int format, final int width,
                                   final int height) {

        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            if (recorder != null) {
                if (isRecording) {
                    recorder.stop();
                    isRecording = false;
                }
                recorder.release();

            }
        }
    };

    private Handler mHandler = new Handler();

    private final Runnable mLoadCamera = new Runnable() {
        public void run() {
//            videoSurfaceView = new SurfaceView(getActivity());
            videoSurfaceView = getSurfaceView();

            RelativeLayout vidSurfaceViewContainer = (RelativeLayout) findViewById(R.id.vidSurfaceViewContainer);
            vidSurfaceViewContainer.removeAllViews();

            previewHolder = videoSurfaceView.getHolder();
            previewHolder.addCallback(surfaceCallback);
//            previewHolder.setFixedSize(fragmentView.getWidth(), fragmentView.getHeight());

            vidSurfaceViewContainer.addView(videoSurfaceView);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_view);

        isReply = getIntent().getBooleanExtra("isReply", false);
        if (isReply)
        {
            aclipsaRecipients = getIntent().getExtras().getParcelableArrayList(com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.EXTRA_RECIPIENTS);
            message_guid = getIntent().getExtras().getString("messageGuid");
        }

        switchCameraButton = (ImageButton) findViewById(R.id.switchCameraButton);
        flashImageButton = (ImageButton) findViewById(R.id.flashImageButton);
        countDownImageView = (ImageView) findViewById(R.id.countDownImageView);

        recordButton = (Button) findViewById(R.id.recordButton);
        recordButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                toggleRecordingState();
            }

        });

        recordLimitChronometer = (Chronometer) findViewById(R.id.recordLimitChronometer);
        recordLimitChronometer.setVisibility(View.INVISIBLE);
        recordLimitChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long t = SystemClock.elapsedRealtime() - chronometer.getBase();
                chronometer.setText(DateFormat.format("m:ss", t));
            }
        });

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        screenSizePoint = new Point();
        display.getSize(screenSizePoint);
    }

    @Override
    public void onStart() {
        super.onStart();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                resetCamera();

                if (cameraID == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
                    showRecordFragment(cameraID);
                } else {
                    cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
                    showRecordFragment(cameraID);
                }

                ZipAClipSettingsHelper.setInt(getApplicationContext(), com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.PREFERENCE_CURRENT_CAMERA, cameraID);
            }
        });

        //Hide the switching of cameras if we only have one camera
        if (Camera.getNumberOfCameras() < 2) {
            switchCameraButton.setVisibility(View.GONE);
        }

        flashImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.stopPreview();
                if (isFlashOn = !isFlashOn) {
                    /**
                     * Turn the flash led on
                     */

                    flashImageButton.setImageResource(R.drawable.flashon);

                    //Turn the flash on

                    try {

                        Camera.Parameters p = camera.getParameters();
                        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        camera.setParameters(p);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    /**
                     * Trun off the flash led
                     */
                    flashImageButton.setImageResource(R.drawable.flashoff);
                    Camera.Parameters p = camera.getParameters();
                    p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(p);

                }
                camera.startPreview();
            }
        });


        startRecordingCountDownTimer = new CountDownTimer(4000, 1000) {

            public void onTick(final long millisUntilFinished) {

                isCountingDown = true;

                long timeRemaining = millisUntilFinished / 1000;

                Log.i(TAG, "timeRemaining = " + timeRemaining);

                if (timeRemaining > 1) {
                    countDownImageView.setVisibility(View.VISIBLE);
                }
                if (timeRemaining >= 3) {

                    countDownImageView.setImageResource(R.drawable.countdown_iphone_3);
                } else if (timeRemaining == 2) {
                    countDownImageView.setImageResource(R.drawable.countdown_iphone_2);
                } else {
                    countDownImageView.setImageResource(R.drawable.countdown_iphone_1);
                }

                Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.countdown_pulse);

                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override

                    public void onAnimationStart(Animation animation) {
                        countDownImageView.setVisibility(View.VISIBLE);//josh???
                    }

                    @Override

                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        countDownImageView.setVisibility(View.INVISIBLE);
                    }
                });

                countDownImageView.startAnimation(animation);

            }


            public void onFinish() {
                startActualRecording();
            }
        };

        //300000 = 5 mins
        recordLimitTimer = new CountDownTimer(300000, 1000) {
            private boolean thirtysecmark = false;
            private boolean oneminutemark = false;
            private boolean twominutemark = false;
            private boolean threeminutemark = false;
            private boolean fourminutemark = false;
            private boolean fiveminutemark = false;

            @Override
            public void onTick(long millisUntilFinished) {
                float seconds = 300 - (millisUntilFinished / 1000);

                if (seconds <= 30) {  // we are under 30 sec mark
                    float x = (360 / 30) * seconds;

                    if (thirtysecmark == false) {
                        thirtysecmark = true;
                    }
                } else if (seconds > 30 && seconds <= 90) {  //a minute and a half mark
                    oneminutemark = true;
                } else if (seconds > 90 && seconds <= 150) {  //a minute and a half mark
                    twominutemark = true;
                } else if (seconds > 150 && seconds <= 210) {  //2 minute mark
                    threeminutemark = true;
                } else if (seconds > 210 && seconds <= 270) {  //2 minute mark
                    fourminutemark = true;
                } else if (seconds > 270 && seconds <= 330) {  //2 minute mark
                    fiveminutemark = true;
                }
            }

            @Override
            public void onFinish() {
                stopRecording();
                //setRecordArc(0);

                resetRecordButton();
            }
        };
    }

    private void startActualRecording(){
        /**
         * WARNING - the try catch is for debugging purposes. theres an intermittent problem with the mediarecorder
         * being started in an invalid state.
         */
        disableRecordButton(); // catch super fast doubletap - Arthur

        isCountingDown = false;

        //reset the views
        countDownImageView.setVisibility(View.INVISIBLE);
        countDownImageView.setImageResource(R.drawable.countdown_iphone_3);

        //Start the recording...
        try {
            recorder.start();
            changeAndStartRecordButton();
            isRecording = true;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    enableRecordButton();
                }
            }, 1500);


        } catch (Exception e) {
            e.printStackTrace();

            isRecording = false;
            recorder.reset();
            recorder.release();
            recorder = null;
            initRecorder();

            resetRecordButton();
            enableRecordButton();
        }

        if (isRecording == true) {
            startChronometer();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recordLimitTimer.start();
                }
            }, 1000);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "RecordFragment.onResume");
        /**
         * Checking the camera for null so that the screen does not flicker in
         * subsequent resumes
         */

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        mHandler.post(mLoadCamera);
    }

    @Override
    public void onPause() {

        Log.i(TAG, "RecordFragment onPause is called");

        if (isRecording == true) {
            stopRecording();
        }

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (camera != null) {
            if (inPreview) {
                camera.stopPreview();
            }

            camera.release();
            camera = null;
            inPreview = false;
        }

        mSensorManager.unregisterListener(this);

        super.onPause();
    }

    public void setCameraFace(int settings) {
        this.cameraID = settings;

    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;
                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        return (result);
    }

    //From Google Sample Code
    //If we use this, it fixes the distortion on the preview of the front camera but currupts the recording - Arthur
    private Camera.Size getOptimalPreviewSize(Camera.Parameters parameters, int w, int h) {

        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();

        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private void resetCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

    public void cancelRecordLimitTimer() {
        if (recordLimitTimer != null)
            recordLimitTimer.cancel();
    }


    //Sensor callbacks
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {


        if (sensorEvent.sensor == mAccelerometer) {
            System.arraycopy(sensorEvent.values, 0, mLastAccelerometer, 0, sensorEvent.values.length);
            mLastAccelerometerSet = true;
        } else if (sensorEvent.sensor == mMagnetometer) {
            System.arraycopy(sensorEvent.values, 0, mLastMagnetometer, 0, sensorEvent.values.length);
            mLastMagnetometerSet = true;
        }

        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);

            double azimuth = Math.toDegrees(mOrientation[0]);
            double pitch = Math.toDegrees(mOrientation[1]);
            double roll = Math.toDegrees(mOrientation[2]);

            if (pitch < -45 && pitch > -135) {
                redrawButtons(ORIENTATION_PORTRAIT);
                orientation = ORIENTATION_PORTRAIT;
            } else if (pitch > 45 && pitch < 135) {
                redrawButtons(ORIENTATION_PORTRAIT);
                orientation = ORIENTATION_PORTRAIT;
            } else if (roll > 45) {
                redrawButtons(ORIENTATION_LANDSCAPE_RIGHT);
                orientation = ORIENTATION_LANDSCAPE_RIGHT;

            } else if (roll < -45) {
                redrawButtons(ORIENTATION_LANDSCAPE_LEFT);
                orientation = ORIENTATION_LANDSCAPE_LEFT;
            }
        }
    }

    private void redrawButtons(int orientation) {
        float screetWidth = getScreenWidth();
        float screenHeight = getScreenHeight();
        float screenDensity = getScreenDensity();

//        Log.i("Arthur","screetWidth = "+ screetWidth+" screenHeight = "+screenHeight +" screenDensity = "+screenDensity);

        if (lastOrientation != orientation) {
            //Redraw the screen
            lastOrientation = orientation;

            switch (orientation) {
                case ORIENTATION_PORTRAIT:
                    flashImageButton.animate().translationX(0);
                    flashImageButton.animate().translationY(0);
                    flashImageButton.animate().rotation(0);

                    switchCameraButton.animate().translationX(0);
                    switchCameraButton.animate().translationY(0);
                    switchCameraButton.animate().rotation(0);

                    recordButton.animate().translationX(0);
                    recordButton.animate().translationY(0);
                    recordButton.animate().rotation(0);

                    recordLimitChronometer.animate().translationX(0);
                    recordLimitChronometer.animate().translationY(0);
                    recordLimitChronometer.animate().rotation(0);

                    countDownImageView.animate().translationX(0);
                    countDownImageView.animate().translationY(0);
                    countDownImageView.animate().rotation(0);

                    rotateButtons(0);

                    break;
                case ORIENTATION_LANDSCAPE_LEFT:
                    flashImageButton.animate().translationY(screenHeight - (180 * screenDensity));
                    flashImageButton.animate().rotation(90);

                    switchCameraButton.animate().translationX(screetWidth - (85 * screenDensity));
                    switchCameraButton.animate().rotation(90);

                    recordButton.animate().translationX(0);
                    recordButton.animate().rotation(90);

                    recordLimitChronometer.animate().translationX(0);
                    recordLimitChronometer.animate().rotation(90);

                    countDownImageView.animate().translationX(0);
                    countDownImageView.animate().rotation(90);

                    rotateButtons(90);

                    break;
                case ORIENTATION_LANDSCAPE_RIGHT:
                    flashImageButton.animate().translationX(-(screetWidth - (85 * screenDensity)));
                    flashImageButton.animate().rotation(-90);

                    switchCameraButton.animate().translationY(screenHeight - (300 * screenDensity));
                    switchCameraButton.animate().rotation(-90);

                    recordButton.animate().translationY(0);
                    recordButton.animate().rotation(-90);

                    break;
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.REQUEST_CHOOSE_VIDEO && resultCode == Activity.RESULT_OK) {
            selectedvideo = data.getData();

            //For preview
            String[] filePathColumn = {MediaStore.Video.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedvideo, filePathColumn, null, null, null);

            if (cursor == null) {
                //try if we could access the uri directly
                File testFile = new File(selectedvideo.getPath());
                if (testFile.exists()) {
                    Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(selectedvideo.getPath(),
                            MediaStore.Images.Thumbnails.MINI_KIND);

                    videoReady(selectedvideo);
                } else {

                    ToastHelper.show(this, "Unable to obtain selected video");
                    return;
                }
            } else {
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String videoFilePath = cursor.getString(columnIndex);
                cursor.close();

//                videoReady(videoFilePath);
                videoReady(selectedvideo);
            }
        }
    }

    //Recording stuff

    private void initRecorder() {// this takes care of all the mediarecorder settings

        recorder = new MediaRecorder();// Instantiate our media recording object

        if (camera != null) {
            try {
                camera.unlock();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        recorder.setCamera(camera);

        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);

        if(CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P))
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        else if(CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_CIF))
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_CIF);
        else if(CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_QVGA))
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA);

        recorder.setProfile(profile);

        String extension = ".mp4";
        if(profile.fileFormat == OutputFormat.THREE_GPP)
            extension = ".3gp";

        String recordFileName = "aclipsaDemoVideo" + "-" + System.currentTimeMillis()+extension;

        recordFile = new File(getExternalFilesDir(null), recordFileName);
        recorder.setOutputFile(recordFile.getPath());

        recorder.setPreviewDisplay(previewHolder.getSurface());

        //TODO: This is 90 if portraint.  need to switch to 0 if lansdcape
        recorder.setOrientationHint(90);

    }

    private void videoReady(Uri videoPath) {

        if (isReply)
            showComposeFragment(com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.DISPLAY_RECORD_VIDEO_READY_FRAGMENT, videoPath.toString(), aclipsaRecipients, message_guid);
        else
            showComposeFragment(com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.DISPLAY_RECORD_VIDEO_READY_FRAGMENT, videoPath.toString(), aclipsaRecipients, null);

    }

    private void prepareRecorder() {
//        recorder.setPreviewDisplay(previewHolder.getSurface());

        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            //finish();
        } catch (IOException e) {
            e.printStackTrace();
            //finish();
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void startRecording() {
        Log.d(TAG, "startRecording");

        if (isRecording == false) {
            if (recorder == null) {
                initRecorder();
            }

            if(isCountingDown == true){  //start recording right away since this is the second click - Arthur
                startRecordingCountDownTimer.cancel();
                startActualRecording();
            }
            else{

                switch (orientation) {
                    case ORIENTATION_PORTRAIT:
                        if(cameraID == CameraInfo.CAMERA_FACING_FRONT)
                            recorder.setOrientationHint(270); // since front camera is upside down
                        else
                            recorder.setOrientationHint(90);
                        break;
                    case ORIENTATION_LANDSCAPE_LEFT:
                        recorder.setOrientationHint(0);
                        break;
                    case ORIENTATION_LANDSCAPE_RIGHT:
                        recorder.setOrientationHint(180);
                        break;
                }

                prepareRecorder();
                startRecordingCountDownTimer.start();
            }
            switchCameraButton.setVisibility(View.INVISIBLE);
            flashImageButton.setVisibility(View.INVISIBLE);
        }
    }

    private void showCameraButtons() {
        switchCameraButton.setVisibility(View.VISIBLE);
        flashImageButton.setVisibility(View.VISIBLE);
    }

    public void stopRecording() {
        if (isRecording == true) {
            try {
                recorder.stop();
                isRecording = false;
                recorder.reset();
                recorder.release();
                recorder = null;

//                initRecorder();
            } catch (IllegalStateException e) {
                Log.d(TAG, "problem stopping");
                e.printStackTrace();
            }
            showCameraButtons();
        }

//        resetMinuteIndicators();

        videoReady(Uri.fromFile(recordFile));
    }

    private void toggleRecordingState() {
        if (isRecording()) {
            stopRecording();
            resetRecordButton();
            cancelRecordLimitTimer();
        } else {
            startRecording();
        }
    }

}
