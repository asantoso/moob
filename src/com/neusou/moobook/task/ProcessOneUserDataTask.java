package com.neusou.moobook.task;

import org.apache.commons.lang.NullArgumentException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.async.UserTask;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.Log;

public class ProcessOneUserDataTask extends UserTask<Bundle,ProcessProgressInfo,Integer> {

	static final String LOG_TAG = "ProcessOneUserTask";
	
	Handler mUIHandler;
	Context mContext;	
	
	int mUpdateCode;
	int mFinishCode;
	int mProgressCode;
	int mStartCode;
	int mTimeoutCode;
	
	int mPeriodicUIUpdateInterval = 5;
	boolean mPeriodicUIUpdateEnabled = false;
	
	ProcessProgressInfo mProgressInfo;
	
	public void setPeriodicUIUpdateEnabled(boolean enabled){
		mPeriodicUIUpdateEnabled = enabled;
	}
	
	public void setPeriodicUIUpdateInterval(int interval){
		mPeriodicUIUpdateInterval = interval;
	}
	
	public ProcessOneUserDataTask(Context ctx, Handler uiHandler,
			int startCode,
			int updateCode,
			int finishCode,
			int progressCode,
			int timeoutCode
			
	) {
		if(uiHandler == null){
			throw new NullArgumentException("uihandler can't be null");
		}
		if(ctx == null){
			throw new NullArgumentException("context can't be null");
		}
	
		mContext = ctx;
		mUIHandler = uiHandler;
		mProgressInfo = new ProcessProgressInfo();
		
		mStartCode = startCode;
		mUpdateCode = updateCode;
		mFinishCode = finishCode;
		mTimeoutCode = timeoutCode;
		mProgressCode = progressCode;
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
	
	@Override
	protected Integer doInBackground(Bundle... params) {
		
		Bundle data = params[0];
		ApplicationDBHelper dbHelper = new ApplicationDBHelper(mContext);
		String response = data.getString(Facebook.XTRA_RESPONSE);
				
		FBWSResponse fbresponse = FBWSResponse.parse(response);		
		JSONArray users = fbresponse.jsonArray;
		int num = users.length();
		Log.d(LOG_TAG,"num users: "+num);
		User user = new User();
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		short[] selection = data.getShortArray(Facebook.XTRA_TABLECOLUMNS_SHORTARRAY);		
		
		assert(selection != null);
		
		JSONObject currentItem;	
				
		db.beginTransaction();
		
		for(int i=0,j=mPeriodicUIUpdateInterval;i<num && mStatus == Status.RUNNING;i++){
			try {								
				currentItem = users.getJSONObject(i);
				user.parse(currentItem, selection);
				
				user.type = User.TYPE_USER;
				long rowId = dbHelper.insertUser(user,selection,db);
				Log.d(LOG_TAG, "user new row id: "+rowId);
				
				mProgressInfo.current = i+1;
				mProgressInfo.total = num;
				
				// send an UI update message
				if(mPeriodicUIUpdateEnabled){
					j--;
					if(j == 0){
						j = mPeriodicUIUpdateInterval;
						Log.d(LOG_TAG,"sending ui update");
						mUIHandler.sendEmptyMessage(mUpdateCode);
					}
				}
				
				
				publishProgress(mProgressInfo);
				
			} 
			catch (JSONException e) {
				e.printStackTrace();				
			}
			finally{
				
			}
		}	
		
		db.setTransactionSuccessful();		
		db.endTransaction();
		db.close();
		dbHelper.close();
		
		mUIHandler.sendEmptyMessage(mFinishCode);
				
		return null;
	}
	
	
}