package com.neusou.moobook.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.neusou.Logger;
import com.neusou.moobook.App;

public class CommonActivityReceiver extends BroadcastReceiver {
	
	static final String LOG_TAG = CommonActivityReceiver.class.getCanonicalName();
	
	public interface IBaseReceiver{
		public void showSessionUserData();
		public void runOnUiThread(Runnable r);
	}
	
	IBaseReceiver act;
	public CommonActivityReceiver(IBaseReceiver act) {
		this.act = act; 
	}
	
	public void selfRegister(Activity act){
		IntentFilter intentFilter = new IntentFilter(App.INTENT_SESSIONUSER_PROFILE_UPDATED);
		act.registerReceiver(this, intentFilter);
	}
	
	public void selfUnregister(Activity act){
		try{
			act.unregisterReceiver(this);}
		catch (IllegalArgumentException e) {
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		//Logger.l(Logger.DEBUG,LOG_TAG,"[BroadcastReceiver] [onReceive()] action: "+intent.getAction());
		String threadName = Thread.currentThread().getName();
		String action = intent.getAction();
		Bundle data = intent.getExtras();
		Logger.l(Logger.DEBUG,LOG_TAG,"[BroadcastReceiver] [onReceive()] thread:"+threadName+", action:"+action);
		if(action.equals(App.INTENT_SESSIONUSER_PROFILE_UPDATED)){
			this.act.runOnUiThread(new Runnable(){
				public void run() {							
					act.showSessionUserData();
				};
				;	
			});					
		}
		
	}
	
}