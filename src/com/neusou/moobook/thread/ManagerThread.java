package com.neusou.moobook.thread;

import java.util.concurrent.CountDownLatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.neusou.LazyThread;
import com.neusou.Logger;
import com.neusou.async.UserTask;
import com.neusou.moobook.App;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.FQL;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;
import com.neusou.moobook.data.Comment;
import com.neusou.moobook.data.Photo;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.RemoteCallResult;
import com.neusou.moobook.task.DeniedTaskCreationException;
import com.neusou.moobook.task.ProcessStreamMultiQueryTask;
import com.neusou.moobook.task.ProcessTaskFactory;
import com.neusou.moobook.task.ProcessUsersTask;
import com.neusou.web.PagingInfo;

public class ManagerThread extends BaseManagerThread {

	public ManagerThread(CountDownLatch cdl) {
		super(cdl);
		data_comments = new JSONArray();
	}

	public static final int CALLBACK_GET_FRIENDS = 1;
	public static final int CALLBACK_GET_ONEUSER_DATA = 2;
	public static final int CALLBACK_GET_STREAMS_MQ = 3;
	public static final int CALLBACK_GET_TAGGED_PHOTOS = 4;
	public static final int CALLBACK_GET_PHOTO_TAGS = 5;
	public static final int CALLBACK_GET_COMMENTS_MULTIQUERY = 6;
	public static final int CALLBACK_GET_COMMENTS = 7;

	public static final int CALLBACK_POST_COMMENT = 8;
	public static final int CALLBACK_POST_STREAM = 9;
	public static final int CALLBACK_DELETE_COMMENT = 10;

	public static final int CALLBACK_GET_COMMENTS_USERS = 11;
	public static final int CALLBACK_GET_USERDATA = 12;
	public static final int CALLBACK_GET_ALBUMS = 13;
	public static final int CALLBACK_POST_PHOTO = 14;
	public static final int CALLBACK_LOGIN_TO_FACEBOOK = 15;
	public static final int CALLBACK_GET_CONTACTS = 16;
	public static final int CALLBACK_GET_SESSION_STREAM = 17;
	public static final int CALLBACK_GET_WALLPOSTS = 18;
	public static final int CALLBACK_GET_PHOTO_ATTR = 19;
	
	public static final int CALLBACK_GET_COMMENTS_FINISHED = 20;
	public static final int CALLBACK_PROCESS_STREAMS_FINISH = 21;
	public static final int CALLBACK_SESSION_RETRIEVED = 22;
	
	public static final int MESSAGE_UPDATE_TAGS = 30;
	public static final int MESSAGE_DISMISS_DIALOG = 31;
	public static final int MESSAGE_PARSE_PHOTOS = 32;
	public static final int MESSAGE_UPDATE_ALBUMS_GRID = 33;
	public static final int MESSAGE_UPDATE_PHOTO_GRID = 34;
	public static final int MESSAGE_UPDATELIST = 35;
	public static final int MESSAGE_COMMENTS_DELETED = 36;
	public static final int MESSAGE_COMMENTS_POSTED = 37;
	public static final int MESSAGE_STREAM_POSTED = 38;

	public static final int CALLBACK_SESSION_EXPIRED = 22;
	public static final int CALLBACK_SESSION_VALID = 23;
	public static final int CALLBACK_SESSION_VALIDATED = 24;
	
	public static final int CALLBACK_ADMOB_ONFAILRECEIVE = 50;					
	public static final int CALLBACK_ADMOB_ONRECEIVE = 51;
	public static final int CALLBACK_ADMOB_ONNEWAD = 52;
	

	ProcessComments mProcessComments;
	CountDownLatch mProcessCommentsWaitCountdown;

	private JSONArray data_comments = null;
	private JSONArray data_users = null;
	private JSONArray data_comments_info = null;

	IManagerResult mListener;

	boolean isMissingComments;
	int total_actualComments;
	int total_reportedComments;

	static final int CALLBACK_PROCESS_STREAMS_MQ_FINISH = 0;

