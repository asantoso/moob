package com.neusou.moobook.activity;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Random;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpConnection;

import com.neusou.moobook.FBConnectionException;
import com.neusou.moobook.R;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

public class ChatActivity extends BaseActivity{
	public static final String LOG_TAG = "ChatActivity";

	public static final String XTRA_UID = "xtra.uid";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat_activity);
		bindViews();
		initObjects();
		initViews();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		
	}
	@Override
	protected void onRestart() {
		super.onRestart();
		
	}
	@Override
	protected void onResume() {
		super.onResume();
		
		test();
		
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	
	}
	
	protected void bindViews(){
		console = (TextView) findViewById(R.id.console);
		test = (Button) findViewById(R.id.test);
	}
	
	protected void initObjects(){
		Intent intent = getIntent();
		uid = intent.getLongExtra(XTRA_UID, 0);
	}
	
	protected void initViews(){
		test.setOnClickListener(mTestOnClickLst);
	}
	
	View.OnClickListener mTestOnClickLst = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			test();
		}
		
	};
	
	long uid;
	Button test;
	TextView console;
	public static final String REQUEST_CONTENT_TYPE = "text/plain";
	public static final String REQUEST_USER_AGENT = "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.8) Gecko/2009033100 Ubuntu/9.04 (jaunty) Firefox/3.0.8";
	public static final String REQUEST_ACCEPT_LANGUAGE  = "en-us,en;q=0.5";
	public static final String REQUEST_ACCEPT  = "text/html, application/xhtml+xml";
	public static final String REQUEST_ACCEPT_CHARSET = "Accept-Charset	ISO-8859-1,utf-8;q=0.7,*;q=0.7";
	
	
	private String createEndpoint(long channel, long random, boolean unknown, String uid, long seq){
		String endpoint = "http://0.channel"+channel+".facebook.com/x/"+random+"/"+unknown+"/p_"+uid+"="+seq;
		return endpoint;
	}
	
	private String createEndpointHost(long channel, long random, boolean unknown, String uid, long seq){
		String endpoint = "0.channel"+channel+".facebook.com";
		return endpoint;
	}
	
	//http://0.channel65.facebook.com/x/1608118385/false/p_100000522301146=2
		
	private String createEndpointFile(long channel, long random, boolean unknown, String uid, long seq){
		String endpoint = "/x/"+random+"/"+unknown+"/p_"+uid+"="+seq;
		return endpoint;
	}
	
	class WaitForChatMessageRunnable implements Runnable{
		transient public boolean STOP_FLAG = false;
		
		public WaitForChatMessageRunnable() {

		}
		
		@Override
		public void run() {
			
			while(!STOP_FLAG){
				
			}			
			
		}
		
		
	};
	
	private void test(){		
		
		HttpURLConnection fbc;
    	String response = "";
    	
    	boolean isConnectionError = false;
    	String connectionErrorMessage = "";
    	String uidstr = Long.toString(uid);
    	boolean unknown = true;
    	long random  = new Random().nextInt();
    	long channel = 65;
    	long seq = 0;
    	
    	String endpoint = createEndpoint(channel, random, unknown, uidstr, seq);
    	String endpointHost = createEndpointHost(channel, random, unknown, uidstr, seq);
    	String endpointFile = createEndpointFile(channel, random, unknown, uidstr, seq);
    	
    	//endpoint = "http://www.google.com";
    	int mConnectionTimeout = 50000;
    	String REQUEST_METHOD = "GET";
    	
    	response="";
    	
    	
    	CookieManager cm = CookieManager.getInstance();
    	String cookies = cm.getCookie("facebook.com");
    	
    	Log.d(LOG_TAG,"cookie: "+cookies);
    	try {    		
    		HttpClientConnection httpClientConn;
    		    		
			URL url = new URL("http",endpointHost,80,endpointFile);
			Log.d(LOG_TAG,"url to connect:"+url.toString());
			fbc = (HttpURLConnection) url.openConnection();		
			fbc.setUseCaches(true);			
			fbc.setConnectTimeout(mConnectionTimeout);
			fbc.setRequestMethod(REQUEST_METHOD);
			fbc.setRequestProperty("Content-type", REQUEST_CONTENT_TYPE);			
			fbc.setRequestProperty("User-Agent",REQUEST_USER_AGENT);
			fbc.setRequestProperty("Accept-Language", REQUEST_ACCEPT_LANGUAGE);
			fbc.setRequestProperty("Accept", REQUEST_ACCEPT);
			fbc.setRequestProperty("Accept-Charset", REQUEST_ACCEPT_CHARSET);
			fbc.setRequestProperty("Cookie",cookies);
			
			//DataOutputStream dos = new DataOutputStream(fbc.getOutputStream());
			//dos.writeBytes(response);
			//dos.flush();
			//dos.close();
			
			
			//fbc.connect();
			//fbc.getContent();
				
			/*
			try{
				Object obj = fbc.getContent();
				Log.d(LOG_TAG,"content class: " + obj.getClass().getCanonicalName());			
			}
			
			catch(Exception e){
				
			}
			*/
			
			//String responseMsg = fbc.getResponseMessage();
			//Log.d(LOG_TAG,"response message:"+responseMsg);
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
			
			Log.d(LOG_TAG,"TEST_DONE");
										
		} catch (MalformedURLException e) {			
			e.printStackTrace();
			isConnectionError = true;
			connectionErrorMessage = "MalformedURLException: "+e.getMessage();
		} catch (ProtocolException e) {
			e.printStackTrace();
			isConnectionError = true;
			connectionErrorMessage = "ProtocolException: "+e.getMessage();
		} catch (UnknownHostException e){
			e.printStackTrace();
			isConnectionError = true;
			connectionErrorMessage = "UnknownHostException: "+e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			isConnectionError = true;
			connectionErrorMessage = "IOException: "+e.getMessage();
		}
		
		
	
	}
}