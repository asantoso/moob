package com.neusou.moobook;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accessibilityservice.AccessibilityService;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.admob.android.ads.AdView;
import com.neusou.Logger;
import com.neusou.async.UserTask;
import com.neusou.async.UserTaskExecutionScope;
import com.neusou.moobook.activity.LoginActivity;
import com.neusou.moobook.activity.NotificationsActivity;
import com.neusou.moobook.activity.StreamActivity;
import com.neusou.moobook.data.Stream;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.moobook.receiver.OnAlarmReceiver;
import com.neusou.moobook.task.ProcessStreamMultiQueryTask;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;
import com.neusou.web.ImageUrlLoader;
import com.neusou.web.ImageUrlLoader2;

public class App extends Application {
	static final String LOG_TAG = "App";
	
	public static App INSTANCE;
	public Notification mNotification;
	public UserTaskExecutionScope mExecScopeListViewTask =
		new UserTaskExecutionScope("ListViewTask",5,1,5,TimeUnit.SECONDS,10);
	public UserTaskExecutionScope mExecScopeImageLoaderTask =
		new UserTaskExecutionScope("ImgLoaderTask",10,2,5,TimeUnit.SECONDS,50);
	public UserTaskExecutionScope mExecScopePrefetcherTask =
		new UserTaskExecutionScope("PrefetcherTask",2,1,10,TimeUnit.SECONDS,0);

	
	public static final String packageprefix = "com.neusou.moobook";	
	
	// Intents
	public static final String INTENT_CHECK_NOTIFICATIONS = packageprefix+".action.CHECK_NOTIFICATIONS";
	public static final String INTENT_CHECK_STREAMS = packageprefix+".action.CHECK_STREAMS";
	public static final String INTENT_LOGIN = "com.neusou.moobook.action.LOGIN_FACEBOOK";///packageprefix+".action.LOGIN_FACEBOOK";
	public static final String INTENT_PROFILE_UPDATED = packageprefix+".intent.PROFILE_UPDATED";
	public static final String INTENT_NEW_NOTIFICATIONS = packageprefix+".intent.NEW_NOTIFICATIONS";
	public static final String INTENT_STREAMCOMMENTS_UPDATED = packageprefix+".intent.STREAMCOMMENTS_UPDATED";
	public static final String INTENT_WALLPOSTS_UPDATED = packageprefix+".intent.WALLPOSTS_UPDATED";
	public static final String INTENT_GET_TAGGED_PHOTOS = packageprefix+".intent.GET_TAGGED_PHOTOS";
	public static final String INTENT_AUTOUPDATE_STREAMS = packageprefix+".intent.AUTOUPDATE_STREAMS";
	public static final String INTENT_PLAY_NOTIFICATIONS_SOUND = packageprefix+".intent.PLAY_NOTIFICATIONS_SOUND";
	public static final String INTENT_STOP_NOTIFICATIONS_SOUND = packageprefix+".intent.STOP_NOTIFICATIONS_SOUND";
		
	static final int CALLBACK_PROCESS_STREAMS_START = 0;
	static final int CALLBACK_PROCESS_STREAMS_UPDATE = 1;
	static final int CALLBACK_PROCESS_STREAMS_FINISH = 2;
	static final int CALLBACK_PROCESS_STREAMS_PROGRESS = 3;
	static final int CALLBACK_PROCESS_STREAMS_TIMEOUT = 4;
	
	public static final byte MENUITEM_LOGOFF = 0;
	public static final byte MENUITEM_SETTINGS = 1;
	public static final byte MENUITEM_CLEAR = 2;
	public static final byte MENUITEM_GET_WALLCOMMENTS = 3;
	public static final byte MENUITEM_GET_WALLPOSTS = 4;
	public static final byte MENUITEM_TOGGLE_LOGGER = 5;
	
	public static ColorMatrix mWhitishColorMatrix;
	public static ColorMatrixColorFilter mColorFilterWhitish;
	public static ColorMatrix mBlueishColorMatrix;
	public static ColorMatrixColorFilter mColorFilterBlueish;
	public static ColorMatrix mReddishColorMatrix;
	public static ColorMatrixColorFilter mColorFilterReddish;

	public static Drawable mLoadingDrawable;
	
