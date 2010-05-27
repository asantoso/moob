package com.neusou.moobook.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.activity.LoginActivity;

public class InternalReceiver extends BroadcastReceiver{
	static final String LOG_TAG = "InternalReceiver";

	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();		
		Logger.l(Logger.DEBUG, LOG_TAG,"[onReceive()] "+action);
		if(action.equals(App.INTENT_LOGIN)){
			Intent loginIntent = new Intent(App.INSTANCE, LoginActivity.class);
			//loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			//App.INSTANCE.startActivity(loginIntent);
		}		
	}
	
}