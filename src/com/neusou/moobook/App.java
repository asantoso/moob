package com.neusou.moobook;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.admob.android.ads.AdManager;
import com.admob.android.ads.AdView;
import com.neusou.Logger;
import com.neusou.async.UserTaskExecutionScope;
import com.neusou.moobook.activity.EventsActivity;
import com.neusou.moobook.activity.LoginActivity;
import com.neusou.moobook.activity.NotificationsActivity;
import com.neusou.moobook.activity.StreamActivity;
import com.neusou.moobook.activity.ViewAlbumsActivity;
import com.neusou.moobook.activity.ViewCommentsActivity;
import com.neusou.moobook.activity.ViewContactsActivity;
import com.neusou.moobook.activity.ViewPhotosActivity;
import com.neusou.moobook.data.ContextProfileData;
import com.neusou.moobook.data.Stream;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.moobook.receiver.OnAlarmReceiver;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;
import com.neusou.moobook.view.ActionBar;
import com.neusou.web.ImageUrlLoader;
import com.neusou.web.ImageUrlLoader2;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderInput;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderProgress;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderResult;

public class App extends Application {
	static final String LOG_TAG = Logger.registerLog(App.class);
	
	public static App INSTANCE;
	public Resources mResources;
	public ActionBar mActionBar;
	
	public Notification mNotification;
	public UserTaskExecutionScope mExecScopeListViewTask =
		new UserTaskExecutionScope("ListViewTask",5,1,5,TimeUnit.SECONDS,10);
	public UserTaskExecutionScope mExecScopeImageLoaderTask =
		new UserTaskExecutionScope("ImgLoaderTask",10,2,5,TimeUnit.SECONDS,50);
	public UserTaskExecutionScope mExecScopePrefetcherTask =
		new UserTaskExecutionScope("PrefetcherTask",2,1,10,TimeUnit.SECONDS,0);

	
	public static final String packageprefix = "com.neusou.moobook";	
	
	// Intents
	public static final String INTENT_CHECK_NOTIFICATIONS = packageprefix+".intent.CHECK_NOTIFICATIONS";
	public static final String INTENT_CHECK_STREAMS = packageprefix+".intent.CHECK_STREAMS";
	public static final String INTENT_LOGIN = packageprefix+".intent.LOGIN_FACEBOOK";///packageprefix+".action.LOGIN_FACEBOOK";
	public static final String INTENT_SESSIONUSER_PROFILE_RECEIVED = packageprefix+".intent.SESSIONUSER_PROFILE_RECEIVED";
	public static final String INTENT_SESSIONUSER_PROFILE_UPDATED = packageprefix+".intent.SESSIONUSER_PROFILE_UPDATED";
	public static final String INTENT_NEW_NOTIFICATIONS = packageprefix+".intent.NEW_NOTIFICATIONS";
	public static final String INTENT_STREAMCOMMENTS_UPDATED = packageprefix+".intent.STREAMCOMMENTS_UPDATED";
	public static final String INTENT_WALLPOSTS_UPDATED = packageprefix+".intent.WALLPOSTS_UPDATED";
	public static final String INTENT_GET_TAGGED_PHOTOS = packageprefix+".intent.GET_TAGGED_PHOTOS";
	public static final String INTENT_AUTOUPDATE_STREAMS = packageprefix+".intent.AUTOUPDATE_STREAMS";
	public static final String INTENT_PLAY_NOTIFICATIONS_SOUND = packageprefix+".intent.PLAY_NOTIFICATIONS_SOUND";
	public static final String INTENT_STOP_NOTIFICATIONS_SOUND = packageprefix+".intent.STOP_NOTIFICATIONS_SOUND";
	public static final String INTENT_DELETE_COMMENT = packageprefix+".intent.DELETE_COMMENT";
	
	//intent result (post processing)
	public static final String INTENT_POST_COMMENT = packageprefix+".intent.POST_COMMENT";
	public static final String INTENT_POST_STREAM = packageprefix+".intent.POST_STREAM";
	
	public static final String INTENT_GET_ALBUMS =  packageprefix+".intent.GET_ALBUMS";
	
