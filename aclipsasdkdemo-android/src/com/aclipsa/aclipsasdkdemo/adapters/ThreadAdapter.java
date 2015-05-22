package com.aclipsa.aclipsasdkdemo.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.externalmodels.AclipsaThread;
import com.aclipsa.aclipsasdkdemo.R;
import com.aclipsa.aclipsasdkdemo.helpers.ZipAClipUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by arthurlim on 8/22/13.
 */
public class ThreadAdapter extends ArrayAdapter<AclipsaThread> {

    private final Context context;
    private final int textViewResourceId;
    private final LayoutInflater inflater;

    public ThreadAdapter(Context context, int textViewResourceId, ArrayList<AclipsaThread> objects) {
        super(context, textViewResourceId, objects);
        this.context = context;
        this.textViewResourceId = textViewResourceId;

        this.inflater = (LayoutInflater) context .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            holder.senderTextView = (TextView)row.findViewById(R.id.senderTextView);
            holder.receiverTextView = (TextView)row.findViewById(R.id.receiverTextView);
            holder.dateTextView = (TextView)row.findViewById(R.id.dateTextView);
            row.setTag(holder);
        }
        else{
            holder = (ViewHolder) convertView.getTag();
        }

        //reset the views
        holder.senderTextView.setText("");
        holder.receiverTextView.setText("");
        holder.dateTextView.setText("");

        //Populate the views
        AclipsaThread thread = getItem(position);

        //Get the first message which will have the original sender
        String sender = thread.originalSender.getTsui();
//        if(sender.equals(AclipsaSDK.getInstance(context).getCurrentUserIdentifier()))
//            holder.senderTextView.setText("Me");
//        else
//            holder.senderTextView.setText(thread.originalSender.getTsui());

        //TODO: handle multiple reciever
        String receivers = "", delimiter = "", tsuiString = "";

        if(sender.equals(AclipsaSDK.getInstance(context).getCurrentUserIdentifier()))
            receivers = "Me" + ", ";
        else
            receivers = thread.originalSender.getTsui() + ", ";

        for (int x = 0; x < thread.getUsers().size(); x++)
        {
            if (x > thread.getUsers().size() -1)
                delimiter = ", ";
            else
                delimiter = "";

            tsuiString = thread.getUsers().get(x).getTsui();
            if(tsuiString.equals(AclipsaSDK.getInstance(context).getCurrentUserIdentifier()))
                tsuiString = "Me";

            receivers = receivers + tsuiString + delimiter;
        }

        holder.senderTextView.setText(receivers);

//        holder.receiverTextView.setText(receivers);

//        Date messageDate = thread.getLastMessage().getCreated_at();
//        holder.dateTextView.setText(new SimpleDateFormat("MM/dd/yyyy").format(messageDate));

        Date messageDate = thread.getLastDate();
        holder.dateTextView.setText(ZipAClipUtils.getFormattedStringFromDate(context, messageDate));

        return row;
    }


    static class ViewHolder {
        TextView senderTextView;
        TextView receiverTextView;
        TextView dateTextView;
    }
}
