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

public abstract class MoobookThread extends LeadingLooperThread{
	public static final String LOG_TAG = "MoobookThread";
	
	public MoobookThread(CountDownLatch cdl) {
		super(cdl);
	}
		
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
	
	@Override
	public void doRun() {
		mInHandler = new Handler();		
	}
	
}