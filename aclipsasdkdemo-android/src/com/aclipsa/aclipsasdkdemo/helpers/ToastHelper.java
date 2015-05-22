package com.aclipsa.aclipsasdkdemo.helpers;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public class ToastHelper {

	/**
	 * Constructs and displays a short toast message
	 * 
	 * @param context
	 * @param value
	 *            String message to display
	 */
	public static void show(Context context, String value) {
		final Toast toast = Toast.makeText(context, value, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
		toast.show();
	}
	
}
