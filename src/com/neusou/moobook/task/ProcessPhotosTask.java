package com.neusou.moobook.task;

import org.apache.commons.lang.NullArgumentException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.data.Stream;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.async.UserTask;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ProcessPhotosTask extends UserTask<Bundle,ProcessProgressInfo,Integer> {

	static final String LOG_TAG = "ProcessStreamTask";
	
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
	
	public ProcessPhotosTask(Context ctx, Handler uiHandler,
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
	
	@Override
	protected Integer doInBackground(Bundle... params) {
		Bundle data = params[0];
		ApplicationDBHelper dbHelper = new ApplicationDBHelper(mContext);
		String response = data.getString(Facebook.XTRA_RESPONSE);
				
		FBWSResponse fbresponse = FBWSResponse.parse(response);		
		JSONArray streams = fbresponse.jsonArray;
		int num = streams.length();
		Log.d(LOG_TAG,"num stream posts: "+num);
		Stream stream = new Stream();
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
	//	short[] selection = data.getShortArray(Facebook.XTRA_TABLECOLUMNS_SHORTARRAY);
		//if(selection == null){
			//Log.e(LOG_TAG,"no columns selection");
		//}

		for(int i=0,j=mPeriodicUIUpdateInterval;i<num && getStatus() == Status.RUNNING;i++){
			JSONObject itemJson;	
			
			try {								
				itemJson = streams.getJSONObject(i);
				stream.post_id = itemJson.getString(Stream.cn_post_id);
				stream.attribution = itemJson.getString(Stream.cn_attribution);
				stream.updated_time = itemJson.getLong(Stream.cn_updated_time);				
				stream.message = itemJson.getString(Stream.cn_message);
				stream.source_id = itemJson.getLong(Stream.cn_source_id);				
				try{stream.target_id = itemJson.getString(Stream.cn_target_id);}catch(JSONException e){}
				stream.actor_id = itemJson.getLong(Stream.cn_actor_id);
							
				
				JSONObject likesObj;
				try{
					likesObj = itemJson.getJSONObject("likes");
					if(likesObj != null){
						
					
								
					try{
						stream.likes_count = likesObj.getLong("count");
					}catch(JSONException e){
						stream.likes_count = 0;
					}
				
					try{
						stream.likes_userlikes = Boolean.parseBoolean(likesObj.getString("user_likes"));
					}catch(JSONException e){
						stream.likes_userlikes = false;
					}
				
					try{
						stream.likes_canlike = Boolean.parseBoolean(likesObj.getString("can_like"));
					}catch(JSONException e){
						stream.likes_canlike = false;
					}
					}
				
				}
				catch(JSONException e){					
				}
				
				long rowId = dbHelper.insertStream(stream, db);
				Log.d(LOG_TAG, "streampost new row id: "+rowId);

				// intermittent listview update per specified number of new items
				
				if(mPeriodicUIUpdateEnabled){
					j--;
					if(j == 0){
						j = mPeriodicUIUpdateInterval;
						Log.d(LOG_TAG,"sending ui update");
						mUIHandler.sendEmptyMessage(mUpdateCode);
					}	
				}

				if(i == num-1){
					mUIHandler.sendEmptyMessage(mFinishCode);
				}else{
					mProgressInfo.current = i+1;
					mProgressInfo.total = num;
					publishProgress(mProgressInfo);
				}
				
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
			finally{
			}
		}	
		
		db.close();
		dbHelper.close();	
				
		return null;
	}
	
	
}