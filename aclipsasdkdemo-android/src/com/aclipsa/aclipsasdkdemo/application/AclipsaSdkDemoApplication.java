package com.aclipsa.aclipsasdkdemo.application;

import com.aclipsa.aclipsasdk.AclipsaSDK;
import com.aclipsa.aclipsasdk.AclipsaSDK.AclipsaSDKHandler;
import com.aclipsa.aclipsasdk.AclipsaSDKApplication;

public class AclipsaSdkDemoApplication extends AclipsaSDKApplication implements AclipsaSDKHandler {

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		//AclipsaSDK.getInstance(this).register("123098", "516dbfe693316ed822000001", this, "REGISTER");

	}

	@Override
	public void apiRequestResponseSuccess(Object tag, int statusCode,
			String errorString) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void apiRequestResponseFailure(Object tag, int statusCode,
			String errorString) {
		// TODO Auto-generated method stub
		
	}
}
