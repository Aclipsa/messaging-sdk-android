package com.aclipsa.aclipsasdkdemo.adapters;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaMessage;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaVideo;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.helpers.ZipAClipUtils;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by arthurlim on 8/2/13.
 */
public class MessageAdapter2 extends ArrayAdapter<AclipsaMessage> {
    private final Context context;
    private final LayoutInflater inflater;
    private int viewResourceID;

    public MessageAdapter2(Context context, int textViewResourceId, ArrayList<AclipsaMessage> objects) {
        super(context, textViewResourceId, objects);
        this.context = context;
        this.viewResourceID = textViewResourceId;
        this.inflater = (LayoutInflater) context .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // A ViewHolder keeps references to children views to avoid unnecessary calls
        // to findViewById() on each row.
        ViewHolder holder;

        if(convertView == null){
            convertView = inflater.inflate(viewResourceID, parent, false);
            holder = new ViewHolder();
            holder.previewImageView = (ImageView)convertView.findViewById(R.id.videoImage);
            holder.messageDetailLayout = (LinearLayout) convertView.findViewById(R.id.messageDetailLayout);
            //holder.messageCellRelativeLayout = (RelativeLayout)convertView.findViewById(R.id.messageCellRelativeLayout);
            holder.senderTextView = (TextView)convertView.findViewById(R.id.senderTsuiTextview);
            holder.timeStamp = (TextView)convertView.findViewById(R.id.dateTextView);
            convertView.setTag(holder);
        }
        else{
            holder = (ViewHolder) convertView.getTag();
        }

        AclipsaMessage message = getItem(position);
        AclipsaVideo embeddedVideo = message.getVideo();

//        TODO: check the messageRecipient relationship to see if this message is yanked from the recipient
        //We check only the first recipient and show if this message is yanked
//        if(message.getRecipients().get(0).isYanked() == true)
//            holder.zipbackContainerView.setVisibility(View.VISIBLE);

        if(embeddedVideo != null)
        {
            Log.d("demo app", "thumbnail:" + embeddedVideo.getThumbnail_low());
            AclipsaSDK.getInstance(context).putImageUrlInImageView(holder.previewImageView, embeddedVideo.getThumbnail_low(), getMyIdentifier());
            holder.senderTextView.setText(message.getFrom().getTsui());
        }
        else{
            //holder.messageTextView.setText(message.getCaption());
        }

        /**
         * display time stamp info
         */
        Date date = message.getCreated_at();
        holder.timeStamp.setText(ZipAClipUtils.getFormattedStringFromDate(context, date));

        float heigh = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics());
        float width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, context.getResources().getDisplayMetrics());

        RelativeLayout.LayoutParams cellLayoutParams = new RelativeLayout.LayoutParams((int)width, (int)heigh);
        cellLayoutParams.setMargins(10, 10, 10, 10);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(10, 10, 10, 10);

        if(message.getFrom().getTsui().equals(AclipsaSDK.getInstance(context).getCurrentUserIdentifier()))
        {
            if(embeddedVideo != null)
                AclipsaSDK.getInstance(context).putImageUrlInImageView(holder.previewImageView, embeddedVideo.getThumbnail_low(), getMyIdentifier());

            cellLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            holder.previewImageView.setLayoutParams(cellLayoutParams);

            params.addRule(RelativeLayout.LEFT_OF, holder.previewImageView.getId());
            holder.messageDetailLayout.setLayoutParams(params);
            holder.messageDetailLayout.setGravity(Gravity.RIGHT);
        }
        else{
            if(embeddedVideo != null)
                AclipsaSDK.getInstance(context).putImageUrlInImageView(holder.previewImageView, embeddedVideo.getThumbnail_low(), getMyIdentifier());

            cellLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            holder.previewImageView.setLayoutParams(cellLayoutParams);

            params.addRule(RelativeLayout.RIGHT_OF, holder.previewImageView.getId());
            holder.messageDetailLayout.setLayoutParams(params);
        }

        return convertView;
    }

    public String getMyIdentifier(){
        return AclipsaSDK.getInstance(context).getCurrentUserIdentifier();
    }

    static class ViewHolder {
        ImageView previewImageView;
        RelativeLayout messageCellRelativeLayout;
        TextView senderTextView;
        TextView timeStamp;
        LinearLayout messageDetailLayout;
    }
}
