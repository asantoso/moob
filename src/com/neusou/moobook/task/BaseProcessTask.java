package com.neusou.moobook.task;

import org.apache.commons.lang.NullArgumentException;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.neusou.async.UserTask;

public abstract class BaseProcessTask extends UserTask<Bundle,ProcessProgressInfo,Integer> {

	protected static final String LOG_TAG = "ProcessStreamTask";
	
	Handler mUIHandler;
	Context mContext;	
	
	int mUpdateCode;
	int mFinishCode;
	int mStartCode;
	int mTimeoutCode;
	int mProgressCode;
	int mPeriodicUIUpdateInterval = 5;
	boolean mPeriodicUIUpdateEnabled = false;
	ProcessProgressInfo mProgressInfo;
	
	public void setPeriodicUIUpdateEnabled(boolean enabled){
		mPeriodicUIUpdateEnabled = enabled;
	}
	
	public void setPeriodicUIUpdateInterval(int interval){
		mPeriodicUIUpdateInterval = interval;
	}
	
	public BaseProcessTask(Context ctx, Handler uiHandler,
			int startCode,
			int updateCode,
			int finishCode,
			int progressCode,
			int timeoutCode
	) throws NullArgumentException{
		if(uiHandler == null){
			throw new NullArgumentException("uihandler can't be null");
		}
		if(ctx == null){
			throw new NullArgumentException("context can't be null");
		}
				
		mContext = ctx;
		mUIHandler = uiHandler;
		
		mStartCode = startCode;
		mUpdateCode = updateCode;
		mFinishCode = finishCode;
		mTimeoutCode = timeoutCode;
		mProgressCode = progressCode;
		
		mProgressInfo = new ProcessProgressInfo();
	}

	@Override
	protected void onPreExecute() {	 
		super.onPreExecute();		
		mUIHandler.sendEmptyMessage(mStartCode);
	}
	
	@Override
	protected void onTimeout(){
		super.onTimeout();
		mUIHandler.sendEmptyMessage(mTimeoutCode);		
	}
	
	@Override
	protected void onProgressUpdate(ProcessProgressInfo... values) {	
		super.onProgressUpdate(values);
		Message m = mUIHandler.obtainMessage(mProgressCode);
		m.obj = values[0];
		m.sendToTarget();	
	}
	
}