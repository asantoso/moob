package com.neusou.moobook.receiver;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends IntentService{
	static final String LOG_TAG = "AlarmReceiver";
	
	public AlarmReceiver(String name) {
		super(name);
	
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(LOG_TAG, "onHandleIntent");
		
	}	
	
}