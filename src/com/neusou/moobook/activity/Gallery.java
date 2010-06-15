package com.neusou.moobook.activity;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ZoomButtonsController;
import android.widget.ViewSwitcher.ViewFactory;

import com.admob.android.ads.AdView;
import com.neusou.Logger;
import com.neusou.async.UserTask;
import com.neusou.moobook.App;
import com.neusou.moobook.FBPhoto;
import com.neusou.moobook.FBPhotoTag;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.FQL;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;
import com.neusou.moobook.thread.ManagerThread;
import com.neusou.moobook.view.ImageViewTouch;
import com.neusou.moobook.view.PhotoTag;
import com.neusou.web.ImageUrlLoader2;
import com.neusou.web.ImageUrlLoader.AsyncLoaderResult;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderProgress;

public class Gallery extends BaseActivity
{
	ZoomButtonsController mZoomButtonsController;
	GestureDetector mGestureDetector; 
	GestureDetector.OnGestureListener mGestureListener;
	View mNextBtn;
	View mPrevBtn;
	Button mCommentsBtn;
	ToggleButton mTagsBtn;
	AdView mAdView;
	
	View.OnTouchListener mNavigationController;
	ImageViewTouch mImageView;
	public static final String LOG_TAG = "Gallery";
	
	public static final String XTRA_PHOTOS = "xtra.photos";	
	public static final String XTRA_USERS = "xtra.users";
	public static final String XTRA_PHOTOTAGS = "xtra.phototags";
	public static final String XTRA_PHOTOID = "xtra.photoid";
	public static final String XTRA_STARTINDEX = "xtra.startindex";
	private static final String XTRA_ACTIVETAGINDEX = "xtra.tag.active";	
	private static final String XTRA_NUMPHOTOS = "xtra.numphotos";
	
	static final int MAX_NUMTAGS = 30;	
	
	JSONArray photosJsonData; //all tagged photos of a particular user.
	JSONArray tagsJsonData; //all tags of a particular user.
	JSONArray usersJsonData;  //all photo owners
	JSONArray currentTagsData;
			
	int mStartIndex;
	int mNumPhotos;
	
	View.OnClickListener mNavigationsOnClick;
	ImageUrlLoader2.AsyncListener mAsyncLoaderListener;
	View.OnTouchListener mImageViewOnTouch;
	TextView mTagInfo;
	ProgressDialog mProgressDialog;
	
	PhotoTag[] mPhotoTags;
	int numTags = 0;
	Facebook mFacebook;
	
	String pid = "";
	
	static boolean isDisplayTags = false;
	
	float x0,y0,x1,y1;
	CountDownLatch mThreadsInitCountdown;
	static WorkerManagerThread mWorkerManagerThread;
	
	ViewGroup mTagsContainer;
	ViewGroup mRootView;
	View mLoadingIndicator;
	TextSwitcher mTopHeaderText;
	Handler mUIHandler;
	
	/**
	 * Used to disable asynchronous image loading that has been cancelled (when the user presses the back button when progress dialog is shown) from updating the ImageView. 
	 */	
	long mImageLoadingCode;
	
	static HashMap<String, JSONArray> mPhotoTagsMap = new HashMap<String, JSONArray>(2,0.75f);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.gallery);
		getWindow().setBackgroundDrawable(null);
		bindViews();
		initObjects();
		initViews();
	}
	
	private ProgressDialog createProgressDialog(){
		ProgressDialog pd = new ProgressDialog(this);
		pd.setIndeterminate(true);
		pd.setTitle(null);
		pd.setCustomTitle(null);
		pd.setCancelable(true);

		pd.setOnDismissListener(new DialogInterface.OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				mImageLoadingCode = 0;
			}
			
		});
		pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				mImageLoadingCode = 0;
			}
			
		});
		return pd;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mFacebook.purgeInactiveOutHandlers(false);
		mFacebook = null;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Bundle extras = getIntent().getExtras();
		String photosData = extras.getString(XTRA_PHOTOS);
