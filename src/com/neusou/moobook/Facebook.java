package com.neusou.moobook;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.text.StrSubstitutor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.neusou.BasicClientHelper;
import com.neusou.Logger;
import com.neusou.async.UserTask;
import com.neusou.moobook.activity.StreamActivity;
import com.neusou.moobook.data.Event;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.moobook.thread.ManagerThread;

public class Facebook {
	
	public static final String LOG_TAG = "Facebook";
	
	public static final String api_rest_endpoint = "http://api.new.facebook.com/restserver.php";
	public static final String login_endpoint = "https://www.facebook.com/login.php";
	
	public static final String wsmethod_auth_createtoken = "auth.createToken";	
	public static final String wsmethod_users_getLoggedInUser = "users.getLoggedInUser";
	public static final String wsmethod_fql_query = "fql.query";
	public static final String wsmethod_fql_multiquery = "fql.multiquery";
	
	public static final String wsmethod_comments_add = "comments.add";
	public static final String wsmethod_comments_get = "comments.get";
	
	public static final String wsmethod_friends_get = "friends.get";
	public static final String wsmethod_stream_get = "stream.get";
	public static final String wsmethod_stream_getComments = "stream.getComments";
	public static final String wsmethod_stream_addComment = "stream.addComment";
	public static final String wsmethod_stream_removeComment = "stream.removeComment";	
	public static final String wsmethod_notifications_get = "notifications.get";
	public static final String wsmethod_users_getInfo = "users.getInfo";
	public static final String wsmethod_message_getThreadsInFolder = "message.getThreadsInFolder";	
	public static final String wsmethod_events_rsvp = "events.rsvp";
	public static final String wsmethod_photos_upload = "facebook.photos.upload";
	
	public static final String param_v = "v";
    public static final String param_method = "method";
	public static final String param_call_id = "call_id";	
	public static final String param_app_id = "app_id";
	public static final String param_api_key = "api_key";
	public static final String param_session_key = "session_key";
	public static final String param_sig = "sig";
	public static final String param_format = "format";
	public static final String param_api_version = "v";
	public static final String param_uid = "uid";
	public static final String param_uids = "uids";
	public static final String param_fields = "fields";
	public static final String param_folder_id = "folder_id";
	public static final String param_fql_query = "query";
	public static final String param_fql_queries = "queries";
	public static final String param_post_id = "post_id";
	public static final String param_comment = "comment";
	public static final String param_object_id = "object_id";
	public static final String param_text = "text";
	public static final String param_subject = "subject";
	public static final String param_comment_id = "comment_id";
	public static final String param_eid = "eid";
	public static final String param_rsvp_status = "rsvp_status";
	public static final String param_event_info = "event_info";
	public static final String param_filterkey = "filterkey";
	public static final String param_owner = "owner";
	public static final String param_aid = "aid";
	
	public static final String subkey_columns = "columns";	
	public static final String subkey_lastUpdateTime = "lastUpdateTime";	
	public static final String subkey_limit = "limit";
	public static final String subkey_offset = "offset";
	public static final String subkey_created_date = "created_date";
	public static final String subkey_updated_date = "updated_date";
	public static final String subkey_updated_date_operator = "updated_time_operator";
	
	public static final String REQUEST_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8";
	//public static final String REQUEST_USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.8) Gecko/2009033100 Ubuntu/9.04 (jaunty) Firefox/3.0.8";
	public static final String REQUEST_USER_AGENT = "FacebookConnect";
	public static final String REQUEST_ACCEPT_LANGUAGE  = "en-us,en;q=0.5";
	public static final String REQUEST_METHOD  = "POST";
	
	public static final String RESPONSE_FORMAT_JSON  = "JSON";
	public static final String RESPONSE_FORMAT_XML  = "XML";
	
	public static final String XTRA_RESPONSE = "fb.xtr.resp"; //the raw response returned by facebook api
	public static final String XTRA_RESPONSEFORMAT = "fb.xtr.resp.format"; //facebook JSON OR XML response format
	public static final String XTRA_RESPONSE_ERROR_CODE = "fb.xtr.resp.err.code"; //error code returned by facebook api
	public static final String XTRA_WEBSERVICE_METHODNAME = "fb.xtr.ws.method";
	public static final String XTRA_WEBSERVICE_REQUEST_CONTENT = "fb.xtra.ws.req.ct";
	public static final String XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE = "fb.xtr.cb.suc.op"; //handler callback id when successful
	public static final String XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE = "fb.xtr.cb.err.op"; //handler callback id when error
	public static final String XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE = "fb.xtr.cb.to.op";
	public static final String XTRA_SERVERCALL_ERROR_MSG = "fb.xtr.svrcall.err.rsn"; //reason for error
	public static final String XTRA_SERVERCALL_ERROR_CODE = "fb.xtr.svrcall.err.code";
	public static final String XTRA_SERVERCALL_STATUS_CODE = "fb.xtr.svrcall.status";
	public static final String XTRA_FBRESPONSE_ERROR_CODE = "fb.resp.err.code";
	public static final String XTRA_FBRESPONSE_ERROR_MESSAGE = "fb.resp.err.msg";
	public static final String XTRA_INCLUDEREQSIG = "fb.xtr.inc.request.sig";
	public static final String XTRA_RESPONSE_BYTELENGTH = "fb.xtr.resp.bytelength";
	public static final String XTRA_METHODPARAMETERSMAP = "fb.xtr.mtd.params.map";
	
	public static final String XTRA_FQL_QUERY = "fb.xtr.fql.qry"; //the fql query
	public static final String XTRA_FQL_QUERIES = "fb.xtr.fql.qrys";
	public static final String XTRA_FQL_QUERY_TYPE = "fb.xtr.fql.qrytype"; //fql.query or fql.multiquery
	public static final String XTRA_FQL_RESPONSE_SCHEMA = "fb.xtr.fql.rsp.type";
	public static final String XTRA_APIMETHOD_ARG_POSTID = "fb.xtr.api.mtd.arg.postid";
	public static final String XTRA_TABLECOLUMNS_SHORTARRAY = "fb.xtr.tblcols.shortarray";
		
	private static final String XTRA_INTERNAL_FBCONNECTIONERROR = "int.fb.conn.err"; //boolean value, to indicate whether there is an internal HTTP connection error when trying to invoke facebook API endpoint.
	public static final String XTRA_INTERNAL_OUTHANDLER_KEY = "int.fb.outh.key"; //int value, a key of out handler
	private static final String XTRA_INTERNAL_BROADCASTLOGININTENT = "int.fb.broadcast.login"; //boolean value, a key of out handler
	private static final String XTRA_INTERNAL_ASYNCTASK_ABORTED_SESSIONEXPIRED = "int.fb.task.aborted"; //boolean, 
	
	public static final byte ERROR_HTTPCONNECTION = 0;
	public static final byte ERROR_FBAPIMETHOD = 1;	
	
	public static final byte FQLRESPONSE_STREAMS = 0;
	public static final byte FQLRESPONSE_EVENTS = 1;
	public static final byte FQLRESPONSE_GROUPS = 2;
	public static final byte FQLRESPONSE_ALBUMS = 3;
	public static final byte FQLRESPONSE_COMMENTS = 4;
	public static final byte FQLRESPONSE_FRIENDS = 5;
	public static final byte FQLRESPONSE_PHOTOS = 6;
	
	public static final byte SERVERCALL_OK = 0;
	public static final byte SERVERCALL_ERROR = 1;
	public static final byte SERVERCALL_ERROR_CODE_CONNECTIONRESET = 13;

	public static final byte COMMENT_TYPE_STREAMPOSTS = 0;
	public static final byte COMMENT_TYPE_OTHERS = 1;

	public static final byte STREAMMODE_LIVEFEED = 0;
	public static final byte STREAMMODE_NEWSFEED = 1;
	
	/**
	 * Get all friend lists
	 */
	
	
	/**
	 * Get all newsfeed streams (with filter) with comments
	 */
	public String FQL_GET_NEWSFEED_STREAM_WITH_COMMENTS;	
	public String FQL_GET_NOTIFICATIONS;
	public String FQL_GET_EVENTS;
	public String FQL_GET_TAGGED_PHOTOS;
	public String FQL_GET_PHOTOTAGS;
	public String FQL_GET_COMMENTS;
	public String FQL_GET_COMMENTS_USERS;
	public String FQL_GET_ALBUMS;
	public String FQL_GET_PHOTOS;
	public String FQL_GET_WALLPOSTS;
	public String FQL_GET_FRIENDLISTS;
	public String FQL_GET_CONTACTS;
	public String FQL_MULTIQUERY_GET_COMMENTS_COMPLETE;
	public String FQL_MULTIQUERY_GET_COMMENTS_PHOTOS_COMPLETE;	
	/**
	 * Get stream filters
	 */
	public static final String FQL_GET_STREAM_FILTER
	= "select name,filter_key,rank, from stream_filter where uid = ${uid}";
	
