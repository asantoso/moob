package com.neusou.moobook.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.neusou.DecoupledHandlerThread;
import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.FBWSResponse;

public class SessionUserInfoReceiver extends BroadcastReceiver{
	static final String LOG_TAG = SessionUserInfoReceiver.class.getCanonicalName();

	public SessionUserInfoReceiver() {
		
	}
	@Override
	public void onReceive(final Context context, final Intent intent) {
		String threadName = Thread.currentThread().getName();
		Logger.l(Logger.DEBUG, LOG_TAG,"[onReceive()] threadName:"+threadName+", action:"+intent.getAction());
		Runnable r = new Runnable(){
			@Override
			public void run() {
				FBWSResponse response = (FBWSResponse) intent.getExtras().getParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT);
				App.saveUserSessionData(response.data);				
				Intent i = new Intent();
				i.setAction(App.INTENT_SESSIONUSER_PROFILE_UPDATED);
				context.sendBroadcast(i);				
			}
		};		
		DecoupledHandlerThread dch = new DecoupledHandlerThread();
		dch.start();
		dch.h.post(r);		
	}
	
}