	public static Pattern mUrlPattern = Pattern.compile(
			"(https?://)?([\\d\\w-.]+?\\.(a[cdefgilmnoqrstuwz]|b[abdefghijmnorstvwyz]|c[acdfghiklmnoruvxyz]|d[ejkmnoz]|e[ceghrst]|f[ijkmnor]|g[abdefghilmnpqrstuwy]|h[kmnrtu]|i[delmnoqrst]|j[emop]|k[eghimnprwyz]|l[abcikrstuvy]|m[acdghklmnopqrstuvwxyz]|n[acefgilopruz]|om|p[aefghklmnrstwy]|qa|r[eouw]|s[abcdeghijklmnortuvyz]|t[cdfghjkmnoprtvwz]|u[augkmsyz]|v[aceginu]|w[fs]|y[etu]|z[amw]|aero|arpa|biz|com|coop|edu|info|int|gov|mil|museum|name|net|org|pro)(\\b|\\W(?<!&|=)(?!\\.\\s|\\.{3}).*?))(\\s|$)"			
	);
		
	public ApplicationDBHelper mDBHelper;
	public SQLiteDatabase mDB; 
	static public String imageSDCacheDirectory;
	static public ImageUrlLoader mImageUrlLoader;
	static public ImageUrlLoader2 mImageUrlLoader2;	
	
	public static final int CALLBACK_ADMOB_ONRECEIVE = 1;
	public static final int CALLBACK_ADMOB_ONFAILRECEIVE = 2;
	
	public static final String LOCALCACHEFILE_SESSIONUSERDATA = "sessionuserdata";
	public static final String LOCALCACHEFILE_PROFILEIMAGE = "sessionuserprofileimage";
	public static final String LOCALCACHEFILE_APPIGNORELIST = "appignorelist";
	
	
	public static FBSession mFbSession;
	
	public ManagerThread mManagerThread;
	CountDownLatch mThreadsInitCountdown;
	
	public Bitmap mDefaultProfileBitmap;
	public Bitmap mEmptyBitmap;

	//PendingIntent mCheckNotifPendingIntent ;
	
	public static final byte ALARM_CHECK_NOTIFICATIONS = 0;  
	BroadcastReceiver mBroadcastReceiver;

	public AlarmManager mAlarmManager;
	
	static LocalService mBoundService;
	
	Handler mUIHandler;

	Facebook mFacebook;

	static final short[] mSelectedUserFields = new short[]{User.col_first_name, User.col_last_name, User.col_status, User.col_profile_blurb, User.col_pic, User.col_pic_big, User.col_timezone, User.col_current_location, User.col_uid};

	private static ServiceConnection mConnection = new ServiceConnection() {
	        public void onServiceConnected(ComponentName className, IBinder service) {
	            mBoundService = ((LocalService.LocalBinder)service).getService();
	            Toast.makeText(App.INSTANCE, "Service is connected", Toast.LENGTH_SHORT).show();
	            	            
	        }

	        public void onServiceDisconnected(ComponentName className) {
	            mBoundService = null;
	            Toast.makeText(App.INSTANCE, "Service disconnected", Toast.LENGTH_SHORT).show();
	        }
	    };
	    
