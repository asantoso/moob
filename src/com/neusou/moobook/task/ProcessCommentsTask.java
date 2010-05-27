/*
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
import com.neusou.moobook.thread.UserTask.Status;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class ProcessCommentsTask extends BaseProcessTask{

	public ProcessCommentsTask(Context ctx, Handler uiHandler, int startCode,
			int updateCode, int finishCode, int progressCode, int timeoutCode)
			throws NullArgumentException {
		super(ctx, uiHandler, startCode, updateCode, finishCode, progressCode,
				timeoutCode);
	}

	static final String LOG_TAG = "ProcessCommentsTask";

	@Override
	protected Integer doInBackground(Bundle... params) {
		Bundle data = params[0];
		ApplicationDBHelper dbHelper = new ApplicationDBHelper(mContext);
		String response = data.getString(Facebook.XTRA_RESPONSE);
				
		FBWSResponse fbresponse = FBWSResponse.parse(response);		
		JSONArray comments = fbresponse.jsonArray;
		int num = comments.length();
		Log.d(LOG_TAG,"num comments: "+num);
		Comment comment = new Comment();
		SQLiteDatabase db = dbHelper.getWritableDatabase();   
		
	//	short[] selection = data.getShortArray(Facebook.XTRA_TABLECOLUMNS_SHORTARRAY);
		//if(selection == null){
			//Log.e(LOG_TAG,"no columns selection");
		//}

		for(int i=0,j=mPeriodicUIUpdateInterval;i<num && getStatus() == Status.RUNNING;i++){
			JSONObject itemJson;	
			
			try {								
				itemJson = comments.getJSONObject(i);
				
				comment.from_id = itemJson.getLong(Comment.cn_from_id);
				comment.comment = itemJson.getString(Comment.cn_comment);
				comment.time = itemJson.getLong(Comment.cn_time);
				
			}
			catch(JSONException e){					
			}
				
			long rowId = dbHelper.insertComment(comment, db);
			Log.d(LOG_TAG, "comment new row id: "+rowId);

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
		
		db.close();
		dbHelper.close();	
				
		return null;

	}
	
	
}
*/