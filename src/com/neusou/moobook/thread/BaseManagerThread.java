package com.neusou.moobook.thread;

import java.util.concurrent.CountDownLatch;

import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Message;


import com.neusou.LeadingLooperThread;
import com.neusou.Logger;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;

public abstract class BaseManagerThread extends LeadingLooperThread{
	
	public static final String LOG_TAG = "ManagerThread";
	public static final int CALLBACK_SERVERCALL_ERROR = 600;
	public static final int CALLBACK_TIMEOUT_ERROR = 601;	
	public static final int CALLBACK_PROCESS_WSRESPONSE_ERROR = 602; 
	public static final String XTRA_CALLBACK_INTENT_ACTION = "xtra.mngrthrd.cb.intent.action";
	
	public BaseManagerThread(CountDownLatch cdl) {
		super(cdl);
	}
	
	CountDownLatch cdl;
	
	protected Handler mInHandler = new Handler();
	protected Handler mOutHandler = new Handler();
	
	public ConditionVariable lock = new ConditionVariable();
	static final long LOCK_MAXWAIT = 3000;	

	public Handler getOutHandler(){
		return mOutHandler;
	}
	
	public Handler getInHandler(){
		return mInHandler;
	}
	
	public void setOutHandler(Handler h){
		mOutHandler = h;
		if(h != null){
			lock.open();
		}
	}
	
	public void informWaitOut(){
		lock.block();
	}
	
	public boolean waitOutHandler(){
		if(mOutHandler == null){
			return lock.block(LOCK_MAXWAIT);
		}
		return true;
	}
	
	public abstract void doBusiness(Bundle data, int code, FBWSResponse fbresponse);
	
	@Override
	public void doRun() {
		mInHandler = new Handler(){

			public void handleMessage(android.os.Message msg) {
				int code = msg.what;
				//Log.d(LOG_TAG, name + " handleMessage msg.what:" + msg.what);					
				Bundle data = msg.getData();
				
				if(code == BaseManagerThread.CALLBACK_TIMEOUT_ERROR){						
					if(waitOutHandler()){
						Message msgError =  mOutHandler.obtainMessage(BaseManagerThread.CALLBACK_TIMEOUT_ERROR);
						msgError.setData(data);
						msgError.sendToTarget();
					}
					return;
				}
				
				// check if connection to server was successful
				int servercallstatus = data.getInt(Facebook.XTRA_SERVERCALL_STATUS_CODE);
				
				if(servercallstatus == Facebook.SERVERCALL_ERROR){
					if(waitOutHandler()){
						Message msgError =  mOutHandler.obtainMessage(BaseManagerThread.CALLBACK_SERVERCALL_ERROR);
						msgError.setData(data);
						msgError.sendToTarget();
					}
					return;
				}				
				
				String response = data.getString(Facebook.XTRA_RESPONSE);
				int bytelength = data.getInt(Facebook.XTRA_RESPONSE_BYTELENGTH);	

				Logger.l(Logger.DEBUG,LOG_TAG,"[handleMessage()] response: "+response);
				Logger.l(Logger.DEBUG,LOG_TAG,"[handleMessage()] bytelength: "+bytelength);
				
				
				// Parse the entire JSON response outputted by Facebook webservice endpoint 
				 
				FBWSResponse fbresponse = FBWSResponse.parse(response);
				
				// Send an error code if it has error.
				
				if(fbresponse == null || fbresponse.hasErrorCode){
					if(waitOutHandler()){
						Message m = mOutHandler.obtainMessage(BaseManagerThread.CALLBACK_PROCESS_WSRESPONSE_ERROR);
						m.obj = fbresponse;
						m.sendToTarget();
					}
					return;
				}
				
				doBusiness(data, code, fbresponse);
			}
		};		
	
	}
	
}