	public static final String FQL_GET_VISIBLE_USERCONNECTION_STREAMS 
	= "SELECT post_id, action_links, attachment, actor_id, target_id, attribution, comments, message FROM stream WHERE source_id in (SELECT target_id FROM connection WHERE source_id=${source_id}) AND is_hidden = 0";
	
	public static final String FQL_GET_USER_STREAM_APPLICATION
	= "SELECT post_id, actor_id, message, attachment, comments, likes, attribution, created_time, updated_time, source_id, target_id, viewer_id FROM stream WHERE filter_key in (SELECT filter_key FROM stream_filter WHERE uid = ${uid} AND type = 'newsfeed') LIMIT 50 OFFSET 0";
	
	public static final String FQL_GET_USER_STREAM_APPLICATION_MULTIQUERY
	= "SELECT post_id, actor_id, message, attachment, comments, likes, attribution, created_time, updated_time, source_id, target_id, viewer_id FROM stream WHERE filter_key in (SELECT filter_key FROM stream_filter WHERE uid = ${uid} AND type = 'newsfeed') LIMIT 50 OFFSET 0";
		
	//See the names of all the events that your friends have been invited to
	public static final String FQL_GET_EVENTS_FRIENDS_INVITED 
	= "SELECT eid, name, tagline, nid, pic_small, pic_big, pic, host, description, event_type, event_subtype, start_time, end_time, creator, update_time, location, venue, privacy, hide_guest_list FROM event WHERE eid IN (SELECT eid from event_member WHERE uid IN (SELECT uid2 FROM friend WHERE uid1=${uid}) )";
	
	public static final String FQL_GET_USER_FRIENDS
	= "SELECT ${columns} FROM user where uid in (select uid1 from friend where uid2 = ${uid})";
		
	public static final String FQL_GET_ONE_USER_DATA
	= "SELECT ${columns} FROM user where uid = ${uid}";
	
	public static final String FQL_GET_USER_FRIENDS_IN_STREAMS
	= "SELECT ${columns} FROM user where uid in (SELECT actor_id FROM stream WHERE source_id = ${uid}) and uid in (select uid1 from friend where uid2=${uid})";
		
	// Get all groups a user is part of
	public static final String FQL_GET_MY_GROUPS 
	= "SELECT name FROM group WHERE gid IN (SELECT gid FROM group_member WHERE uid = ''${uid}'')";
	
	// Get uids of friends of $app_user with birthdays today (where $today is formatted like December 18)
	public static final String FQL_GET_BIRTHDAY
	= "SELECT uid FROM user WHERE strpos(birthday, '${today}') = 0 AND uid IN (SELECT uid2 FROM friend WHERE uid1 = $app_user)";
	
	// Get all albums owned by a user
	public static final String FQL_GET_USER_ALBUMS 
	= "SELECT pid FROM photo WHERE aid IN ( SELECT aid FROM album WHERE owner=''${user_id}'' ) ORDER BY created DESC LIMIT 1,42";
	
	// Get all comments of a stream post
	public static final String FQL_GET_POST_COMMENTS
	= "SELECT id, fromid, time, text, username, reply_xid, xid, object_id from comment where post_id=${post_id}"; 

	// Get all latest photos of friends
	public static final String FQL_GET_LATEST_FRIENDS_PHOTOS
	=	"" +
		"{" +
		"\"photos\":\"select pid, src_small, caption, created, modified, owner from photo where aid in (select aid from album where owner in (select uid1 from friend where uid2=${uid}) order by name desc ) order by modified desc limit ${limit} offset ${offset}\"" +
		","+
		"\"profiles\":\"select name, url, id, pic_square from profile where id in (select owner from #photos)\"" +
		"}" +
		"";
		
	static Facebook INSTANCE;
	static final int CONNECTION_TIMEOUT = 100000;
	static final int READ_TIMEOUT = 100000;
	static final String PREF = "--";
	static final String CRLF = "\r\n";
    static int UPLOAD_BUFFER_SIZE = 1024;    
    public static final int MESSAGE_UPDATE_UPLOAD_COMMENCING = 0;
    public static final int MESSAGE_UPDATE_UPLOAD_PROGRESS = 1;
    public static final int MESSAGE_UPDATE_UPLOAD_FINISHED = 2;
    public static final int MESSAGE_UPDATE_UPLOAD_SUCCESS = 3;
    public static final int MESSAGE_UPDATE_UPLOAD_ERROR = 4;
    
    private static int mConnectionTimeout = 20000;
    volatile public Handler mListenerHandler = new Handler();  //Facebook API listener handler
  
    FBSession mSession;	  
    HashMap<Integer,Handler> mOutRegistry = new HashMap<Integer, Handler>(1,0.50f);
    AtomicLong wscallcount = new AtomicLong(new Date().getTime());
    TreeMap<String,String> fqlmap;
    Context mContext;    
	long mReExecuteWaitTime = 2000;
    
    public boolean registerOutHandler(int key, Handler outHandler){
    	if(!mOutRegistry.containsKey(key)){
    		mOutRegistry.put(key, outHandler);
    		return true;
    	}else{
    		mOutRegistry.remove(key);
    		mOutRegistry.put(key, outHandler);
    		return true;
    	}    	
    }
    
    public boolean unregisterOutHandler(int key, Handler outHandler){
    	if(mOutRegistry.containsKey(key)){
    		mOutRegistry.remove(key);
    		return true;
    	}
    	return false;
    }
    
    public void purgeInactiveOutHandlers(boolean forcePurge){    	
    	Iterator<Entry<Integer,Handler>> it = mOutRegistry.entrySet().iterator();
    	while(it.hasNext()){
    		Entry<Integer, Handler> entry = it.next();
    		Handler h = entry.getValue();
    		if(h == null || forcePurge){
    			mOutRegistry.remove(entry.getKey());
    		}
    	}
    }
    
    Handler mInterceptorHandler = new Handler(){
    	public void handleMessage(Message msg) {
    		int code = msg.what;
    		switch(code){
    			
    		}    		
    	};
    };
  
        
    public void setOutHandler(Handler h){    	
    	mListenerHandler = h;
    }
    
    public void setConnectionTimeout(int seconds){
    	mConnectionTimeout = seconds;
    }    
    
    public void checkSession(int cbSuccessOp, int cbErrorOp){
    	Bundle data = new Bundle();
    	data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
    	data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
    	
    	TreeMap<String,String> map = new TreeMap<String,String>(); 
		map.put(param_api_key, FBApp.api_key);
		map.put(param_format, RESPONSE_FORMAT_JSON);
		map.put(param_session_key, mSession.key);
		//map.put(param_call_id, Long.toString(SystemClock.elapsedRealtime()));
		map.put(param_method, wsmethod_users_getLoggedInUser);
    	
		data.putSerializable(XTRA_METHODPARAMETERSMAP, map);
		executeRest(data, 0);
					
	}
	
