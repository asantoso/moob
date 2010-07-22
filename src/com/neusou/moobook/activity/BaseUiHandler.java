package com.neusou.moobook.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.thread.BaseManagerThread;

public abstract class BaseUiHandler extends Handler {
	
	protected Activity mContext;
	
	public static final int mMessageDuration = 2000;
	
	public BaseUiHandler(Activity ctx) {
		this.mContext = ctx;
	}
	
	public abstract void onTimeoutError();
	public abstract void onServerCallError();
	public abstract void onWsResponseError();
	
	
	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
		
		int code = msg.what;
		Bundle data = msg.getData();
		
		switch(code){
			case BaseManagerThread.CALLBACK_PROCESS_WSRESPONSE_ERROR:{				
				FBWSResponse fbresponse = (FBWSResponse) msg.getData().getParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT);
				Toast.makeText(mContext,fbresponse.errorMessage , mMessageDuration).show();
				onWsResponseError();	
				break;
			}			
			case BaseManagerThread.CALLBACK_TIMEOUT_ERROR:{
				Toast.makeText(mContext, "Request to Facebook timed out", mMessageDuration).show();				
				onTimeoutError();
				break;
			}			
			case BaseManagerThread.CALLBACK_SERVERCALL_ERROR:{					
				String reason = (String)data.getString(Facebook.XTRA_SERVERCALL_ERROR_MSG);
				String errorCode = ""; 
				if(data.containsKey(Facebook.XTRA_SERVERCALL_ERROR_CODE)){
					errorCode = Integer.toString(data.getInt(Facebook.XTRA_SERVERCALL_ERROR_CODE));
					errorCode+=":";
				}
				Toast.makeText(this.mContext, errorCode+reason,  mMessageDuration).show();			
				onServerCallError();
				break;
			}
		}
	}
}