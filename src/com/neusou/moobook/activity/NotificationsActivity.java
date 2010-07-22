package com.neusou.moobook.activity;

import java.util.concurrent.CountDownLatch;

import com.admob.android.ads.AdView;
import com.neusou.DecoupledHandlerThread;
import com.neusou.ProactiveThread;
import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;
import com.neusou.moobook.adapters.AdapterDataSetObserver;
import com.neusou.moobook.controller.CursorListAdapter;
import com.neusou.moobook.controller.NotificationsListViewFactory;
import com.neusou.moobook.controller.StandardImageAsyncLoadListener;
import com.neusou.moobook.controller.StandardUiHandler;
import com.neusou.moobook.data.FBNotification;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

public class NotificationsActivity extends BaseActivity{
	public static final String LOG_TAG = "NotificationsActivity";

	DecoupledHandlerThread mHT = new DecoupledHandlerThread();
	Handler mUIHandler;
	ProgressDialog mProgressDialog;
	ManagerThread mWorkerThread;
	
	public static Intent getIntent(Context ctx) {		
		return new Intent(ctx, NotificationsActivity.class);
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.notifications_activity);
		getWindow().setBackgroundDrawableResource(R.drawable.metal);
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
	protected void onPause() {		
		c.deactivate();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		App.INSTANCE.stopNotificationRingtone();
		resolveCursor();
		updateNotifications();		
	}
	
	@Override
	protected void onStop() {	
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {		
		removeBroadcastReceivers();
		super.onDestroy();	
	}
	
	protected void bindViews(){		
		mProgressDialog = new ProgressDialog(this);
		mLoadingIndicator = findViewById(R.id.loadingindicator);
		mTopHeaderText = (TextSwitcher) findViewById(R.id.topheader);
		mListView = (ListView) findViewById(R.id.list);
		mAdView = (AdView) findViewById(R.id.ad);
		mAdViewWrapper = findViewById(R.id.adview_wrapper);
		mBottomHeader = findViewById(R.id.bottomheader);
		
	}
	
	protected void initBroadcastReceivers(){		
		mBroadcastReceiver = new BroadcastReceiver() {			
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				Logger.l(Logger.DEBUG, LOG_TAG, "[onReceive()] action: "+action);				
				if(action.equals(App.INTENT_NEW_NOTIFICATIONS)){
					resolveCursor();
					updateNotifications();
				}				
			}
		};
		registerReceiver(mBroadcastReceiver, new IntentFilter(App.INTENT_NEW_NOTIFICATIONS));		 
	}
	
	protected void removeBroadcastReceivers(){
		unregisterReceiver(mBroadcastReceiver);
	}	
	
	protected void initObjects(){
		mHT.start();
				
		mWorkerThread = App.INSTANCE.mManagerThread;
		mWorkerThread.setOutHandler(mUIHandler);
		
		mFacebook = Facebook.getInstance();			
		mFacebook.registerOutHandler(R.id.outhandler_activity_notifications, mWorkerThread.getInHandler());
		
		mListViewFactory = new NotificationsListViewFactory(this, mListView);
		mListAdapter = new CursorListAdapter(this, mListViewFactory, FBNotification.col_rowid);
		mTopHeaderText.setFactory(new ViewFactory() {
			@Override
			public View makeView() {
				
				TextView t = new TextView(NotificationsActivity.this);
				t = (TextView) mLayoutInflater.inflate(
						R.layout.t_topheadertext, null);
				return t;
			}
		});
				
		mListViewFactory.setDataSetObserver(new AdapterDataSetObserver(mListAdapter));
		mUIHandler = new StandardUiHandler(this, null, mAdViewWrapper, mBottomHeader){
			@Override
			public void handleMessage(Message msg) {			
				super.handleMessage(msg);
				int code = msg.what;
				switch (code) {
					case ManagerThread.CALLBACK_GET_PHOTO_ATTR:{
						
						break;
					}
				}
			}
		};
		
		initBroadcastReceivers();
	}
	
	private void resolveCursor(){
		if(c == null){
			c = App.INSTANCE.mDBHelper.getAllNotifications(App.INSTANCE.mDB, null);
		}else{
			c.requery();
		}
	}
	
	public void showComments(Bundle data){
		byte objectType = data.getByte(Facebook.XTRA_FBURL_OBJECTTYPE);
		
		switch(objectType){
			case Facebook.FBURL_OBJECTTYPE_STREAM:{
				String oid = data.getString(Facebook.XTRA_FBURL_OBJECTID);
				App.showPost(NotificationsActivity.this, oid);
				break;
			}
			case Facebook.FBURL_OBJECTTYPE_PHOTO:{
				String pid = data.getString(Facebook.XTRA_FBURL_PHOTOID);
				String oid = data.getString(Facebook.XTRA_FBURL_USERID);
				showPhotoComments(pid, Long.parseLong(oid));
				break;
			}
		}
	}
	
