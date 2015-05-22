package com.aclipsa.aclipsasdkdemo.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDKVideo;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaVideo;
import com.aclipsa.aclipsasdkdemo.R;

public class VideoAdapter extends ArrayAdapter<AclipsaVideo> {
	private final Context context;
	private int mItemLayout;
	private LayoutInflater inflater;

	public VideoAdapter(Context context, ArrayList<AclipsaVideo> objects,
			int itemLayout) {
		super(context, R.layout.video_item, objects);
		this.context = context;
		this.mItemLayout = itemLayout;
		this.inflater =  (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View row = convertView;
		
		// A ViewHolder keeps references to children views to avoid unnecessary calls
    	// to findViewById() on each row.
    	ViewHolder holder;
    	
    	if (row == null) {
    		row = inflater.inflate(mItemLayout, parent, false);

    		// Creates a ViewHolder and store references to the children views
    		// we want to bind data to.
    		holder = new ViewHolder();
    		holder.thumbnailImageView = (ImageView) row.findViewById(R.id.videoThumbnailImage);
    		holder.title = (TextView)row.findViewById(R.id.titleTextView);
    		row.setTag(holder);
    	}	
    	else{
    		holder = (ViewHolder) convertView.getTag();
    	}

    	AclipsaVideo video = getItem(position);

		holder.title.setText(video.getTitle());

		AclipsaSDK.getInstance(context).putImageUrlInImageView((ImageView)holder.thumbnailImageView, video.getThumbnail_low(), getMyIdentifier());

		return row;
	}

    public String getMyIdentifier(){
        return AclipsaSDK.getInstance(context).getCurrentUserIdentifier();
    }
	
	static class ViewHolder {
        TextView title;
        ImageView thumbnailImageView;
    }
}
