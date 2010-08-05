/*
  package com.neusou.moobook.activity;
 

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.FQL;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.data.Stream;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class FriendListsActivity extends BaseActivity {
	public static final String LOG_TAG = "FriendListsActivity";

	public static Intent getIntent(Context ctx) {
	
		return new Intent(ctx, FriendListsActivity.class);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(layoutResID);
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
		
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
	
	}
	
	protected void bindViews(){
		
	}
	
	protected void initObjects(){	
		fb =  Facebook.getInstance();
		fb.setOutHandler(mWorkerThread.h);		
		mProgressDialog = new ProgressDialog(this);	
	}
	
	protected void initViews(){
		
	}
	
	Facebook fb;
	static WorkerManagerThread mWorkerThread;
	ProgressDialog mProgressDialog;
	static final int UIMETHOD_RELOADLIST = 2;
	static final int CALLBACK_GET_FRIENDLISTS = 998;
	static final int CALLBACK_SERVERCALL_ERROR = 999;
	static final int CALLBACK_TIMEOUT_ERROR = 9991;
	static final int CALLBACK_PROCESS_WSRESPONSE_HAS_ERRORCODE = 130;
	
	
	Handler mUIHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			//Log.d(LOG_TAG,"ui hander handle message what:"+msg.what);
			int code = msg.what;
			Bundle data = msg.getData();
		
			switch(code){
								
				case CALLBACK_TIMEOUT_ERROR:{
					Toast.makeText(FriendListsActivity.this, "Request to Facebook timed out", 2000).show();
					//onFinishUpdatingComments();
					mProgressDialog.dismiss();
					break;
				}
				
				case CALLBACK_SERVERCALL_ERROR:{					
					String reason = (String)data.getString(Facebook.XTRA_SERVERCALL_ERROR_MSG);
					int errorCode = data.getInt(Facebook.XTRA_SERVERCALL_ERROR_CODE);
					Toast.makeText(FriendListsActivity.this, errorCode+":"+reason, 2000).show();			
					//onFinishUpdatingComments();
					mProgressDialog.dismiss();
					break;
				}
				
				case CALLBACK_PROCESS_WSRESPONSE_HAS_ERRORCODE:{
					FBWSResponse fbResponse = (FBWSResponse) msg.obj;					
					Toast.makeText(FriendListsActivity.this,fbResponse.errorMessage, 1000).show();
					//onFinishUpdatingComments();
					mProgressDialog.dismiss();
					break;
				}			
				
				case UIMETHOD_RELOADLIST:{
					
					break;
				}
					
			}
		};
	};	
	
	class WorkerManagerThread extends Thread {
		public Handler h = new Handler();
		public void run() {
			Looper.prepare();
			try{
				android.os.Process.setThreadPriority((int)Process.myTid(), Process.THREAD_PRIORITY_DEFAULT);
			}catch(Exception e){				
			}
			h = new Handler(){
				final private String name = "CallbackHandler";
				public void handleMessage(android.os.Message msg) {
					mProgressDialog.dismiss();
					int code = msg.what;
					//Log.d(LOG_TAG, name + " handleMessage msg.what:" + msg.what);					
					Bundle data = msg.getData();
					
					if(code == CALLBACK_TIMEOUT_ERROR){
						
						Message msgError =  mUIHandler.obtainMessage(CALLBACK_TIMEOUT_ERROR);
						msgError.setData(data);
						msgError.sendToTarget();
						return;
					}
					
					// check if connection to server was successful
					int servercallstatus = data.getInt(Facebook.XTRA_SERVERCALL_STATUS_CODE);
					
					if(servercallstatus == Facebook.SERVERCALL_ERROR){
						Message msgError =  mUIHandler.obtainMessage(CALLBACK_SERVERCALL_ERROR);
						msgError.setData(data);
						msgError.sendToTarget();
						return;
					}
										
					String response = data.getString(Facebook.XTRA_RESPONSE);
					int bytelength = data.getInt(Facebook.XTRA_RESPONSE_BYTELENGTH);	

					FBWSResponse fbresponse = FBWSResponse.parse(response);
					
					if(fbresponse.hasErrorCode){						
						Message m = mUIHandler.obtainMessage(CALLBACK_PROCESS_WSRESPONSE_HAS_ERRORCODE);
						m.obj = fbresponse;
						m.sendToTarget();
						return;
					}
					
					switch(code){
						
						case CALLBACK_GET_FRIENDLISTS:{
							mProgressDialog.dismiss();
							//String comment_id = fbresponse.data;					
							//Log.d(LOG_TAG,"comment_id : "+comment_id);		
							Message msgError =  mUIHandler.obtainMessage(UIMETHOD_RELOADLIST);
							msgError.setData(data);
							msgError.sendToTarget();							
							break;
						}
											
						
					}
				};
			};
			
			try{
				android.os.Process.setThreadPriority((int)Process.myTid(), Process.THREAD_PRIORITY_DEFAULT);
			}catch(Exception e){				
			}
			
			Looper.loop();	
		};
		
	};
	
}
*/