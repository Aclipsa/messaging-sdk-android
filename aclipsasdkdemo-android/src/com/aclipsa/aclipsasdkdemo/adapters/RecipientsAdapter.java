package com.aclipsa.aclipsasdkdemo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aclipsa.aclipsasdk.externalmodels.AclipsaRecipient;
import com.aclipsa.aclipsasdkdemo.R;

import java.util.ArrayList;


/**
 * Created by arthurlim on 9/6/13.
 */
public class RecipientsAdapter extends ArrayAdapter<AclipsaRecipient> {
    private final Context context;
    private final int textViewResourceId;
    private final LayoutInflater inflater;


    public RecipientsAdapter(Context context, int textViewResourceId, ArrayList<AclipsaRecipient> objects){
        super(context, textViewResourceId, objects);
        this.context = context;
        this.textViewResourceId = textViewResourceId;

        this.inflater = (LayoutInflater) context .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        ViewHolder holder;

        if (row == null) {
            row = inflater.inflate(textViewResourceId, parent, false);

            // Creates a ViewHolder and store references to the children views
            // we want to bind data to.
            holder = new ViewHolder();
            holder.receiverTextView = (TextView)row.findViewById(R.id.receiverTextView);
            row.setTag(holder);
        }
        else{
            holder = (ViewHolder) convertView.getTag();
        }

        //reset the views
        holder.receiverTextView.setText("");

        //Populate the views
        AclipsaRecipient recipient = getItem(position);

        holder.receiverTextView.setText(recipient.getTsui());
//        if(recipient.isUnread()) {
//            holder.unviewedView.setVisibility(View.VISIBLE);
//            holder.viewedView.setVisibility(View.GONE);
//        } else {
//            holder.unviewedView.setVisibility(View.GONE);
//            holder.viewedView.setVisibility(View.VISIBLE);
//        }

        return row;
    }

    static class ViewHolder {
        TextView receiverTextView;
//        View unviewedView;
//        View viewedView;
    }
}
