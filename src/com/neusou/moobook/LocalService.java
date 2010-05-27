package com.neusou.moobook;

import com.neusou.Logger;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class LocalService extends Service {

    public static final String LOG_TAG = "LocalService";

    public class LocalBinder extends Binder {
        LocalService getService() {
            return LocalService.this;
        }
    }
    
    @Override
    public void onCreate() {
    	Logger.l(Logger.DEBUG, LOG_TAG, "[onCreate()]");
    }

    @Override
    public void onDestroy() {
    	Logger.l(Logger.DEBUG, LOG_TAG, "[onDestroy()]");
    }

    @Override
    public IBinder onBind(Intent intent) {
    	String action = intent.getAction();
    	Logger.l(Logger.DEBUG, LOG_TAG, "[onBind()] action: "+action);
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	String action = "";
    	if(intent!=null){
    		action = intent.getAction();
    	}
    	Logger.l(Logger.DEBUG, LOG_TAG, "[onStartCommand()] action: " + action);
    	return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
    	Logger.l(Logger.DEBUG, LOG_TAG, "[onStart()] action: "+intent.getAction());
    	super.onStart(intent, startId);
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
    	Logger.l(Logger.DEBUG, LOG_TAG, "[onUnbind()] action: "+intent.getAction());
    	return super.onUnbind(intent);
    }
    
    @Override
    public void onRebind(Intent intent) {
    	Logger.l(Logger.DEBUG, LOG_TAG, "[onRebind()] action: "+intent.getAction());
    	super.onRebind(intent);
    }
}

