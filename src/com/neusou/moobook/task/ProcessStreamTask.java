package com.neusou.moobook.task;

import org.apache.commons.lang.NullArgumentException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.data.Comment;
import com.neusou.moobook.data.Stream;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.async.UserTask;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ProcessStreamTask extends com.neusou.async.UserTask<Bundle,ProcessProgressInfo,Integer> {

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
				notify();
			}
		}		
	}	
	private boolean makeSureUiHandlerIsConnected(){
		synchronized (uiHandlerLock) {
			if(!isUiHandlerConnected){
				try {
					wait();
				} catch (InterruptedException e) {				
					e.printStackTrace();
					return true;
				}
			}
			return true;
		}		
	}
	///////////////////////////////////////////////////
	
	public ProcessStreamTask(Context ctx, Handler uiHandler,
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
		makeSureUiHandlerIsConnected();
		this.mUIHandler.sendEmptyMessage(mStartCode);
	}
	
	@Override
	protected void onTimeout(){
		super.onTimeout();
		makeSureUiHandlerIsConnected();
		this.mUIHandler.sendEmptyMessage(mTimeoutCode);
	}
	
	@Override
	protected void onProgressUpdate(ProcessProgressInfo... values) {	
		super.onProgressUpdate(values);
		//makeSureUiHandlerIsConnected();
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
		String attachmentJSON;
		int num = streams.length();
		Log.d(LOG_TAG,"num stream posts: "+num);
		Stream stream = new Stream();
		Comment comment = new Comment();
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
	//	short[] selection = data.getShortArray(Facebook.XTRA_TABLECOLUMNS_SHORTARRAY);
		//if(selection == null){
			//Log.e(LOG_TAG,"no columns selection");
		//}

		db.beginTransaction();
		
		for(int i=0,j=mPeriodicUIUpdateInterval;i<num && getStatus() == Status.RUNNING;i++){
			JSONObject itemJson;	
			
			try {								
				itemJson = streams.getJSONObject(i);
				stream.post_id = itemJson.getString(Stream.cn_post_id);
				stream.attribution = itemJson.getString(Stream.cn_attribution);
				stream.updated_time = itemJson.getLong(Stream.cn_updated_time);				
				stream.message = itemJson.getString(Stream.cn_message);
				stream.source_id = itemJson.getLong(Stream.cn_source_id);				
				
				try{
					stream.target_id = itemJson.getString(Stream.cn_target_id);
				}
				catch(JSONException e){}
				
				stream.actor_id = itemJson.getLong(Stream.cn_actor_id);
							
				//process attachment
				JSONObject att = itemJson.getJSONObject(Stream.cn_attachment);
				attachmentJSON = att.toString();
				Log.d("ProcessStreamTask","extracted attachment json: "+attachmentJSON);
				stream.attachment = attachmentJSON;				
				
				//process likes
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
				//end of process likes
				
				//begin process comments				
				JSONArray commentsArray = null;
				comment = new Comment();
				
				boolean hasCommentsProperty = false;
				JSONObject commentsObj = null;
				try{
					commentsObj = itemJson.getJSONObject("comments");
					hasCommentsProperty = true;
				}
				catch(JSONException e){					
				}
				
				/*
				
				if(hasCommentsProperty){//begin process comments 			
									
						try{
							commentsArray = commentsObj.getJSONArray("comment_list");
						}catch(JSONException e){
							
						}
					
						JSONObject commentObj;
						if(commentsArray != null){				
							int numComments = commentsArray.length();
							Log.d(LOG_TAG,"num of comments: "+numComments);
							
							//comment attributes
							//http://wiki.developers.facebook.com/index.php/Comment_%28FQL%29
						
							db.beginTransaction();
							
							try{
								for(int ci=0;ci<numComments;ci++){					
									commentObj = commentsArray.getJSONObject(ci);
									comment.post_id = stream.post_id;
									comment.from_id = commentObj.getLong("fromid");
									comment.time = commentObj.getLong("time");
									comment.comment = commentObj.getString("text");		
									comment.comment_id = commentObj.getString("id");
							
									long commentRowId = dbHelper.insertComment(comment, db);
									Log.d("agus", "new comment with post_id: "+stream.post_id);
									Log.d("agus", "new comment row id: "+commentRowId); 
								}
								db.setTransactionSuccessful();
							}
							catch(Exception e){								
							}
							finally{
								db.endTransaction();	
							}
						}else{
							Log.d(LOG_TAG,"no comments");
						}
				}//end of process comments
				*/
				long rowId = dbHelper.insertStream(stream, db);
				Log.d(LOG_TAG, "[doInBackground()] streampost new row id: "+rowId);

				// send an UI update message
				
				if(mPeriodicUIUpdateEnabled){
					j--;
					if(j == 0){
						j = mPeriodicUIUpdateInterval;
						//makeSureUiHandlerIsConnected();
						Log.d(LOG_TAG,"sending ui update");
						mUIHandler.sendEmptyMessage(mUpdateCode);
					}	
				}

				if(i == num-1){
					makeSureUiHandlerIsConnected();
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
				db.endTransaction();
			}
			
		}	
		
		
		db.close();
		dbHelper.close();	
				
		return null;
	}
	
	
}