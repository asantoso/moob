/*
 package com.neusou.moobook;
 

import java.util.concurrent.CountDownLatch;

import com.neusou.LeadingLooperThread;
import com.neusou.Logger;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Message;


public abstract class ManagerThread extends LeadingLooperThread{
	
	public ManagerThread(CountDownLatch cdl) {
		super(cdl);
	}

	public static final String LOG_TAG = "CountdownThread";
	

	public static final int CALLBACK_GET_FRIENDS = 1;
	public static final int CALLBACK_GET_ONEUSER_DATA = 2;
	public static final int CALLBACK_GET_STREAMS_MQ = 3;
	public static final int CALLBACK_GET_TAGGED_PHOTOS = 4;
	public static final int CALLBACK_GET_PHOTO_TAGS = 5;
	public static final int CALLBACK_GET_COMMENTS_MULTIQUERY = 6; 	
	public static final int CALLBACK_POSTCOMMENT = 7;	
	public static final int CALLBACK_DELETECOMMENT = 8;
	public static final int CALLBACK_GET_COMMENTS = 9;
	public static final int CALLBACK_GET_COMMENTS_USERS = 10;
	public static final int CALLBACK_GET_USERDATA = 11;
	public static final int CALLBACK_GET_ALBUMS = 12;
	public static final int CALLBACK_POSTPHOTO = 13;
	public static final int CALLBACK_LOGIN_TO_FACEBOOK = 14;
	public static final int CALLBACK_GET_WALLPOSTS = 15; 
		
	public static final int MESSAGE_UPDATE_TAGS = 30;	
	public static final int MESSAGE_DISMISS_DIALOG = 31;	
	
	public static final int CALLBACK_SESSION_EXPIRED = 50;
	public static final int CALLBACK_SESSION_VALID = 51;
	public static final int CALLBACK_SESSION_VALIDATED =53;
	
	public static final int CALLBACK_SERVERCALL_ERROR = 600;
	public static final int CALLBACK_TIMEOUT_ERROR = 601;	
	public static final int CALLBACK_PROCESS_WSRESPONSE_HAS_ERRORCODE = 602; 
	
	public static final int CALLBACK_ADMOB_ONFAILRECEIVE = 700;					
	public static final int CALLBACK_ADMOB_ONRECEIVE = 701;
	
	CountDownLatch cdl;
	
	protected Handler mInHandler = new Handler();
	protected Handler mOutHandler = new Handler();
	public ConditionVariable lock = new ConditionVariable();
	static final long LOCK_MAXWAIT = 3000;	

	public void setOutHandler(Handler h){
		mOutHandler = h;
		if(h != null){
			lock.open();
		}
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
			final private String name = "CallbackHandler";
			public void handleMessage(android.os.Message msg) {
				int code = msg.what;
				//Log.d(LOG_TAG, name + " handleMessage msg.what:" + msg.what);					
				Bundle data = msg.getData();
				
				if(code == ManagerThread.CALLBACK_TIMEOUT_ERROR){						
					if(waitOutHandler()){
						Message msgError =  mOutHandler.obtainMessage(ManagerThread.CALLBACK_TIMEOUT_ERROR);
						msgError.setData(data);
						msgError.sendToTarget();
					}
					return;
				}
				
				// check if connection to server was successful
				int servercallstatus = data.getInt(Facebook.XTRA_SERVERCALL_STATUS_CODE);
				
				if(servercallstatus == Facebook.SERVERCALL_ERROR){
					if(waitOutHandler()){
						Message msgError =  mOutHandler.obtainMessage(ManagerThread.CALLBACK_SERVERCALL_ERROR);
						msgError.setData(data);
						msgError.sendToTarget();
					}
					return;
				}				
				
				String response = data.getString(Facebook.XTRA_RESPONSE);
				int bytelength = data.getInt(Facebook.XTRA_RESPONSE_BYTELENGTH);	

				Logger.l(Logger.DEBUG,LOG_TAG,"[WorkerManagerThread] [handleMessage()] response: "+response);
				Logger.l(Logger.DEBUG,LOG_TAG,"[WorkerManagerThread] [handleMessage()] bytelength: "+bytelength);
				
				
				// Parse the entire JSON response outputted by Facebook webservice endpoint 
				 
				FBWSResponse fbresponse = FBWSResponse.parse(response);
				
				// Send an error code if it has error.
				
				if(fbresponse == null || fbresponse.hasErrorCode){
					if(waitOutHandler()){
						Message m = mOutHandler.obtainMessage(ManagerThread.CALLBACK_PROCESS_WSRESPONSE_HAS_ERRORCODE);
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
*/