//		String tagsData = getIntent().getStringExtra(XTRA_TAGS);
		String usersData = getIntent().getStringExtra(XTRA_USERS);
		mStartIndex = extras.getInt(XTRA_STARTINDEX,0);
		pid = extras.getString(XTRA_PHOTOID);
		Logger.l(Logger.DEBUG, LOG_TAG, "photo start index: "+mStartIndex);
		if(photosData != null){
		try{
			photosJsonData = new JSONArray(photosData);
			//tagsJsonData = new JSONArray(tagsData);
			usersJsonData = new JSONArray(usersData);
			mNumPhotos = photosJsonData.length();
		}catch(JSONException e){			
		}
		}		
			
//		mImageView.setScaleType(ScaleType.CENTER_CROP);	
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		getAllTagsTaskIdentifier = 0;
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		String photosData = savedInstanceState.getString(XTRA_PHOTOS);
		pid = savedInstanceState.getString(XTRA_PHOTOID);
		//String tagsData = savedInstanceState.getString(XTRA_TAGS);
		String usersData = savedInstanceState.getString(XTRA_USERS);
		String photoTagsData = savedInstanceState.getString(XTRA_PHOTOTAGS);
		
		mNumPhotos = savedInstanceState.getInt(XTRA_NUMPHOTOS);
		mStartIndex =  savedInstanceState.getInt(XTRA_STARTINDEX);
		
		int activeTag = savedInstanceState.getInt(XTRA_ACTIVETAGINDEX);
		mImageView.setActiveTag(activeTag);
		
		try{
			photosJsonData = new JSONArray(photosData);			
		}catch(JSONException e){			
		}
				
		try{
			usersJsonData = new JSONArray(usersData);
		}catch(JSONException e){			
		}
		
		
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(XTRA_NUMPHOTOS, mNumPhotos);
		outState.putString(XTRA_PHOTOID, pid);
		outState.putString(XTRA_PHOTOS, photosJsonData.toString());
		outState.putString(XTRA_USERS, usersJsonData.toString());
		outState.putInt(XTRA_STARTINDEX, mStartIndex);
		outState.putInt(XTRA_ACTIVETAGINDEX, mImageView.getActiveTag());
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		getAllTagsTaskIdentifier = 0;
	}
	//boolean isImageShown = false;
	@Override
	protected void onResume() {
		super.onResume();		
		getCurrentPhotoId();
		mTagsBtn.setChecked(isDisplayTags);
		currentTagsData = mPhotoTagsMap.get(pid);
		displayImage(mStartIndex);
		if(currentTagsData == null){
			mImageView.clearTags();			
			//if(mGetTagsTask != null) mGetTagsTask.cancel(true);
			getAllTags(pid);
		}else{
			if(isDisplayTags){
				if(!isLoadingImage){
					createTags(currentTagsData);	
					showTagInfo(mImageView.getActiveTag());
				}
			}
			else{
				mImageView.clearTags();
			}
		}

		mFacebook.registerOutHandler(R.id.outhandler_activity_gallery, mWorkerManagerThread.getInHandler());
				
		mImageView.postInvalidateDelayed(10);
		mTopHeaderText.setText("Caption");
	}
	
	public void bindViews(){
		mTopHeaderText = (TextSwitcher) findViewById(R.id.topheader);
		mLoadingIndicator = findViewById(R.id.loadingindicator);
		mImageView = (ImageViewTouch) findViewById(R.id.image);
	    mNextBtn = findViewById(R.id.next);
	    mPrevBtn = findViewById(R.id.prev);
	    mCommentsBtn = (Button) findViewById(R.id.comments_btn);
	    mTagsBtn = (ToggleButton) findViewById(R.id.tags_btn);
	    mTagsContainer = (ViewGroup) findViewById(R.id.tags_container);
	    mTagInfo = (TextView) findViewById(R.id.taginfo);
	    mAdView = (AdView) findViewById(R.id.ad);
	}

	public void initObjects(){
		Logger.l(Logger.DEBUG,LOG_TAG,"initObjects()");
		if(mWorkerManagerThread == null){
			mThreadsInitCountdown = new CountDownLatch(1);
			mWorkerManagerThread = new WorkerManagerThread(mThreadsInitCountdown, null);
			mWorkerManagerThread.start();
			try{
					mThreadsInitCountdown.await();
			}catch(InterruptedException e){			
			}
		}
		
		mFacebook = Facebook.getInstance();
		mNavigationsOnClick = new  OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int id = v.getId();
			//	Logger.l(Logger.DEBUG,LOG_TAG,"id:"+id);
				switch(id){
				
					case R.id.next:{
						clearTagInfo();
						mImageView.clearTags();
						mImageView.setActiveTag(-1);
						mStartIndex++;	
						if(mStartIndex > mNumPhotos){
							mStartIndex = mNumPhotos - 1;
						}
						updatePhoto(mStartIndex);
						break;
					}
					
					case R.id.prev:{
						clearTagInfo();
						mImageView.clearTags();
						mImageView.setActiveTag(-1);
						mStartIndex--;
						if(mStartIndex < 0){
							mStartIndex = 0;
						}
						updatePhoto(mStartIndex);
						break;
					}
					
					case R.id.comments_btn:{		
						Intent showComments = ViewCommentsActivity.getIntent(Gallery.this); 
							//new Intent(Gallery.this, ViewCommentsActivity.class);
						pid = getCurrentPhotoId();
						showComments.putExtra(ViewCommentsActivity.XTRA_OBJECTID, pid);
						showComments.putExtra(ViewCommentsActivity.XTRA_CLEARDATA, true);
						startActivity(showComments);
						break;
					}
					
					case R.id.tags_btn:{
						
						isDisplayTags = mTagsBtn.isChecked();
						if(isDisplayTags){
							currentTagsData = mPhotoTagsMap.get(pid);
							if(currentTagsData != null){								
								createTags(currentTagsData);
								mImageView.postInvalidateDelayed(10);
								showTagInfo(mImageView.getActiveTag());
							}else{
								getAllTags(pid);
							}
						}else{
							clearTagInfo();
							mImageView.clearTags();
						}
						break;
					}
				}
				
			}
		};	
		
		 mAsyncLoaderListener = new ImageUrlLoader2.AsyncListener() {
			
			@Override
			public void onPreExecute() {
				
			}
			
			@Override
			public void onPostExecute(ImageUrlLoader2.AsyncLoaderResult result) {
				Logger.l(Logger.DEBUG, LOG_TAG, "[onPostExecute()]");
				if(mProgressDialog != null){
					mProgressDialog.dismiss();
				}
				isLoadingImage = false;
				
				if(result.status == AsyncLoaderResult.SUCCESS){					
					//mImageView.setImageBitmap(result.);					
					currentTagsData = mPhotoTagsMap.get(pid);
					if(currentTagsData != null){					
						if(isDisplayTags){
							createTags(currentTagsData);
							int activeTag = mImageView.getActiveTag();
							showTagInfo(activeTag);
						}
					}
				}
				
			}
			
			@Override
			public void onCancelled() {				
			}

			@Override
			public void onPublishProgress(AsyncLoaderProgress progress) {	
				Logger.l(Logger.DEBUG, LOG_TAG, "[onPublishProgress()] code:"+progress.code+", mImageLoadingCode:"+mImageLoadingCode);
			//	if(progress.success){
					//if(progress.code == mImageLoadingCode){
						Logger.l(Logger.DEBUG, LOG_TAG, "setting bitmap");
						progress.imageView.setImageBitmap(progress.bitmap);
					//}
				//}		
					mImageView.setVisibility(View.VISIBLE);
			}
			
		};
		
		mGestureListener = new GestureDetector.OnGestureListener() {
			
			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				Logger.l(Logger.DEBUG, LOG_TAG, "[onSingleTapUpl()]" );
				return false;
			}
			
			@Override
			public void onShowPress(MotionEvent e) {
				Logger.l(Logger.DEBUG, LOG_TAG, "[onShowPress()]" );
			}
			
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
					float distanceY) {
				int numPointers = e1.getPointerCount();
				Logger.l(Logger.DEBUG, LOG_TAG, "[onScroll()] numPointers:"+numPointers);
				if(numPointers >= 2){
					int numHistory1 = e1.getHistorySize();
					int numHistory2 = e2.getHistorySize();
					Logger.l(Logger.DEBUG, LOG_TAG, "[onScroll()] nh1:"+numHistory1+" nh2:"+numHistory2);
					
					
				}
				
				Logger.l(Logger.DEBUG, LOG_TAG, "[onScroll()]" +
						" ("+e1.getX()+","+e1.getY()+")" +
						" ("+e2.getX()+","+e2.getY()+")");
				float scale = mImageView.getScale();
				Logger.l(Logger.DEBUG, LOG_TAG, "[onScroll()] scale:"+scale);
				mImageView.panBy(-distanceX,-distanceY);
				return false;
			}
			
			@Override
			public void onLongPress(MotionEvent e) {
					Logger.l(Logger.DEBUG, LOG_TAG, "[onLongPress()] "+e.getAction());				
			}
			
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
					float velocityY) {
				Logger.l(Logger.DEBUG, LOG_TAG, "[onFling()] "+e1.getAction());
				return false;
			}
			
			@Override
			public boolean onDown(MotionEvent e) {
				int numPointers = e.getPointerCount();
								
				x0 = e.getX(0);
				y0 = e.getY(0);
				
				if(numPointers > 1){
					x1 = e.getX(1);
					y1 = e.getY(1);
				}
				
				Logger.l(Logger.DEBUG, LOG_TAG, "[onDown()] "+e.getAction()+", numPointers:"+numPointers+" x0:"+x0+",y0:"+y0+",x1:"+x1+",y1:"+y1);
				
				return false;
			}
			
			
		};
		
		mUIHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				int code = msg.what;
				Bundle data = msg.getData();
				Logger.l(Logger.DEBUG,LOG_TAG,"[UIHandler][handleMessage()] code: "+code);
				super.handleMessage(msg);				
				switch(code){
					case ManagerThread.CALLBACK_TIMEOUT_ERROR:{
						if(mProgressDialog != null){
							mProgressDialog.dismiss();
						}
						Toast.makeText(Gallery.this, "Request to Facebook timed out", 2000).show();
						Logger.l(Logger.WARN,LOG_TAG,"[UIHandler] [handleMessage()] remote request timed out.");
						
						break;
					}
					case ManagerThread.CALLBACK_SERVERCALL_ERROR:{
						if(mProgressDialog != null){
							mProgressDialog.dismiss();
						}
						String reason = (String)data.getString(Facebook.XTRA_SERVERCALL_ERROR_MSG);
						int errorCode = data.getInt(Facebook.XTRA_SERVERCALL_ERROR_CODE);
						Logger.l(Logger.ERROR,LOG_TAG,"[UIHandler] [handleMessage()] failed invoking remote request. error: "+errorCode+", reason:"+reason);
						Toast.makeText(Gallery.this, errorCode+":"+reason, 2000).show();
						postAtTime(
								new Runnable() {									
									@Override
									public void run() {
										getAllTags(pid);										
									}
								}
								, 2000);
						break;
					}	
					
					case ManagerThread.MESSAGE_UPDATE_TAGS:{
						if(mProgressDialog != null){
							mProgressDialog.dismiss();
						}
						String phototags = msg.getData().getString(XTRA_PHOTOTAGS);
						String photoId = msg.getData().getString(XTRA_PHOTOID);
						clearTagInfo();
						try{
							JSONArray photoTagsJsonData = new JSONArray(phototags);
							mPhotoTagsMap.put(photoId, photoTagsJsonData);														
							if(isDisplayTags){
								if(!isLoadingImage){
									createTags(photoTagsJsonData);
									mImageView.setActiveTag(0);
									showTagInfo(mImageView.getActiveTag());
								}
							}
						}catch(Exception e){
						}						
						break;
					}
					
				
				case ManagerThread.CALLBACK_ADMOB_ONFAILRECEIVE:{					
					mAdView.setVisibility(View.GONE);
					break;
				}
				case ManagerThread.CALLBACK_ADMOB_ONRECEIVE:{
					mAdView.setVisibility(View.VISIBLE);
					break;
				}
				}
			}
		};
		
		mTopHeaderText.setFactory(new ViewFactory() {
			@Override
			public View makeView() {
				TextView t = new TextView(Gallery.this);
				t = (TextView) mLayoutInflater.inflate(
						R.layout.t_topheadertext, null);
				return t;
			}
		});
		
		
		mWorkerManagerThread.setOutHandler(mUIHandler);
		
		Logger.l(Logger.DEBUG,LOG_TAG,"initObjects() finished.");
	}

	public void initViews(){
		mProgressDialog = createProgressDialog();
		
		mNextBtn.setOnClickListener(mNavigationsOnClick);
	    mPrevBtn.setOnClickListener(mNavigationsOnClick);
	    mCommentsBtn.setOnClickListener(mNavigationsOnClick);
	    mTagsBtn.setOnClickListener(mNavigationsOnClick);
	    
	    mRootView = (ViewGroup) findViewById(R.id.rootLayout);
	    mRootView.setOnTouchListener(new View.OnTouchListener() {        	
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mGestureDetector.onTouchEvent(event);
				int numPointers = event.getPointerCount();
				int action = event.getAction();
				Logger.l(Logger.DEBUG, LOG_TAG, "[onTouch()] numPointers:"+numPointers+", action: "+event.getAction());
				
				if(numPointers >= 2){
					if(action == MotionEvent.ACTION_DOWN){						
						int pid1 = event.getPointerId(0);
						int pid2 = event.getPointerId(1);
						Logger.l(Logger.DEBUG, LOG_TAG, "[onTouch()] onDown: id1:"+pid1+", id2:"+pid2);
					}
					
					
				}
				
				return true;
			}
		});
	    setupOnScreenControls(findViewById(R.id.rootLayout),mImageView);
	    
	    mImageView.setOnKeyListener(new View.OnKeyListener(){
	    	@Override
	    	public boolean onKey(View v, int keyCode, KeyEvent event) {
	    		int action = event.getAction();
	    	//	Logger.l(Logger.DEBUG, LOG_TAG, "imageView onkey(), code: "+keyCode+" action: "+action);
	    		if(keyCode == KeyEvent.KEYCODE_BACK){	    			
	    			finishActivity(ViewPhotosActivity.REQUEST_PHOTOVIEW);
	    		}	    		
	    		
	    		switch(keyCode){
	    			case KeyEvent.KEYCODE_DPAD_RIGHT:{
	    				if(action == KeyEvent.ACTION_UP){
	    					if(isDisplayTags){
	    							mImageView.viewNextTag();	    				
	    							int activeTag = mImageView.getActiveTag();
	    							showTagInfo(activeTag);
	    							return true;
	    					}
	    				}	    				
	    			}
	    			case KeyEvent.KEYCODE_DPAD_LEFT:{
	    				if(isDisplayTags){
	    					if(action == KeyEvent.ACTION_UP){
	    						mImageView.viewPreviousTag();
	    				
	    						int activeTag = mImageView.getActiveTag();
	    						showTagInfo(activeTag);
	    						return true;	    			
	    					}
	    				}
	    			}
	    			case KeyEvent.KEYCODE_DPAD_DOWN:{
	    				View canFocusView = mImageView.findFocus();
	    				String name = v.getClass().getName();
	    				Logger.l(Logger.DEBUG,LOG_TAG,"can focus on: "+name);
	    				canFocusView.requestFocus();
	    				return true;
	    			}
	    		}
	    		return false;
	    	}
	    });
	    
	    findViewById(R.id.rootLayout).setOnKeyListener(new View.OnKeyListener() {
	
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				Logger.l(Logger.DEBUG, LOG_TAG, "root onkey(), code: "+keyCode);
				if(keyCode == KeyEvent.KEYCODE_BACK){
					finish();
				}
	    		return false;
			}
			
		
		});
	    
		mAdView.setVisibility(View.VISIBLE);
		mAdView.setKeywords("gift");		
		mAdView.setBackgroundColor(0x00000000);
		mAdView.setTextColor(0xFF111111);
		mAdView.setGoneWithoutAd(false);
		mAdView.setRequestInterval(60);
		mAdView.setListener(new AdView.AdListener() {			
			@Override
			public void onReceiveAd(AdView adView) {
				mUIHandler.sendEmptyMessage(ManagerThread.CALLBACK_ADMOB_ONRECEIVE);
				adView.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onNewAd() {
				
			}
			
			@Override
			public void onFailedToReceiveAd(AdView adView) {
				mUIHandler.sendEmptyMessage(ManagerThread.CALLBACK_ADMOB_ONFAILRECEIVE);
				adView.setVisibility(View.GONE);
			}			
		});
		
		
	}

	boolean isLoadingImage = false;
	

	private void showLoadingIndicator(){	
			mLoadingIndicator.setVisibility(View.VISIBLE);
			setProgressBarIndeterminateVisibility(true);
		}
		
		private void hideLoadingIndicator(){
			mLoadingIndicator.setVisibility(View.INVISIBLE);
			setProgressBarIndeterminateVisibility(false);
		}
		
	private boolean displayImage(int index){
		boolean hasImmediate = false;
		try{
			JSONObject photo = photosJsonData.getJSONObject(index);
			String imageSource = photo.getString("src_big");
			Logger.l(Logger.DEBUG, LOG_TAG, "image:"+imageSource);
			
			Bitmap imageDrawable = App.mImageUrlLoader2.loadImage(imageSource, true);
			
			if(imageDrawable == null){
				mProgressDialog = createProgressDialog();
//				Message cancelMessage = Message.obtain();
	//			mProgressDialog.setCancelMessage(cancelMessage);
				mProgressDialog.setMessage("Loading image..");
				isLoadingImage = true;
				ImageUrlLoader2.AsyncLoaderInput input = new ImageUrlLoader2.AsyncLoaderInput();
				input.imageUri = imageSource; 
				input.imageView = mImageView;
				mImageLoadingCode = SystemClock.currentThreadTimeMillis();
				input.code = mImageLoadingCode;
				App.mImageUrlLoader2.loadImageAsync(App.INSTANCE.mExecScopeImageLoaderTask,input,mAsyncLoaderListener);
				mProgressDialog.show();
			}else{
				isLoadingImage = false;
				hasImmediate = true;
				mImageView.setImageBitmap(imageDrawable);
				mImageView.setVisibility(View.VISIBLE);
			}
			
		}catch(Exception e){
			
		}
		return hasImmediate;
	} 

	private void clearTagInfo(){
		mTagInfo.setText("");
	}
	
	private void updatePhoto(int mStartIndex){
		//try to display image immediately
		boolean hasImmediate = displayImage(mStartIndex);
				
		//the current displayed photo id
		pid = getCurrentPhotoId();
		
		
		//TODO Do somethinh at the end of the photo list, either load the next batch or stop
		if(pid == null){//if null. that means we are at the end of the array, no more photos
			return;
		}
		
		//try to get cached tags data in memory
		currentTagsData = mPhotoTagsMap.get(pid);
		if(currentTagsData == null){
			//if(mGetTagsTask != null) mGetTagsTask.cancel(true);
			//fetch tags data from facebook.
			getAllTags(pid);					
		}else{			
			//if the image is available immediately and shown on screen then display tags
			if(hasImmediate){
				if(isDisplayTags){
					createTags(currentTagsData);
					mImageView.setActiveTag(0);
					int activeTag = mImageView.getActiveTag();					
					showTagInfo(activeTag);
				}
			}							
		}
	}
	
	private void showTagInfo(int activeTag){
		JSONArray data = mPhotoTagsMap.get(pid);
		
		if(data == null){
			return;
		}
		
		try{
			
			JSONObject tag = data.getJSONObject(activeTag);
			String subject = tag.getString(FBPhotoTag.fields_text);			
			Logger.l(Logger.VERBOSE,LOG_TAG,"[showTag()] subject:"+subject);
			mTagInfo.setText(subject);			
			mImageView.invalidate();			
			//mTagInfo.setAnimation(AnimationUtils.loadAnimation(this, ));
		}catch(Exception e){
			Logger.l(Logger.ERROR,LOG_TAG,"[showTag()] message: "+e.getMessage());
			
		}
	}
	
	private String getCurrentPhotoId(){		
		try{
			JSONObject photo = photosJsonData.getJSONObject(mStartIndex);			
			pid = photo.getString(FBPhoto.fields_pid);
			return pid;
		}catch(JSONException e)
		{			
		}
		return null;
	}
	
	//UserTask mGetTagsTask;
	static public long getAllTagsTaskIdentifier;
	
	private void getAllTags(String pid){	
		//if(mGetTagsTask!=null){mGetTagsTask.cancel(true);}
		//mGetTagsTask = 
		Bundle callbackData = new Bundle();
		getAllTagsTaskIdentifier = SystemClock.currentThreadTimeMillis();
		callbackData.putLong(Facebook.XTRA_INTERNAL_CALLID, getAllTagsTaskIdentifier);
			mFacebook.getPhotoTags(R.id.outhandler_activity_gallery,callbackData,pid,MAX_NUMTAGS,0,
				ManagerThread.CALLBACK_GET_PHOTO_TAGS, 
				ManagerThread.CALLBACK_SERVERCALL_ERROR, 
				ManagerThread.CALLBACK_TIMEOUT_ERROR,
				0);	
	}
	
	@SuppressWarnings("deprecation")
	private void createTags(JSONArray photoTags){
		Logger.l(Logger.DEBUG,LOG_TAG,"createTags()");
		int numTags = 0;
		if(photoTags != null){
			numTags = photoTags.length();
		}else{			
			mImageView.setPhotoTags(null);			
			mImageView.postInvalidateDelayed(100);
		}
		
		mPhotoTags = new PhotoTag[numTags];
		
		Logger.l(Logger.DEBUG,LOG_TAG,"createTags() numTags:"+numTags );
		JSONObject tag;
		for(int i=0;i<numTags;i++){
			try{
				tag = photoTags.getJSONObject(i);
				Logger.l(Logger.DEBUG,LOG_TAG,"[createTags()] creating tag#: "+i+", raw:"+tag.toString());				
				String text = tag.getString(FBPhotoTag.fields_text);
				float xcoord = (float) (tag.getDouble(FBPhotoTag.fields_xcoord));
				float ycoord = (float) (tag.getDouble(FBPhotoTag.fields_ycoord));				
				mPhotoTags[i] = new PhotoTag(Gallery.this);
				mPhotoTags[i].xcoord = xcoord;
				mPhotoTags[i].ycoord = ycoord;						
			}
			catch(JSONException e){
				Logger.l(Logger.ERROR,LOG_TAG,"[createTags()] error: "+e.getMessage());
			}
		}	
		mImageView.setPhotoTags(mPhotoTags);		
	}
	
	private void hideTags(){
		
	}
	
	
		
	private void setupOnScreenControls(View rootView, View ownerView) {
	    //    mNextBtn.setOnClickListener(this);
	     //   mPrevBtn.setOnClickListener(this);
	        setupZoomButtonController(rootView);
	        setupOnTouchListeners(rootView);
	    }
	 
	  private void setupZoomButtonController(final View ownerView) {
		  Logger.l(Logger.DEBUG, LOG_TAG, "[setupZoomButtonController()]");
		  
	        mZoomButtonsController = new ZoomButtonsController(ownerView);
	        mZoomButtonsController.setAutoDismissed(false);
	        mZoomButtonsController.setZoomSpeed(100);
	        mZoomButtonsController.setOnZoomListener(
	                new ZoomButtonsController.OnZoomListener() {
	            public void onVisibilityChanged(boolean visible) {
	                if (visible) {
	                    updateZoomButtonsEnabled();
	                }
	            }

	            public void onZoom(boolean zoomIn) {
	                if (zoomIn) {
	                   // mImageView.zoomIn(10);
	                } else {
	                   // mImageView.zoomOut(10);
	                }
	                mZoomButtonsController.setVisible(true);
	                updateZoomButtonsEnabled();
	            }
	        });
	        
	    }
	  
	  
	  private void setupOnTouchListeners(View rootView) {
		  mGestureDetector = new GestureDetector(this, mGestureListener);
	      
	  }


	    private void updateZoomButtonsEnabled() {
	        ImageView imageView = mImageView;
	        float scale = 1;
	        mZoomButtonsController.setZoomInEnabled(scale < 5);
	        mZoomButtonsController.setZoomOutEnabled(scale > 1);
	    }

	    /*
	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		Logger.l(Logger.DEBUG, LOG_TAG, "[onFling()] velocityX:"+velocityX);
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		Logger.l(Logger.DEBUG, LOG_TAG, "[onLongPress()] action:"+e.getAction());
		
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	@Override
	public void onVisibilityChanged(boolean visible) {
		
	}

	@Override
	public void onZoom(boolean zoomIn) {
		
	}
	


*/
		
		public static class WorkerManagerThread extends ManagerThread {
			
	
			IPhotosGallery mActivity;
			
			public WorkerManagerThread(CountDownLatch cdl, IPhotosGallery act) {
				super(cdl);
				mActivity = act;
			}
			
			public void setActivity(ViewPhotosActivity act){
				mActivity = act;
			}
				
			@Override
			public void doBusiness(Bundle data, int code,
					FBWSResponse fbresponse) {
				long callid = data.getLong(Facebook.XTRA_INTERNAL_CALLID);
				
				switch(code){
				
					case ManagerThread.CALLBACK_GET_PHOTO_TAGS:{
						if(callid != getAllTagsTaskIdentifier){
							
						}
					String parsed;
					try {							
						parsed = fbresponse.jsonArray.toString(2);
						Logger.l(Logger.DEBUG, LOG_TAG,"[WorkerManagerThread] [handleMessage()] [callback_get_photo_tags] json data: "+ parsed);	
						
						/*
						if(fbresponse.hasErrorCode){
							
						}else{
							int numPhotos = mActivity.parsePhotosJson(fbresponse.data);
							
							mActivity.updatePaging(numPhotos);								
							Logger.l(Logger.DEBUG,LOG_TAG,"[WorkerManagerThread] [handleMessage()] [callback_get_tagged_photos] sending message to ui handler.");
							
													
						}
						*/
						JSONArray tagsData = fbresponse.jsonArray.getJSONObject(0).getJSONArray(FQL.FQL_RESULT_SET);
						String pid = tagsData.getJSONObject(0).getString(FBPhoto.fields_pid);
						String photoTags = tagsData.toString();
						
						Message m = mOutHandler.obtainMessage(ManagerThread.MESSAGE_UPDATE_TAGS);								
						Logger.l(Logger.DEBUG,LOG_TAG,"[WorkerManagerThread] photoTags: "+photoTags);								
						Bundle b = new Bundle();
						b.putString(XTRA_PHOTOTAGS, photoTags);
						b.putString(XTRA_PHOTOID, pid);
						m.setData(b);
						m.sendToTarget();
								
					} catch (JSONException e) {
						e.printStackTrace();
					}						
					break;
				}
		
				}//end of switch
			}
		};
}