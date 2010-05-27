/*
package com.neusou.moobook;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.LinearGradient;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class FBApi{
	
	public static final String LOG_TAG = "FBApi";
	
	public static final String api_rest_endpoint = "http://api.new.facebook.com/restserver.php";
	public static final String login_endpoint = "https://www.facebook.com/login.php";
	
	public static final String wsmethod_stream_addComment = "stream.addComment";
	public static final String wsmethod_stream_removeComment = "stream.removeComment";
	public static final String wsmethod_comments_add = "comments.add";
	public static final String wsmethod_auth_createtoken = "auth.createToken";
	public static final String wsmethod_users_getLoggedInUser = "users.getLoggedInUser";
	public static final String wsmethod_friends_get = "friends.get";
	public static final String wsmethod_notifications_get = "notifications.get";
	public static final String wsmethod_users_getInfo = "users.getInfo";
	public static final String wsmethod_message_getThreadsInFolder = "message.getThreadsInFolder";
	public static final String wsmethod_stream_get = "stream.get";	
	public static final String wsmethod_fql_query = "fql.query";
	public static final String wsmethod_events_rsvp = "events.rsvp";
	
	public static final String param_fql_query = "query";
    public static final String param_method = "method";
	public static final String param_call_id = "call_id";	
	public static final String param_api_id = "api_id"; //TODO: Seems to have different effects on certain api methods
	public static final String param_api_key = "api_key";
	public static final String param_session_key = "session_key";
	public static final String param_sig = "sig";
	public static final String param_format = "format";
	public static final String param_api_version = "v";
	public static final String param_uids = "uids";
	public static final String param_fields = "fields";
	public static final String param_folder_id = "folder_id";
	public static final String param_limit = "limit";
	public static final String param_offset = "offset";
	public static final String param_comment = "comment";
	public static final String param_post_id = "post_id";
	public static final String param_eid = "eid";
	public static final String param_rsvp_status = "rsvp_status";
			
	public static final String REQUEST_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8";
	public static final String REQUEST_USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.8) Gecko/2009033100 Ubuntu/9.04 (jaunty) Firefox/3.0.8";
	public static final String REQUEST_ACCEPT_LANGUAGE  = "en-us,en;q=0.5";
	public static final String REQUEST_METHOD  = "POST";
	
	public static final String RESPONSE_FORMAT_JSON  = "JSON";
	public static final String RESPONSE_FORMAT_XML  = "XML";
	
	public static final String XTRA_RESPONSE = "xtra.response";
	public static final String XTRA_RESPONSEFORMAT = "xtra.response.format";
	public static final String XTRA_RESPONSE_ERROR_CODE = "xtra.response.error_code";
	public static final String XTRA_WEBSERVICEMETHOD = "xtra.ws.method";
	public static final String XTRA_CALLER = "xtra.caller";
	public static final String XTRA_INCLUDEREQSIG = "xtra.include.request.sig";
	public static final String XTRA_RESPONSE_BYTELENGTH = "xtra.response.bytelength";

	public static final String FQL_GET_VISIBLE_USERCONNECTION_STREAMS 
	= "SELECT post_id, action_links, attachment, actor_id, target_id, attribution, comments, message FROM stream WHERE source_id in (SELECT target_id FROM connection WHERE source_id=${source_id}) AND is_hidden = 0";
	
	public static final String FQL_GET_USER_STREAM_APPLICATION
	= "SELECT post_id, actor_id, target_id, message, attachment, comments, attribution FROM stream WHERE filter_key in (SELECT filter_key FROM stream_filter WHERE uid = ${uid} AND type = 'application')";
	
	//See the names of all the events that your friends have been invited to
	public static final String FQL_GET_EVENTS_FRIENDS_INVITED 
	= "SELECT name FROM event WHERE eid IN (SELECT eid from event_member WHERE uid IN (SELECT uid2 FROM friend WHERE uid1=''${user_id}'') )";
	
	// Get all groups a user is part of
	public static final String FQL_GET_MY_GROUPS 
	= "SELECT name FROM group WHERE gid IN (SELECT gid FROM group_member WHERE uid = ''${uid}'')";
	
	// Get uids of friends of $app_user with birthdays today (where $today is formatted like December 18)
	public static final String FQL_GET_BIRTHDAY
	= "SELECT uid FROM user WHERE strpos(birthday, '${today}') = 0 AND uid IN (SELECT uid2 FROM friend WHERE uid1 = $app_user)";
	
	// Get all albums owned by a user
	public static final String FQL_GET_USER_ALBUMS 
	= "SELECT pid FROM photo WHERE aid IN ( SELECT aid FROM album WHERE owner=''${user_id}'' ) ORDER BY created DESC LIMIT 1,42";
	
	
	//public Handler h;
	
	public FBApi(){
		
	}
	
	public static String computeRequestSig(TreeMap<String,String> tmap, String session_secret){
		//TreeMap<String,String> tmap = new TreeMap<String,String>(map);
		Iterator<String> it = tmap.keySet().iterator();
		String ckvp = "";
					
		while(it.hasNext()){
			String key = it.next();
			//Log.d(LOG_TAG,key);
			ckvp+=key+"="+tmap.get(key);
		}
		
		ckvp += session_secret;  //concatenated key value pair k=v without a space between pairs
		Log.d(LOG_TAG,"string to be hashed: "+ckvp);
		
		String computedSig = "";
		try {
			final String digestAlgorithm = "MD5"; 
			MessageDigest m;
			m = MessageDigest.getInstance(digestAlgorithm);
			m.update(ckvp.getBytes(),0,ckvp.length());
			computedSig = new BigInteger(1,m.digest()).toString(16);
			Log.d(LOG_TAG,"MD5: "+ computedSig);
			return computedSig;
		} catch (NoSuchAlgorithmException e) {
			Log.e(LOG_TAG,"NoSuchAlgorithm "+e.getMessage());
			e.printStackTrace();
			return null;
		}			
	}
		
    public Bundle sendPost(String endpoint, TreeMap<String,String> requestParams,FBSession fbsession, boolean includeRequestSignature, Bundle xtra){    	
    	HttpURLConnection fbc;
    	String response = "";
		try {
			URL url = new URL(endpoint);
			
			
			fbc = (HttpURLConnection) url.openConnection();
			
			fbc.setUseCaches(false);
			fbc.setDefaultUseCaches(false);		
			
			fbc.setDoInput(true);
			fbc.setDoOutput(true);
			fbc.setConnectTimeout(5000);
			fbc.setRequestMethod(REQUEST_METHOD);
			fbc.setRequestProperty("Content-type", REQUEST_CONTENT_TYPE);			
			fbc.setRequestProperty("User-Agent",REQUEST_USER_AGENT);
			fbc.setRequestProperty("Accept-Language", REQUEST_ACCEPT_LANGUAGE);
			//fbc.setRequestProperty("Accept-Encoding", "gzip,deflate");
					
			if(includeRequestSignature){
				String requestSignature = FBApi.computeRequestSig(requestParams, fbsession.secret);
				if(requestSignature == null){ //can't compute signature?
					return null;
				}else{				
					requestParams.put(param_sig, requestSignature);
				}
			}
			
			String content = generateRequestContent(requestParams);
			
			//String content = StrSubstitutor.replace(template,requestParams);
			
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
				Log.d(LOG_TAG,linecount+":"+line);
			}
										
		} catch (MalformedURLException e) {			
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		int numBytes = response.getBytes().length;
		
		Bundle bundle = new Bundle(); 
		bundle.putString(XTRA_RESPONSE, response);
		bundle.putInt(XTRA_RESPONSE_BYTELENGTH, numBytes);
		
		Log.d(LOG_TAG,"response "+response);
		Log.d(LOG_TAG,"response's total bytes: "+numBytes);
		Log.d(LOG_TAG,"postSend finished.");
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
		return content;
	}
    
}
*/