    public void getStreamsFilters(String uid, int cbSuccessOp, int cbErrorOp){
    	Bundle data = new Bundle();
		
		fqlmap.clear();
		fqlmap.put(param_uid, uid);
				
		String fql_query = StrSubstitutor.replace(FQL_GET_STREAM_FILTER, fqlmap);		
		Log.d(LOG_TAG, "get stream filters fql: "+fql_query);
				
		data.putString(XTRA_FQL_QUERIES, fql_query);
		data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_multiquery);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);		
		//data.putInt(XTRA_FQL_RESPONSE_SCHEMA, FQLRESPONSE_PHOTOS);
				
		executeFQL(data, 0);
		
	
    }
    
    /**
     * Get complete facebook streams
     * @param uid current facebook user id
     * @param filterKey the stream filter key
     * @param lastStreamPostUpdateTime last update time in seconds since unix epoch 
     * @param cbSuccessOp sucessful callback handler msg.what 
     * @param cbErrorOp error callback handler msg.what 
     * @param cbTimeoutOp timeout callback handler msg.what 
     * @param timeoutMillisecs 
     * @param limit max number of stream posts to return
     * @param offset the offset
     * @param comment_limit
     * @param comment_offset
     */
    public void getStreamsComplete(int outHandlerKey, String uid, String filterKey, long lastStreamPostUpdateTime, int cbSuccessOp, int cbErrorOp, int cbTimeoutOp, long timeoutMillisecs, long limit, long offset, long comment_limit, long comment_offset){

    	Bundle data = new Bundle();
		
		fqlmap.clear();		
		fqlmap.put(param_uid, uid);		
		fqlmap.put(subkey_limit, Long.toString(limit));
		fqlmap.put(subkey_offset, Long.toString(offset));
		fqlmap.put(param_filterkey, filterKey);
		fqlmap.put(subkey_lastUpdateTime, Long.toString(lastStreamPostUpdateTime));
		
		String fql_query = StrSubstitutor.replace(FQL_GET_NEWSFEED_STREAM_WITH_COMMENTS, fqlmap).trim();		
		Logger.l(Logger.DEBUG,LOG_TAG, "[getStreamsComplete()] fql: "+fql_query);
				
		data.putString(XTRA_FQL_QUERIES, fql_query);
		data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_multiquery);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
		data.putInt(XTRA_INTERNAL_OUTHANDLER_KEY, outHandlerKey);
		
		executeFQL(data, 0);				
    }
    
    
    public void getCommentsRest(int outHandlerKey, String post_id, int cbSuccessOp, int cbErrorOp, int cbTimeoutOp){
		
		Bundle data = new Bundle();
				
		data.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
    	data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
    	data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
    	
    	data.putInt(XTRA_INTERNAL_OUTHANDLER_KEY, outHandlerKey);
    	
    	TreeMap<String,String> map = new TreeMap<String,String>(); 
		map.put(param_api_key, FBApp.api_key);
		map.put(param_format, RESPONSE_FORMAT_JSON);
		map.put(param_session_key, mSession.key);		
		map.put(param_method, wsmethod_stream_getComments);
		map.put(param_post_id, post_id);
		
		data.putSerializable(XTRA_METHODPARAMETERSMAP, map);
				
		executeRest(data, 0);		
    }
		
    /**
	 * 
	 * @param type 
	 * @param object_id
	 * @param post_id
	 * @param cbSuccessOp
	 * @param cbErrorOp
	 * @param cbTimeoutOp
	 * @param limit
	 * @param offset
	 */
	public void getPostComments(int outHandlerKey, byte type, String object_id, String post_id, int cbSuccessOp, int cbErrorOp, int cbTimeoutOp, long limit, long offset){
		
		Bundle data = new Bundle();
		fqlmap.clear();
		fqlmap.put(param_object_id, type==COMMENT_TYPE_OTHERS?object_id:"");
		fqlmap.put(param_post_id, type==COMMENT_TYPE_STREAMPOSTS?post_id:"");
		fqlmap.put(subkey_limit, Long.toString(limit));
		fqlmap.put(subkey_offset,  Long.toString(offset));	
		
		String fql_query;
	
		if(type == COMMENT_TYPE_STREAMPOSTS){ 
			//only for stream posts
			fql_query = StrSubstitutor.replace(FQL_MULTIQUERY_GET_COMMENTS_COMPLETE, fqlmap).trim();
		}else{
			//only for media comments
			fql_query = StrSubstitutor.replace(FQL_MULTIQUERY_GET_COMMENTS_PHOTOS_COMPLETE, fqlmap).trim();
		}
		
		Logger.l(Logger.DEBUG,LOG_TAG, "[getPostComments()] fql:"+fql_query);
		
		data.putString(XTRA_FQL_QUERIES, fql_query);
		data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_multiquery);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
		data.putInt(XTRA_FQL_RESPONSE_SCHEMA, FQLRESPONSE_COMMENTS);
		data.putInt(XTRA_INTERNAL_OUTHANDLER_KEY, outHandlerKey);
		
		executeFQL(data, 0);
		
	}

	/**
     * 
     * @param type 0:streamposts , 1:others
     * @param objectId
     * @param postId
     * @param cbSuccessOp
     * @param cbErrorOp
     * @param cbTimeoutOp
     * @param timeoutMillisecs
     * @param limit
     * @param offset
     */
    public void getComments(int outHandlerKey, byte type, String object_id, String post_id, int cbSuccessOp, int cbErrorOp, int cbTimeoutOp, long timeoutMillisecs, long limit, long offset){
    	Bundle data = new Bundle();		
		fqlmap.clear();
		fqlmap.put(param_object_id, type==COMMENT_TYPE_OTHERS?object_id:"");
		fqlmap.put(param_post_id, type==COMMENT_TYPE_STREAMPOSTS?post_id:"");
		fqlmap.put(subkey_limit, String.valueOf(limit));
		fqlmap.put(subkey_offset, String.valueOf(offset));
		
		String fql_query;

		if(type == COMMENT_TYPE_STREAMPOSTS){ 
			//only for stream posts
			fql_query = StrSubstitutor.replace(FQL_GET_COMMENTS, fqlmap).trim();
		}else{
			//only for media comments
			fql_query = StrSubstitutor.replace(FQL_MULTIQUERY_GET_COMMENTS_PHOTOS_COMPLETE, fqlmap).trim();
		}
				
		Logger.l(Logger.DEBUG,LOG_TAG, "[getComments()] fql: "+fql_query);
				
		data.putString(XTRA_FQL_QUERY, fql_query);
		data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_query);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
		data.putInt(XTRA_INTERNAL_OUTHANDLER_KEY, outHandlerKey);				
		executeFQL(data, 0);
    }
    
    public void getCommentsUsers(int outHandlerKey, String[] uids, int cbSuccessOp, int cbErrorOp, int cbTimeoutOp, long timeoutMillisecs){
    	Bundle data = new Bundle();		
		fqlmap.clear();
		
		StringBuffer sb = new StringBuffer();
		for(int i=0,N=uids.length;i<N;i++){
			sb.append(uids[i]);
			if(i<N-1){
				sb.append(",");
			}
		}
		
		fqlmap.put(param_uids, sb.toString());
		
		String fql_query = StrSubstitutor.replace(FQL_GET_COMMENTS_USERS, fqlmap).trim();		
		Logger.l(Logger.DEBUG,LOG_TAG, "[getCommentsUsers()] fql: "+fql_query);
				
		data.putString(XTRA_FQL_QUERY, fql_query);
		data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_query);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
		data.putInt(XTRA_INTERNAL_OUTHANDLER_KEY, outHandlerKey);
		executeFQL(data, 0);
    }
    
    public void getFriendLists(int cbSuccessOp, int cbErrorOp, int cbTimeoutOp, long timeoutMillisecs){
    	Bundle data = new Bundle();		
		fqlmap.clear();		
		fqlmap.put(param_owner, Long.toString(mSession.uid));
		
		String fql_query = StrSubstitutor.replace(FQL_GET_FRIENDLISTS, fqlmap).trim();		
		Logger.l(Logger.DEBUG,LOG_TAG, "[getFriendLists()] fql: "+fql_query);
				
		data.putString(XTRA_FQL_QUERIES, fql_query);
		data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_multiquery);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
						
		executeFQL(data, 0);
    }
    
    public void getAllLatestFriendsPhotos(String uid, int cbSuccessOp, int cbErrorOp, long limit, long offset){
    	
    	Bundle data = new Bundle();
		
		fqlmap.clear();		
		fqlmap.put(param_uid, uid);
		fqlmap.put(subkey_limit, Long.toString(limit));
		fqlmap.put(subkey_offset, Long.toString(offset));
		
		String fql_query = StrSubstitutor.replace(FQL_GET_LATEST_FRIENDS_PHOTOS, fqlmap);		
		Log.d(LOG_TAG, "getFriendsLatestPhotos fql: "+fql_query);
				
		data.putString(XTRA_FQL_QUERIES, fql_query);
		data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_multiquery);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);		
		data.putInt(XTRA_FQL_RESPONSE_SCHEMA, FQLRESPONSE_PHOTOS);
		
		executeFQL(data, 0);
    };
    
    public void getOneUserData(int outHandlerKey, Bundle inData, String uid, short selection[],  int cbSuccessOp, int cbErrorOp, int cbTimeoutOp){
    	Bundle data = new Bundle(inData);
		
		fqlmap.clear();		
		fqlmap.put(param_uid, uid);
		String selectedColumns = User.createColumnNames(selection);
		fqlmap.put("columns", selectedColumns);
		
		String fql_query = StrSubstitutor.replace(FQL_GET_ONE_USER_DATA, fqlmap);		
		Logger.l(Logger.DEBUG,LOG_TAG, "[getOneUserData()] fql: "+fql_query);
				
		data.putString(XTRA_FQL_QUERY, fql_query);
		data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_query);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);	
		data.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
		data.putInt(XTRA_INTERNAL_OUTHANDLER_KEY, outHandlerKey);
		data.putShortArray(XTRA_TABLECOLUMNS_SHORTARRAY, selection);// <-important
		data.putInt(XTRA_FQL_RESPONSE_SCHEMA, FQLRESPONSE_FRIENDS);
		data.putInt(XTRA_INTERNAL_OUTHANDLER_KEY, outHandlerKey);
				
		executeFQL(data, 0);
    };
        
    public void getFriends(String uid, short selection[], int cbSuccessOp, int cbErrorOp){
    	Bundle data = new Bundle();
		
		fqlmap.clear();		
		fqlmap.put(param_uid, uid);
		String selectedColumns = User.createColumnNames(selection);
		fqlmap.put("columns", selectedColumns);
		
		String fql_query = StrSubstitutor.replace(FQL_GET_USER_FRIENDS, fqlmap);		
		Log.d(LOG_TAG, "getFriends fql: "+fql_query);
				
		data.putString(XTRA_FQL_QUERY, fql_query);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
		data.putInt(XTRA_FQL_RESPONSE_SCHEMA, FQLRESPONSE_PHOTOS);
		data.putShortArray(XTRA_TABLECOLUMNS_SHORTARRAY, selection);
		data.putInt(XTRA_FQL_RESPONSE_SCHEMA, FQLRESPONSE_FRIENDS);
				
		executeFQL(data, 0);
    }
    

	public void getEventsFriendsInvited(String uid, int cbSuccessOp){
		
		Bundle data = new Bundle();
		fqlmap.clear();
		fqlmap.put("uid", uid);
		String fql_query = StrSubstitutor.replace(FQL_GET_EVENTS_FRIENDS_INVITED, fqlmap);		
		
		data.putString(XTRA_FQL_QUERY, fql_query);
		data.putInt(XTRA_FQL_RESPONSE_SCHEMA, FQLRESPONSE_EVENTS);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		
		executeFQL(data, 0);
	}
	
	private FBWSResponse doSendRequest(TreeMap<String, String> map) throws FBConnectionException {
		
		
		String requestSignature;				
		requestSignature = computeRequestSig(map, mSession.secret);
					
		if(requestSignature == null){ //can't compute signature?
			return null;
		}else{				
			map.put(param_sig, requestSignature);
		}	
		
		String content = generateRequestContent(map);
		Bundle postresponse;
		FBWSResponse fbwsresponse = null;

		postresponse = sendPost(api_rest_endpoint, content);
		String wsresponse = postresponse.getString(XTRA_RESPONSE);
		fbwsresponse = FBWSResponse.parse(wsresponse);
		return fbwsresponse;		
		
	}
	
	 public void getWallPosts(int outHandlerKey, Bundle inData, String uid, long created_date, int cbSuccessOp, int cbErrorOp, int cbTimeoutOp, long timeoutMillisecs, long limit, long offset){

	    	Bundle data = new Bundle(inData);
			
			fqlmap.clear();		
			fqlmap.put(param_uid, uid);
			fqlmap.put(subkey_limit, Long.toString(limit));
			fqlmap.put(subkey_offset, Long.toString(offset));			
			fqlmap.put(subkey_created_date, Long.toString(created_date));
			
			String fql_query = StrSubstitutor.replace(FQL_GET_WALLPOSTS, fqlmap).trim();		
			Logger.l(Logger.DEBUG,LOG_TAG, "[getWallPosts()] fql: "+fql_query);
					
			data.putString(XTRA_FQL_QUERIES, fql_query);
			data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_multiquery);
			data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
			data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
			data.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
			data.putInt(XTRA_INTERNAL_OUTHANDLER_KEY,outHandlerKey);
					
			executeFQL(data, 0);	
			
	 }
	 
	
	 
	 public FBWSResponse getEvents(long uid, long start_time) throws FBConnectionException{		
		fqlmap.clear();
		fqlmap.put("uid", Long.toString(uid));
		fqlmap.put("start_time", Long.toString(start_time));
		String fql_query = StrSubstitutor.replace(FQL_GET_EVENTS, fqlmap);
		TreeMap<String,String> map = new TreeMap<String,String>();
		map.put(param_fql_queries, fql_query);
		map.put(param_api_key, FBApp.api_key);
		map.put(param_format, RESPONSE_FORMAT_JSON);
		map.put(param_session_key, mSession.key);
		map.put(param_method, wsmethod_fql_multiquery);
		map.put(param_call_id, Long.toString(SystemClock.elapsedRealtime()));							
		return doSendRequest(map);
		
	}
	
	public FBWSResponse getNotifications(long uid, long created_time) throws FBConnectionException{		
		
		fqlmap.clear();
		fqlmap.put("uid", Long.toString(uid));
		fqlmap.put("created_time", Long.toString(created_time));
		String fql_query = StrSubstitutor.replace(FQL_GET_NOTIFICATIONS, fqlmap);
		TreeMap<String,String> map = new TreeMap<String,String>();
		map.put(param_fql_queries, fql_query);		
		map.put(param_api_key, FBApp.api_key);
		map.put(param_format, RESPONSE_FORMAT_JSON);
		map.put(param_session_key, mSession.key);
		map.put(param_method, wsmethod_fql_multiquery);
		map.put(param_call_id, Long.toString(SystemClock.elapsedRealtime()));							
		return doSendRequest(map);
		
	}
	
	/**
	 * 
	 * @param subject can be either user id, event id, or group id
	 * @param limit 
	 * @param offset
	 * @return
	 */
	public void getTaggedPhotos(int outHandlerKey, Bundle inData, long subject, int limit, int offset, int cbSuccessOp, int cbErrorOp, int cbTimeoutOp, long timeoutMillisecs){
		TreeMap<String,String> fqlmap = new TreeMap<String,String>(); 
		
		Bundle data = new Bundle(inData);
		
		fqlmap.clear();		
		fqlmap.put(param_subject, String.valueOf(subject));
		fqlmap.put(subkey_limit, Long.toString(limit));
		fqlmap.put(subkey_offset, Long.toString(offset));		
		
		String fql_query = StrSubstitutor.replace(FQL_GET_TAGGED_PHOTOS, fqlmap).trim();		
		Logger.l(Logger.DEBUG,LOG_TAG, "[getTaggedPhoto()] fql: "+fql_query);
				
		data.putString(XTRA_FQL_QUERIES, fql_query);
		data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_multiquery);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
		data.putInt(XTRA_INTERNAL_OUTHANDLER_KEY, outHandlerKey);
		
		executeFQL(data, 0);
	}
	
	/**
	 * 
	 * @param pid Photo Id
	 * @param limit max number of returned rows
	 * @param offset starting retrieval offset 
	 * @param cbSuccessOp sucess code passed on to listener handler
	 * @param cbErrorOp error code passed on to listener handler
	 * @param cbTimeoutOp timeout code passed on to listener handler
	 * @param timeoutMillisecs timeout in milliseconds
	 */
	public FQLTask getPhotoTags(String pid, int limit, int offset, int cbSuccessOp, int cbErrorOp, int cbTimeoutOp, long timeoutMillisecs){
		TreeMap<String,String> fqlmap = new TreeMap<String,String>(); 
		
		Bundle data = new Bundle();
		
		fqlmap.clear();		
		fqlmap.put("pid", pid);
		fqlmap.put("limit", Long.toString(limit));
		fqlmap.put("offset", Long.toString(offset));		
		
		String fql_query = StrSubstitutor.replace(FQL_GET_PHOTOTAGS, fqlmap).trim();		
		Logger.l(Logger.DEBUG,LOG_TAG, "[getPhotoTags()] fql: "+fql_query);
				
		data.putString(XTRA_FQL_QUERIES, fql_query);
		data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_multiquery);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
				
		return executeFQL(data, 0);		
	}
	
	
	public FBWSResponse events_rsvp(long eid, Event.RSVPStatus rsvpStatus) throws FBConnectionException{		
		
		TreeMap<String,String> map = new TreeMap<String,String>(); 
		
		map.put(param_api_key, FBApp.api_key);
		map.put(param_format, RESPONSE_FORMAT_JSON);
		map.put(param_session_key, mSession.key);
		map.put(param_eid, Long.toString(eid));
		map.put(param_rsvp_status, rsvpStatus.toString());
		map.put(param_method, wsmethod_events_rsvp);
		map.put(param_v, "1.0");
		long elapsedRealTime = SystemClock.elapsedRealtime();
		long elapsedSeconds = elapsedRealTime/1000;
		long elapsedMilliseconds = elapsedRealTime % 1000;
		String call_id =  elapsedSeconds+"."+elapsedMilliseconds;
	//	Log.d("debug","call_id:"+call_id);
		map.put(param_call_id, Long.toString(elapsedRealTime));							
		return doSendRequest(map);		
	}
	

	public FBWSResponse events_create(long eid, Event.RSVPStatus rsvpStatus) throws FBConnectionException{		
				
		RestMethod m = new RestMethod(true);
		
		TreeMap<String,String> map = new TreeMap<String,String>(); 
		String eventInfo = "";
		
		map.put(param_api_key, FBApp.api_key);
		map.put(param_format, RESPONSE_FORMAT_JSON);
		map.put(param_session_key, mSession.key);
		map.put(param_event_info, eventInfo);
		map.put(param_method, wsmethod_events_rsvp);
		map.put(param_v, "1.0");
		map.put(param_call_id, Long.toString(SystemClock.elapsedRealtime()));							
		
		String requestSignature = computeRequestSig(map, mSession.secret);
		
		if(requestSignature == null){ //can't compute signature?
			return null;
		}else{		
			map.remove(param_sig);
			map.put(param_sig, requestSignature);
		}					
								
		return doSendRequest(map);		
	}
	
	public void getSessionUserInfo(short[] selectedFields){		
		Bundle data = new Bundle();
		data.putString(ManagerThread.XTRA_CALLBACK_INTENT_ACTION, App.INTENT_PROFILE_UPDATED);
		getOneUserData(R.id.outhandler_app, data, String.valueOf(getSession().uid), selectedFields, ManagerThread.CALLBACK_GET_USERDATA, ManagerThread.CALLBACK_SERVERCALL_ERROR, ManagerThread.CALLBACK_TIMEOUT_ERROR);
	}

	
	public void onSessionValidated(boolean isValid, int errCode, String errMessage){
		Log.d(LOG_TAG,"onSessionValidated isValid: "+isValid+" , errCode: "+errCode+" ,"+errMessage);
	}
	
	public void postComment(String comment, String post_id, int cbSuccessOp, int cbErrorOp, int cbTimeoutOp){
			
		RestMethod m = new RestMethod(true);
		
		TreeMap<String,String> map = new TreeMap<String,String>(); 
		
		map.put(param_api_key, FBApp.api_key);
		map.put(param_format, RESPONSE_FORMAT_JSON);
		map.put(param_session_key, mSession.key);
		map.put(param_post_id, post_id);
		map.put(param_comment, comment);		
		map.put(param_method, wsmethod_stream_addComment);
		
		String requestSignature = computeRequestSig(map, mSession.secret);
		
		if(requestSignature == null){ //can't compute signature?
			return;
		}else{		
			map.put(param_sig, requestSignature);
		}					
			
		String content = generateRequestContent(map);		
		
		Bundle b = new Bundle();
		b.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
		b.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		b.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
		b.putString(XTRA_WEBSERVICE_REQUEST_CONTENT, content);
		m.execute(b);
	}
	
	public void getAlbums(long uid, int cbSuccessOp, int cbErrorOp, int cbTimeoutOp, long timeout, long limit, long offset){
		TreeMap<String,String> fqlmap = new TreeMap<String,String>(); 
		
		Bundle data = new Bundle();
		
		fqlmap.clear();		
		fqlmap.put(param_owner, Long.toString(uid));
		fqlmap.put(subkey_limit, Long.toString(limit));
		fqlmap.put(subkey_offset, Long.toString(offset));		
		
		String fql_query = StrSubstitutor.replace(FQL_GET_ALBUMS, fqlmap).trim();		
		Logger.l(Logger.DEBUG,LOG_TAG, "[getAlbums()] limit: "+limit+", offset:"+offset);
		Logger.l(Logger.DEBUG,LOG_TAG, "[getAlbums()] fql: "+fql_query);
				
		data.putString(XTRA_FQL_QUERIES, fql_query);
		data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_multiquery);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
				
		executeFQL(data, 0);
	}
	
	public void getPhotos (long aid, int cbSuccessOp, int cbErrorOp, int cbTimeoutOp, long timeoutMilli, long limit, long offset){
		TreeMap<String,String> fqlmap = new TreeMap<String,String>(); 
		
		Bundle data = new Bundle();
		
		fqlmap.clear();		
		fqlmap.put(param_aid, Long.toString(aid));
		fqlmap.put(subkey_limit, Long.toString(limit));
		fqlmap.put(subkey_offset, Long.toString(offset));
		
		String fql_query = StrSubstitutor.replace(FQL_GET_TAGGED_PHOTOS, fqlmap).trim();		
		Logger.l(Logger.DEBUG,LOG_TAG, "[getPhotos()] fql: "+fql_query);
				
		data.putString(XTRA_FQL_QUERIES, fql_query);
		data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_multiquery);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
				
		executeFQL(data, 0);
	}
	
	public void getContacts (int outHandlerId, long uid, int cbSuccessOp, int cbErrorOp, int cbTimeoutOp, long timeoutMilli, long limit, long offset){
		TreeMap<String,String> fqlmap = new TreeMap<String,String>(); 
		
		Bundle data = new Bundle();
		
		fqlmap.clear();
		fqlmap.put(param_uid, Long.toString(uid));
		fqlmap.put(subkey_limit, Long.toString(limit));
		fqlmap.put(subkey_offset, Long.toString(offset));
		
		String fql_queries = StrSubstitutor.replace(FQL_GET_CONTACTS, fqlmap).trim();		
		Logger.l(Logger.DEBUG,LOG_TAG, "[getContacts()] fql: "+fql_queries);
				
		data.putString(XTRA_FQL_QUERIES, fql_queries);
		data.putString(XTRA_FQL_QUERY_TYPE, wsmethod_fql_multiquery);
		data.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
		data.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
		data.putInt(XTRA_INTERNAL_OUTHANDLER_KEY, outHandlerId);		
		executeFQL(data, 0);
	}

	public void deleteComment(String comment_id, int cbSuccessOp, int cbErrorOp, int cbTimeoutOp){
			
		RestMethod m = new RestMethod(true);
		
		TreeMap<String,String> map = new TreeMap<String,String>(); 
		
		map.put(param_api_key, FBApp.api_key);
		map.put(param_format, RESPONSE_FORMAT_JSON);
		map.put(param_session_key, mSession.key);
		map.put(param_comment_id, comment_id);		
		map.put(param_method, wsmethod_stream_removeComment);
		
		String requestSignature = computeRequestSig(map, mSession.secret);
		
		if(requestSignature == null){ //can't compute signature?
			return;
		}else{		
			map.remove(param_sig);
			map.put(param_sig, requestSignature);
		}					
			
		String content = generateRequestContent(map);		
		
		Bundle b = new Bundle();
		b.putInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE, cbErrorOp);
		b.putInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE, cbSuccessOp);
		b.putInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE, cbTimeoutOp);
		b.putString(XTRA_WEBSERVICE_REQUEST_CONTENT, content);
		m.execute(b);
	}
	
	class RestMethod extends AsyncTask<Bundle, Void, Bundle>{
		Bundle inData = null;
		Exception mInvocationException; 
		boolean mAutoBroadcastLogin;
		
		
		public RestMethod(boolean autoBroadCastLogin) {
			mAutoBroadcastLogin = autoBroadCastLogin;
		}
		
		protected Bundle doInBackground(Bundle... params) {
			
			if(params == null || params.length == 0){
				cancel(true);
			}else{
				inData = params[0];
				params[0] = null;
				params = null;
			}
						

			if(!quickCheckSession(mSession, mAutoBroadcastLogin)){
				cancel(true);
				onInvalidSession();
				Bundle out = new Bundle(inData);
				out.putBoolean(XTRA_INTERNAL_ASYNCTASK_ABORTED_SESSIONEXPIRED, true);
				return out;
			}
						
			TreeMap<String,String> mapping = (TreeMap<String,String>)((TreeMap<String,String>) inData.getSerializable(XTRA_METHODPARAMETERSMAP)).clone();			
			mapping.put(param_call_id,  Long.toString(SystemClock.elapsedRealtime()));			
			
			String requestSignature = computeRequestSig(mapping, mSession.secret);						
			if(requestSignature == null){ //can't compute signature?
				cancel(true);
				return null;
			}else{				
				mapping.put(param_sig, requestSignature);
			}	
			
			String content = generateRequestContent(mapping);
			
			Bundle outData = new Bundle(inData);
			
			synchronized(this) {
				try{
					Bundle postresponse = sendPost(api_rest_endpoint,content);					
					String wsresponse = postresponse.getString(XTRA_RESPONSE);
					FBWSResponse fbwsresponse = FBWSResponse.parse(wsresponse);
					if(fbwsresponse == null){
						cancel(true);
						return null;
					}						
					Logger.l(Logger.DEBUG, LOG_TAG, "[RestMethod] [doInBackground()] response check: "+wsresponse);
					outData.putAll(postresponse);					
					return outData;							
				} 
				catch(FBConnectionException e){		
					outData.putBoolean(XTRA_INTERNAL_FBCONNECTIONERROR, true);
					this.mInvocationException = e;					
				}
			}			
			return outData;
		}
		
	
		protected void onConnectionError(Bundle data, Exception e){	
			Logger.l(Logger.ERROR,LOG_TAG,"[RestMethod] [onConnectionError()] "+e.getMessage());
			Handler outHandler = mListenerHandler;
			if(data.containsKey(XTRA_INTERNAL_OUTHANDLER_KEY)){
				int handlerKey = data.getInt(XTRA_INTERNAL_OUTHANDLER_KEY);
				if(mOutRegistry.containsKey(handlerKey)){
					outHandler = mOutRegistry.get(handlerKey);					
				}	
			}
			
			if(outHandler == null){
				Logger.l(Logger.ERROR, LOG_TAG, "[RestMethod] [onConnectionError()] listener handler is null");
				return;
			}
			
			int messageCode = data.getInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE);
			Message msg = outHandler.obtainMessage(messageCode);
			data.putInt(XTRA_SERVERCALL_STATUS_CODE, SERVERCALL_ERROR);								
			data.putString(XTRA_SERVERCALL_ERROR_MSG, e.getMessage());
			msg.setData(data);				
			msg.sendToTarget();				
		}
		
		/**
		 * Handles success
		 * @param data
		 * @param e
		 */
		protected void onSuccess(Bundle data){			
			Handler outHandler = mListenerHandler;
			int handlerKey = -1;
			if(data.containsKey(XTRA_INTERNAL_OUTHANDLER_KEY)){
				handlerKey = data.getInt(XTRA_INTERNAL_OUTHANDLER_KEY);
				if(mOutRegistry.containsKey(handlerKey)){
					outHandler = mOutRegistry.get(handlerKey);					
				}	
			}
			
			if(outHandler == null){
				Logger.l(Logger.ERROR, LOG_TAG, "[onSuccess()] listener handler is null");
				return;
			}
			int messageCode = data.getInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE);
			Logger.l(Logger.DEBUG,LOG_TAG,"[RestMethod] [onSuccess()] sending success message to listener handler. Handler key:"+handlerKey+" Code:"+messageCode+", CodeName:"+XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE);
			Message msg = outHandler.obtainMessage(messageCode);
			msg.setData(data);
			msg.sendToTarget();
		}
		
		
		@Override
		protected void onPreExecute() {			
			super.onPreExecute();	
		
		}
		
		@Override
		protected void onPostExecute(Bundle result) {
			super.onPostExecute(result);
			assert result != null;
			
			boolean aborted = result.getBoolean(XTRA_INTERNAL_ASYNCTASK_ABORTED_SESSIONEXPIRED);
			
			if(aborted){
				return;
			}
			
			boolean hasConnectionError = result.getBoolean(XTRA_INTERNAL_FBCONNECTIONERROR, false);
			if(hasConnectionError){
				onConnectionError(inData, mInvocationException);
				return;
			}
			
			String wsresponse = result.getString(XTRA_RESPONSE);
			FBWSResponse fbwsresponse = FBWSResponse.parse(wsresponse);
			assert fbwsresponse != null;
			if(fbwsresponse != null){
				if(fbwsresponse.hasErrorCode){
					result.putInt(Facebook.XTRA_FBRESPONSE_ERROR_CODE, fbwsresponse.errorCode);
					result.putString(Facebook.XTRA_FBRESPONSE_ERROR_MESSAGE, fbwsresponse.errorDesc);					
					switch(fbwsresponse.errorCode){
						case FBWSErrorCodes.API_EC_PARAM_SIGNATURE:{
							//re-invoke fql
							Logger.l(Logger.ERROR, LOG_TAG, "[RestMethod] [onPostExecute()] signature error, requesting again.");
							executeRest(result, mReExecuteWaitTime);
							return;
						}
						case FBWSErrorCodes.SESSIONEXPIRED:
						case FBWSErrorCodes.API_EC_SESSION_REQUIRED:
						case FBWSErrorCodes.API_EC_SESSION_INVALID:
						case FBWSErrorCodes.API_EC_SESSION_TIMED_OUT:{
							broadcastLogin();
							return;
						}	
					}
				}				
			}	
			onSuccess(result);
		}


		private void onInvalidSession(){
			Handler outHandler = mListenerHandler;
			int handlerKey = -1;
			if(inData.containsKey(XTRA_INTERNAL_OUTHANDLER_KEY)){
				handlerKey = inData.getInt(XTRA_INTERNAL_OUTHANDLER_KEY);
				if(mOutRegistry.containsKey(handlerKey)){
					outHandler = mOutRegistry.get(handlerKey);					
				}	
			}
			Message msg = outHandler.obtainMessage( inData.getInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE));		
			msg.sendToTarget();
		}
	}
	
	
	//single fql query or multi fql queries
	public class FQLTask extends UserTask<Bundle, Void, Bundle>
	{
		Bundle inData;
		FBConnectionException mFBConnectionException;
		boolean mAutoBroadcastLogin;
		
		public FQLTask(boolean autoBroadCastLogin) {
			mAutoBroadcastLogin = autoBroadCastLogin;
		}
		
		@Override
		protected void onTimeout() {		
			super.onTimeout();
			if(mListenerHandler != null){
				Message msg = mListenerHandler.obtainMessage();			
				msg.setData(inData);
				msg.what = inData.getInt(XTRA_CALLBACK_SERVERCALL_TIMEOUT_OPCODE);
				msg.sendToTarget();
			}
		}
		
		@Override
		protected Bundle doInBackground(Bundle... params) {
			if(params == null || params.length == 0){
				cancel(true);
			}else{
				inData = params[0];
			}
			
			if(!quickCheckSession(mSession, mAutoBroadcastLogin)){
				cancel(true);				
				onInvalidSession();	
				Bundle out = new Bundle(inData);
				out.putBoolean(XTRA_INTERNAL_ASYNCTASK_ABORTED_SESSIONEXPIRED, true);
				return out;
			}
			
			TreeMap<String,String> map = new TreeMap<String,String>();
			String fqlQueries;
			String fqlQuery;			
			String methodUsed = inData.getString(XTRA_FQL_QUERY_TYPE);			
			
			//if query type is not provided in the bundle data then set default to single fql query. 
			if(methodUsed == null || methodUsed.compareTo(wsmethod_fql_query) == 0){
				methodUsed = wsmethod_fql_query;
				fqlQuery = inData.getString(XTRA_FQL_QUERY);
				if(fqlQuery == null){
					cancel(true);
				}
				map.put(param_fql_query,fqlQuery);
			}else{
				if(methodUsed.compareTo(wsmethod_fql_multiquery) == 0){
					fqlQueries = inData.getString(XTRA_FQL_QUERIES);
					if(fqlQueries == null){
						cancel(true);
					}
					map.put(param_fql_queries,fqlQueries);
				}
			}
		
			map.put(param_api_key, FBApp.api_key);
			map.put(param_format, RESPONSE_FORMAT_JSON);
			map.put(param_session_key, mSession.key);
			map.put(param_call_id, Long.toString(SystemClock.elapsedRealtime()));
			map.put(param_method, methodUsed);
			
			//copy the provided bundled data to the out data.
			Bundle outData = new Bundle(inData);
			
			synchronized(this) {
				
				try{
				
					FBWSResponse fbwsresponse = null;
					String content, requestSignature;					
					requestSignature = computeRequestSig(map, mSession.secret);
						
					if(requestSignature == null){ //can't compute signature?
						return null;
					}else{				
						map.put(param_sig, requestSignature);
					}					
						
					content = generateRequestContent(map);											
					Bundle postResponse = sendPost(api_rest_endpoint, content);	 			
					String wsresponse = postResponse.getString(XTRA_RESPONSE);
					fbwsresponse = FBWSResponse.parse(wsresponse);						
					outData.putAll(postResponse);
					return outData;
					
				} catch(FBConnectionException e){		
					mFBConnectionException = e;					
					outData.putBoolean(XTRA_INTERNAL_FBCONNECTIONERROR, true);
				}
				
				return outData;
			}			
		}
		
		/**
		 * Handles error 
		 * @param data
		 * @param e
		 */
		protected void onConnectionError(Bundle data, Exception e){
			Handler outHandler = mListenerHandler;
			if(data.containsKey(XTRA_INTERNAL_OUTHANDLER_KEY)){
				int handlerKey = data.getInt(XTRA_INTERNAL_OUTHANDLER_KEY);
				if(mOutRegistry.containsKey(handlerKey)){
					outHandler = mOutRegistry.get(handlerKey);					
				}	
			}
			
			if(outHandler == null){
				Logger.l(Logger.ERROR, LOG_TAG, "[onError()] listener handler is null");
				return;
			}
			int messageCode = data.getInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE);
			Logger.l(Logger.DEBUG,LOG_TAG,"[FQLTask] [onConnectionError()] sending error message to listener handler. Code:"+messageCode+", CodeName:"+XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE);
			
			Message msg = outHandler.obtainMessage(messageCode);			
			data.putInt(XTRA_SERVERCALL_STATUS_CODE, SERVERCALL_ERROR);
			data.putString(XTRA_SERVERCALL_ERROR_MSG, e.getMessage());			
			msg.setData(data);
			msg.sendToTarget();
		}
		
		/**
		 * Handles success
		 * @param data
		 * @param e
		 */
		protected void onSuccess(Bundle data){	
			Handler outHandler = mListenerHandler;
			int handlerKey = -1;
			if(data.containsKey(XTRA_INTERNAL_OUTHANDLER_KEY)){
				handlerKey = data.getInt(XTRA_INTERNAL_OUTHANDLER_KEY);
				if(mOutRegistry.containsKey(handlerKey)){
					outHandler = mOutRegistry.get(handlerKey);					
				}	
			}
			if(outHandler == null){
				//TODO Remove logger
				Logger.l(Logger.ERROR, LOG_TAG, "[onSuccess()] listener handler is null");
				return;
			}
			
			int messageCode = data.getInt(XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE);
			//TODO Remove logger
			Logger.l(Logger.DEBUG,LOG_TAG,"[FQLTask][onSuccess()] sending success message to listener handler. Handler key:"+handlerKey+" Code:"+messageCode+", CodeName:"+XTRA_CALLBACK_SERVERCALL_SUCCESS_OPCODE);
			Message msg = outHandler.obtainMessage(messageCode);
			msg.setData(data);
			msg.sendToTarget();
		}
		
		@Override
		protected void onPreExecute() {			
			super.onPreExecute();						
			
		}
		
		@Override
		protected void onPostExecute(Bundle result) {
			super.onPostExecute(result);
			assert result != null;
			boolean aborted = result.getBoolean(XTRA_INTERNAL_ASYNCTASK_ABORTED_SESSIONEXPIRED);
		
			if(aborted){
				return;
			}
			
			boolean hasConnectionError = result.getBoolean(XTRA_INTERNAL_FBCONNECTIONERROR);
			if(hasConnectionError){
				onConnectionError(inData, mFBConnectionException);
				return;
			}			
			String wsresponse = result.getString(XTRA_RESPONSE);
			FBWSResponse fbwsresponse = FBWSResponse.parse(wsresponse);
			assert fbwsresponse != null;
			if(fbwsresponse != null){
				if(fbwsresponse.hasErrorCode){	
					result.putInt(Facebook.XTRA_FBRESPONSE_ERROR_CODE, fbwsresponse.errorCode);
					result.putString(Facebook.XTRA_FBRESPONSE_ERROR_MESSAGE, fbwsresponse.errorDesc);
					switch(fbwsresponse.errorCode){
						case FBWSErrorCodes.API_EC_PARAM_SIGNATURE:{
							Logger.l(Logger.ERROR, LOG_TAG, "[FQLTask] [onPostExecute()] signature error, requesting again.");
							//re-invoke fql
							executeFQL(result, mReExecuteWaitTime);
							return;
						}
						case FBWSErrorCodes.SESSIONEXPIRED:
						case FBWSErrorCodes.API_EC_SESSION_REQUIRED:
						case FBWSErrorCodes.API_EC_SESSION_INVALID:
						case FBWSErrorCodes.API_EC_SESSION_TIMED_OUT:{
							broadcastLogin();
							return;
						}	
					//	default:{	
						//	onConnectionError(inData, new Exception(fbwsresponse.errorDesc));
						//}
					}
				}		
								
			}
			onSuccess(result);
		}

		private void onInvalidSession(){
			Handler outHandler = mListenerHandler;
			int handlerKey = -1;
			if(inData.containsKey(XTRA_INTERNAL_OUTHANDLER_KEY)){
				handlerKey = inData.getInt(XTRA_INTERNAL_OUTHANDLER_KEY);
				if(mOutRegistry.containsKey(handlerKey)){
					outHandler = mOutRegistry.get(handlerKey);					
				}	
			}
			Message msg = outHandler.obtainMessage( inData.getInt(XTRA_CALLBACK_SERVERCALL_ERROR_OPCODE));		
			msg.sendToTarget();
		}
	}
	
	private FQLTask executeFQL(final Bundle executionDescriptors, long delayMillis){
		final boolean broadcastLogin = executionDescriptors.getBoolean(XTRA_INTERNAL_BROADCASTLOGININTENT);
		final FQLTask task = new FQLTask(broadcastLogin);
		Runnable r = new Runnable(){
			public void run() {	
				task.execute(executionDescriptors);				
			};
		};
		mInterceptorHandler.postDelayed(r, delayMillis);
		return task;
	}

	private boolean executeRest(final Bundle executionDescriptors, long delayMillis){
		Runnable r = new Runnable(){
			public void run() {
				final boolean broadcastLogin = executionDescriptors.getBoolean(XTRA_INTERNAL_BROADCASTLOGININTENT);
				new RestMethod(broadcastLogin).execute(executionDescriptors);				
			};
		};
		return mInterceptorHandler.postDelayed(r, delayMillis);
	}

	public static Facebook getInstance(){
		if(INSTANCE == null){
			INSTANCE = new Facebook(App.INSTANCE);
		}		
		return INSTANCE;
	}
	
	private Facebook(Context ctx){
		init(ctx);
	}
	
	private void init(Context ctx){
		
		mSession = new FBSession();		
		this.mContext = ctx;
		fqlmap = new TreeMap<String, String>();
		Resources mResources = ctx.getResources();
		
		FQL_GET_NEWSFEED_STREAM_WITH_COMMENTS = mResources.getString(R.string.fql_get_streams);
		FQL_MULTIQUERY_GET_COMMENTS_COMPLETE = mResources.getString(R.string.fql_get_comments_complete);
		FQL_GET_FRIENDLISTS = mResources.getString(R.string.fql_get_friendlists);
		FQL_GET_NOTIFICATIONS = mResources.getString(R.string.fql_get_notifications);
		FQL_GET_EVENTS = mResources.getString(R.string.fql_get_events);		
		FQL_GET_TAGGED_PHOTOS = mResources.getString(R.string.fql_get_taggedphotos); 
		FQL_GET_PHOTOTAGS = mResources.getString(R.string.fql_get_tags_with_users);
		FQL_GET_COMMENTS = mResources.getString(R.string.fql_get_comments);
		FQL_GET_COMMENTS_USERS = mResources.getString(R.string.fql_get_comments_users);
		FQL_GET_ALBUMS = mResources.getString(R.string.fql_get_albums);
		FQL_GET_PHOTOS = mResources.getString(R.string.fql_get_photos);
		FQL_GET_CONTACTS = mResources.getString(R.string.fql_get_contacts);
		FQL_GET_WALLPOSTS = mResources.getString(R.string.fql_get_wallposts);
		FQL_MULTIQUERY_GET_COMMENTS_PHOTOS_COMPLETE = mResources.getString(R.string.fql_get_comments_photos_complete);
		
	}
	
	public static String computeRequestSig(TreeMap<String,String> tmap, String session_secret){
		Iterator<String> it = tmap.keySet().iterator();
		String ckvp = "";
					
		while(it.hasNext()){
			String key = it.next();
			//Log.d(LOG_TAG,key);
			if(!key.equals("[no name]")){
				ckvp+=key+"="+tmap.get(key);	
			}else{
				ckvp+=tmap.get(key);
			}
			
		}
		
		ckvp += session_secret;  //concatenated key value pair k=v without a space between pairs
		Log.d(LOG_TAG,"string to be hashed: "+ckvp);
		
		String computedSig = "";
		try {
			final String digestAlgorithm = "MD5"; 
			MessageDigest m;
			m = MessageDigest.getInstance(digestAlgorithm);			
			m.update(ckvp.trim().getBytes("UTF-8"),0,ckvp.length());			
			computedSig = new BigInteger(1,m.digest()).toString(16).trim();
			Log.d(LOG_TAG,"MD5: "+ computedSig);
			return computedSig;
		} catch (NoSuchAlgorithmException e) {
			Log.e(LOG_TAG,"NoSuchAlgorithm "+e.getMessage());
			e.printStackTrace();
			return null;
		} catch (UnsupportedEncodingException e) {
			Log.e(LOG_TAG,"UnsupportedEncodingException "+e.getMessage());
			e.printStackTrace();
			return null;
		}
		
	}

	public static synchronized Bundle sendPost(String endpoint, String content) throws FBConnectionException
    {   
       	
    	HttpURLConnection fbc;
    	String response = "";
    	
    	boolean isConnectionError = false;
    	String errorMessage = "";
		try {
			URL url = new URL(endpoint);
			fbc = (HttpURLConnection) url.openConnection();		
			fbc.setUseCaches(false);
			fbc.setDoInput(true);
			fbc.setDoOutput(true);
			fbc.setConnectTimeout(mConnectionTimeout);
			fbc.setRequestMethod(REQUEST_METHOD);
			fbc.setRequestProperty("Content-type", REQUEST_CONTENT_TYPE);			
			fbc.setRequestProperty("User-Agent",REQUEST_USER_AGENT);
			fbc.setRequestProperty("Accept-Language", REQUEST_ACCEPT_LANGUAGE);
						
			Log.d(LOG_TAG,"post content: "+content.substring(0,content.length()/2));
			Log.d(LOG_TAG,"post content: "+content.substring(content.length()/2,content.length()));
			DataOutputStream dos = new DataOutputStream(fbc.getOutputStream());
			dos.writeBytes(content);
			dos.flush();
			dos.close();
			
			InputStream is = fbc.getInputStream();
			BufferedReader breader = new BufferedReader(new InputStreamReader(is,Charset.forName("UTF-8")));
			int linecount=0;
			
			while(true){
				String line = breader.readLine();				
				if(line == null){
					break;					
				}
				linecount++;
				response += line;
				Log.d(LOG_TAG,"[line#:"+linecount+"] "+line);
			}
										
		} catch (MalformedURLException e) {			
			isConnectionError = true;
			errorMessage = "MalformedURLException: "+e.getMessage();
		} catch (ProtocolException e) {
			isConnectionError = true;
			errorMessage = "ProtocolException: "+e.getMessage();
		} catch (UnknownHostException e){
			isConnectionError = true;
			errorMessage = "UnknownHostException: "+e.getMessage();
		} catch (IOException e) {			
			isConnectionError = true;
			errorMessage = "IOException: "+e.getMessage();
		}
		
		if(isConnectionError){			
			throw new FBConnectionException(errorMessage);
		}
		
		int numBytes = response.getBytes().length;
	
		Logger.l(Logger.DEBUG,LOG_TAG,"[sendPost()] response's total bytes: "+numBytes);
		Logger.l(Logger.DEBUG,LOG_TAG,"[sendPost()] finished.");
	
		Bundle bundle = new Bundle();
		bundle.putString(XTRA_RESPONSE, response);
		bundle.putInt(XTRA_RESPONSE_BYTELENGTH, numBytes);
		return bundle;
		
    }
      
    private static String generateRequestContent(TreeMap<String,String> params){
    	
		Iterator<String> it = params.keySet().iterator();
		String content = "";	
		boolean first = true;
		while(it.hasNext()){
			if(!first){
				content += "&";				
			}else{
				first = false;
			}
			String key = it.next();			
			content += key+"="+params.get(key);
		}
		Log.d(LOG_TAG,"req content: "+content);
		return content.trim();
		
	}
    
    public void setSession(FBSession session){
    	mSession = session;
    }
    
    public FBSession getSession(){
    	return mSession;
    }

    public boolean quickCheckSession(boolean sendLoginIntent){
    	return quickCheckSession(mSession, sendLoginIntent);
    }
    
	public boolean quickCheckSession(FBSession session, boolean sendLoginIntent){
    	boolean isValid = true;
    	if(session == null){
    		isValid = false;
    	}
    	else if(session.uid == 0){
    		isValid = false;
    	}
    	else if(session.secret==null){
    		isValid = false;
    	}
    	
    	if(!isValid && sendLoginIntent){
    		broadcastLogin();
    		return false;
    	}
    	
    	Logger.l(Logger.DEBUG, LOG_TAG, "[quickCheckSession()] isValid: "+isValid);
    	return isValid;
    }
    
    private void broadcastLogin(){
    	Logger.l(Logger.DEBUG, LOG_TAG, "[broadcastLogin()]");
    	Intent loginIntent = new Intent(App.INTENT_LOGIN);
    	loginIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    	App.INSTANCE.sendBroadcast(loginIntent);
    }
    
	public static HashMap<String,JSONArray> parseMultiFQLResult(FBWSResponse data, HashMap<String,JSONArray> cache){
		if(data == null){
			return cache;
		}
		if(cache == null){
			cache = new HashMap<String,JSONArray>();
		}
				
		JSONArray arr = data.jsonArray;
		int count = arr.length();
		
		for(int i=0;i<count;i++){
			try{
				JSONObject result = arr.getJSONObject(i);
				String name = result.getString("name");
				JSONArray fql_result_set = result.getJSONArray("fql_result_set");
				cache.put(name, fql_result_set);
			}catch(JSONException e){	
			}
		}
		
		return cache;
	}
	
		
	public FBObject getFbObjectTypeFromUri(String uri){
		//   \?(\w*)=(\d*)
		Pattern p = Pattern.compile("\\?(\\w*)=(\\d*)");
		Matcher m = p.matcher(uri);
		String g0=null;
		String g1=null;
		String g2=null;
		
		while(m.find()){
			g0 = m.group(0);
			g1 = m.group(1);
			g2 = m.group(2);
			Logger.l(Logger.VERBOSE, LOG_TAG, g0+", "+g1+", "+g2);
		}	
		
		if(g1 != null){			
			return FBObject.parseFromIdName(g1,g2);						
		}
		
		return null;
	}
	
	public String uploadPhoto(File imageFile, Handler h){
		FileInputStream fis = null;
		try{
			fis = new FileInputStream(imageFile);
			long length = imageFile.length();
			return uploadPhoto(fis, length, h);
		}catch(FileNotFoundException e){
		}finally{
			BasicClientHelper.close(fis);
		}
		return null;
	}

	public String uploadPhoto(InputStream imageInputStream, long length, final Handler h){
		String method = wsmethod_photos_upload;
		String version = FBApp.api_version;
		String session_key = mSession.key;
		String call_id = Long.toString(SystemClock.elapsedRealtime());
		String sig = "";
		
		TreeMap<String,String> map = new TreeMap<String, String>();
		map.put("v", version);		
		map.put("api_key", FBApp.api_key);
		map.put("session_key",session_key);
		map.put("call_id", call_id);
		map.put("method", method);
		map.put("format", Facebook.RESPONSE_FORMAT_JSON);
			
		sig = Facebook.computeRequestSig(map, mSession.secret);
		map.put("sig",sig);
		String response = null; 
		try{			
			response = uploadFile(map, "image.png", imageInputStream, length, h );
			Logger.l(Logger.DEBUG,LOG_TAG,response);
		}catch(IOException e){		
			e.printStackTrace();
		}finally{
		 BasicClientHelper.close(imageInputStream);
		}
		return response;
	}		
	
	
    
	public String uploadFile(Map<String,String> params, String fileName, InputStream fileStream, long length , final Handler h) throws IOException {
         URL _serverUrl = new URL(Facebook.api_rest_endpoint);
         int _timeout = CONNECTION_TIMEOUT;
         int _readTimeout = READ_TIMEOUT;
		  HttpURLConnection con = null;
         OutputStream urlOut = null;
         InputStream in = null;       
        
         
         int totalReadSoFar = 0;
         try {
                 String boundary = Long.toString( System.currentTimeMillis(), 16 );
                 con = (HttpURLConnection) _serverUrl.openConnection();
                 if ( _timeout != -1 ) {
                         con.setConnectTimeout( _timeout );
                 }
                 if ( _readTimeout != -1 ) {
                         con.setReadTimeout( _readTimeout );
                 }
                 con.setDoInput( true );
                 con.setDoOutput( true );
                 con.setUseCaches( false );
                 con.setRequestProperty( "Content-Type", "multipart/form-data; boundary=" + boundary );
                 con.setRequestProperty( "MIME-version", "1.0" );

                 urlOut = con.getOutputStream();
                 DataOutputStream out = new DataOutputStream( urlOut );

                 for ( Map.Entry<String,String> entry : params.entrySet() ) {
                         out.writeBytes( PREF + boundary + CRLF );
                         out.writeBytes( "Content-Type: text/plain;charset=utf-8" + CRLF );
                         out.writeBytes( "Content-disposition: form-data; name=\"" + entry.getKey() + "\"" + CRLF );
                         out.writeBytes( CRLF );
                         byte[] valueBytes = entry.getValue().toString().getBytes( "UTF-8" );
                         out.write( valueBytes );
                         out.writeBytes( CRLF );
                 }

                 out.writeBytes( PREF + boundary + CRLF );
                 out.writeBytes( "Content-Type: image" + CRLF );
                 out.writeBytes( "Content-disposition: form-data; filename=\"" + fileName + "\"" + CRLF );
                 
                 // Write the file
                 out.writeBytes( CRLF );
                 byte buf[] = new byte[UPLOAD_BUFFER_SIZE];
                 int len = 0;
                 
                 Facebook.sendUpdate(h, MESSAGE_UPDATE_UPLOAD_COMMENCING, length, totalReadSoFar);
                 while ( len >= 0 ) {
                         out.write( buf, 0, len );
                         len = fileStream.read( buf );
                         totalReadSoFar += len!=-1?len:0; //dont add -1
                         Facebook.sendUpdate(h, MESSAGE_UPDATE_UPLOAD_PROGRESS, length, totalReadSoFar);
                 }
                 out.writeBytes( CRLF + PREF + boundary + PREF + CRLF );
                 out.flush();               
                 in = con.getInputStream();
                 return BasicClientHelper.getResponse(in );
         }
         finally {
                 BasicClientHelper.close( urlOut );
                 BasicClientHelper.close( in );
                 BasicClientHelper.disconnect( con );
                 Facebook.sendUpdate(h, MESSAGE_UPDATE_UPLOAD_FINISHED, length, totalReadSoFar);
         }
	 }

	public static void sendUpdate(final Handler h, int code, double total, int count){
		if(h != null){
			Message updateMessage = h.obtainMessage(code);
			if(total != 0){
				updateMessage.arg1=(int)(count/total*100);
			}
        	updateMessage.sendToTarget();        
		}
	}
	
	
}