	ProcessStreamMultiQueryTask mProcessStreamMultiQueryDataTask;

	private void clearCommentsRelatedData() {
		data_comments = null;
		data_users = null;
		data_comments_info = null;
	}

	Handler internalHandler = new Handler() {
		public void handleMessage(Message msg) {
			int code = msg.what;
			switch (code) {
			case CALLBACK_PROCESS_STREAMS_MQ_FINISH: {
				Logger
						.l(Logger.DEBUG, LOG_TAG,
								"[internalHandler] [callback_process_streams mq finish]");
				Message m = mOutHandler
						.obtainMessage(CALLBACK_PROCESS_STREAMS_FINISH);
				m.setData(msg.getData());
				m.sendToTarget();
				break;
			}
			}
		};
	};

	public void setListener(IManagerResult listener) {
		mListener = listener;
	}

	public interface IManagerResult {
		public void setUsersData(JSONArray data);

		public void setCommentsData(JSONArray data);

		public void setCommentsInfoData(JSONArray data);

		public void setPagingInfoData(PagingInfo data);

		public PagingInfo getPagingInfoData();

		public boolean hasObjectId();

		public boolean hasPostId();

		public String getObjectId();

		public String getPostId();

	}

	private long getReturnedCommentsTotalCount(JSONObject data) {
		try {
			return data.getLong("count"); // total of returned comments
		} catch (JSONException e) {
			// return large number, hopefully isMissingData() will return true.
			return (long) (Math.random() * 1000);
		}
	}

	private boolean isMissingData(JSONArray data_comments_info,
			JSONArray data_comments

	) {
		/*
		 * if(data_comments_info != null){ try{ JSONObject obj =
		 * data_comments_info.getJSONObject(0); //Logger.l(Logger.DEBUG, "agus",
		 * "comments_info:"+obj.toString(1)); //JSONObject commentsObj =
		 * obj.getJSONObject("comments"); //total_reportedComments = (int)
		 * getReturnedCommentsCount(commentsObj); //total_actualComments =
		 * data_comments.length(); //Logger.l(Logger.DEBUG, "agus",
		 * "num actual:"
		 * +total_actualComments+", num reported:"+total_reportedComments);
		 * //if(total_reportedComments != total_actualComments){ // return true;
		 * //} return false; }catch(JSONException e){ } }
		 */
		return false;
	}

	private void processCommentsPaging(PagingInfo paging) {

		int remoteSize = 0;
		int time0 = PagingInfo.RECORD_ID_UNKNOWN;
		int time1 = PagingInfo.RECORD_ID_UNKNOWN;
		int localSize = 0;

		if (data_comments != null) {
			localSize = data_comments.length();
			if (localSize >= 1) {
				JSONObject comment0;
				JSONObject comment1;
				try {
					comment0 = (JSONObject) data_comments.getJSONObject(0);
					time0 = comment0.getInt(Comment.cn_time);
					comment1 = (JSONObject) data_comments.getJSONObject(1);
					time1 = comment1.getInt(Comment.cn_time);

				} catch (JSONException e) {
				} catch (NullPointerException e) {
				}
			}

		} else {
			localSize = 0;
		}

		if (data_comments_info != null) {
			try {
				JSONObject obj = data_comments_info.getJSONObject(0);
				if (data_comments_info.length() > 0) {
					JSONObject commentsObj = obj.getJSONObject("comments");
					remoteSize = (int) getReturnedCommentsTotalCount(commentsObj);
				}
			} catch (JSONException e) {
			}
		}

		// update paging information
		// determine if the freshly arriving batch of data is actually different
		// than the last batch
		// Note: this is a solution to facebook api returning cached request
		// even when different paging parameters are used.

		paging.update(time0, time1, localSize, remoteSize);

	}

