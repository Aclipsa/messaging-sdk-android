package com.aclipsa.aclipsasdkdemo.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.VideoView;

import com.aclipsa.aclipsasdkdemo.R;
import com.coremedia.iso.boxes.Container;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import com.googlecode.mp4parser.authoring.Movie;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by evetorres on 12/9/13.
 */
public class TrimVideoActivity extends Activity {

    private Button dragButton1, dragButton2;
    private GestureDetectorCompat mLeftDetector, mRightDetector;
    private VideoView previewVideoView;
    private File videoFile;
    private int totalVideoDuration;
    private long trimTimeLeft, trimTimeRight;
    Context context;

    int screenWitdth = 0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.trim_video_view);

        String videoFilePath = getIntent().getStringExtra(com.aclipsa.aclipsasdkdemo.constants.DemoAppConstants.EXTRA_VIDEO_PATH);
        Uri videoUri = Uri.parse(videoFilePath);
        videoFile = new File(videoUri.getPath());

        context = this;

        dragButton1 = (Button) findViewById(R.id.dragButton1);
        dragButton2 = (Button) findViewById(R.id.dragButton2);

        previewVideoView = (VideoView) findViewById(R.id.previewVideoView);

        dragButton1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                int dx = 0, x = 0;
                switch(motionEvent.getAction())
                {

                    case MotionEvent.ACTION_MOVE :
                    {
                        x = (int)motionEvent.getRawX();
                        marginLayoutParams.leftMargin = (int)(x-dx);
                        view.setLayoutParams(marginLayoutParams);

                        if (videoFile.exists()) {
                            jumpVideoToFromLeft((int)(x-dx));
                        }
                    }
                    break;
                    case MotionEvent.ACTION_UP :
                    {

                    }
                    break;
                }
                return true;
            }
        });

        dragButton2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                int dx = 0, x = 0;
                switch(motionEvent.getAction())
                {
                    case MotionEvent.ACTION_MOVE :
                    {
                        x = (int)motionEvent.getRawX();
                        marginLayoutParams.rightMargin = (int)(screenWitdth - x);
                        view.setLayoutParams(marginLayoutParams);

                        if (videoFile.exists()) {
                            jumpVideoToFromRight((int) (screenWitdth - x));
                        }
                    }
                    break;
                    case MotionEvent.ACTION_UP :
                    {

                    }
                    break;
                }
                return true;


            }
        });

        Button saveButton = (Button) findViewById(R.id.savebButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.i("Arthur", "trimTimeLeft = " + (trimTimeLeft / 1000) + " trimTimeRight = " + (screenWitdth - (trimTimeRight / 1000)));
                new TrimTask().execute((trimTimeLeft /1000), screenWitdth - (trimTimeRight/1000));
            }
        });

        Button cancelButton = (Button) findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWitdth = size.x;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i("Arthur","videoFile.exists() = "+videoFile.exists());
        if(videoFile.exists()){
            previewVideoView.setVideoPath(videoFile.getPath());
            previewVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                public void onPrepared(MediaPlayer mp) {
                    totalVideoDuration = previewVideoView.getDuration();
                    Log.i("Arthur","totalVideoDuration = "+ totalVideoDuration);

                    mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener()  {
                        @Override
                        public void onSeekComplete(MediaPlayer mp) {
                            previewVideoView.pause();
                        }

                    });

                }
            });

            previewVideoView.seekTo(1);
        }
    }

    public void setVideoFile(File vidFile){
        videoFile = vidFile;
    }

    private void jumpVideoToFromLeft(int margin){

        float time = (margin * totalVideoDuration )/screenWitdth;
        trimTimeLeft = (int)time;

//        Log.i("Arthur","trimTimeLeft = "+trimTimeLeft);

        previewVideoView.start();
        previewVideoView.seekTo((int)trimTimeLeft);

    }

    private void jumpVideoToFromRight(int margin){

        float time = (margin * totalVideoDuration )/screenWitdth;
        trimTimeRight = totalVideoDuration - (int)time;

//        Log.i("Arthur","trimTimeRight = "+trimTimeRight);

        previewVideoView.start();
        previewVideoView.seekTo((int)trimTimeRight);
    }

    private void doShorten(final long _startTime, final long _endTime) {
        try {
            Movie movie = MovieCreator.build(videoFile.getPath());

            List<Track> tracks = movie.getTracks();
            movie.setTracks(new LinkedList<Track>());

            double startTime = _startTime;
            double endTime = _endTime;//(double) getDuration(tracks.get(0)) / tracks.get(0).getTrackMetaData().getTimescale();

            boolean timeCorrected = false;

            // Here we try to find a track that has sync samples. Since we can only start decoding
            // at such a sample we SHOULD make sure that the start of the new fragment is exactly
            // such a frame
            for (Track track : tracks) {
                if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                    if (timeCorrected) {
                        // This exception here could be a false positive in case we have multiple tracks
                        // with sync samples at exactly the same positions. E.g. a single movie containing
                        // multiple qualities of the same video (Microsoft Smooth Streaming file)

                        throw new RuntimeException("The startTime has already been corrected by another track with SyncSample. Not Supported.");
                    }
                    startTime = correctTimeToSyncSample(track, startTime, false);
                    endTime = correctTimeToSyncSample(track, endTime, true);
                    timeCorrected = true;
                }
            }


            for (Track track : tracks) {
                long currentSample = 0;
                double currentTime = 0;
                double lastTime = -1;
                long startSample = -1;
                long endSample = -1;

                for (int i = 0; i < track.getSampleDurations().length; i++) {
                    long delta = track.getSampleDurations()[i];

                    if (currentTime > lastTime && currentTime <= startTime) {
                        // current sample is still before the new starttime
                        startSample = currentSample;
                    }
                    if (currentTime > lastTime && currentTime <= endTime) {
                        // current sample is after the new start time and still before the new endtime
                        endSample = currentSample;
                    }

                    currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
                    currentSample++;
                }

                movie.addTrack(new AppendTrack(new CroppedTrack(track, startSample, endSample)));

            }


            Container out = new DefaultMp4Builder().build(movie);

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String actualFilePath = videoFile.getAbsolutePath().substring(0, videoFile.getAbsolutePath().lastIndexOf(File.separator));
            String filename = actualFilePath + File.separator + String.format("TMP4_APP_OUT-%f-%f", startTime, endTime) + "_" + timeStamp + ".mp4";
            FileOutputStream fos = new FileOutputStream(filename);

            FileChannel fc = fos.getChannel();
//            out.getBox(fc);
            out.writeContainer(fc);
            fc.close();
            fos.close();

            //Replace the the old file with the new one
            String originalFilename = videoFile.getPath();
            videoFile.delete();

            File toRenameFile = new File(filename);
            toRenameFile.renameTo(new File(originalFilename));

            finish();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];

            if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                // samples always start with 1 but we start with zero therefore +1
                timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
            }
            currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
            currentSample++;

        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }

    private class TrimTask extends AsyncTask<Long, Void, Void> {

        protected ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(TrimVideoActivity.this, "", "Processing...", true, false);
        }

        @Override
        protected Void doInBackground(Long... longs) {

            long startTime = longs[0];
            long endTime = longs[1];

            doShorten(startTime, endTime);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            //finish();
        }
    }
}