	public static final String INTENT_ACTION_VIEW_FEED = "view.feed";
	public static final String INTENT_ACTION_VIEW_WALL = "view.wall";
	public static final String INTENT_ACTION_VIEW_PROFILE = "view.profile";
	public static final String INTENT_ACTION_VIEW_PHOTOS = "view.photos";
	public static final String INTENT_ACTION_VIEW_TAGGED_PHOTOS = "view.tagged.photos";
	public static final String INTENT_ACTION_VIEW_EVENTS = "view.events";
	
	public static final String XTRA_SESSION_USER_SELECTED_FIELDS = "xtr.sess.usr.sel.flds";
	
	static final int CALLBACK_PROCESS_STREAMS_START = 0;
	static final int CALLBACK_PROCESS_STREAMS_UPDATE = 1;
	static final int CALLBACK_PROCESS_STREAMS_FINISH = 2;
	static final int CALLBACK_PROCESS_STREAMS_PROGRESS = 3;
	static final int CALLBACK_PROCESS_STREAMS_TIMEOUT = 4;
	
	public static final int MENUITEM_LOGOFF = 1;
	public static final int MENUITEM_SETTINGS = 2;
	public static final int MENUITEM_CLEAR = 3;
	public static final int MENUITEM_GET_WALLCOMMENTS = 4;
	public static final int MENUITEM_GET_WALLPOSTS = 5;
	public static final int MENUITEM_TOGGLE_LOGGER = 6;
	public static final int MENUITEM_USER_WALL = 7;
	public static final int MENUITEM_VIEWALBUMS = 8;
	public static final int MENUITEM_STREAMS = 9;
	public static final int MENUITEM_NETWORKS = 10;
	public static final int MENUITEM_NOTIFICATIONS = 11;
	public static final int MENUITEM_EVENTS = 12;
	public static final int MENUITEM_TOGGLE_MODE = 13;
	public static final int MENUITEM_REFRESH_STREAM = 14;
	public static final int MENUITEM_CLEAR_STREAM = 15;
	public static final int MENUITEM_CHAT = 16;
	public static final int MENUITEM_CLEARALL = 17;	
	public static final int CONTEXT_MENUITEM_PROFILE  = 18;
	
	public static final int CONTEXT_MENU_TOPHEADER_PROFILE  = 1;
	public static final int CONTEXT_MENU_OTHERS  = 2;
	

	// process flags, a marker in the database rows to indicate if a user is a friend or not, or if a streampost is part of the session user livefeed or not.
	public static final int PROCESS_FLAG_STREAM_SESSIONUSER = 1;
	public static final int PROCESS_FLAG_STREAM_OTHER = 2;
	public static final int PROCESS_FLAG_IGNORE = 0;
	public static final int PROCESS_FLAG_USER_CONNECTED = 1;
	public static final int PROCESS_FLAG_USER_OTHER = 2;
	
	
	public static final int USERACTION_VIEWWALL = 1;
	public static final int USERACTION_VIEWSTREAM = 2;	
	public static final int USERACTION_VIEWCOMMENT = 3;
	public static final int USERACTION_OTHERS = 4;
	
	
	public static final int ACTIVITY_VIEW_MY_FEED = 1;
	public static final int ACTIVITY_VIEW_MY_WALL = 2;
	public static final int ACTIVITY_VIEW_MY_EVENTS = 3;
	public static final int ACTIVITY_VIEW_MY_NOTIFICATIONS = 4;
	public static final int ACTIVITY_VIEW_MY_NETWORK = 5;
	public static final int ACTIVITY_VIEW_MY_PHOTOS = 6;
	public static final int ACTIVITY_VIEW_MY_TAGGED_PHOTOS = 7;
	public static final int ACTIVITY_VIEW_MY_PROFILE = 8;
	public static final int ACTIVITY_VIEW_COMMENTS = 9;
	public static final int ACTIVITY_VIEW_SETTINGS = 10;	
	public static final int ACTIVITY_VIEW_OTHER_WALL = 11;
	public static final int ACTIVITY_VIEW_OTHER_PHOTOS = 12;
	public static final int ACTIVITY_VIEW_OTHER_TAGGED_PHOTOS = 13;
	public static final int ACTIVITY_VIEW_OTHER_PROFILE = 14;
	
	// process flag for streams
	public static final String FLAG_STREAM_PROCESS_FLAG = "prsflg.stream";
	// process flag for user
	public static final String FLAG_USER_PROCESS_FLAG = "prsflag.user";
	
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
	
