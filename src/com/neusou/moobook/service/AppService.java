package com.neusou.moobook.service;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.media.SoundPool;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.FBConnectionException;
import com.neusou.moobook.FBSession;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.FQL;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;
import com.neusou.moobook.data.FBApplication;
import com.neusou.moobook.data.Profile;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.moobook.task.ProcessNotificationsTask;
import com.neusou.moobook.task.ResponseProcessor;

public class AppService extends WakefulIntentService {
	static final String LOG_TAG = "AppService";

	static final int CALLBACK_SERVERCALL_ERROR = 1110;
	static final int CALLBACK_TIMEOUT_ERROR = 1111;
	static final int CALLBACK_PROCESS_WSRESPONSE_HAS_ERRORCODE = 1112;
	static final int CALLBACK_GET_NOTIFICATIONS = 1113;

	static final int CALLBACK_PROCESS_NOTIFICATIONS_START = 0;
	static final int CALLBACK_PROCESS_NOTIFICATIONS_UPDATE = 1;
	static final int CALLBACK_PROCESS_NOTIFICATIONS_FINISH = 2;
	static final int CALLBACK_PROCESS_NOTIFICATIONS_PROGRESS = 3;
	static final int CALLBACK_PROCESS_NOTIFICATIONS_TIMEOUT = 4;

	NotificationManager mNotificationManager;
	ProcessNotificationsTask mProcessNotificationsTask;
	Facebook mFacebook;

	public AppService() {
		super("AppService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mFacebook = Facebook.getInstance();
		String action = intent.getAction();

		Logger.l(Logger.DEBUG, LOG_TAG, "[onHandleIntent()] action:" + action);
		if (action != null) {
			if (action.compareTo(App.INTENT_CHECK_NOTIFICATIONS) == 0) {
				getLatestNotifications();
			} else if (action.compareTo(App.INTENT_CHECK_STREAMS) == 0) {

			}
		}

		// release wakelock.
		super.onHandleIntent(intent);
	}

	private void getLatestNotifications() {
		FBWSResponse response;

		try {
			FBSession session = mFacebook.getSession();
			if(session != null){
				response = mFacebook.getNotifications(session.uid, 0);
			}else{
				return;
			}
		} catch (FBConnectionException e) {
			return;
		}

		if (response == null || response.jsonArray == null
				|| response.hasErrorCode) {
			return;
		}

		HashMap<String, JSONArray> data = Facebook.parseMultiFQLResult(response, null);

		/*
		try {
			JSONArray notifications = response.jsonArray.getJSONObject(0).getJSONArray(FQL.FQL_RESULT_SET);
			JSONArray users = response.jsonArray.getJSONObject(1).getJSONArray(	FQL.FQL_RESULT_SET);
			JSONArray applications = response.jsonArray.getJSONObject(2).getJSONArray(FQL.FQL_RESULT_SET);
			int numNotifications = notifications.length();
			Logger.l(Logger.DEBUG, LOG_TAG, "# notifications: "	+ numNotifications);
		} catch (JSONException e) {
		}
		 */
		try{
			Logger.l(Logger.DEBUG, LOG_TAG, "# notifications: "	+ data.get("notifications").length());
		}catch(NullPointerException e){
			Logger.l(Logger.ERROR, LOG_TAG, "notifications data is null");
		}

		if (data != null) {
			ResponseProcessor.processUsers(data.get("users"), new short[]{User.col_uid, User.col_name, User.col_pic_square}, App.mDB, App.mDBHelper);
			HashMap<Long, FBApplication> appnames = ResponseProcessor.processApplications(data.get("applications"), null);
			ResponseProcessor.processNotifications(data.get("notifications"), appnames, App.mDB, App.mDBHelper, mNotificationManager, this);
		}
		
	}

}
