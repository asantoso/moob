package com.neusou.moobook.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.neusou.moobook.Facebook;
import com.neusou.moobook.thread.BaseManagerThread;

public abstract class BaseUiHandler extends Handler {
	
	Context ctx;
	public BaseUiHandler(Context ctx) {
		this.ctx = ctx;
	}
	
	public abstract void onTimeoutError();
	public abstract void onServerCallError();
	
	@Override
	public void handleMessage(Message msg) {
		super.handleMessage(msg);
		
		int code = msg.what;
		Bundle data = msg.getData();
		
		switch(code){
			case BaseManagerThread.CALLBACK_TIMEOUT_ERROR:{
				Toast.makeText(ctx, "Request to Facebook timed out", 2000).show();				
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
				Toast.makeText(this.ctx, errorCode+reason, 2000).show();			
				onServerCallError();
				break;
			}
		}
	}
}