	public static int MINIMUM_IMAGE_WIDTH = 10;
	public static int MINIMUM_IMAGE_HEIGHT = 10;

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
		Logger.l(Logger.DEBUG,LOG_TAG,"onCreate()");
		INSTANCE = this;
		mAlarmManager = (AlarmManager) getSystemService(Service.ALARM_SERVICE);		
		Prefs.init(this);
		initObjects();
		updateNotificationAlarm();
		initBroadcastReceivers();
		Intent i = new Intent(this,  LocalService.class);
		//bindService(i, mConnection, Context.BIND_AUTO_CREATE);
		startService(i);		
		
		AnimationDrawable ad = new AnimationDrawable(){
			
		};
		
		AdManager.setInTestMode(false);
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
		mActionBar = new ActionBar();
		mDefaultProfileBitmap = BitmapFactory.decodeResource(res, R.drawable.defaultprofileimage);
		mEmptyBitmap = BitmapFactory.decodeResource(res, R.drawable.empty_bitmap);
		mResources = getResources();
		mUIHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				int code = msg.what;
				
				switch(code){
					case CALLBACK_PROCESS_STREAMS_START:{
						
						break;	
					}
					case CALLBACK_PROCESS_STREAMS_FINISH:{
						
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
				0.5f, 0.2f, 0.0f, 0.0f, 0.0f });
	
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
	
	/**
	 * Gets basic information about current user session.
	 * These information are retrieved: Name, Picture
	 */
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
		Logger.l(Logger.DEBUG, LOG_TAG, "Deleting session info");
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
		
		/**
		 * Saves current session user profile data
		 * @param data JSON text data
		 * @throws FileNotFoundException
		 */
	public static void saveUserSessionData(String data) {
		Util.writeStringToLocalCache(App.INSTANCE,data,App.LOCALCACHEFILE_SESSIONUSERDATA);
	}
	
	public static User getUserSessionData() throws FileNotFoundException{
		String cachedUserInfo = null;
		
		cachedUserInfo = Util.readStringFromLocalCache(App.INSTANCE, App.LOCALCACHEFILE_SESSIONUSERDATA);
		
		if(cachedUserInfo == null){
			return null;
		}
		
		User user = new User();
		try{
			JSONArray users = new JSONArray(cachedUserInfo);
			JSONObject userJson = users.getJSONObject(0);													
			String firstname = userJson.getString(FBUser.fields_first_name);
			String lastname = userJson.getString(FBUser.fields_last_name);
			user.name = firstname + " " + lastname;			
			user.pic  = userJson.getString(FBUser.fields_pic);
			user.pic_big  = userJson.getString(FBUser.fields_pic_big);
			JSONObject statusJson = userJson.getJSONObject(FBUser.fields_status);
			user.status = statusJson.getString(FBUser.fields_status_message);
			user.profile_blurb = userJson.getString(FBUser.fields_profile_blurb);
			user.timezone = userJson.getString(FBUser.fields_timezone);
			user.current_location = userJson.getString(FBUser.fields_current_location);			
		}catch(JSONException e){				
			e.printStackTrace();
		}
		
		return user;
	}
	
	public void showVirtualKeyboard(View view){
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
		

	}
	
