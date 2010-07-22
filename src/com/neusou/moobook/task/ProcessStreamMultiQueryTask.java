package com.neusou.moobook.task;

import org.apache.commons.lang.NullArgumentException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.neusou.Logger;
import com.neusou.async.UserTask;
import com.neusou.moobook.App;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.data.Stream;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;

public class ProcessStreamMultiQueryTask extends UserTask<Bundle, ProcessProgressInfo, Integer> {

	static final String LOG_TAG = "ProcessStreamMultiQueryTask";

	Context mContext;
	Handler mUIHandler;	
	int mFinishCode;
	int mProcessFlag;
	int numUpdatedPosts;
	ProcessProgressInfo mProgressInfo;
	ApplicationDBHelper dbHelper;
	SQLiteDatabase db;	
	String[] mUpdatedPostsIds;
	
	public static final String XTRA_COUNT_UPDATED_POSTS = "xtra.proc.stream.mq.rs.num.new.posts";
	public static final String XTRA_UPDATED_POSTSIDS = "xtra.updated.posts.ids";
	
	private short[] userTableFieldsSelection = new short[] { User.col_uid,
			User.col_name, User.col_pic_small, User.col_pic_big,
			User.col_pic_square, User.col_pic, User.col_first_name,
			User.col_last_name, User.col_type };

	private short[] pageTableFieldsSelection = new short[] { User.col_uid,
			User.col_name, User.col_pic, User.col_pic_small,
			User.col_pic_square, User.col_type };

	Runnable mFinishRunnable;
	