	public void initBroadcastReceivers(){
		mBroadcastReceiver = new BroadcastReceiver() {			
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				Logger.l(Logger.DEBUG, LOG_TAG, "[BroadcastReceiver][onReceive()] action: "+action);
				if(action.equals(App.INTENT_LOGIN)){
					Intent loginIntent = new Intent(App.this, LoginActivity.class);
					loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
							Intent.FLAG_ACTIVITY_NO_HISTORY);
					startActivity(loginIntent);
				}else if(action.equals(App.INTENT_STOP_NOTIFICATIONS_SOUND)){
					App.INSTANCE.stopNotificationRingtone();
				}
				else if(action.equals(App.INTENT_PLAY_NOTIFICATIONS_SOUND)){
					App.INSTANCE.playNotificationRingtone();
				}
				
			}
		};
		IntentFilter intentFilters = new IntentFilter(INTENT_LOGIN);	
		intentFilters.addAction(INTENT_STOP_NOTIFICATIONS_SOUND);
		intentFilters.addAction(INTENT_PLAY_NOTIFICATIONS_SOUND);
		registerReceiver(mBroadcastReceiver, intentFilters);
	}
	
	public void removeBroadcastReceivers(){		
		unregisterReceiver(mBroadcastReceiver);
	}
	
	@Override
	public void onCreate() {
		
		super.onCreate();
		INSTANCE = this;
		mAlarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);
		
		Prefs.init(this);
		Logger.l(Logger.DEBUG,LOG_TAG,"onCreate()");
		initObjects();
		updateNotificationAlarm();
		initBroadcastReceivers();
		Intent i = new Intent(this,  LocalService.class);
		//bindService(i, mConnection, Context.BIND_AUTO_CREATE);
		startService(i);		
		
		AnimationDrawable ad = new AnimationDrawable(){
			
		};
	}

	@Override
	public void onTerminate() {
		Logger.l(Logger.DEBUG,LOG_TAG,"onTerminate()");
		super.onTerminate();		
	
		NotificationManager mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mNM.cancelAll();
		
		
		mDB.close();
		mDBHelper.close();
		mFacebook.purgeInactiveOutHandlers(true);
		mFacebook = null;
		
		removeBroadcastReceivers();
	}

	private void initObjects(){
		loadNotificationRingtone();
		Resources res = getResources();
		mDefaultProfileBitmap = BitmapFactory.decodeResource(res, R.drawable.defaultprofileimage);
		mEmptyBitmap = BitmapFactory.decodeResource(res, R.drawable.empty_bitmap);
		
		mUIHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				int code = msg.what;
				
				switch(code){
					case CALLBACK_PROCESS_STREAMS_START:{
						Toast.makeText(App.INSTANCE, "Start", 2000).show();
						break;	
					}
					case CALLBACK_PROCESS_STREAMS_FINISH:{
						Toast.makeText(App.INSTANCE, "Start", 2000).show();
						break;	
					}
					case CALLBACK_PROCESS_STREAMS_TIMEOUT:{
						break;	
					}
					case CALLBACK_PROCESS_STREAMS_PROGRESS:{
						break;	
					}
					case CALLBACK_PROCESS_STREAMS_UPDATE:{
						break;	
					}
				}
				
			}
		};
		
		try {
			ApplicationDBHelper dbh = new ApplicationDBHelper(this);
			dbh.createDataBase(new int[] { R.raw.moobook });
			dbh.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		Util.init(this);
		
		mBlueishColorMatrix = new ColorMatrix(new float[] { 
				0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 
				0.5f, 0.4f, 0.1f, 0.0f, 0.0f, 
				0.7f, 0.4f, 0.1f, 0.0f, 0.0f, 
				0.5f, 0.0f, 0.0f, 0.0f, 0.0f });
	
		mColorFilterBlueish = new ColorMatrixColorFilter(mBlueishColorMatrix);
		
		mWhitishColorMatrix = new ColorMatrix(new float[] { 
				0.5f, 0.4f, 0.0f, 0.0f, 0.0f, 
				0.5f, 0.4f, 0.0f, 0.0f, 0.0f, 
				0.5f, 0.4f, 0.0f, 0.0f, 0.0f, 
				0.6f, 0.0f, 0.0f, 0.0f, 0.0f });
		
		mColorFilterWhitish = new ColorMatrixColorFilter(mWhitishColorMatrix);
		
		mReddishColorMatrix = new ColorMatrix(new float[] { 1.0f, 0.1f, 0.1f,
				0.0f, 0.5f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f });
	
		mColorFilterReddish = new ColorMatrixColorFilter(mReddishColorMatrix);
		
		Resources resources = this.getResources();
		mLoadingDrawable = resources.getDrawable(R.drawable.mobook2_exclaimation_64);
	
		mImageUrlLoader = new ImageUrlLoader(this);
		mImageUrlLoader2 = new ImageUrlLoader2(this);
		mDBHelper = new ApplicationDBHelper(this);
		mDB = mDBHelper.getWritableDatabase();
		mFacebook = Facebook.getInstance();
		mFbSession = getSessionInfo();
		mFacebook.setSession(mFbSession);
		
		mThreadsInitCountdown = new CountDownLatch(1);
		mManagerThread = new ManagerThread(mThreadsInitCountdown);
		mManagerThread.start();
		try{
			mThreadsInitCountdown.await();
		}catch(InterruptedException e){			
		}
		mManagerThread.setOutHandler(mUIHandler);
		mFacebook.registerOutHandler(R.id.outhandler_app, mManagerThread.getInHandler());
	}

	private void initPeriodicSchedule() {
		SharedPreferences sp = this.getSharedPreferences("moobook", 0);
		AlarmManager am = (AlarmManager) this.getSystemService(Service.ALARM_SERVICE);
		boolean isPeriodicCheck = sp.getBoolean(Prefs.KEY_PERIODIC_CHECK_ENABLED, true);
		
		//periodic checking of streams
		if (isPeriodicCheck) {			
			Intent i = new Intent(this, OnAlarmReceiver.class); 
			i.setAction(INTENT_CHECK_STREAMS);
			PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi);
		}
		else{			
		}
				
		//periodic checking of notifications
		boolean isPeriodicNotificationCheck = sp.getBoolean(Prefs.KEY_PERIODIC_NOTIFICATION_CHECK_ENABLED, true);
		if (isPeriodicNotificationCheck) {
			long notificationCheckInterval = 10000000l; 
			Intent i = new Intent(this, OnAlarmReceiver.class);
			i.setAction(INTENT_CHECK_NOTIFICATIONS);
			PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);			
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime(),
					notificationCheckInterval,
					pi);
		}
		else{			
		}
		
	}

	private PendingIntent createPendingIntent(String intentAction){		
		Intent i = new Intent(this, OnAlarmReceiver.class);			
		i.setAction(intentAction);
		PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
		return pi;
	}
		
	public void setAlarm(long interval, String intentAction){
		PendingIntent pi = createPendingIntent(intentAction);		
		mAlarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+interval,interval,pi);		
	}
	
	public void clearAlarm(String intentAction){
		PendingIntent pi = createPendingIntent(intentAction);
		mAlarmManager.cancel(pi);		
	}
	
	public void getSessionUserInfo(){		
		mFacebook.getSessionUserInfo(mSelectedUserFields);		
	}

	/**
	 * Saves session info in SharedPreferences
	 * 
	 * @param fbsession
	 */
	public void saveSessionInfo(FBSession session) {
		Log.d(LOG_TAG, "Saving session info");
		if(session == null){
			Log.d(LOG_TAG, "Saving session info. provided session is null");
			return;
		}
		SharedPreferences settings = getSharedPreferences(Prefs.session_preffile, Context.MODE_PRIVATE);
		Editor editor = settings.edit();
		editor.putInt(Prefs.session_expires, session.expires);
		editor.putLong(Prefs.session_uid, session.uid);
		editor.putString(Prefs.session_sig, session.sig);
		editor.putString(Prefs.session_secret, session.secret);
		editor.putString(Prefs.session_key, session.key);
		editor.putString(Prefs.session_json, session.json);
		boolean saved = editor.commit();
		Log.d(LOG_TAG, "Saving session info. Success?: "+saved);
	}

	/**
	 * Restores Facebook session data from SharedPreferences
	 * @param fbSession the session instance, can use existing instance to avoid 
	 * @return
	 */
	public FBSession getSessionInfo() {
		Log.d(LOG_TAG, "Get session info");
		SharedPreferences settings = getSharedPreferences(Prefs.session_preffile, Context.MODE_PRIVATE);
		FBSession session = new FBSession();
		session.expires = settings.getInt(Prefs.session_expires,session.expires);
		session.uid = settings.getLong(Prefs.session_uid, session.uid);
		session.sig = settings.getString(Prefs.session_sig, session.sig);
		session.secret = settings.getString(Prefs.session_secret,session.secret);
		session.key = settings.getString(Prefs.session_key,session.key);
		session.json = settings.getString(Prefs.session_json, session.json);
		Log.d(LOG_TAG, "restoreSessionInfo. json = " + session.json+ ", sesskey: " + session.key);				
		return session;
	}

	/**
	 * Deletes Facebook session data in SharedPreferences
	 */
	public void deleteSessionInfo() {
		Log.d(LOG_TAG, "Deleting session info");
		SharedPreferences settings = getSharedPreferences(Prefs.session_preffile, Context.MODE_PRIVATE);
		Editor editor = settings.edit();
		editor.remove(Prefs.session_expires);
		editor.remove(Prefs.session_uid);
		editor.remove(Prefs.session_sig);
		editor.remove(Prefs.session_secret);
		editor.remove(Prefs.session_key);
		editor.remove(Prefs.session_json);		
		editor.commit();		
		mFacebook.setSession(null);
	}
	
	  public void writeField(DataOutputStream out, String boundary, String name, String value) throws IOException{
		 String encoding ="UTF-8"; 
		out.write("--".getBytes(encoding));
		 out.write(boundary.getBytes(encoding));
		 out.write("\r\n".getBytes(encoding));
		 // write content header
		 out.write(("Content-disposition: form-data; name=\"" + name + "\"").getBytes(encoding));
		 out.write("\r\n".getBytes(encoding));
		 out.write("\r\n".getBytes(encoding));
		 // write content
		 out.write(value.getBytes(encoding));
		 out.write("\r\n".getBytes(encoding));
		}

		public void writeField(DataOutputStream out, String boundary, String mimeType, String fileName, InputStream data) throws IOException{
		 String encoding ="UTF-8";
			out.write("--".getBytes(encoding));
		 out.write(boundary.getBytes(encoding));
		 out.write("\r\n".getBytes(encoding));

		 out.write(("Content-disposition: form-data; filename=\"" + fileName + "\"").getBytes(encoding));
		 out.write("\r\n".getBytes(encoding));
		 
		 if(mimeType != null) {
			 out.write(("Content-Type: " + mimeType).getBytes(encoding));
		 //  out.write("\r\n".getBytes(encoding));
		 }

		 BufferedInputStream bis = new BufferedInputStream(data);
 		 byte[] buffer = new byte[1024];
		 while(true){
			 
			 int numbytesread = bis.read(buffer);
			 if(numbytesread == -1){				
				break;
			}
			 Logger.l(Logger.DEBUG,LOG_TAG,"writing.."+numbytesread);
			out.write(buffer, 0, numbytesread);
		 }		
		 out.write("\r\n".getBytes(encoding));
		 out.write("--".getBytes(encoding));
		 out.write(boundary.getBytes(encoding));
		 out.write("--".getBytes(encoding));
		// out.write("\r\n".getBytes(encoding));
		 Logger.l(Logger.DEBUG,LOG_TAG,"writing.. done. "+out.size());
		}

		public static String[] getCommentsUids(JSONArray data, String[] holder){
			int numComments = data.length();
			if(holder == null || holder.length < numComments){
				holder = new String[numComments];
			}
			for(int i=0;i<numComments;i++){
				try{
					holder[i] = data.getJSONObject(i).getString(FBComment.fields_fromid);
				}catch(JSONException e){				
				}
			}
			return holder;
		}
		
		
	public void test(){
		
	}	
	
	public void getWallPostsFromCloud(long created_date){
		Logger.l(Logger.DEBUG, LOG_TAG, "[getWallPosts()] created_date: "+created_date);
		FBSession session = mFacebook.getSession();
		
		Bundle callbackData = new Bundle();
		callbackData.putString(ManagerThread.XTRA_CALLBACK_INTENT_ACTION, App.INTENT_WALLPOSTS_UPDATED);
		
		mFacebook.getWallPosts(
				R.id.outhandler_app,
				callbackData,
				Long.toString(session.uid),
				created_date,
				ManagerThread.CALLBACK_GET_WALLPOSTS,
				ManagerThread.CALLBACK_SERVERCALL_ERROR,
				ManagerThread.CALLBACK_TIMEOUT_ERROR, 
				0l, 
				10l, 
				0l);
				
	}
	
	public void getWallPostsCommentsFromCloud(){
		Logger.l(Logger.DEBUG, LOG_TAG, "[getWallPostsComments()]");
		FBSession session = mFacebook.getSession();
		//get the first post entry
		
		Cursor c = mDBHelper.getStreams(mDB, Facebook.STREAMMODE_NEWSFEED, session.uid, null, 1, 0);
		int numComments = 0;
		if(c != null){
			numComments = c.getCount();
			Logger.l(Logger.DEBUG, LOG_TAG, "[getWallPostsComments()] numComments: "+numComments);
		}
		
		if(numComments > 0){
			if(c.moveToFirst()){
				String message = c.getString(mDBHelper.START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC + Stream.col_message);
				String post_id =  c.getString(mDBHelper.START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC + Stream.col_post_id);
			
				Logger.l(Logger.DEBUG, LOG_TAG, "[getWallPostsComments()] post_id: "+post_id+", message: "+message);		
				mFacebook.getComments(R.id.outhandler_app, Facebook.COMMENT_TYPE_STREAMPOSTS, null, post_id, ManagerThread.CALLBACK_GET_COMMENTS, BaseManagerThread.CALLBACK_SERVERCALL_ERROR, BaseManagerThread.CALLBACK_TIMEOUT_ERROR, 0, 20, 0);
			}
		}
		
	}

	Ringtone mNotifRingtone;
	public Uri mNotifRingtoneUri;
	
	public static void initAdView(final AdView mAdView, final Handler mUIHandler){
		mAdView.setVisibility(View.VISIBLE);
		mAdView.setKeywords("gift");		
		mAdView.setBackgroundColor(0xFFaaaaaa);
		mAdView.setTextColor(0xFF111111);
		mAdView.setGoneWithoutAd(false);
		mAdView.setRequestInterval(60);
		mAdView.setListener(new AdView.AdListener() {			
			@Override
			public void onReceiveAd(AdView adView) {
				if(mUIHandler != null){
					mUIHandler.sendEmptyMessage(CALLBACK_ADMOB_ONRECEIVE);
				}				
			}
			
			@Override
			public void onNewAd() {				
			}
			
			@Override
			public void onFailedToReceiveAd(AdView adView) {
				if(mUIHandler != null){
					mUIHandler.sendEmptyMessage(CALLBACK_ADMOB_ONFAILRECEIVE);
				}				
			}			
		});		
	}

	public void stopNotificationRingtone(){
		if(mNotifRingtone != null){
			if(mNotifRingtone.isPlaying()){
				mNotifRingtone.stop();		
			}
		}
	}
	
	public void delayedBroadcast(final Intent i, long delay){
		mUIHandler.postDelayed(
				new Runnable(){
					@Override
					public void run() {
						sendBroadcast(i);						
					}
				}, delay
		);
	}
	
	public void playNotificationRingtone(){
		
		if(isNotificationEnabled() && (mNotifRingtoneUri != null) && mNotifRingtone != null){
			
			//TODO Logger remove
			Logger.l(Logger.VERBOSE, LOG_TAG, "[playNotificationRingtone()] Ringtone URI:"+mNotifRingtoneUri.toString());
			
			if(mNotifRingtone.isPlaying()){
				mNotifRingtone.stop();
				mUIHandler.postDelayed(new Runnable(){
					@Override
					public void run() {						
						mNotifRingtone.play();
					}
					
				}, 500);
			}else{
				mNotifRingtone.play();
			}
		}		
	}
	
	public boolean isNotificationVibrationEnabled(){
		Resources res = getResources();
		SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(this);						
		boolean notifVibrationEnabled = sprefs.getBoolean(res.getString(R.string.preference_notification_vibration_enabled), true);		
		return notifVibrationEnabled;
	}
	
	public boolean isNotificationLedsEnabled(){
		Resources res = getResources();
		SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(this);						
		boolean notifLightsEnabled = sprefs.getBoolean(res.getString(R.string.preference_notification_light_enabled), true);		
		return notifLightsEnabled;		
	}
	
	public boolean isNotificationEnabled(){
		Resources res = getResources();
		SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(this);						
		boolean notifEnabled = sprefs.getBoolean(res.getString(R.string.preference_notification_enabled), true);		
		return notifEnabled;		
	}
	
	/**
	 * Loads a ringtone saved in shared preferences
	 */
	public void loadNotificationRingtone(){
		mNotifRingtoneUri = getNotificationRingtoneUri();
		loadNotificationRingtone(mNotifRingtoneUri);				
	}
	
	/**
	 * Loads a ringtone from the given Uri
	 * @param soundUri
	 */
	public void loadNotificationRingtone(Uri soundUri){		
		if(soundUri != null){
			mNotifRingtone = RingtoneManager.getRingtone(this, soundUri);
			mNotifRingtone.setStreamType(AudioManager.STREAM_NOTIFICATION);			
		}
	}
	
	/**
	 * Reads selected ringtone uri in SharedPreferences
	 * @return
	 */
	public Uri getNotificationRingtoneUri(){
		//AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		//SoundPool sp = new android.media.SoundPool(1,AudioManager.STREAM_NOTIFICATION,0);
		Resources res = getResources();
		SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(this);						
		String soundUri = sprefs.getString(res.getString(R.string.preference_notification_sound_resource),null);
		if(soundUri != null && soundUri.length() > 0){
			return Uri.parse(soundUri);
		}else{
			return null;
		}
	}
	
	/**
	 * Gets notification polling interval in minutes
	 * @return
	 */
	public int getNotificationPollingInterval(){
		Resources res = getResources();
		SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(this);						
		int defaultIntervalMinutes = 60;
		String intervalStr = sprefs.getString(res.getString(R.string.preference_notification_checkpollinginterval), null);
		int interval;
		try{
			interval = Integer.valueOf(intervalStr);		
		}
		catch(NumberFormatException e){
			interval = defaultIntervalMinutes;
		}
		return interval;
	}
	
	public Notification createNotification(String content_title, String content_text, int total){		
		Notification notification = new Notification();
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, NotificationsActivity.class),
				Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
		);
		notification.setLatestEventInfo(this, content_title, content_text, contentIntent);
		boolean isVibrate = isNotificationVibrationEnabled();
		if(isVibrate){
			notification.vibrate = new long[]{
					//1000,200,800,190,640,175,512,150,409,100,327
					//off, on, ...
					0,700,300,300,150,150,1500,
					1000,300,300,150,150,					
					//0,500,500,500,500,100,500,100,500
					};
			
		}
		notification.when = System.currentTimeMillis();
		notification.number = total;
		notification.icon = android.R.drawable.ic_dialog_info;
		//notification.flags = Notification.DEFAULT_LIGHTS;
		
		if(isNotificationLedsEnabled()){
			notification.ledARGB = 0xffff0000;
			notification.ledOffMS = 4000;
			notification.ledOnMS = 5000;
			notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		}
		
		notification.audioStreamType = AudioManager.STREAM_NOTIFICATION;		
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		Intent deleteIntent = new Intent(INTENT_STOP_NOTIFICATIONS_SOUND);
		PendingIntent deletePendingIntent = PendingIntent.getBroadcast(this, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		notification.deleteIntent = deletePendingIntent;
		return notification;
	}
	
	  
    /**
     * Updates alarm for notification from the preferences
     */
	public void updateNotificationAlarm(){
    	boolean enabled = App.INSTANCE.isNotificationEnabled();
		updateNotificationAlarm(enabled);
    }
    
    /**
     * Updates alarm for notification manually, interval is read from preferences.
     * @param enabled
     */
    public void updateNotificationAlarm(boolean enabled){
    	clearAlarm(App.INTENT_CHECK_NOTIFICATIONS);
		if(enabled){
			long pollingInterval = App.INSTANCE.getNotificationPollingInterval() * 1000;
			App.INSTANCE.setAlarm(pollingInterval, App.INTENT_CHECK_NOTIFICATIONS);
		}
    }
    
    /**
     * Updates alarm for notification manually using specified interval
     * @param enabled
     */
    public void updateNotificationAlarm(boolean enabled, long pollingInterval){
    	clearAlarm(App.INTENT_CHECK_NOTIFICATIONS);
    	if(enabled){			
			setAlarm(pollingInterval, App.INTENT_CHECK_NOTIFICATIONS);
		}		
    }
        
    public void addAppToIgnoreList(String name, int id){
    	JSONArray ignoreList = getAppIgnoreList();
    	if(ignoreList == null){
    		ignoreList = new JSONArray();
    		JSONObject obj = new JSONObject();
    		try{
    			obj.put(name, id);
    			ignoreList.put(obj);
    			Util.writeToLocalCache(this, ignoreList.toString(), App.LOCALCACHEFILE_APPIGNORELIST);
    		}catch(JSONException e){    			
    		} 
    	}    	    	
    }
    
    public JSONArray getAppIgnoreList(){
    	String text = Util.readStringFromLocalCache(this, App.LOCALCACHEFILE_APPIGNORELIST);
    	try{
    		JSONArray jsonArray = new JSONArray(text);
    		return jsonArray;
    	}catch(JSONException e){
    		return null;
    	}
    }
    
	/*
	public static class WorkerManagerThread extends BaseManagerThread {
		
		public WorkerManagerThread(CountDownLatch cdl) {
			super(cdl);			
		}
	
		CountDownLatch mWaitCommentsUsersCountdown;
		ProcessStreamMultiQueryTask mProcessStreamMultiQueryDataTask = null;
		
		@Override
		public void doBusiness(Bundle data, int code, FBWSResponse fbresponse) {
			switch(code){
				
				case ManagerThread.CALLBACK_GET_WALLPOSTS:{
					Logger.l(Logger.DEBUG, LOG_TAG, "[WorkerManagerThread][callback_get_wallposts] ");
					
					if(
							mProcessStreamMultiQueryDataTask == null ||						
							mProcessStreamMultiQueryDataTask.getStatus() == UserTask.Status.FINISHED
					){
						Logger.l(Logger.DEBUG, LOG_TAG, "[WorkerManagerThread][callback_get_wallposts] processing wall posts.");
						
					Toast.makeText(App.INSTANCE, "processing wallposts data", 2000).show();
						
						mProcessStreamMultiQueryDataTask = new ProcessStreamMultiQueryTask(
						App.INSTANCE, mOutHandler, CALLBACK_PROCESS_STREAMS_FINISH, null
						);					
						mProcessStreamMultiQueryDataTask.execute(data);
						
					}else{
						Toast.makeText(App.INSTANCE, "Cant call fetch streams mq: "+mProcessStreamMultiQueryDataTask.getStatus(), 2000).show();
					}
					
					Intent updateWall = new Intent(App.INTENT_WALLPOSTS_UPDATED);
					updateWall.putExtras(data);
					App.INSTANCE.sendBroadcast(updateWall);
					
					break;
				}
			
				
				case ManagerThread.CALLBACK_POSTPHOTO:{
					
					break;
				}
				
				case ManagerThread.CALLBACK_GET_COMMENTS:{
					
					Logger.l(Logger.DEBUG, LOG_TAG, "[callback_get_comments] "+fbresponse.data);
					if(!fbresponse.hasErrorCode && fbresponse.jsonArray != null){
						int numComments = fbresponse.jsonArray.length();
						if(numComments > 0){
							String[] uids = App.getCommentsUids(fbresponse.jsonArray, null);									
							Logger.l(Logger.DEBUG, LOG_TAG, "[callback_get_comments] getting users");

							Facebook.getInstance().getCommentsUsers(R.id.outhandler_app, uids, ManagerThread.CALLBACK_GET_COMMENTS_USERS, BaseManagerThread.CALLBACK_SERVERCALL_ERROR, BaseManagerThread.CALLBACK_TIMEOUT_ERROR, 0);							
							mWaitCommentsUsersCountdown = new CountDownLatch(1);
														
							
						}
					}						
					Intent updateComments = new Intent(App.INTENT_STREAMCOMMENTS_UPDATED);
					Logger.l(Logger.DEBUG, LOG_TAG, "[callback_get_comments] fbwsresponse xtra:"+ FBWSResponse.XTRA_OBJECT);
					data.putParcelable(FBWSResponse.XTRA_OBJECT, fbresponse);
					updateComments.putExtras(data);
					App.INSTANCE.sendBroadcast(updateComments);					
					break;
				}
				
				case ManagerThread.CALLBACK_GET_COMMENTS_USERS:{
					Logger.l(Logger.DEBUG, LOG_TAG, "[WorkerManagerThread] [callback_get_comments_users] "+fbresponse.data);
					//data_users = fbresponse.jsonArray;							
					mWaitCommentsUsersCountdown.countDown();
					break;
				}
				
				
				case ManagerThread.CALLBACK_GET_USERDATA:{
					String parsed;
					if(fbresponse.jsonArray!=null){
						try {							
							parsed = fbresponse.jsonArray.toString(2);
							Logger.l(Logger.DEBUG, LOG_TAG,"[WorkerManagerThread] [handleMessage()] [callback_get_user_data] json data: "+ parsed);	
							if(fbresponse.hasErrorCode){								
							}else{
								//Logger.l(Logger.DEBUG,LOG_TAG,"[WorkerManagerThread] [handleMessage()] [callback_get_user_data] sending message to ui handler.");																
							}
							Logger.l(Logger.DEBUG, LOG_TAG,"[WorkerManagerThread] [handleMessage()] [callback_get_user_data] saving to local cache");
							Util.saveToLocalCache(App.INSTANCE, fbresponse.data,  LOCALCACHEFILE_SESSIONUSERDATA);
							String restored = Util.readStringFromLocalCache(App.INSTANCE, LOCALCACHEFILE_SESSIONUSERDATA);
							Logger.l(Logger.DEBUG, LOG_TAG,"[WorkerManagerThread] [handleMessage()] [callback_get_user_data] restored cache: "+ restored);
						
							Intent updateProfileIntent = new Intent(INTENT_PROFILE_CHANGED);
							App.INSTANCE.sendBroadcast(updateProfileIntent);
						} catch (JSONException e) {
							e.printStackTrace();
						}		
					}
					break;
				}
				
			}
				
		}
	
	};
	*/
	
}