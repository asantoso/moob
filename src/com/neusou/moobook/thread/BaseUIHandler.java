/*package com.neusou.moobook.thread;

import com.neusou.Logger;
import com.neusou.moobook.Facebook;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class BaseUIHandler extends Handler {
	static final String LOG_TAG = "BaseUIHandler";
	
	Activity mActivity;
	
	public BaseUIHandler(Activity act){
		mActivity = act;
	}
	public void handleMessage(Message msg) {
		int code = msg.what;
		Bundle data = msg.getData();
		Logger.l(Logger.DEBUG,LOG_TAG,"[UIHandler][handleMessage()] code: "+code);
		super.handleMessage(msg);				
		switch(code){
			case BaseManagerThread.CALLBACK_TIMEOUT_ERROR:{				
				Toast.makeText(mActivity, "Request to Facebook timed out", 2000).show();
				Logger.l(Logger.WARN,LOG_TAG,"[UIHandler] [handleMessage()] remote request timed out.");
				break;
			}
			case BaseManagerThread.CALLBACK_SERVERCALL_ERROR:{	
				String reason = (String)data.getString(Facebook.XTRA_SERVERCALL_ERROR_MSG);
				int errorCode = data.getInt(Facebook.XTRA_SERVERCALL_ERROR_CODE);
				Logger.l(Logger.ERROR,LOG_TAG,"[UIHandler] [handleMessage()] failed invoking remote request. error: "+errorCode+", reason:"+reason);
				Toast.makeText(mActivity, errorCode+":"+reason, 2000).show();
				break;
			}					
		}
	}
}
*/