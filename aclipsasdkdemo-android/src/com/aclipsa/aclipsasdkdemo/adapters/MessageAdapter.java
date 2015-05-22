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
import com.aclipsa.aclipsasdk.AclipsaSDKMessage;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaMessage;
import com.aclipsa.aclipsasdkdemo.R;

public class MessageAdapter extends ArrayAdapter<AclipsaMessage> {
	
	private final Context context;
	private final ArrayList<AclipsaMessage> items;
	private final int textViewResourceId;
	private final LayoutInflater inflater;
	

	public MessageAdapter(Context context, int textViewResourceId, ArrayList<AclipsaMessage> objects) {
		super(context, textViewResourceId, objects);
		this.context = context;
		this.items = objects;
		this.textViewResourceId = textViewResourceId;
		
		this.inflater = (LayoutInflater) context .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
		
		// A ViewHolder keeps references to children views to avoid unnecessary calls
    	// to findViewById() on each row.
    	ViewHolder holder;
    	
    	if (row == null) {
    		row = inflater.inflate(textViewResourceId, parent, false);

    		// Creates a ViewHolder and store references to the children views
    		// we want to bind data to.
    		holder = new ViewHolder();
    		holder.thumbnailImageView = (ImageView) row.findViewById(R.id.thumbnailImageView);
    		holder.title = (TextView)row.findViewById(R.id.titleTextView);
    		row.setTag(holder);
    	}	
    	else{
    		holder = (ViewHolder) convertView.getTag();
    	}

    	//reset the views
    	holder.thumbnailImageView.setVisibility(View.VISIBLE);
    	holder.title.setText("");
    	
    	//Populate the views
    	AclipsaMessage message = items.get(position);
		holder.title.setText(message.getTitle());
		if (message.getVideo() != null)
		{
			AclipsaSDK.getInstance(context).putImageUrlInImageView((ImageView)holder.thumbnailImageView, message.getVideo().getThumbnail_low(), getMyIdentifier());
		} else {
			holder.thumbnailImageView.setVisibility(View.GONE);
		}
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
