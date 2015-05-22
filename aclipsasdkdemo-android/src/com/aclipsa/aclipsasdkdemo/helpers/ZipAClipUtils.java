package com.aclipsa.aclipsasdkdemo.helpers;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;

/**
 * Created by arthurlim on 11/13/13.
 */
public class ZipAClipUtils {

    public static String getFormattedStringFromDate(Context context, Date date){
        Date now = new Date();
        long diff =  now.getTime() - date.getTime();
        long numDays = diff / (24 * 60 * 60 * 1000);

        if(numDays < 1){
            DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(context);
            return dateFormat.format(date);
        }else if(numDays > 1 && numDays <7 ){
            return numDays + " days ago";
        }else if(numDays == 1){
            return "Yesterday"; //return numDays + " day ago";
        }else {
            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
            return dateFormat.format(date);
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
}