	public void hideVirtualKeyboard(View view){
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
		
		Cursor c = mDBHelper.getStreams(mDB, Facebook.STREAMMODE_NEWSFEED, PROCESS_FLAG_STREAM_SESSIONUSER, session.uid, null, 1, 0);
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
	
	public static void initAdMob(final AdView mAdView, final Handler mUIHandler){		
		mAdView.setVisibility(View.VISIBLE);
		mAdView.setKeywords("games flower gifts dating iphone love");
		mAdView.setBackgroundColor(0xFFaaaaaa);
		mAdView.setTextColor(0xFF111111);
		mAdView.setGoneWithoutAd(true);
		mAdView.setRequestInterval(30);
		mAdView.setListener(new AdView.AdListener() {			
			@Override
			public void onReceiveAd(AdView adView) {
				if(mUIHandler != null){
					mUIHandler.sendEmptyMessage(ManagerThread.CALLBACK_ADMOB_ONRECEIVE);
				}				
			}
			
			@Override
			public void onNewAd() {		
				if(mUIHandler != null){
					mUIHandler.sendEmptyMessage(ManagerThread.CALLBACK_ADMOB_ONNEWAD);
				}
			}
			
			@Override
			public void onFailedToReceiveAd(AdView adView) {
				if(mUIHandler != null){
					mUIHandler.sendEmptyMessage(ManagerThread.CALLBACK_ADMOB_ONFAILRECEIVE);
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
        
    public void gotoFirstPage(){
    	Intent i = new Intent(this, StreamActivity.class);
		ContextProfileData cpd;
		cpd = App.createSessionUserContextProfileData();
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.putExtra(StreamActivity.XTRA_STREAMMODE, Facebook.STREAMMODE_LIVEFEED);
		i.putExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT, cpd);
		startActivity(i);
    }
    
    public void logOff(){
		App.INSTANCE.deleteSessionInfo();
		Intent i = new Intent(INTENT_LOGIN);
		i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY|
				Intent.FLAG_ACTIVITY_NEW_TASK|
				Intent.FLAG_ACTIVITY_CLEAR_TOP
		);
		App.INSTANCE.mDBHelper.clearAllTables(App.INSTANCE.mDB);
		sendBroadcast(i);
    }
    
    public void addAppToIgnoreList(String name, int id){
    	JSONArray ignoreList = getAppIgnoreList();
    	if(ignoreList == null){
    		ignoreList = new JSONArray();
    		JSONObject obj = new JSONObject();
    		try{
    			obj.put(name, id);
    			ignoreList.put(obj);
    			Util.writeStringToLocalCache(this, ignoreList.toString(), App.LOCALCACHEFILE_APPIGNORELIST);
    		}catch(JSONException e){    			
    		} 
    	}    	    	
    }
    
    public JSONArray getAppIgnoreList(){
    	String text = null;
    	try{
    		text = Util.readStringFromLocalCache(this, App.LOCALCACHEFILE_APPIGNORELIST);
    	}catch(FileNotFoundException e){
    		return null;
    	}
    	try{
    		JSONArray jsonArray = new JSONArray(text);
    		return jsonArray;
    	}catch(JSONException e){
    		return null;
    	}
    }
        
    public static void showPost(Activity act, String id) {	
		Intent showComments = ViewCommentsActivity.getIntent(act);
		showComments.putExtra(ViewCommentsActivity.XTRA_POSTID, id);
		showComments.putExtra(ViewCommentsActivity.XTRA_OBJECTID, id);
		showComments.putExtra(ViewCommentsActivity.XTRA_CLEARDATA, true);
		showComments.setFlags( Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
				| Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		act.startActivity(showComments);
	}
    
    public static void showUserWall(Activity act, long uid, String username) {
    	Intent viewWall = StreamActivity.getIntent(act);
    	//viewWall.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    	Bundle b = new Bundle();    	
    	ContextProfileData cpd = new ContextProfileData();
    	cpd.name = username;
    	cpd.actorId = uid;    	
    	//b.putString(StreamActivity.XTRA_USERNAME, username);
    	b.putParcelable(ContextProfileData.XTRA_PARCELABLE_OBJECT, cpd);
    	b.putLong(StreamActivity.XTRA_USERID, uid);
    	b.putByte(StreamActivity.XTRA_STREAMMODE, Facebook.STREAMMODE_WALLFEED);
    	viewWall.putExtras(b);    	
    	act.startActivity(viewWall);
    }
    
    /**
     * Extracts one fql result set out of the larger multi queries result set.
     * 
     * @param key The FQL key in the multiquery result set.
     * @param jsonMultiQueriesResult The larger multiqueries result set.
     * @return  The JSON array corresponding to the FQL key.
     */
    public static JSONArray getFqlResultSet(String key, String jsonMultiQueriesResult){
    	try{
    		JSONArray multiQueryResult = new JSONArray(jsonMultiQueriesResult);
    		return getFqlResultSet(key, multiQueryResult);
    	}catch(JSONException e){
    		return null;
    	}
    }
    
    /**
     * Extracts one fql result set out of the larger multi queries result set.
     * 
     * @param key The FQL key in the multiquery result set.
     * @param jsonMultiQueriesResult The larger multiqueries result set.
     * @return  The JSON array corresponding to the FQL key.
     */
    public static JSONArray getFqlResultSet(String key, JSONArray multiQueriesResult){
    	int num = multiQueriesResult.length();
    	try{
    	Iterator<String> it = multiQueriesResult.toJSONObject(null).keys();
    	for(int i=0;it.hasNext();i++){
    		if(it.next().equals(key)){
    			return multiQueriesResult.getJSONObject(i).getJSONArray(FQL.FQL_RESULT_SET);
    		}
    	}
    	}catch(JSONException e){    		
    	}
    	return null;
    }
    
    
    public static boolean isSessionUser(long userId){
    	return(userId == Facebook.getInstance().getSession().uid);	
    }
    
    public static Dialog createProfileMenuDialog(ContextProfileData cpd){
		
		Context mContext = App.INSTANCE.getApplicationContext();
		Dialog dialog = new Dialog(mContext);
	
		dialog.setContentView(R.layout.profile_menu_dialog);
		dialog.setTitle(cpd.name);
		
		return dialog;
	}

	public static void createActorMenu(
		Menu menu,
		ContextProfileData cpd,
		Context ctx){
		
		Logger.l(Logger.DEBUG, "App","createActorMenu() uid:"+cpd.actorId+", name:"+cpd.name);
		
		
		Resources res = ctx.getResources();
		final SubMenu actorNameSM = menu.addSubMenu(cpd.name);		
    	Bitmap headerIconBmp = App.mImageUrlLoader2.loadImage(cpd.profileImageUri, true);
    	Drawable actorProfileDrawable = null;
    	if(headerIconBmp != null){
    		actorProfileDrawable = new BitmapDrawable(headerIconBmp);
    		actorNameSM.setHeaderIcon(actorProfileDrawable);
    		//actorNameSM.setIcon(actorProfileDrawable);
    	}else{
    		//asynchronously load profile image
    		
    		AsyncLoaderInput input = new AsyncLoaderInput();
    		input.imageUri = cpd.profileImageUri;
    		App.mImageUrlLoader2.loadImageAsync(    				
    				App.INSTANCE.mExecScopeImageLoaderTask,
    				input,
    				new ImageUrlLoader2.AsyncListener(){
    					
				@Override
				public void onCancelled() {
				}

				@Override
				public void onPostExecute(AsyncLoaderResult result) {
				}

				@Override
				public void onPreExecute() {
				}

				@Override
				public void onPublishProgress(final AsyncLoaderProgress progress) {
					
					
					try{
						BitmapDrawable bd = new BitmapDrawable(progress.bitmap);
						actorNameSM.setHeaderIcon(bd);
					}catch(Throwable e){
						
					}
				}
    			
    		});
    	}
    	
		actorNameSM.setHeaderTitle(cpd.name);
		MenuItem viewFeedMenuItem = actorNameSM.add(0, App.CONTEXT_MENUITEM_PROFILE,0, res.getString(R.string.view_feed));
		MenuItem viewWallMenuItem = actorNameSM.add(0, App.CONTEXT_MENUITEM_PROFILE,0, res.getString(R.string.view_wall));		
		MenuItem viewProfileMenuItem = actorNameSM.add(0,App.CONTEXT_MENUITEM_PROFILE,1, res.getString(R.string.view_profile));
		MenuItem viewPhotosMenuItem = actorNameSM.add(0,App.CONTEXT_MENUITEM_PROFILE,2, res.getString(R.string.view_photos));
		MenuItem viewTaggedPhotosMenuItem = actorNameSM.add(0, App.CONTEXT_MENUITEM_PROFILE, 3, res.getString(R.string.view_tagged_photos));
		

		Intent i;
		i = new Intent(INTENT_ACTION_VIEW_FEED);
		i.putExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT, cpd);
		viewFeedMenuItem.setIntent(i);
		
		i = new Intent(INTENT_ACTION_VIEW_WALL);
		i.putExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT, cpd);
		viewWallMenuItem.setIntent(i);
		
		i = new Intent(INTENT_ACTION_VIEW_PROFILE);
		i.putExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT, cpd);
		viewProfileMenuItem.setIntent(i);
		
		i = new Intent(INTENT_ACTION_VIEW_PHOTOS);
		i.putExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT, cpd);
		viewPhotosMenuItem.setIntent(i);
		
		i = new Intent(INTENT_ACTION_VIEW_TAGGED_PHOTOS);
		i.putExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT, cpd);
		viewTaggedPhotosMenuItem.setIntent(i);
		
    }
	
	
	public static ContextProfileData createSessionUserContextProfileData(){
		FBSession fbSession = Facebook.getInstance().getSession();
		ContextProfileData cpd = new ContextProfileData();
		User sessionUser;
		try{
			sessionUser = App.getUserSessionData();
			cpd.name = sessionUser.name;
			cpd.profileImageUri = sessionUser.pic;
		}catch(FileNotFoundException e){	
			cpd.name = "";
			cpd.profileImageUri = null;
		}
		
		cpd.actorId = fbSession.uid;	
		return cpd;
	}
	