	/*
	 * private void returnComments(){ Logger.l(Logger.DEBUG, LOG_TAG,
	 * "[WorkerManagerThread] paging: "+mPagingInfo.toString()); //update
	 * comments list view Message msg =
	 * mOutHandler.obtainMessage(MESSAGE_UPDATELIST); Bundle outData = new
	 * Bundle(); outData.putString(XTRA_COMMENTS, comments.toString());
	 * outData.putString(XTRA_COMMENTSINFO, commentsinfo.toString());
	 * outData.putString(XTRA_USERS, users.toString()); msg.setData(outData);
	 * msg.sendToTarget(); }
	 */

	private void broadcastResult(Bundle data) {
		if(data.containsKey(BaseManagerThread.XTRA_CALLBACK_INTENT_ACTION)){
			String callbackAction = data.getString(BaseManagerThread.XTRA_CALLBACK_INTENT_ACTION);
			Logger.l(Logger.DEBUG, LOG_TAG, "[broadcastResult()] action: "
				+ callbackAction);
			Intent i = new Intent(callbackAction);
			i.putExtras(data);
			App.INSTANCE.sendOrderedBroadcast(i,null);
		}
	}

	static short[][] mSelectedUserColumns = new short[][] {
			{ User.col_name, User.col_pic, User.col_timezone,
					User.col_pic_square, User.col_uid,
					User.col_hometown_location, User.col_current_location,
					User.col_birthday, User.col_birthday_date,
					User.col_proxied_email },
			{ User.col_name, User.col_pic, User.col_timezone,
					User.col_pic_square, User.col_uid,
					User.col_hometown_location, User.col_current_location,
					User.col_birthday, User.col_birthday_date,
					User.col_proxied_email } };
	static final byte SELECTEDCOLUMNS_USER_DISPLAY = 0;
	static final byte SELECTEDCOLUMNS_USER_FETCH = 1;

	public void doBusiness(final Bundle data, int code, FBWSResponse fbresponse) {

		Logger.l(Logger.DEBUG, LOG_TAG, "[doBusiness()] code:" + code);

		switch (code) {
		
		case CALLBACK_SESSION_RETRIEVED:{
			String parsed;
			try {
				parsed = fbresponse.jsonArray.toString(2);
				Logger.l(Logger.DEBUG, LOG_TAG, "[callback_session_retrieved]: "
						+ parsed);
			
				broadcastResult(data);
			}
			catch(JSONException e){
					
			}
			break;
		}
		
		case CALLBACK_GET_PHOTO_ATTR:{
			String parsed;
			try {
				parsed = fbresponse.jsonArray.toString(2);
				Logger.l(Logger.DEBUG, LOG_TAG, "[callback_get_photo_attr]: "
						+ parsed);
				data.putParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT,
						fbresponse);
				JSONArray photoResultSet = App.getFqlResultSet("photo", fbresponse.data);
				Logger.l(Logger.DEBUG, LOG_TAG, photoResultSet.toString(2));			
				JSONObject photoObj = photoResultSet.getJSONObject(0);
				Photo photo = Photo.parseJson(photoObj,null);
				data.putParcelable(Photo.XTRA_PARCELABLE_OBJECT, photo);
				broadcastResult(data);
			} catch (JSONException e) {
			}
			break;
		}
		
		case CALLBACK_GET_WALLPOSTS: {
			String parsed;
			try {
				parsed = fbresponse.jsonArray.toString(2);
				Logger.l(Logger.DEBUG, LOG_TAG, "[callback_get_wallposts]: "
						+ parsed);
				data.putParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT,
						fbresponse);
				broadcastResult(data);
			} catch (JSONException e) {
			}
			break;
		}

