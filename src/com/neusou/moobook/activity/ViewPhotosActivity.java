package com.neusou.moobook.activity;

import java.util.concurrent.CountDownLatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
//import android.graphics.ColorMatrix;
//import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ViewSwitcher.ViewFactory;

import com.admob.android.ads.AdView;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.FBPhoto;
import com.neusou.moobook.FBSession;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.controller.CursorListAdapter;
import com.neusou.moobook.controller.StreamListViewFactory;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.moobook.task.ProcessFriendsTask;
import com.neusou.moobook.task.ProcessStreamTask;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;
import com.neusou.web.ImageUrlLoader;
import com.neusou.web.ImageUrlLoader.AsyncLoaderResult;

interface IPhotosGallery{
	public int parsePhotosJson(final String data);
	public void updatePaging(int numPhotos);		
	
}
public class ViewPhotosActivity extends BaseActivity implements IPhotosGallery{ 
	static final String LOG_TAG = "ViewPhotosActivity";
	
	static final String XTRA_FACEBOOKUSERID = "xtras.fb.uid";
	static final String XTRA_PHOTOS = "xtras.photos";	
	static final String XTRA_PHOTOTAGS = "xtras.phototags";
	static final String XTRA_USERS = "xtras.users";
	static final String XTRA_STARTINDEX = "xtras.startindex";
	static final String XTRA_NEXTSTARTINDEX = "xtras.nextstartindex";
		
	int MY_DATA_CHECK_CODE = 9000;
	Facebook mFacebook;	
	StreamListViewFactory mListViewItemFactory;
	CursorListAdapter mListViewAdapter;	
	
	TextSwitcher mTopHeaderText;
	//Button mTestBtn;
//	Button mTest2Btn;
	
	Handler mThreadUIHandler;	
	FBSession fbSession;
	ApplicationDBHelper mDB;
	Cursor mStreamsCursor;	
	GridView mPhotosGrid;	
	TextToSpeech mTts;	
	View mLoadingIndicator;
	
	static WorkerManagerThread mWorkerThread;
	ProcessFriendsTask mProcessFriendsTask;
	ProcessStreamTask mProcessStreamsTask;
		
	JSONArray mPhotosData;	
	Drawable mPlaceholderDrawable;
	
	static int nextStartIndex = 0; 
	static int startIndex = 0;
	static boolean hasNext = true;
	static boolean hasPrev = false;
	static final int NUM_PHOTOS_PER_REQUEST = 12;
	static final int NUM_THREADS_INIT = 1;
	static final int REQUEST_PHOTOVIEW = 0;
	
	static final byte MODE_TAGGED = 0; //photos that were tagged
	static final byte MODE_ALL = 1; //all photos belonging to a user
	
	byte mode = MODE_TAGGED;
	
	static CountDownLatch mThreadsInitCountdown;
	AppsAdapter mPhotoGridAdapter;
		
	Handler mUIHandler;

	View.OnClickListener mNextOnClick;
	View.OnClickListener mPrevOnClick;
	Button mNextBtn;
	Button mPrevBtn;
	AdView mAdView;
	
	static JSONArray mTags,mPhotos,mUsers;
	
	static ProgressDialog mProgressDialog;
	
