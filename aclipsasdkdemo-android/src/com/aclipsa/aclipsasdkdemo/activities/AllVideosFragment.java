package com.aclipsa.aclipsasdkdemo.activities;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDK.AclipsaSDKVideoHandler;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaVideo;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.adapters.VideoAdapter;
import com.aclipsa.aclipsasdkdemo.helpers.ToastHelper;

public class AllVideosFragment extends Fragment implements AclipsaSDKVideoHandler  {

	private final String TAG = AllVideosFragment.class.getName();
	private ProgressDialog progressdialog;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		progressdialog = ProgressDialog.show(getActivity(), null, "Loading...", true);
		AclipsaSDK.getInstance(getActivity()).requestVideos(this, "ALLVIDEOS");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.all_videos_activity, null);
	
		return v;
		
	}

	private void bindVideoList(ArrayList<AclipsaVideo> list)
	{
		if(getActivity() != null){ // null checks
			
			final ListView listView = (ListView) getActivity().findViewById(R.id.listView1);
			ArrayList<AclipsaVideo> videoList = list;
			int itemLayout = R.layout.video_item;

			final VideoAdapter adapter = new VideoAdapter(getActivity(), videoList, itemLayout);
			listView.setAdapter(adapter);

			listView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
					try {
						final Intent intent = new Intent(getActivity(), VideoDetailActivity.class);
						intent.putExtra("VIDEO", (Parcelable)adapter.getItem(position));
						startActivity(intent);
						//					overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_left);
					} catch (Exception ex) {
						ex.printStackTrace();
					}

				}
			});
		}
		progressdialog.dismiss();
	}

	@Override
	public void apiVideoRequestResponseSuccess(Object tag, int statusCode,
			String errorString, ArrayList<AclipsaVideo> videos) {
		
		if(progressdialog != null)
			progressdialog.dismiss();
		
		if (tag == "ALLVIDEOS")
		{
			if (videos == null)
			{
				ToastHelper.show(getActivity(), "No Videos Available");
			} else {
				bindVideoList(videos);
			}
		}
	}
	
	@Override
	public void apiVideoRequestResponseFailure(Object tag, int statusCode,
			String errorString) {
		if(progressdialog != null)
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

	@Override
	public void apiCreateVideoSuccess(Object tag, int statusCode,
			String errorString, String guid) {
		// TODO Auto-generated method stub
		
	}
}