		case CALLBACK_GET_TAGGED_PHOTOS: {
			String parsed;
			try {
				parsed = fbresponse.jsonArray.toString(2);
				Logger.l(Logger.DEBUG, LOG_TAG,
						"[callback_get_tagged_photos]: " + parsed);
				data.putParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT,
						fbresponse);
				broadcastResult(data);
			} catch (JSONException e) {
			}
			break;
		}

		case CALLBACK_GET_STREAMS_MQ: {
			if (mProcessStreamMultiQueryDataTask == null
					|| mProcessStreamMultiQueryDataTask.getStatus() == UserTask.Status.FINISHED) {
				
				int processFlag = data.getInt(App.FLAG_STREAM_PROCESS_FLAG, 0);
				
				mProcessStreamMultiQueryDataTask = new ProcessStreamMultiQueryTask(
						App.INSTANCE, internalHandler, processFlag,
						CALLBACK_PROCESS_STREAMS_MQ_FINISH, new Runnable() {
							final Bundle mData = data;

							@Override
							public void run() {
								Logger
										.l(Logger.DEBUG, LOG_TAG,
												"[runnable][run()] finished get streams mq");
								broadcastResult(mData);
							}
						});
				mProcessStreamMultiQueryDataTask.execute(data);
			} else {
				// Toast.makeText(App.INSTANCE,
				// "Cant call fetch streams mq: "+mProcessStreamMultiQueryDataTask.getStatus(),
				// 2000).show();
			}
			break;
		}

			/**
			 * single fql query version of getting comments
			 */
		case CALLBACK_GET_COMMENTS: {

			clearCommentsRelatedData();

			Logger.l(Logger.DEBUG, LOG_TAG, "[callback_get_comments] "
					+ fbresponse.data);

			int outHandlerKey = data
					.getInt(Facebook.XTRA_INTERNAL_OUTHANDLER_KEY);
			// Facebook.getInstance().getCommentsRest(outHandlerKey, mPostId,
			// ManagerThread.CALLBACK_GET_COMMENTS_FINISHED,
			// ManagerThread.CALLBACK_SERVERCALL_ERROR,
			// ManagerThread.CALLBACK_TIMEOUT_ERROR);

			if (isMissingComments) {
				if (fbresponse.jsonArray == null) {
					Message m = mOutHandler
							.obtainMessage(CALLBACK_SERVERCALL_ERROR);
					data
							.putString(Facebook.XTRA_SERVERCALL_ERROR_MSG,
									"Unable to retrieve comments due to Facebook bug#: 9405");
					m.setData(data);
					m.sendToTarget();

					return;
				}
			}

			if (!fbresponse.hasErrorCode && fbresponse.jsonArray != null) {
				int numComments = fbresponse.jsonArray.length();
				if (numComments > 0) {
					String[] uids = App.getCommentsUids(fbresponse.jsonArray,
							null);
					Logger.l(Logger.DEBUG, LOG_TAG,
							"[callback_get_comments] getting users");

					Facebook.getInstance().getCommentsUsers(outHandlerKey,
							uids, CALLBACK_GET_COMMENTS_USERS,
							BaseManagerThread.CALLBACK_SERVERCALL_ERROR,
							BaseManagerThread.CALLBACK_TIMEOUT_ERROR, 0);

					// fbresponse.jsonArray.getJSONObject(0).getJSONArray(FQL.FQL_RESULT_SET);
					data_comments = fbresponse.jsonArray;

					mProcessCommentsWaitCountdown = new CountDownLatch(1);

					if (mProcessComments != null) {
						mProcessComments.signalDestroy();
					}

					mProcessComments = new ProcessComments(
							mProcessCommentsWaitCountdown, mInHandler,
							CALLBACK_GET_COMMENTS_FINISHED);
					mProcessComments.start();
				}

			} else if (fbresponse.jsonArray == null) {
				processCommentsPaging(mListener.getPagingInfoData());
				Message m = mInHandler
						.obtainMessage(CALLBACK_GET_COMMENTS_FINISHED);
				m.setData(data);
				m.sendToTarget();
			}
			break;
		}

		case CALLBACK_GET_COMMENTS_FINISHED: {
			mOutHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					mListener.setCommentsData(data_comments);
					mListener.setCommentsInfoData(data_comments_info);
					mListener.setUsersData(data_users);
					mOutHandler.sendEmptyMessage(MESSAGE_UPDATELIST);
				}
			}, 0);

			break;
		}

		case CALLBACK_GET_COMMENTS_USERS: {
			Logger.l(Logger.DEBUG, LOG_TAG,
					"[WorkerManagerThread] [callback_get_comments_users] "
							+ fbresponse.data);

			// fbresponse.jsonArray.getJSONObject(0).getJSONArray(FQL.FQL_RESULT_SET);
			data_users = fbresponse.jsonArray;
			mProcessCommentsWaitCountdown.countDown();
			break;
		}

		case CALLBACK_GET_COMMENTS_MULTIQUERY: {

			clearCommentsRelatedData();

			// extract responses from queries
			Logger.l(Logger.DEBUG, LOG_TAG,
					"[WorkerManagerThread] [callback_get_comments_multiquery");

			for (int i = 0, num = fbresponse.jsonArray.length(); i < num; i++) {
				try {
					JSONObject obj = fbresponse.jsonArray.getJSONObject(i);
					String queryName = obj.getString("name");
					// Log.d(LOG_TAG,"response set "+i+" : name:"+queryName);
					JSONArray rs = fbresponse.jsonArray.getJSONObject(i)
							.getJSONArray("fql_result_set");
					if (queryName.compareTo(FQL.QUERY_COMMENTS) == 0) {
						data_comments = rs;
					}
					if (queryName.compareTo(FQL.QUERY_USERS) == 0) {
						data_users = rs;
					}
					if (queryName.compareTo(FQL.QUERY_COMMENTSINFO) == 0) {
						data_comments_info = rs;
					}
				} catch (JSONException e) {
					//e.printStackTrace();
				}
			}

			// TODO a solution to facebook multiquery comments bug.
			isMissingComments = isMissingData(data_comments_info, data_comments);
			if (isMissingComments) {
				Facebook fb = Facebook.getInstance();
				if (fb == null) {
					Logger.l(Logger.ERROR, LOG_TAG, "facebook is null");
				} else {

					String postId = mListener.getPostId();
					String objectId = mListener.getObjectId();
					boolean hasPostId = mListener.hasPostId();
					boolean hasObjectId = mListener.hasObjectId();

					Logger.l(Logger.DEBUG, LOG_TAG,
							"reported comments count does not match.");
					Logger.l(Logger.DEBUG, LOG_TAG, "objectId: "
							+ mListener.getPostId() + ", objectId: "
							+ mListener.getObjectId());
					PagingInfo paging = mListener.getPagingInfoData();
					fb
							.getComments(
									R.id.outhandler_activity_viewcomments,
									mListener.hasPostId() ? Facebook.COMMENT_TYPE_STREAMPOSTS
											: Facebook.COMMENT_TYPE_OTHERS,
									hasObjectId ? objectId : null,
									hasPostId ? postId : null,
									CALLBACK_GET_COMMENTS,
									BaseManagerThread.CALLBACK_SERVERCALL_ERROR,
									BaseManagerThread.CALLBACK_TIMEOUT_ERROR,
									0, (long) paging.windowSize, (long) paging
											.getNextStart());
				}
			} else {
				processCommentsPaging(mListener.getPagingInfoData());
				Message m = mInHandler
						.obtainMessage(CALLBACK_GET_COMMENTS_FINISHED);
				m.setData(data);
				m.sendToTarget();
			}

			break;
		}

		case CALLBACK_POST_STREAM: {
			Logger.l(Logger.DEBUG, LOG_TAG, "[callback_post_stream]: "+ fbresponse.data);
			RemoteCallResult rcr = new RemoteCallResult();
			rcr.status = true; // assume every new stream is successfully posted.
			data.putParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT, fbresponse);
			data.putParcelable(RemoteCallResult.XTRA_PARCELABLE_OBJECT, rcr);
			broadcastResult(data);
			break;
		}
		
		case CALLBACK_POST_COMMENT: {

			Logger.l(Logger.DEBUG, LOG_TAG, "[callback_postcomment]: "
					+ fbresponse.data);
			RemoteCallResult rcr = new RemoteCallResult();
			rcr.status = true; // assume every new comment is successfully
								// posted.
			data.putParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT, fbresponse);
			data.putParcelable(RemoteCallResult.XTRA_PARCELABLE_OBJECT, rcr);
			broadcastResult(data);

			break;
		}

		case CALLBACK_DELETE_COMMENT: {
		
			// TODO Object pool: RemoteCallResult

			// parsed = fbresponse.jsonArray.toString(2);
			Logger.l(Logger.DEBUG, LOG_TAG, "[callback_deletecomment]: "
					+ fbresponse.data);
			RemoteCallResult rcr = new RemoteCallResult();
			rcr.status = Boolean.parseBoolean(fbresponse.data);
			data.putParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT, fbresponse);
			data.putParcelable(RemoteCallResult.XTRA_PARCELABLE_OBJECT, rcr);
			broadcastResult(data);

			// Logger.l(Logger.DEBUG, LOG_TAG, "deleted comment rsp: "+success);

			break;
		}

		case CALLBACK_GET_USERDATA: {
			String parsed;
			try {
				parsed = fbresponse.jsonArray.toString(2);
				Logger.l(Logger.DEBUG, LOG_TAG, "[callback_get_userdata]: "	+ parsed);
				data.putParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT,	fbresponse);
				broadcastResult(data);
			} catch (JSONException e) {
			}
			break;
		}

		case ManagerThread.CALLBACK_GET_CONTACTS: {
			Logger.l(Logger.DEBUG, LOG_TAG,
					"[ViewContactsAct] [callback_get_comments] "
							+ fbresponse.data);
			if (!fbresponse.hasErrorCode && fbresponse.jsonArray != null) {
				int numComments = fbresponse.jsonArray.length();
				if (numComments > 0) {
					// String[] uids = App.getCommentsUids(fbresponse.jsonArray, null);
					Logger.l(Logger.DEBUG, LOG_TAG,
							"[ViewContactsAct] [callback_get_contacts] response: "
									+ fbresponse.data);
					data.putShortArray(Facebook.XTRA_TABLECOLUMNS_SHORTARRAY,
							mSelectedUserColumns[SELECTEDCOLUMNS_USER_FETCH]);
					//ProcessUsersTask mProcessUsersTask = new
					//ProcessUsersTask(mOutHandler,0,0,ManagerThread.MESSAGE_UPDATELIST,0,0);
					
					
					try{
						ProcessUsersTask mProcessUsersTask;
						//by using the default flag value below, we assume that every user is a not a friend of this session user
						int processFlag = data.getInt(App.FLAG_USER_PROCESS_FLAG, App.PROCESS_FLAG_USER_OTHER);						
						mProcessUsersTask = ProcessTaskFactory.create(ProcessUsersTask.class);
						mProcessUsersTask.init(mOutHandler,0,0, ManagerThread.MESSAGE_UPDATELIST,0,0,processFlag);
						mProcessUsersTask.execute(data);							
						//Toast.makeText(App.INSTANCE, "Success Task Creation", 2000).show();
					}catch(DeniedTaskCreationException e){	
						//Toast.makeText(App.INSTANCE, "Denied Task Creation", 2000).show();
					}
									
				}
			}
			break;
		}
		}

	}

	public class ProcessComments extends LazyThread {

		CountDownLatch mWaitCountdown;
		Handler mOutHandler;
		int mFinishCode;

		public ProcessComments(CountDownLatch wait, Handler outHandler,
				int finishCode) {
			super(wait, 1000);
			mWaitCountdown = wait;
			mOutHandler = outHandler;
			mFinishCode = finishCode;
		}

		public void signalDestroy() {
			mWaitCountdown = null;
			mOutHandler = null;
		}

		@Override
		public void doRun() {
			processCommentsPaging(mListener.getPagingInfoData());
			PagingInfo paging = mListener.getPagingInfoData();
			Logger.l(Logger.DEBUG, LOG_TAG, "[ProcessComments.run()] paging: "
					+ paging.toString());
			mOutHandler.sendEmptyMessage(mFinishCode);
		}

	}

};
