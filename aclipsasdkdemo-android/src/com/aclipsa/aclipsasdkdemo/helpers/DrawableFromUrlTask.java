package com.aclipsa.aclipsasdkdemo.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;

public class DrawableFromUrlTask extends AsyncTask<String, Void, Drawable> {
	View holder;

	public DrawableFromUrlTask(View view){
		this.holder = view;
	}
	
	@Override
	protected Drawable doInBackground(String... urls) {

		Drawable drawable = null;
		String imageWebAddress = urls[0];

		try
		{
			InputStream inputStream = new URL(imageWebAddress).openStream();
			drawable = Drawable.createFromStream(inputStream, null);
			inputStream.close();
		}
		catch (MalformedURLException ex) { }
		catch (IOException ex) { }

		return drawable;
	}

	@Override
	protected void onPostExecute(Drawable result) {
		holder.setBackground(result);
	}
	
}
