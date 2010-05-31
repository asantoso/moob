package com.neusou.moobook.activity;

//master

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import com.admob.android.ads.AdView;
import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.FBPhotoUploadTask;
import com.neusou.moobook.FBSession;
import com.neusou.moobook.FBUser;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.OptionsMenu;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.adapters.CommentsAdapter;
import com.neusou.moobook.data.PageableJsonData;
import com.neusou.moobook.data.User;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;
import com.neusou.web.ImageUrlLoader2;
import com.neusou.web.PagingInfo;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderProgress;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderResult;

public class HomeActivity extends BaseActivity {
	
	public static final String LOG_TAG = "HomeActivity";

	public static final int REQUEST_PICKPHOTO = 4;
	
	//Button mStatusUpdateBtn;
	//View.OnClickListener mStatusUpdateOnClick;
	View.OnClickListener mStatusBoxOnClick;
	
	Button mPostPictureBtn;
	View.OnClickListener mPostPictureOnClick;
	
	Button mPostVideoBtn;
	View.OnClickListener mPostVideoOnClick;
	
	Button mPostLinkBtn;
	View.OnClickListener mPostLinkOnClick;
	
	Button mViewStreamsBtn;
	View.OnClickListener mViewStreamsOnClick;
	
	Button mViewFriendsBtn;
	View.OnClickListener mViewFriendsOnClick;
	
	Button mViewNotificationsBtn;
	View.OnClickListener mViewNotificationsOnClick;
	
	/*
	Button mGetPhotosBtn;
	View.OnClickListener mGetPhotosOnClick;
		*/
	Button mViewEventsBtn;
	View.OnClickListener mViewEventsOnClick;
	
	Button mViewAllUserPhotosBtn;
	View.OnClickListener mViewAllUserPhotosOnClick;
	
	Button mViewTaggedPhotosBtn;
	View.OnClickListener mViewTaggedPhotosOnClick;	
	
	Button mTestBtn;
	View.OnClickListener mTestOnClick;
		
	Button mNetworksBtn;
	View.OnClickListener mNetworksOnClick;
	
	TextSwitcher mTopHeader;
	ViewFactory mTopHeaderViewFactory;
		
	Facebook mFacebook;
	ListView mListView;
	
	static ManagerThread mWorkerThread;
	AdView mAdView;
	
	CountDownLatch mThreadsInitCountdown; 
	static int mNumThreadsInitializations = 1;
	
	ManagerThread.IManagerResult mManagerListener;
	
	boolean mIsOrientationLocked;

	IntentFilter mIntentFilter;
	
	BroadcastReceiver mIntentReceiver;

	//ArrayList<Map<String,?>> data;
	ImageView mProfileImage;
	TextView mStatusBox; 
	Notification mUploadPhotoNotification;
	NotificationManager mNotificationManager;
	
	
	//temporary variables
	String firstname;
	String lastname;
	String pic;
	String pic_big;
	JSONObject statusJson;
	String status;
	String profileblurb;
	String timezone;
	String current_location;
	