	CountDownLatch mPhotoCommentsLatch;
	public void showPhotoComments(String pid_str, long owner){
		long pid = Long.parseLong(pid_str);
		mPhotoCommentsLatch = new CountDownLatch(1);
		mFacebook.getPhotoAttributes(R.id.outhandler_activity_notifications, null, pid, owner, ManagerThread.CALLBACK_GET_PHOTO_ATTR, BaseManagerThread.CALLBACK_SERVERCALL_ERROR, BaseManagerThread.CALLBACK_TIMEOUT_ERROR,  3000);
		
		mHT.h.post(new Runnable() {
			@Override
			public void run() {
				try{
					mPhotoCommentsLatch.await();
				}catch(InterruptedException e){			
				}
				
			}
		});
	}
	
	protected void initViews(){
		
		
		mListView.setFocusableInTouchMode(true);
		mListView.setFocusable(true);
		mListView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
		mListView.setDrawingCacheBackgroundColor(Color.TRANSPARENT);		
		mListView.setAnimationCacheEnabled(true);
		mListView.setAlwaysDrawnWithCacheEnabled(true);
		mListView.setAdapter(mListAdapter);
		mListView.setBackgroundColor(Color.TRANSPARENT);
		mListView.setCacheColorHint(Color.TRANSPARENT);
		mListView.setSmoothScrollbarEnabled(true);
		mListView.setDrawSelectorOnTop(false);
		mListView.setRecyclerListener(mListViewFactory);
		
		Drawable stateListDrawable = mListView.getSelector();
		stateListDrawable.setColorFilter(App.mColorFilterBlueish);			
		
		try {
			mListView.setDivider(
					mResources
					.getDrawable(android.R.drawable.divider_horizontal_bright));			
		} catch (Exception e) {

		}
		
		mListView.setOnItemClickListener(
				new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> adapter, View v,
							int position, long arg3) {
						c.moveToPosition(position);
						FBNotification note = FBNotification.parseCursor(c, null);
						Logger.l(Logger.DEBUG, LOG_TAG, note.href);
						//Toast.makeText(NotificationsActivity.this, note.href, 3000).show();
						Bundle data = Facebook.extractDataFromFacebookUrl(note.href);
						if(data != null){
							//String scriptName = data.getString(Facebook.XTRA_FBURL_SCRIPTNAME);
							//byte objectType = data.getByte(Facebook.XTRA_FBURL_OBJECTTYPE);
							//String genericId = data.getString(Facebook.XTRA_FBURL_OBJECTID);
							//String version = data.getString(Facebook.XTRA_FBURL_VERSION);
							//String story_id = data.getString(Facebook.XTRA_FBURL_STORYID);
							//String user_id = data.getString(Facebook.XTRA_FBURL_USERID);
							
							showComments(data);
							/*
							if(version.equals(Facebook.FBURL_VERSION_WALL) && 
								scriptName.equals(Facebook.FBURL_SCRIPT_PROFILE)){
								App.showPost(NotificationsActivity.this, user_id+"_"+story_id);
							}else if(scriptName.equals(Facebook.FBURL_SCRIPT_VIDEO)){
								App.showPost(NotificationsActivity.this, version);
							}
							*/
						}
					}			
				}
		);
				
		App.initAdMob(mAdView, mUIHandler);
	}	

	private void showLoadingIndicator(){	
		mLoadingIndicator.setVisibility(View.VISIBLE);
		setProgressBarIndeterminateVisibility(true);
	}
		
	private void hideLoadingIndicator(){
		mLoadingIndicator.setVisibility(View.INVISIBLE);
		setProgressBarIndeterminateVisibility(false);
	}
		
	private void updateNotifications(){
		mListAdapter.setData(c);
		mListAdapter.notifyDataSetChanged();
		int numNotifications = c.getCount();
		mTopHeaderText.setText("Notifications ("+numNotifications+")");
	}
	
	View mLoadingIndicator;
	CursorListAdapter mListAdapter;
	NotificationsListViewFactory mListViewFactory; 
	Cursor c;
	Facebook mFacebook;
	ListView mListView;
	AdView mAdView;
	View mAdViewWrapper;
	View mBottomHeader;
	TextSwitcher mTopHeaderText;
	BroadcastReceiver mBroadcastReceiver;
}