    /**
     * 
     * @param ctx
     * @param cpd If null then will use the current session user
     * @param activity
     * @param menu
     */
	public static void prepareOptionsMenu(Activity ctx, ContextProfileData cpd, int activity, Menu menu){
		menu.clear();		
		MenuItem streams = menu.add(0,App.MENUITEM_STREAMS, 0, "Feed");
		MenuItem events = menu.add(0,App.MENUITEM_EVENTS, 1, "Events");
		MenuItem networks = menu.add(0,App.MENUITEM_NETWORKS, 2, "Network");
		MenuItem notifications = menu.add(0,App.MENUITEM_NOTIFICATIONS, 3, "Notifications");
		//MenuItem clear = menu.add(0,App.MENUITEM_CLEAR,4,"Clear");
		MenuItem logoff = menu.add(0,App.MENUITEM_LOGOFF, 5,"Logoff");
		//MenuItem settings = menu.add(0,App.MENUITEM_SETTINGS, 6,"Settings");
		
		if(cpd == null){
			FBSession fbSession = Facebook.getInstance().getSession();
			cpd = new ContextProfileData();
			User sessionUser;
			try{
				sessionUser = App.getUserSessionData();
				cpd.name = sessionUser.name;
				cpd.profileImageUri = sessionUser.pic;
			}catch(FileNotFoundException e){				
			}
			
			cpd.actorId = fbSession.uid;						
		}
			
		Intent i = new Intent();	
		i.putExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT, cpd);
		streams.setIntent(i);
		events.setIntent(i);
		networks.setIntent(i);
		notifications.setIntent(i);
		logoff.setIntent(i);
		OptionsMenu.createPreference(ctx, menu, 0, App.MENUITEM_SETTINGS, 6);
		
	}

	/**
	 * 
	 * 
	 * @param ctx
	 * @param cpd if the contextprofiledata in the intent is not null then it will be used to override this data
	 * @param activity
	 * @param item
	 * @return
	 */
	public static boolean onOptionsItemSelected(Activity ctx, ContextProfileData cpd, int activity, MenuItem item){
	    	
	    	Intent itemIntent = item.getIntent();
	    	if(itemIntent != null){
	    		if(cpd == null || itemIntent.hasExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT)){
	    			cpd = itemIntent.getParcelableExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT);
	    		}
	    	}
	    	
			int id = item.getItemId();
	    	
			
			switch(id){
			
				case App.MENUITEM_EVENTS:{
					Intent viewEvents = EventsActivity.getIntent(ctx);
					viewEvents.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
					ctx.startActivity(viewEvents);
					return true;
				}
				
				case App.MENUITEM_NOTIFICATIONS:{
					Intent viewNotificationsIntent = NotificationsActivity.getIntent(ctx);
					viewNotificationsIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
					ctx.startActivity(viewNotificationsIntent);
					return true;
				}
				
				case App.MENUITEM_STREAMS:{
					Intent i = StreamActivity.getIntent(ctx);
					//ContextProfileData cpd = new ContextProfileData();
					//FBSession fbSession = Facebook.getInstance().getSession();
					//User userSessionData = App.getUserSessionData();
					//cpd.name = userSessionData.name;
					//cpd.profileImageUri = userSessionData.pic;
					//cpd.actorId = fbSession.uid;
					//Toast.makeText(ctx, Long.toString(fbSession.uid), 3000).show();								
					
					//we need to set both intents because
					//the first one is intended when no instance is created.
					i.putExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT, cpd);			
					i.putExtra(StreamActivity.XTRA_STREAMMODE, Facebook.STREAMMODE_LIVEFEED);
					i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					
					//the second intent is to modify the first instance extras
					Intent currentIntent = ctx.getIntent();
					currentIntent.putExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT, cpd);
					currentIntent.putExtra(StreamActivity.XTRA_STREAMMODE, Facebook.STREAMMODE_LIVEFEED);
		            ctx.startActivity(i);		           
		            return true;
				}
				
				case App.MENUITEM_NETWORKS:{				
					Intent i = ViewContactsActivity.getIntent(ctx);
					i.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);					
					i.putExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT, cpd);
					ctx.startActivity(i); 
					return true;
				}
				
				case App.MENUITEM_VIEWALBUMS:{
					Intent i = ViewAlbumsActivity.getIntent(ctx);
					ctx.startActivity(i);
					return true;
				}
				
				case App.MENUITEM_LOGOFF:{
	//				Toast.makeText(ctx, "LOGOFF", 3000).show();
					App.INSTANCE.logOff();
					ctx.finish();
					return true;
				}
				
				case App.MENUITEM_SETTINGS:{
					
					return true;
				}
				
				case App.MENUITEM_CLEAR:{
					App.INSTANCE.mDBHelper.deleteAllNotifications(App.INSTANCE.mDB);
					return true;
				}
				
				case App.MENUITEM_GET_WALLCOMMENTS:{
					App.INSTANCE.getWallPostsCommentsFromCloud();
					return true;
				}
				
				case App.MENUITEM_GET_WALLPOSTS:{
					App.INSTANCE.getWallPostsFromCloud(0);
					return true;
				}
				
				case App.MENUITEM_TOGGLE_LOGGER:{
					Logger.show = !Logger.show;
					return true;
				}
				
				case App.MENUITEM_USER_WALL:{		
					//Intent viewStreamsIntent = StreamActivity.getIntent(ctx);
					//viewStreamsIntent.putExtra(StreamActivity.XTRA_STREAMMODE, Facebook.STREAMMODE_LIVEFEED);
					//iewStreamsIntent.putExtra(StreamActivity.XTRA_USERID, Facebook.INSTANCE.getSession().uid);
					//iewStreamsIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					//ctx.startActivity(viewStreamsIntent);
					
					App.showUserWall(ctx, cpd.actorId, cpd.name);
					/*
					Intent viewWall = StreamActivity.getIntent(this);
					Bundle b = new Bundle();
					b.putLong(StreamActivity.XTRA_USERID,Facebook.INSTANCE.getSession().uid);
					b.putByte(StreamActivity.XTRA_STREAMMODE, Facebook.STREAMMODE_WALLFEED);
					viewWall.putExtras(b);
					startActivity(viewWall);
					*/
					return true;
				}
			}
			
			return false;
			
	    }

	public static boolean onContextItemSelected(Activity ctx, MenuItem item, ProgressDialog pd) {
		
		int itemId = item.getItemId();
		switch(itemId){
			case App.CONTEXT_MENUITEM_PROFILE:{
				Intent i = item.getIntent();
				String action = i.getAction();
				ContextProfileData cpd = i.getParcelableExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT);
			
				if(action.equals(App.INTENT_ACTION_VIEW_PROFILE)){
				
				}
				else if(action.equals(App.INTENT_ACTION_VIEW_PHOTOS)){
					
				}
				else if(action.equals(App.INTENT_ACTION_VIEW_TAGGED_PHOTOS)){			
					Bundle data = new Bundle();
					data.putString(ManagerThread.XTRA_CALLBACK_INTENT_ACTION, App.INTENT_GET_TAGGED_PHOTOS);
					data.putLong(Facebook.param_uid, cpd.actorId);
					Facebook.INSTANCE.getTaggedPhotos(
							cpd.outhandler,
							data,
							cpd.actorId,
							ViewPhotosActivity.NUM_PHOTOS_PER_REQUEST, 0,
							ManagerThread.CALLBACK_GET_TAGGED_PHOTOS,
							BaseManagerThread.CALLBACK_SERVERCALL_ERROR,
							BaseManagerThread.CALLBACK_TIMEOUT_ERROR, 0);
					pd.setTitle("Loading");
					pd.setMessage("Getting tagged photos");
					pd.show();
					return true;						
				}				
				else if(action.equals(App.INTENT_ACTION_VIEW_WALL)){
					App.showUserWall(ctx, cpd.actorId, cpd.name);	
					return true;
				}
				break;
			}
		}
		return false;
	}
	
	
	
}