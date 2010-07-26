package com.neusou.moobook.task;

import org.apache.commons.lang.NullArgumentException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
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

	static final String LOG_TAG = Logger.registerLog(ProcessUsersTask.class);
	
	
	Handler mOutHandler;
	//Context mContext;	
	
	int mUpdateCode;
	int mFinishCode;
	int mProgressCode;
	int mStartCode;
	int mTimeoutCode;
	int mProcessFlag;
	
	
	ProcessProgressInfo mProgressInfo;
	
	public ProcessUsersTask(){
		
	}
	
	public ProcessUsersTask(
			Handler uiHandler,
			int startCode,
			int updateCode,
			int finishCode,
			int progressCode,
			int timeoutCode,
			int processFlag
			
	) {
		init(uiHandler,startCode,updateCode,finishCode,progressCode,timeoutCode,processFlag);
	}
	
	public void init(
			Handler uiHandler,
			int startCode,
			int updateCode,
			int finishCode,
			int progressCode,
			int timeoutCode,
			int processFlag
			
	) {
		
		if(uiHandler == null){
			throw new NullArgumentException("uihandler can't be null");
		}
		
		mProcessFlag = processFlag;
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
		ProcessTaskFactory.finish(ProcessUsersTask.class);
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
		Logger.l(Logger.DEBUG, LOG_TAG,"num users: "+num);
		User user = new User();
		
		short[] selection = data.getShortArray(Facebook.XTRA_TABLECOLUMNS_SHORTARRAY);		
		
		assert(selection != null);
		
		JSONObject currentItem;	
				
		App.INSTANCE.mDB.beginTransaction();
		
		//determine if we want to save the processFlag field.
		//don't save flag if it is not session user
		int processFlag = App.PROCESS_FLAG_IGNORE;
		if(mProcessFlag == App.PROCESS_FLAG_USER_CONNECTED){
			processFlag = mProcessFlag;			
		}
		
		for(int i=0;i<num && mStatus == Status.RUNNING;i++){
			try {								
				currentItem = users.getJSONObject(i);
				//Logger.l(Logger.DEBUG, LOG_TAG,"processUsersTasl:  flag:"+mProcessFlag);
				user.parse(currentItem, selection);
				user._process_flag = processFlag;
				
				long rowId = App.INSTANCE.mDBHelper.insertUser(user, selection,App.INSTANCE.mDB);
				Logger.l(Logger.DEBUG, LOG_TAG, "user new row id: "+rowId);				
				
			} 
			catch (JSONException e) {
				e.printStackTrace();				
			}
			finally{
				
			}
		}	
		
		App.INSTANCE.mDB.setTransactionSuccessful();		
		App.INSTANCE.mDB.endTransaction();
		
		
				
		return null;
	}
	
	
}