	static final byte DIALOG_STATUSUPDATE = 0;
	static final byte DIALOG_CONTACTS = 1;
	static final byte DIALOG_EVENTS = 2;
	
	
	BroadcastReceiver mCommentsUpdatedReceiver;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		getWindow().setBackgroundDrawableResource(R.drawable.metal);
		setContentView(R.layout.home_activity);
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
		
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);		  
	
	}

	@Override
	protected void onRestart() {
		super.onRestart();

	}

	@Override
	protected void onPause() {	
		super.onPause();
		unregisterReceiver(mIntentReceiver);
	}
	
	@Override
	protected void onResume() {
		super.onResume();	
		App.INSTANCE.getSessionUserInfo();
		registerReceiver(mIntentReceiver, mIntentFilter);
		showSessionUserData();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(!onResume){ //strange activity lifecycle. onDestroy is called immediately after onResume() when the previous starting activity started this activity inside the onResume code.
			return;
		}		
		
	}

	
	protected void bindViews() {
		
		
		//mStatusUpdateBtn = (Button) findViewById(R.id.poststatus);
		mProfileImage = (ImageView) findViewById(R.id.profilepic);
		mStatusBox = (TextView) findViewById(R.id.message); 
		mPostPictureBtn = (Button) findViewById(R.id.post);
		mNetworksBtn =  (Button) findViewById(R.id.network);
		//mPostVideoBtn = (Button) findViewById(R.id.postvideo);
		//mPostLinkBtn = (Button) findViewById(R.id.postlink);
		mTopHeader = (TextSwitcher) findViewById(R.id.topheader);
		mViewStreamsBtn = (Button) findViewById(R.id.streams);
		//mViewFriendsBtn = (Button) findViewById(R.id.viewfriends);
		//mViewEventsBtn = (Button) findViewById(R.id.viewevents);
		mViewNotificationsBtn = (Button) findViewById(R.id.notifs);
		mViewAllUserPhotosBtn = (Button) findViewById(R.id.photos);
		//mViewTaggedPhotosBtn = (Button) findViewById(R.id.viewtaggedphotos);
		mTestBtn = (Button) findViewById(R.id.test);
		mListView = (ListView) findViewById(R.id.list);
		mAdView = (AdView) findViewById(R.id.ad);
	}

	protected void initViews() {
		mTopHeader.setFactory(mTopHeaderViewFactory);
		mTopHeader.setText("Dashboard");		
	
		setOnClick(mNetworksBtn,mNetworksOnClick); 
		setOnClick(mStatusBox,mStatusBoxOnClick); 
		setOnClick(mPostPictureBtn, mPostPictureOnClick);
		setOnClick(mPostVideoBtn, mPostVideoOnClick);
		setOnClick(mViewStreamsBtn, mViewStreamsOnClick);
		setOnClick(mViewEventsBtn, mViewEventsOnClick);
		setOnClick(mViewNotificationsBtn, mViewNotificationsOnClick);	
		
	}
	
	Cursor mCommentsCursor;
	Cursor mStreamsCursor;

	protected void initObjects() {
		
		//data = new ArrayList<Map<String,?>>();
		
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(App.INTENT_STREAMCOMMENTS_UPDATED);
		mIntentFilter.addAction(App.INTENT_PROFILE_UPDATED);
		mIntentFilter.addAction(App.INTENT_WALLPOSTS_UPDATED);
		
		mThreadsInitCountdown = new CountDownLatch(mNumThreadsInitializations);
		mWorkerThread = new ManagerThread(mThreadsInitCountdown);
		mWorkerThread.start();
		
		try{
			mThreadsInitCountdown.await();
		}catch(InterruptedException e){
			
		}
		mWorkerThread.setOutHandler(mUIHandler);
		
		mIntentReceiver = new BroadcastReceiver() {			
			@Override
			public void onReceive(Context context, Intent intent) {
				Logger.l(Logger.DEBUG,LOG_TAG,"[BroadcastReceiver] [onReceive()] action: "+intent.getAction());
				String action = intent.getAction();
				Bundle data = intent.getExtras();
				
				if(action.equals(App.INTENT_PROFILE_UPDATED)){
					FBWSResponse response = (FBWSResponse) intent.getExtras().getParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT);
					Util.writeToLocalCache(App.INSTANCE,response.data,App.LOCALCACHEFILE_SESSIONUSERDATA);
					showSessionUserData();
				}
				
				else if(action.equals(App.INTENT_STREAMCOMMENTS_UPDATED)){					
					
					
					//PagingInfo pi = null;
					FBWSResponse response = null;
				
					/*
					if(data.containsKey(PagingInfo.XTRA_OBJECT)){
						pi = (PagingInfo) (data.getParcelable(PagingInfo.XTRA_OBJECT));
					}
					*/
					
					if(data.containsKey(FBWSResponse.XTRA_PARCELABLE_OBJECT)){
						response = (FBWSResponse) (data.getParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT));
					}
					
					if(response != null && !response.hasErrorCode){
						int numComments = response.jsonArray.length();
						Logger.l(Logger.DEBUG,LOG_TAG,"numComments:"+numComments);	
					}
				}
				
				else if(action.equals(App.INTENT_WALLPOSTS_UPDATED)){
					FBSession session = mFacebook.getSession();
					mStreamsCursor = App.INSTANCE.mDBHelper.getWallPosts(App.INSTANCE.mDB, session.uid, 1, 0);					
					int numPosts = mStreamsCursor.getCount();				
					Logger.l(Logger.DEBUG,LOG_TAG,"numPosts:"+numPosts);					
					
				}
				
			}
		};
				
		mFacebook = Facebook.getInstance(); 
		
		///
		
		mTopHeaderViewFactory = new ViewFactory() {
			@Override
			public View makeView() {
				TextView t = new TextView(HomeActivity.this);
				t = (TextView) mLayoutInflater.inflate(
						R.layout.t_topheadertext, null);
				return t;
			}
		};
		
		/*
		mStatusUpdateOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				createDialog(DIALOG_STATUSUPDATE).show();
			}
		};
		*/
		mNetworksOnClick = new OnClickListener(){
			@Override
			public void onClick(View v) {
				Logger.l(Logger.DEBUG,LOG_TAG,"onNetworksOnClick");				
				Intent i = new Intent(HomeActivity.this, ViewContactsActivity.class);
	            startActivity(i); 
			}
		};
		
		mPostPictureOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Logger.l(Logger.DEBUG,LOG_TAG,"onPostPicture");				
				Intent i = new Intent(Intent.ACTION_GET_CONTENT);
	             i.setType("image/*");
	            startActivityForResult(i, REQUEST_PICKPHOTO); 
			}
		};
		
		mPostVideoOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Logger.l(Logger.DEBUG,LOG_TAG,"onPostVideo");		
			}
		};
		
		mViewStreamsOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent viewStreamsIntent = new Intent(HomeActivity.this,StreamActivity.class);
				viewStreamsIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
				startActivity(viewStreamsIntent);
			}
		};
		
		mViewFriendsOnClick= new OnClickListener() {
			@Override
			public void onClick(View v) {
				createDialog(DIALOG_CONTACTS).show();
			}
		};
		
		mViewEventsOnClick= new OnClickListener() {
			@Override
			public void onClick(View v) {
				createDialog(DIALOG_EVENTS).show();
			}
		};
		
		mViewNotificationsOnClick= new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent viewNotificationsIntent = new Intent(HomeActivity.this,NotificationsActivity.class);
				viewNotificationsIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
				startActivity(viewNotificationsIntent);
			}
		};	
		
		mViewAllUserPhotosOnClick= new OnClickListener() {
			@Override
			public void onClick(View v) {
				Logger.l(Logger.DEBUG,LOG_TAG,"onPostVideo");		
				Intent viewAlbumsIntent = new Intent(HomeActivity.this, ViewAlbumsActivity.class);
				viewAlbumsIntent.putExtra(ViewAlbumsActivity.XTRA_FACEBOOKUSERID, mFacebook.getSession().uid);
				viewAlbumsIntent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
				startActivity(viewAlbumsIntent);
			}
		};
		
		mTestOnClick= new OnClickListener() {
			@Override
			public void onClick(View v) {
				//Intent test = new Intent(HomeActivity.this, TestGallery.class);
				//test.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
				//startActivity(test);
				//App.INSTANCE.postPhotoToFacebook(null);
				App.INSTANCE.test();
			}
		};
	
		mStatusBoxOnClick = new OnClickListener() {			
			@Override
			public void onClick(View v) {
				
				
			}
		};
		
		mManagerListener = new ManagerThread.IManagerResult() {
			
			@Override
			public void setUsersData(JSONArray data) {
				data_users = data;
			}
			
			@Override
			public void setPagingInfoData(PagingInfo data) {
				
			}
			
			@Override
			public void setCommentsInfoData(JSONArray data) {
				data_comments_info = data;				
			}
			
			@Override
			public void setCommentsData(JSONArray data) {				
				data_comments.set(data);	
			}
			
			@Override
			public boolean hasPostId() {

				return false;
			}
			
			@Override
			public boolean hasObjectId() {

				return false;
			}
			
			@Override
			public String getPostId() {

				return null;
			}
			
			@Override
			public PagingInfo getPagingInfoData() {

				return null;
			}
			
			@Override
			public String getObjectId() {

				return null;
			}
		};
	}

	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){
			case REQUEST_PICKPHOTO:{
				 if (resultCode == RESULT_OK) {                     
	                 try {
						AssetFileDescriptor thePhoto = getContentResolver().openAssetFileDescriptor(data.getData(), "r");
						
						InputStream photoStream = thePhoto.createInputStream();
						Bitmap image = BitmapFactory.decodeStream(photoStream);
						int imageWidth = image.getWidth();
						int imageHeight = image.getHeight();						
						Toast.makeText(this, imageWidth+", "+imageHeight, 2000).show();
						Bitmap scaled = Bitmap.createScaledBitmap(image,400,300,true);
						File cacheDir = getCacheDir();
						File tmp = new File(cacheDir.getPath()+"/photo");
						tmp.createNewFile();
						FileOutputStream fos = new FileOutputStream(tmp);
						boolean successCompress = scaled.compress(CompressFormat.PNG,100,fos);
						
						Logger.l(Logger.DEBUG, LOG_TAG, "success compress? "+successCompress);
						
						FileInputStream fis = new FileInputStream(tmp);
						
						final InputStream uploadStream = fis;// thePhoto.createInputStream();
						final long length = tmp.length();
						Bitmap preview = Bitmap.createScaledBitmap(image, 32, 32, true);
						mAsyncPhotoUploader = new FBPhotoUploadTask(this, uploadStream, length, preview );
						mAsyncPhotoUploader.execute(null);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch(IOException e){
						e.printStackTrace();
					}
	             }
	             if (resultCode == RESULT_CANCELED) {
	             
	             }
				break;
			}
		}
		
		Logger.l(Logger.DEBUG,LOG_TAG,"onActivityResult() done");
	}

	private void setOnClick(View v, View.OnClickListener l){
		if(v!=null){
			v.setOnClickListener(l);	
		}
	}

	
	private Handler createUploadPhotoStatusListener(){
		return new Handler(){
			@Override
			public void handleMessage(Message msg) {		
				Logger.l(Logger.DEBUG, LOG_TAG, "[UploadPhotoListener] [handlerMessage()] ");
				super.handleMessage(msg);
				int code = msg.what;
				switch(code){
				case Facebook.MESSAGE_UPDATE_UPLOAD_COMMENCING:{
						mUploadPhotoNotification = new Notification(android.R.drawable.ic_btn_speak_now,"uploading photos..",SystemClock.elapsedRealtime());
		                mUploadPhotoNotification.flags = Notification.FLAG_ONGOING_EVENT;
		                mUploadPhotoNotification.contentView =  new RemoteViews(getPackageName(),R.layout.remote_photo_upload);
		                Toast.makeText(HomeActivity.this,"uploading photo..", 2000).show();
		               
		                mNotificationManager.notify(R.id.notification_uploadphoto, mUploadPhotoNotification);
						break;
					}
				case Facebook.MESSAGE_UPDATE_UPLOAD_PROGRESS:{
						mUploadPhotoNotification.contentView.setTextViewText(R.id.status,"uploading photo to facebook..");
						mUploadPhotoNotification.contentView.setInt(R.id.progress, "setProgress", msg.arg1);
						break;
					}
				case Facebook.MESSAGE_UPDATE_UPLOAD_FINISHED:{
						mUploadPhotoNotification.contentView.setInt(R.id.progress, "setProgress", msg.arg1);
						mNotificationManager.cancel(R.id.notification_uploadphoto);	
						postDelayed(new Runnable(){
							@Override
							public void run() {
								mNotificationManager.cancel(R.id.notification_uploadphoto);								
							}
						}, 5000l);
						break;					
					}
				case Facebook.MESSAGE_UPDATE_UPLOAD_ERROR:{
						mUploadPhotoNotification.contentView.setTextViewText(R.id.status,"error sending photo to facebook.");
						mUploadPhotoNotification.icon = android.R.drawable.ic_dialog_alert;
						mUploadPhotoNotification.contentView.setInt(R.id.progress, "setProgress", 0);
						mUploadPhotoNotification.tickerText="Error uploading photo to Facebook";
						mUploadPhotoNotification.when = SystemClock.elapsedRealtime();
						postDelayed(new Runnable(){
							@Override
							public void run() {
								mNotificationManager.cancel(R.id.notification_uploadphoto);								
							}
						}, 60000l);
						break;				
					}
				}
			}
		};
		
		
	}
	
	Handler uploadListener; 
	FBPhotoUploadTask mAsyncPhotoUploader;
	
	ImageUrlLoader2.AsyncListener mAsyncImageLoaderListener = new ImageUrlLoader2.AsyncListener() {
		
		@Override
		public void onPreExecute() {
			
		}
		
		@Override
		public void onPostExecute(AsyncLoaderResult result) {
			if(result.status == AsyncLoaderResult.SUCCESS){
				//Matrix m = new Matrix();
				
				//m.setValues(null);
				/*
				Bitmap bmp = result.drawable;
				int w = bmp.getWidth();
				int h = bmp.getHeight();
				int[] pixels = new int[w*h];
				int stride = w*h;
				bmp.getPixels(pixels,0,stride,0,0,w,h);
				*/
					
				//getWindow().setBackgroundDrawable(null);
			}
		}
		
		@Override
		public void onCancelled() {
			
		}

		@Override
		public void onPublishProgress(AsyncLoaderProgress progress) {
			int code = (int)progress.code;
			switch(code){
				case R.id.asyncimageloader_profileimage:{
					if(progress.success){
						mProfileImage.setImageBitmap(progress.bitmap);
					}else{
						mProfileImage.setImageBitmap(App.INSTANCE.mDefaultProfileBitmap);
					}
				}
			}
		}
		
	};
	

	private void showSessionUserData(){
		
		String cachedUserInfo = Util.readStringFromLocalCache(App.INSTANCE, App.LOCALCACHEFILE_SESSIONUSERDATA);
		if(cachedUserInfo == null){
			return;
		}
		
		try{
			JSONArray users = new JSONArray(cachedUserInfo);
			JSONObject userJson = users.getJSONObject(0);													
			firstname = userJson.getString(FBUser.fields_first_name);
			lastname = userJson.getString(FBUser.fields_last_name);
			pic  = userJson.getString(FBUser.fields_pic);
			pic_big  = userJson.getString(FBUser.fields_pic_big);
			statusJson = userJson.getJSONObject(FBUser.fields_status);
			status = statusJson.getString(FBUser.fields_status_message);
			profileblurb = userJson.getString(FBUser.fields_profile_blurb);
			timezone = userJson.getString(FBUser.fields_timezone);
			current_location = userJson.getString(FBUser.fields_current_location);
			mTopHeader.setText(firstname+" "+lastname);
			Bitmap profileImage = App.mImageUrlLoader2.loadImage(pic_big, true);			
			if(profileImage == null){
				ImageUrlLoader2.AsyncLoaderInput input = new ImageUrlLoader2.AsyncLoaderInput();
				input.imageUri = pic_big;
				input.code = R.id.asyncimageloader_profileimage;
				App.mImageUrlLoader2.loadImageAsync(App.INSTANCE.mExecScopeImageLoaderTask, input, mAsyncImageLoaderListener);
			}else{
				mProfileImage.setImageBitmap(profileImage);
			}
			mStatusBox.setText(status);
		}catch(JSONException e){				
			e.printStackTrace();
		}		
	}

	static final short[] mSelectedUserFields = new short[]{User.col_first_name, User.col_last_name, User.col_status, User.col_profile_blurb, User.col_pic, User.col_pic_big};
	
	private void onFinishFetchingCommentsFromCloud(){
		
		
		
	}
	
	private AlertDialog createDialog(int what) {
		
		switch (what) {
		case DIALOG_EVENTS:{
			 return new AlertDialog.Builder(HomeActivity.this)
             .setTitle("Events")             
             .setItems(R.array.events_options, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int which) {                	                     
                   
                     
                     Intent showEvents = new Intent(HomeActivity.this, EventsActivity.class);
                     startActivity(showEvents);
                 }
             })
             .create();
			
		}
		case DIALOG_CONTACTS:{
			return new AlertDialog.Builder(HomeActivity.this)
            .setTitle("Contacts")    
            .setItems(R.array.contacts_options, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    String[] items = getResources().getStringArray(R.array.contacts_options);                    
                    new AlertDialog.Builder(HomeActivity.this)
                            .setMessage("You selected: " + which + " , " + items[which])
                            .show();
                }
            })
            .create();
			
		}
		case DIALOG_STATUSUPDATE: {

			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(
					R.layout.dialog_statusupdate, null);
			return new AlertDialog.Builder(HomeActivity.this).setIcon(
					android.R.drawable.ic_dialog_alert).setTitle(
					R.string.statusupdate).setView(textEntryView)
					.setPositiveButton(R.string.alert_dialog_ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked OK so do some stuff */
								}
							}).setNegativeButton(R.string.alert_dialog_cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked cancel so do some stuff */
								}
							}).create();

		}
		}
		return null;
	}
	
	
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		OptionsMenu.createPreference(this,menu, 0, App.MENUITEM_SETTINGS, 0);
		MenuItem clear = menu.add(0,App.MENUITEM_CLEAR,2,"Clear");
		MenuItem logoff = menu.add(0,App.MENUITEM_LOGOFF, 1,"Logoff");
		MenuItem getWallComments = menu.add(0,App.MENUITEM_GET_WALLCOMMENTS, 1,"Test");
		MenuItem getWallPosts = menu.add(0,App.MENUITEM_GET_WALLPOSTS, 1,"Get wall posts");
		MenuItem toggleLogger= menu.add(0,App.MENUITEM_TOGGLE_LOGGER, 1,"Logger");
		//MenuItem settings = menu.add(0,App.MENUITEM_SETTINGS, 2,"Settings");
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int id = item.getItemId();
		switch(id){
			case App.MENUITEM_LOGOFF:{
				App.INSTANCE.deleteSessionInfo();
				break;
			}
			case App.MENUITEM_SETTINGS:{
				
				break;
			}
			case App.MENUITEM_CLEAR:{
				App.INSTANCE.mDBHelper.deleteAllNotifications(App.INSTANCE.mDB);
				break;
			}
			case App.MENUITEM_GET_WALLCOMMENTS:{
				App.INSTANCE.getWallPostsCommentsFromCloud();
				break;
			}
			case App.MENUITEM_GET_WALLPOSTS:{
				App.INSTANCE.getWallPostsFromCloud(0);
				break;
			}
			case App.MENUITEM_TOGGLE_LOGGER:{
				Logger.show = !Logger.show;
				break;
			}
		}
		
		return super.onMenuItemSelected(featureId, item);
	}
	
	
	//Temporary data stores
	static PageableJsonData data_comments = null;
	static JSONArray data_users = null;
	static JSONArray data_comments_info = null;
	//
	
	CommentsAdapter mListAdapter;
	
	Handler mUIHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			//Log.d(LOG_TAG,"ui hander handle message what:"+msg.what);
			int code = msg.what;
			Bundle data = msg.getData();
		
			switch(code){
				case ManagerThread.MESSAGE_DISMISS_DIALOG:{
					//mProgressDialog.dismiss();
					break;
				}
			
				case ManagerThread.MESSAGE_UPDATELIST:{
					Logger.l(Logger.DEBUG,LOG_TAG,"[Handler()] [handleMessage()] update list");
					mListAdapter.onFinishedLoading();
					mListAdapter.parseUsersJsonData(data_users);
					//mListAdapter.setPagingInfo(mPagingInfo);					
					mListAdapter.setData(data_comments);
					mListAdapter.notifyDataSetChanged();
					onFinishFetchingCommentsFromCloud();	
					break;
				}
				
				case BaseManagerThread.CALLBACK_ADMOB_ONFAILRECEIVE:{					
					mAdView.setVisibility(View.GONE);
					break;
				}
				
				case BaseManagerThread.CALLBACK_ADMOB_ONRECEIVE:{
					mAdView.setVisibility(View.VISIBLE);
					break;
				}
			
				case BaseManagerThread.CALLBACK_TIMEOUT_ERROR:{
					Toast.makeText(HomeActivity.this, "Request to Facebook timed out", 2000).show();
					onFinishFetchingCommentsFromCloud();
					mListAdapter.onFinishedLoading();
					
					break;
				}
				
				case BaseManagerThread.CALLBACK_SERVERCALL_ERROR:{					
					String reason = (String)data.getString(Facebook.XTRA_SERVERCALL_ERROR_MSG);
					String errorCode = ""; 
					if(data.containsKey(Facebook.XTRA_SERVERCALL_ERROR_CODE)){
						errorCode = Integer.toString(data.getInt(Facebook.XTRA_SERVERCALL_ERROR_CODE));
						errorCode+=":";
					}
					Toast.makeText(HomeActivity.this, errorCode+reason, 2000).show();			
					onFinishFetchingCommentsFromCloud();
					
					mListAdapter.onFinishedLoading();
					break;
				}
							
				case BaseManagerThread.CALLBACK_PROCESS_WSRESPONSE_ERROR:{
					FBWSResponse fbResponse = (FBWSResponse) msg.obj;					
					Toast.makeText(HomeActivity.this,fbResponse.errorDesc, 1000).show();
					onFinishFetchingCommentsFromCloud();
					
					mListAdapter.onFinishedLoading();
					break;
				}			
					
			}
		};
	};	
		


}