	public ProcessStreamMultiQueryTask(Context ctx, Handler uiHandler, int processFlag, int finishCode, Runnable finishRunnable) throws NullArgumentException {
		if (uiHandler == null) {
			throw new NullArgumentException("uihandler can't be null");
		}
		if (ctx == null) {
			throw new NullArgumentException("context can't be null");
		}

		mContext = ctx;
		mUIHandler = uiHandler;
		mFinishCode = finishCode;
		mProcessFlag = processFlag;
		
		mProgressInfo = new ProcessProgressInfo();
		dbHelper = App.INSTANCE.mDBHelper;			
		db = App.INSTANCE.mDB;
		mFinishRunnable = finishRunnable;
		 
	}

	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		Logger.l(Logger.DEBUG, LOG_TAG, "[onPreExecute()]");		
	}

	@Override
	protected Integer doInBackground(Bundle... params) {
		
		Bundle data = params[0];		
		String response = data.getString(Facebook.XTRA_RESPONSE);			
		FBWSResponse fbresponse = FBWSResponse.parse(response);
		JSONArray responseArray = fbresponse.jsonArray;

		Logger.l(Logger.DEBUG,LOG_TAG, "[doInBackground()] response array length: "+responseArray.length());

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
					if (name.compareTo("posts") == 0) {
						processPosts(result_set);				
					}
					else if (name.compareTo("users") == 0) {										
						processUsers(result_set, userTableFieldsSelection);	
					} 
					else if (name.compareTo("pages") == 0) {
						processPages(result_set, pageTableFieldsSelection);					
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	
		Logger.l(Logger.DEBUG,LOG_TAG, "[doInBackground()] finished.");			
		return null;
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
						
		
		Logger.l(Logger.DEBUG,LOG_TAG,"[onPostExecute()] send finish message to UI handler.");
		Message msg = mUIHandler.obtainMessage(mFinishCode);
		Bundle data = new Bundle();
		if(result != null){
			data.putInt(XTRA_COUNT_UPDATED_POSTS, result);
		}
		data.putStringArray(XTRA_UPDATED_POSTSIDS, mUpdatedPostsIds);
		msg.setData(data);
		msg.sendToTarget();
		Logger.l(Logger.DEBUG,LOG_TAG,"[onPostExecute()] finished process stream multi query.");
		
		if(mFinishRunnable != null){
			mFinishRunnable.run();
		}
		
	}

	@Override
	protected void onTimeout() {
		super.onTimeout();
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();		
	}

	@Override
	protected void onProgressUpdate(ProcessProgressInfo... values) {
		super.onProgressUpdate(values);
	}
	
	private boolean processPosts(JSONArray data) {
		if (data == null) {
			return false;
		}
		String attachmentJSON;
		int num = data.length();
		Stream stream = new Stream();
		mUpdatedPostsIds = new String[num];
		
		db.beginTransaction();
		for (int i = 0; i < num	&& getStatus() == Status.RUNNING; i++) {
			JSONObject itemJson;			
			try {
				itemJson = data.getJSONObject(i);				
				
				String strJson = itemJson.toString(2);
				Logger.l(Logger.DEBUG, LOG_TAG, "[processPosts()] ["+i+"] json: "+strJson);
				
				stream.post_id = itemJson.getString(Stream.cn_post_id);
				stream.attribution = itemJson.getString(Stream.cn_attribution);
				stream.updated_time = itemJson.getLong(Stream.cn_updated_time);
				stream.created_time = itemJson.getLong(Stream.cn_created_time);
				stream.message = itemJson.getString(Stream.cn_message);
				stream.source_id = itemJson.getLong(Stream.cn_source_id);
				stream.target_id = itemJson.getString(Stream.cn_target_id);
				stream.actor_id = itemJson.getLong(Stream.cn_actor_id);
								
				//don't save flag if it is not session user
				if(mProcessFlag == App.PROCESS_FLAG_SESSIONUSER){
					stream._process_flag = mProcessFlag;						
				}else{
					stream._process_flag = App.PROCESS_FLAG_IGNORE;
				}
				
				try{
					stream.app_id = itemJson.getInt(Stream.fields_appid);
					Logger.l(Logger.DEBUG,"crucial2",""+stream.app_id);
				}catch(Exception e){
					stream.app_id = -1;
				}
				
				// process attachment
				JSONObject att = itemJson.getJSONObject(Stream.cn_attachment);
				attachmentJSON = att.toString();
				Logger.l(Logger.DEBUG, LOG_TAG, "[processPosts()] ["+i+"] attribution:" + stream.attribution +", attachment json:" + attachmentJSON);
				stream.attachment = attachmentJSON;
				// process comments

				JSONObject commentsObj;

				commentsObj = itemJson.getJSONObject("comments");
				if (commentsObj != null) {

					try {
						stream.comments_can_post = Boolean
								.parseBoolean(commentsObj.getString("can_post"));

					} catch (JSONException e) {
					}

					try {
						stream.comments_can_remove = Boolean
								.parseBoolean(commentsObj
										.getString("can_remove"));

					} catch (JSONException e) {
					}

					try {
						stream.comments_count = commentsObj.getLong("count");
					} catch (JSONException e) {
					}
				}

				// process likes
				JSONObject likesObj;
				try {
					likesObj = itemJson.getJSONObject("likes");
					if (likesObj != null) {

						try {
							stream.likes_count = likesObj.getLong("count");
						} catch (JSONException e) {
							stream.likes_count = 0;
						}

						try {
							stream.likes_userlikes = Boolean
									.parseBoolean(likesObj
											.getString("user_likes"));
						} catch (JSONException e) {
							stream.likes_userlikes = false;
						}

						try {
							stream.likes_canlike = Boolean
									.parseBoolean(likesObj
											.getString("can_like"));
						} catch (JSONException e) {
							stream.likes_canlike = false;
						}

					}
				} catch (JSONException e) {
				}
				// end of process likes

				
				if (mStatus == Status.RUNNING && db != null && db.isOpen()) {	
					
					long rowId = dbHelper.insertStream(stream, db);		
					Logger.l(Logger.DEBUG,LOG_TAG,"[processPosts()] ["+i+"] inserting stream. pid:"+stream.post_id+", table rowid:"+rowId);
					
					mUpdatedPostsIds[i] = stream.post_id;
				} else {				
					db.endTransaction();				
					return false;
				}

				if (i == num - 1) {
					mUIHandler.sendEmptyMessage(mFinishCode);
				} else {
					mProgressInfo.current = i + 1;
					mProgressInfo.total = num;
					publishProgress(mProgressInfo);
				}

			} catch (JSONException e) {
				e.printStackTrace();
			} finally {
			}

		}
		db.setTransactionSuccessful();
		db.endTransaction();		
	
		Logger.l(Logger.DEBUG, LOG_TAG, "[processPosts()] finished");
		return true;
	}
	
	private boolean processUsers(final JSONArray data, short[] selection) {
		if (data == null) {
			return false;
		}

		Logger.l(Logger.DEBUG,LOG_TAG, "[processUsers()]");

		User user = new User();
		int numUsers = 0;
		if (data == null) {
//			Logger.l(Logger.DEBUG,LOG_TAG, "process user data no data");
			return false;

		} else {
			numUsers = data.length();
			Logger.l(Logger.DEBUG,LOG_TAG, "[processUsers()] total users: " + numUsers);
			if (numUsers == 0) {
				return false;
			}
		}

		JSONObject userObj = null;

		db.beginTransaction();
		for (int i = 0; i < numUsers; i++) {
			try {
				userObj = data.getJSONObject(i);
				user.parse(userObj, selection);
				user.type = User.TYPE_USER;
				if (mStatus == Status.RUNNING && db != null && db.isOpen()) {
					long profileRowId = dbHelper.insertUser(user, selection, db);
				} else {
					db.endTransaction();
					//db.close();
					//db.releaseReference();
					return false;
				}
			} catch (JSONException e) {
				db.endTransaction();
				//db.close();
				//db.releaseReference();
				return false;
			}
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		
		Logger.l(Logger.DEBUG, LOG_TAG, "[processUsers()] finished");
		return true;
	}

	/***
	 * Process pages
	 * 
	 * Note that we both store Facebook Users and Facebook Pages in the same
	 * 'users' table
	 * 
	 * @param data
	 * @param selection
	 */
	private void processPages(final JSONArray data, short[] selection) {
		if (data == null) {
			return;
		}

		Logger.l(Logger.DEBUG, LOG_TAG, "[processPages()]");

		User user = new User();
		int numPages = 0;
		if (data == null) {
			Logger.l(Logger.DEBUG,LOG_TAG, "process page data no data");
			return;

		} else {
			numPages = data.length();
			Logger.l(Logger.DEBUG, LOG_TAG, "[processPages()] total pages: " + numPages);
			if (numPages == 0) {
				return;
			}
		}

		JSONObject userObj = null;
				
		db.beginTransaction();
		for (int i = 0; i < numPages; i++) {
			try {
				userObj = data.getJSONObject(i);
				user.uid = userObj.getLong("page_id");
				user.name = userObj.getString("name");
				user.pic = userObj.getString("pic");
				user.pic_small = userObj.getString("pic_small");
				user.pic_square = userObj.getString("pic_square");
				user.type = User.TYPE_PAGE;
				Logger.l(Logger.DEBUG,LOG_TAG,"[processPages()] uid:"+ user.uid + ", pic:" + user.pic_square);

				if (mStatus == Status.RUNNING && db != null && db.isOpen()) {					
					long pageRowId = dbHelper.insertUser(user, selection, db);					
				} else {
					return;
				}

			} catch (JSONException e) {

			}
		}
		db.setTransactionSuccessful();
		db.endTransaction();
		
		Logger.l(Logger.DEBUG, LOG_TAG, "[processPages()] finished");
	}	
}