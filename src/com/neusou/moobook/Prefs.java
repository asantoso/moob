package com.neusou.moobook;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
	
	public static final String session_preffile = "session.pref";
	public static final String session_expires = "sess.expires";
	public static final String session_uid = "sess.uid";
	public static final String session_sig = "sess.sig" ;
	public static final String session_secret = "sess.secret" ;
	public static final String session_key = "sess.key" ;
	public static final String session_json = "sess.json" ; //the original session json object that was returned right after logging in
	public static final String session_isvalid = "sess.isvalid";
	
	public static final String stream_livefeed_lastfetch = "stream.livefeed.lastfetch";
	
	public static final String favoriten_count = "favn.count";
	
	public static final String KEY_PERIODIC_CHECK_ENABLED = "periodic.update";
	public static final boolean DEFAULT_PERIODIC_CHECK_ENABLED = true; 
	public static final String KEY_PERIODIC_CHECK_INTERVAL = "periodic.update.interval";
	public static final long DEFAULT_PERIODIC_CHECK_INTERVAL = 1*15*1000; //an hour in milliseconds 
	public static final String KEY_STREAMS_MAX_UPDATE_ITEMS = "streams.max.update.items";	
	public static final int DEFAULT_STREAMS_MAX_UPDATE_ITEMS = 15;  
	public static final String KEY_STREAMS_LASTUPDATED = "streams.lastupdated"; //last update time in milliseconds since unix epoch 
	
	public static final String KEY_PERIODIC_NOTIFICATION_CHECK_ENABLED = "periodic.notification.check";
	
	//last time notifications are retrieved (in seconds) 
	public static final String KEY_NOTIFICATION_LAST_CHECK_TIME = "notification.lastchecktime";
	
	public static final String PREF_MAIN = "default.pref";
	
	static Context mContext;
	
	public static void init(Context ctx){
		mContext = ctx;
	}
	
	public static long getNumFetchStreams(){
		SharedPreferences settings = mContext.getSharedPreferences(session_preffile, Context.MODE_PRIVATE);
		return settings.getLong(KEY_STREAMS_MAX_UPDATE_ITEMS, DEFAULT_STREAMS_MAX_UPDATE_ITEMS);		
	}
	
	
}