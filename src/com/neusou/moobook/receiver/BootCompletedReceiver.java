package com.neusou.moobook.receiver;

import com.neusou.Logger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class BootCompletedReceiver extends BroadcastReceiver{
	static final String LOG_TAG = "BootCompletedReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		Logger.l(Logger.DEBUG, LOG_TAG,"[onReceive()]");
		/*
		long interval = 5000l;
		
		AlarmManager am = (AlarmManager) context.getSystemService(Service.ALARM_SERVICE);
		Intent i = new Intent(context, OnAlarmReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context,0, i, 0);
		am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime(),interval,pi);
		*/				
	}
	
}