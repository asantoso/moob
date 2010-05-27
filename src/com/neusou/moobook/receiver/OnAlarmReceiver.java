package com.neusou.moobook.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.service.AppService;
import com.neusou.moobook.service.WakefulIntentService;

public class OnAlarmReceiver extends BroadcastReceiver{
	static final String LOG_TAG = "OnAlarmReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Logger.l(Logger.DEBUG, LOG_TAG,"[onReceive()] action: "+intent.getAction());
		//App.INSTANCE.setupAlarm();
		
		WakefulIntentService.acquireStaticLock(context);
		Intent serviceIntent = new Intent(context, AppService.class);
		serviceIntent.setAction(intent.getAction());
		context.startService(serviceIntent);
			
	}
	
}