	static long mFBUserId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);			
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setBackgroundDrawableResource(R.drawable.metal);
		setContentView(R.layout.viewphotos_activity);		
		bindViews();
		getExtras();
		initObjects();
		initViews();	
		initData();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mFacebook.purgeInactiveOutHandlers(false);
		mFacebook = null;
		//mStreamsCursor.close();
		//mDB.close();		
	}

	@Override
	protected void onStart() {
		super.onStart();
		mFacebook.setOutHandler(mWorkerThread.getInHandler());
		
	}

	@Override
	protected void onStop() {	
		super.onStop();
		
	}
	
	
	@Override
	protected void onRestart() {
		super.onRestart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();		
		updatePagingButtons();
		mPhotoGridAdapter.setData(mPhotos);
		mPhotoGridAdapter.notifyDataSetChanged();
		mTopHeaderText.setText("Photos");
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);		
		Logger.l(Logger.DEBUG, LOG_TAG, "uid: "+mFBUserId);
		outState.putLong(XTRA_FACEBOOKUSERID, mFBUserId);
		outState.putString(XTRA_PHOTOS, mPhotos.toString());
		outState.putString(XTRA_PHOTOTAGS, mTags.toString());
		outState.putString(XTRA_USERS, mUsers.toString());
		outState.putInt(XTRA_STARTINDEX, startIndex);
		outState.putInt(XTRA_NEXTSTARTINDEX, nextStartIndex);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mFBUserId = savedInstanceState.getLong(XTRA_FACEBOOKUSERID, 0);
		startIndex = savedInstanceState.getInt(XTRA_STARTINDEX, 0);
		nextStartIndex = savedInstanceState.getInt(XTRA_NEXTSTARTINDEX, 0);
		
		try{
			mPhotos = new JSONArray(savedInstanceState.getString(XTRA_PHOTOS));
			mTags = new JSONArray(savedInstanceState.getString(XTRA_PHOTOTAGS));
			mUsers = new JSONArray(savedInstanceState.getString(XTRA_USERS));
		}catch(JSONException e){			
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {	
		super.onActivityResult(requestCode, resultCode, data);
		try{
			mFacebook.registerOutHandler(R.id.outhandler_activity_viewphotos, mWorkerThread.getInHandler());
		}catch(Exception e){			
		}
	}
			
	protected void getExtras(){
		
	}
	
	@Override
	protected void bindViews() {	
		super.bindViews();
		
		mLoadingIndicator = findViewById(R.id.loadingindicator);
				
		mTopHeaderText = (TextSwitcher) findViewById(R.id.topheader);
		
		mPhotosGrid = (GridView) findViewById(R.id.photos_container);
		mNextBtn = (Button) findViewById(R.id.next);
		mPrevBtn = (Button) findViewById(R.id.prev);
		
		mAdView = (AdView) findViewById(R.id.ad);
		
	}

	protected void initViews() {
			mPhotosGrid.setAdapter(mPhotoGridAdapter);
			mPhotosGrid.setSelector(mResources.getDrawable(R.drawable.thumbnail_box_selector));
			mPhotosGrid.setDrawSelectorOnTop(true);	
			mPhotosGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long duration) {
					Logger.l(Logger.DEBUG,LOG_TAG,"position: "+position);
					Intent i = new Intent(ViewPhotosActivity.this, Gallery.class);
					i.putExtra(Gallery.XTRA_STARTINDEX,position);
					i.putExtra(Gallery.XTRA_PHOTOS,mPhotos.toString());
					i.putExtra(Gallery.XTRA_USERS,mUsers.toString());
					startActivityForResult(i,REQUEST_PHOTOVIEW);			
				}
				
			});
			mProgressDialog = createProgressDialog();					
			mTopHeaderText.setFactory(new ViewFactory() {
				@Override
				public View makeView() {
					TextView t = new TextView(ViewPhotosActivity.this);
					t = (TextView) mLayoutInflater.inflate(
							R.layout.t_topheadertext, null);
					return t;
				}
			});
			
			mNextBtn.setOnClickListener(mNextOnClick);
			mPrevBtn.setOnClickListener(mPrevOnClick);
			
			mAdView.setVisibility(View.VISIBLE);
			mAdView.setKeywords("gift");		
			mAdView.setBackgroundColor(0xFFaaaaaa);
			mAdView.setTextColor(0xFF111111);
			mAdView.setGoneWithoutAd(false);
			mAdView.setRequestInterval(60);
			mAdView.setListener(new AdView.AdListener() {			
				@Override
				public void onReceiveAd(AdView adView) {
					mUIHandler.sendEmptyMessage(BaseManagerThread.CALLBACK_ADMOB_ONRECEIVE);
					adView.setVisibility(View.VISIBLE);
				}
				
				@Override
				public void onNewAd() {
					
				}
				
				@Override
				public void onFailedToReceiveAd(AdView adView) {
					mUIHandler.sendEmptyMessage(BaseManagerThread.CALLBACK_ADMOB_ONFAILRECEIVE);
					adView.setVisibility(View.GONE);
				}			
			});
			
		}

	
	protected void initObjects() {
		if(mWorkerThread == null){
			mThreadsInitCountdown = new CountDownLatch(NUM_THREADS_INIT);
			mWorkerThread = new WorkerManagerThread(mThreadsInitCountdown,this);
			mWorkerThread.start();
			try {
				mThreadsInitCountdown.await();
			} catch (InterruptedException e) {			
				e.printStackTrace();
			}
		}
		mWorkerThread.setActivity(this);
		mPlaceholderDrawable = mResources.getDrawable(R.drawable.mobook2_64);
		mDB = new ApplicationDBHelper(this);
		mFacebook = Facebook.getInstance();
		mFacebook.registerOutHandler(R.id.outhandler_activity_viewphotos, mWorkerThread.getInHandler());
		
		mPhotoGridAdapter = new AppsAdapter();
		
		mNextOnClick = new View.OnClickListener() {				
			@Override
			public void onClick(View v) {
				if(hasNext){
					nextStartIndex = startIndex+NUM_PHOTOS_PER_REQUEST;
					nextStartIndex = Util.boundIndex(0,nextStartIndex,-1);
					getNextBatch();
				}
			}
		};
			
		mPrevOnClick = new View.OnClickListener() {					
			@Override
			public void onClick(View v) {
				if(hasPrev){
					nextStartIndex = startIndex-NUM_PHOTOS_PER_REQUEST;
					nextStartIndex = Util.boundIndex(0,nextStartIndex,-1);
					getPrevBatch();
				}
			}
		};
		
		mUIHandler = new Handler(){
			public void handleMessage(android.os.Message msg) {
				Log.d(LOG_TAG,"[UIHandler] [handleMessage()] code:"+msg.what);
				int code = msg.what;
				Bundle data = msg.getData();
				switch(code){
					case ManagerThread.CALLBACK_SERVERCALL_ERROR:{					
						mProgressDialog.dismiss();
						String reason = (String)data.getString(Facebook.XTRA_SERVERCALL_ERROR_MSG);
						Toast.makeText(ViewPhotosActivity.this, reason, 2000).show();
						break;
					}
					case ManagerThread.CALLBACK_PROCESS_WSRESPONSE_ERROR:{
						mProgressDialog.dismiss();
						FBWSResponse fbResponse = (FBWSResponse) msg.obj;					
						Toast.makeText(ViewPhotosActivity.this,fbResponse.errorDesc, 1000).show();									
						break;
					}			
					case ManagerThread.MESSAGE_UPDATE_PHOTO_GRID:{
						Logger.l(Logger.DEBUG,LOG_TAG," mUIHandler: "+this.toString());
						updatePagingButtons();
						Toast.makeText(ViewPhotosActivity.this,"UPDATING", 3000).show();
						mPhotoGridAdapter.setData(mPhotos);			
						mPhotoGridAdapter.notifyDataSetChanged();
						hideLoadingIndicator();
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
				}
			};
		};
		Logger.l(Logger.DEBUG,"debug3","mUIHandler:"+mUIHandler.toString());
		mWorkerThread.setOutHandler(mUIHandler);
				
	}

	private void getNextBatch(){
		if(mode == MODE_TAGGED){
			getTaggedPhotos();
		}else{
			getUserPhotos();
		}
	}
	
	private void getPrevBatch(){
		if(mode == MODE_TAGGED){
			getTaggedPhotos();
		}else{
			getUserPhotos();
		}
	}
	

	private void initData(){
		Logger.l(Logger.DEBUG, LOG_TAG, "[initData()]");
		Bundle extras = getIntent().getExtras();
		String photosJsonArrayString = extras.getString(XTRA_PHOTOS);
		long uid  = extras.getLong(XTRA_FACEBOOKUSERID);
		if(uid != mFBUserId){
			clearPagingInfo();
			clearPhotoData();
			mPhotos = null;
		}
		mFBUserId = uid;
		if(photosJsonArrayString != null){
			parsePhotosJson(photosJsonArrayString);
		}
	}

	private void getTaggedPhotos(){
		Bundle data = new Bundle();
		data.putLong(Facebook.param_uid, mFBUserId);
		mFacebook.getTaggedPhotos(R.id.outhandler_activity_viewphotos, data, mFBUserId, NUM_PHOTOS_PER_REQUEST, nextStartIndex , ManagerThread.CALLBACK_GET_TAGGED_PHOTOS,  BaseManagerThread.CALLBACK_SERVERCALL_ERROR,   BaseManagerThread.CALLBACK_TIMEOUT_ERROR, 0);		
		mProgressDialog = createProgressDialog();
		mProgressDialog.show();
	}
	private void getUserPhotos(){
		//mFacebook.get(mFBUserID,NUM_PHOTOS_PER_REQUEST, nextStartIndex , CALLBACK_GET_TAGGED_PHOTOS, CALLBACK_SERVERCALL_ERROR,  CALLBACK_TIMEOUT_ERROR, 0);		
		mProgressDialog = createProgressDialog();
		mProgressDialog.show();
	}
	
	
	private ProgressDialog createProgressDialog(){
		ProgressDialog pd;
		pd = new ProgressDialog(this);
		pd.setIndeterminate(true);
		pd.setMessage("Loading photos..");
		pd.setCustomTitle(null);
		pd.setCancelable(true);		
		return pd;
	}

	private void clearPagingInfo(){
		startIndex = 0;
		nextStartIndex = 0;
		hasNext = true;
		hasPrev = false;
	}

	private void clearPhotoData(){
		mPhotos = null;
		mUsers = null;
		mTags = null;
	}
	
	private void updatePagingButtons(){
		mPrevBtn.setClickable(hasPrev);
		mNextBtn.setClickable(hasNext);
		mPrevBtn.setEnabled(hasPrev);
		mNextBtn.setEnabled(hasNext);
		mPrevBtn.setVisibility(hasPrev?View.VISIBLE:View.INVISIBLE);
		mNextBtn.setVisibility(hasNext?View.VISIBLE:View.INVISIBLE);
		
	}

	public void updatePaging(int numPhotos){
		startIndex  = nextStartIndex;
		Util.boundIndex(0, startIndex, -1);
		if(startIndex > 0){
			hasPrev = true;
		}else{
			hasPrev = false;
		}
				
		if(numPhotos > 0){
			if(numPhotos < NUM_PHOTOS_PER_REQUEST){
				hasNext = false;
			}else{
				hasNext = true;
			}
		}else{
			hasNext = false;
		}
		
		
		
	}
	
	public int parsePhotosJson(final String jsonString){
		Logger.l(Logger.DEBUG, LOG_TAG, "[parsePhotosJson()] ");
		mProgressDialog.dismiss();
		
		if(jsonString == null){
			return -1;
		}
		
		JSONArray data;
		
		try{
			data = new JSONArray(jsonString);
			data.length();
			mTags = data.getJSONObject(0).getJSONArray("fql_result_set");
			mPhotos = data.getJSONObject(1).getJSONArray("fql_result_set");
			mUsers = data.getJSONObject(2).getJSONArray("fql_result_set");
		}catch(JSONException e){
			return -1;
		}			

		int numTags = 0;
		int numPhotos = 0;
		int numUsers = 0;

		try
		{
			numTags = mTags.length();
			numPhotos = mPhotos.length();
			numUsers = mUsers.length();
		}
		catch(Exception e){
			
		}
		
		Logger.l(Logger.DEBUG, LOG_TAG, "[parsePhotosJson()] numTags: "+numTags+", numPhotos: "+numPhotos+", numUsers:"+numUsers);
		
		return numPhotos;
	}
			
	/*
	protected void onActivityResult(
	        int requestCode, int resultCode, Intent data) {
	    if (requestCode == MY_DATA_CHECK_CODE) {
	        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
	            // success, create the TTS instance
	            mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
					
					@Override
					public void onInit(int status) {
						//
						mTts.setLanguage(Locale.US);
					}
					
				});
	            mTts.setPitch(-20);
	            mTts.setSpeechRate(1.3f);
	        } else {
	            // missing data, install it
	            Intent installIntent = new Intent();
	            installIntent.setAction(
	                TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
	            startActivity(installIntent);
	        }
	    }
	}
	*/

	
	public static class WorkerManagerThread extends BaseManagerThread {
		
		IPhotosGallery mActivity;
		
		public WorkerManagerThread(CountDownLatch cdl, IPhotosGallery act) {
			super(cdl);
			mActivity = act;
		}
		
		public void setActivity(ViewPhotosActivity act){
			mActivity = act;
		}
		
		@Override
		public void doBusiness(Bundle data, int code, FBWSResponse fbresponse) {
			switch(code){
			
			case ManagerThread.CALLBACK_GET_TAGGED_PHOTOS:{
				String parsed;
				try {							
					parsed = fbresponse.jsonArray.toString(2);
					Logger.l(Logger.DEBUG, LOG_TAG,"[WorkerManagerThread] [handleMessage()] [callback_get_tagged_photos] json data: "+ parsed);	
					if(fbresponse.hasErrorCode){								
					}else{
						int numPhotos = mActivity.parsePhotosJson(fbresponse.data);						
						mActivity.updatePaging(numPhotos);								
						Logger.l(Logger.DEBUG,LOG_TAG,"[WorkerManagerThread] [handleMessage()] [callback_get_tagged_photos] sending message to ui handler.");
						if(waitOutHandler()){
							mOutHandler.sendEmptyMessage(ManagerThread.MESSAGE_UPDATE_PHOTO_GRID);
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}						
				break;
			}
			}
		
		}
	};

	
	 public class AppsAdapter extends BaseAdapter {
		 JSONArray data;
		 static final String LOG_TAG = "PhotoGridAdapter";
		 public void setData(JSONArray data){
			 this.data = data;
		 }
		 
		 public String whichImageSize = FBPhoto.fields_src;
		 
	        public AppsAdapter() {
	        }

	        public class Holder {		
	    		public ImageView thumbnailImage;
	    		public String thumbnailImageUrl;	    		
	    		public int position; //cancel asynctask if the recycled row is used as a new row
	    	}
	        
	        public View getView(int position, View convertView, ViewGroup parent) {	            
	        	Logger.l(Logger.DEBUG,LOG_TAG,"[getView()] position:"+position+", thread:"+Thread.currentThread().getName());
	            if (convertView == null) {
	            	convertView = mLayoutInflater.inflate(R.layout.t_photo,parent, false);
	            	
	            	GridView.LayoutParams layout = new GridView.LayoutParams(GridView.LayoutParams.WRAP_CONTENT, GridView.LayoutParams.WRAP_CONTENT);
	                convertView.setLayoutParams(layout);
	            } 
	            
	        	Holder tag = (Holder) convertView.getTag();

	    		//bind child views and store in tag
	    		if (tag == null) {
	    			tag = new Holder();
	    			tag.thumbnailImage = (ImageView) convertView.findViewById(R.id.thumbnail);	    				
	    		}	    		
	    		
	            String pic_small_url = "";
	            try {
					JSONObject photo = data.getJSONObject(position);
					pic_small_url = photo.getString(whichImageSize);			
					
					tag.thumbnailImageUrl = pic_small_url;
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				Logger.l(Logger.DEBUG,LOG_TAG,"[getView()] loading: "+pic_small_url);
				
				// when no image for profile is available, use default profile image
				if (tag.thumbnailImageUrl == null || tag.thumbnailImageUrl.length() == 0) {					
					tag.thumbnailImage.setImageDrawable(mPlaceholderDrawable);
				} 
				
				else {
					// otherwise try to immediately load the image from cache.				
					Drawable img;// = mPlaceholderDrawable;	 
					img = App.mImageUrlLoader.loadImage(tag.thumbnailImageUrl, true);
					if (img == null) {
						App.mImageUrlLoader.loadImageAsync(tag.thumbnailImageUrl,mImageAsyncLoaderListener);
						img = mPlaceholderDrawable;
						convertView.setVisibility(View.INVISIBLE);
					} 	
					tag.thumbnailImage.setImageDrawable(img);
					convertView.setVisibility(View.VISIBLE);
				}
				return convertView;
	        }

	        public final int getCount() {
	        	if(data == null){
	        		return 0 ;
	        	}
	        	return data.length();
	        }

	        public final Object getItem(int position) {
	        	if(data == null){
	        		return 0 ;
	        	}
	        	try {
					return data.getJSONObject(position);
				} catch (JSONException e) {
				
				}
				return null;
	        }

	        public final long getItemId(int position) {
	        	if(data == null){
	        		return 0 ;
	        	}
	        	return position;
	        }
	    }
	 
	/*
	private void fetchPhotosFromCloud(){
		mTopHeaderText.setText("Fetching photos..");		
		if(mFacebook.getSession() == null){
			Toast.makeText(this, "Session is null", 1000).show();
			return;
		}
		showLoadingIndicator();
		mFacebook.getAllLatestFriendsPhotos(Long.toString(fbSession.uid),  ManagerThread.CALLBACK_GET_FRIENDS_PHOTOS,  ManagerThread.CALLBACK_SERVERCALL_ERROR, 100, 0);
	}
	*/
	 
	private void showLoadingIndicator(){	
		mLoadingIndicator.setVisibility(View.VISIBLE);
		setProgressBarIndeterminateVisibility(true);
	}
	
	private void hideLoadingIndicator(){
		mLoadingIndicator.setVisibility(View.INVISIBLE);
		setProgressBarIndeterminateVisibility(false);
	}


	ImageUrlLoader.AsyncListener mImageAsyncLoaderListener = new ImageUrlLoader.AsyncListener() {
		
		@Override
		public void onPreExecute() {
						
		}
		
		@Override
		public void onPostExecute(AsyncLoaderResult result) {
			if(result != null && result.status == AsyncLoaderResult.SUCCESS){
				mPhotoGridAdapter.notifyDataSetChanged();
			}			
		}
		
		@Override
		public void onCancelled() {
						
		}
	}; 
	
}
