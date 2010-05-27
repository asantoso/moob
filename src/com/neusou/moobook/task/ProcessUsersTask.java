package com.neusou.moobook.task;

import org.apache.commons.lang.NullArgumentException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.neusou.Logger;
import com.neusou.async.UserTask;
import com.neusou.moobook.App;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.FQL;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;

public class ProcessUsersTask extends UserTask<Bundle,ProcessProgressInfo,Integer> {

	static final String LOG_TAG = "ProcessUsersTask";
	
	Handler mOutHandler;
	//Context mContext;	
	
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
	
	public ProcessUsersTask(
			Handler uiHandler,
			int startCode,
			int updateCode,
			int finishCode,
			int progressCode,
			int timeoutCode
			
	) {
		if(uiHandler == null){
			throw new NullArgumentException("uihandler can't be null");
		}
		
		//mContext = ctx;
		mOutHandler = uiHandler;
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
		mOutHandler.sendEmptyMessage(mStartCode);
	}
	
	@Override
	protected void onPostExecute(Integer result) {	
		super.onPostExecute(result);
		Logger.l(Logger.DEBUG, LOG_TAG, "[onPostExecute()]");
		
		mOutHandler.sendEmptyMessage(mFinishCode);
	}
		
	@Override
	protected void onTimeout(){
		super.onTimeout();
		mOutHandler.sendEmptyMessage(mTimeoutCode);		
	}
	
	@Override
	protected void onProgressUpdate(ProcessProgressInfo... values) {	
		super.onProgressUpdate(values);
		Message m = mOutHandler.obtainMessage(mProgressCode);
		m.obj = values[0];
		m.sendToTarget();	
	}
	
	@Override
	protected Integer doInBackground(Bundle... params) {
		Logger.l(Logger.DEBUG, LOG_TAG, "[doInBackground()] ");
		Bundle data = params[0];		
		String response = data.getString(Facebook.XTRA_RESPONSE);
				
		FBWSResponse fbresponse = FBWSResponse.parse(response);		
		JSONArray users;
		
		try{
			users = fbresponse.jsonArray.getJSONObject(0).getJSONArray(FQL.FQL_RESULT_SET);
		}catch(JSONException e){
			users = new JSONArray();			
		}
		
		int num = users.length();
		Log.d(LOG_TAG,"num users: "+num);
		User user = new User();
		
		short[] selection = data.getShortArray(Facebook.XTRA_TABLECOLUMNS_SHORTARRAY);		
		
		assert(selection != null);
		
		JSONObject currentItem;	
				
		App.mDB.beginTransaction();
		
		for(int i=0,j=mPeriodicUIUpdateInterval;i<num && mStatus == Status.RUNNING;i++){
			try {								
				currentItem = users.getJSONObject(i);
				Log.d(LOG_TAG,""+currentItem.toString());
				user.parse(currentItem, selection);
				
				long rowId = App.mDBHelper.insertUser(user,selection,App.mDB);
				Log.d(LOG_TAG, "user new row id: "+rowId);
				
				mProgressInfo.current = i+1;
				mProgressInfo.total = num;
				
				// send an UI update message
				if(mPeriodicUIUpdateEnabled){
					j--;
					if(j == 0){
						j = mPeriodicUIUpdateInterval;
						Log.d(LOG_TAG,"sending ui update");
						mOutHandler.sendEmptyMessage(mUpdateCode);
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
		
		App.mDB.setTransactionSuccessful();		
		App.mDB.endTransaction();
		
		
				
		return null;
	}
	
	
}