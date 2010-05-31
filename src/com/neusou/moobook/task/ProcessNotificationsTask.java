/*
package com.neusou.moobook.task;

import java.util.HashMap;

import org.apache.commons.lang.NullArgumentException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.neusou.moobook.App;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.data.FBNotification;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.async.UserTask;

public class ProcessNotificationsTask extends UserTask<Bundle, ProcessProgressInfo, Integer> {

	static final String LOG_TAG = "ProcessNotificationsTask";

//////////////////////////////////////////////////////
	Object uiHandlerLock = new Object();
	transient boolean isUiHandlerConnected = true;	
	long mUiHandlerSignature = 0;
	public void setUiHandlerConnection(long signature){
		this.mUiHandlerSignature = signature;
	}
	public void disconnectUiHandler(){
		this.isUiHandlerConnected = false;
	}	
	public void connectUiHandler(Handler h, long signature){
		if(signature == this.mUiHandlerSignature){
			this.mUIHandler = h;
			this.isUiHandlerConnected = true;
			synchronized (uiHandlerLock) {
				uiHandlerLock.notify();
			}
		}		
	}	
	private boolean makeSureUiHandlerIsConnected(){
		synchronized (uiHandlerLock) {
			if(!isUiHandlerConnected){
				try {
					uiHandlerLock.wait();
				} catch (InterruptedException e) {				
					e.printStackTrace();
					return true;
				}
			}
			return true;
		}		
	}
	
	// concurrency control
	
	
	Context mContext;
	public Handler mUIHandler;
	int mUpdateCode;
	int mFinishCode;
	int mStartCode;
	int mTimeoutCode;
	int mProgressCode;
	ProcessProgressInfo mProgressInfo;
	
	ApplicationDBHelper dbHelper = App.INSTANCE.mDBHelper;
	SQLiteDatabase db = App.INSTANCE.mDB;

	JSONArray senders;
	JSONArray notifications;
	
	public ProcessNotificationsTask(Context ctx, Handler uiHandler,
			int startCode, int updateCode, int finishCode, int progressCode, int timeoutCode) throws NullArgumentException {
		if (uiHandler == null) {
			throw new NullArgumentException("uihandler can't be null");
		}
		if (ctx == null) {
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
		makeSureUiHandlerIsConnected();
		if(mUIHandler != null){
			mUIHandler.sendEmptyMessage(mStartCode);
		}
	}

	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);

		
		makeSureUiHandlerIsConnected();
		if(mUIHandler != null){
			Message msg = mUIHandler.obtainMessage(mFinishCode);
			msg.sendToTarget();
		}
		
	}

	@Override
	protected void onTimeout() {
		super.onTimeout();
	
		if(mUIHandler != null){
			mUIHandler.sendEmptyMessage(mTimeoutCode);
		}
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
	
	}

	@Override
	protected void onProgressUpdate(ProcessProgressInfo... values) {
		super.onProgressUpdate(values);
		makeSureUiHandlerIsConnected();
		
		if(mUIHandler != null){
			Message m = mUIHandler.obtainMessage(mProgressCode);		
			m.obj = values[0];
			m.sendToTarget();
		}
	}

	private HashMap<Long,String> processSenders(JSONArray data){
		HashMap<Long, String> userMaps = new HashMap<Long,String>();
		if (data == null) {
			return null;
		}
		int count = data.length();
		if(count == 0){
			return null;
		}
		for(int i=0;i<count;i++){
			try{
				JSONObject user = data.getJSONObject(i);
				String name = user.getString("name");
				Log.d(LOG_TAG,"name: "+name);
				long uid = user.getLong("id");
				userMaps.put(uid, name);
			}catch(JSONException e){				
			}
		}
		return userMaps;
	}
	
	private void processNotifications(JSONArray data, HashMap<Long,String> usernames) {
		if (data == null) {
			return;
		}
		
		int num = data.length();
		FBNotification notif = new FBNotification();
		

		for (int i = 0; i < num	&& getStatus() == Status.RUNNING; i++) {
			JSONObject itemJson;
			
			try {
				itemJson = data.getJSONObject(i);
								
				notif.notification_id = itemJson.getLong(FBNotification.cn_notification_id);
				notif.body_text = itemJson.getString(FBNotification.fields_body_text);
				notif.title_text = itemJson.getString(FBNotification.fields_title_text);
				notif.created_time = itemJson.getLong(FBNotification.fields_created_time);
				notif.href = itemJson.getString(FBNotification.fields_href);
				notif.sender_id = itemJson.getLong(FBNotification.fields_sender_id);
				notif.app_id = itemJson.getLong(FBNotification.fields_app_id);
				if (mStatus == Status.RUNNING && db != null && db.isOpen()) {
					dbHelper.insertNotification(notif, db);
				} else {					
					return;
				}		

			} catch (JSONException e) {
				e.printStackTrace();
			} finally {
			}
		}		
	}
			
	@Override
	protected Integer doInBackground(Bundle... params) {
		Bundle data = params[0];
		String response = data.getString(Facebook.XTRA_RESPONSE);
	
		FBWSResponse fbresponse = FBWSResponse.parse(response);
		JSONArray responseArray = fbresponse.jsonArray;

		Log.d(LOG_TAG, "doInBackground...");
		
		for (int i = 0; i < responseArray.length(); i++) {
			try {
				JSONObject entry = responseArray.getJSONObject(i);
				String name = entry.getString("name");

				JSONArray result_set = null;

				boolean isSuccessParseResultSet = false;

				try {
					result_set = entry.getJSONArray("fql_result_set");
					isSuccessParseResultSet = true;
				} catch (JSONException e) {
				}

				if (isSuccessParseResultSet) {
					if (name.compareTo("notifications") == 0) {
						notifications = result_set;
					}
					else if (name.compareTo("senders") == 0) {
						senders = result_set;
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		db.beginTransaction();
		HashMap<Long,String> userMaps = processSenders(senders);
		processNotifications(notifications, userMaps);
		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
		dbHelper.close();
		return null;
	}

	
}*/