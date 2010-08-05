package com.neusou.moobook.activity;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import com.admob.android.ads.AdView;
import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.FBAlbum;
import com.neusou.moobook.FBPhoto;
import com.neusou.moobook.FBSession;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.controller.StreamListViewFactory;
import com.neusou.moobook.task.ProcessFriendsTask;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;
import com.neusou.web.ImageUrlLoader;
import com.neusou.web.ImageUrlLoader.AsyncLoaderResult;

interface IAlbums{
	public int parseData(final String data);
	public void updatePaging(int numPhotos);
}

public class ViewAlbumsActivity extends BaseActivity implements IAlbums{ 
	static final String LOG_TAG = "ViewAlbumsActivity";
	
	static final String XTRA_FACEBOOKUSERID = "xtras.fb.uid";
	static final String XTRA_DATA = "xtras.albums";
	static final String XTRA_COVERS = "xtras.covers";
	static final String XTRA_USERS = "xtras.users";
	static final String XTRA_STARTINDEX = "xtras.startindex";
	static final String XTRA_NEXTSTARTINDEX = "xtras.nextstartindex";
	
	Facebook mFacebook;	
	StreamListViewFactory mListViewItemFactory;
	TextSwitcher mTopHeaderText;
	
	Handler mThreadUIHandler;	
	FBSession fbSession;
	
	Cursor mStreamsCursor;	
	GridView mAlbumsGrid;	
	TextToSpeech mTts;	
	View mLoadingIndicator;
	
	static WorkerManagerThread mWorkerThread;
	ProcessFriendsTask mProcessFriendsTask;
	//ProcessStreamTask mProcessStreamsTask;
		
	JSONArray mPhotosData;	
	Drawable mPlaceholderDrawable;
	
	static int nextStartIndex = 0; 
	static int startIndex = 0;
	static boolean hasNext = true;
	static boolean hasPrev = false;
	static final int NUM_ALBUMS_PER_REQUEST = 12;
	static final int NUM_THREADS_INIT = 1;
	static final int REQUEST_PHOTOVIEW = 0;
	
	static final byte MODE_TAGGED = 0; //photos that were tagged
	static final byte MODE_ALL = 1; //all photos belonging to a user
	
	byte mode = MODE_ALL;
	
	static CountDownLatch mThreadsInitCountdown;
	AlbumsAdapter mAlbumsAdapter;
		
	Handler mUIHandler;

	View.OnClickListener mNextOnClick;
	View.OnClickListener mPrevOnClick;
	Button mNextBtn;
	Button mPrevBtn;
	AdView mAdView;
	
	static JSONArray mAlbums;
	static JSONArray mCovers;
	HashMap<String,JSONObject> mCoversMap;
	
	static ProgressDialog mProgressDialog;
	
	static long mFbUserId;

	public static Intent getIntent(Context ctx){
		return new Intent(ctx, ViewAlbumsActivity.class);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);			
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setBackgroundDrawableResource(R.drawable.metal);
		setContentView(R.layout.viewalbums_activity);		
		bindViews();
		initObjects();
		initViews();	
		initData();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mFacebook.purgeInactiveOutHandlers(false);
		mFacebook = null;
		mUIHandler = null;
		mWorkerThread.informWaitOut();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mFacebook.setOutHandler(mWorkerThread.getInHandler());
		mWorkerThread.setOutHandler(mUIHandler);
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
		
		mAlbumsAdapter.setData(mAlbums,mCoversMap);
		mAlbumsAdapter.notifyDataSetChanged();
		mTopHeaderText.setText("Albums");
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);		
		Logger.l(Logger.DEBUG, LOG_TAG, "uid: "+mFbUserId);
		outState.putLong(XTRA_FACEBOOKUSERID, mFbUserId);
		outState.putString(XTRA_DATA, mAlbums.toString());
		outState.putString(XTRA_COVERS, mCovers.toString());
		outState.putInt(XTRA_STARTINDEX, startIndex);
		outState.putInt(XTRA_NEXTSTARTINDEX, nextStartIndex);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mFbUserId = savedInstanceState.getLong(XTRA_FACEBOOKUSERID, 0);
		startIndex = savedInstanceState.getInt(XTRA_STARTINDEX, 0);
		nextStartIndex = savedInstanceState.getInt(XTRA_NEXTSTARTINDEX, 0);
		try{			
			mAlbums = new JSONArray(savedInstanceState.getString(XTRA_DATA));
			mCovers = new JSONArray(savedInstanceState.getString(XTRA_COVERS));
		}catch(JSONException e){	
		}	
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {	
		super.onActivityResult(requestCode, resultCode, data);
		try{
			mFacebook.setOutHandler(mWorkerThread.mInHandler);
		}catch(Exception e){
			
		}
	}
			
	@Override
	protected void bindViews() {	
		super.bindViews();
		
		mLoadingIndicator = findViewById(R.id.loadingindicator);				
		mTopHeaderText = (TextSwitcher) findViewById(R.id.topheader);		
		mAlbumsGrid = (GridView) findViewById(R.id.albums_container);
		mNextBtn = (Button) findViewById(R.id.next);
		mPrevBtn = (Button) findViewById(R.id.prev);		
		mAdView = (AdView) findViewById(R.id.ad);
		
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
		mFacebook = Facebook.getInstance();
		
		mPlaceholderDrawable = mResources.getDrawable(R.drawable.mobook2_64);
		mAlbumsAdapter = new AlbumsAdapter();
		
		mNextOnClick = new View.OnClickListener() {				
			@Override
			public void onClick(View v) {
				if(hasNext){
					nextStartIndex = startIndex+NUM_ALBUMS_PER_REQUEST;
					nextStartIndex = Util.boundIndex(0,nextStartIndex,-1);
					getNextBatch();
				}
			}
		};
			
		mPrevOnClick = new View.OnClickListener() {					
			@Override
			public void onClick(View v) {
				if(hasPrev){
					nextStartIndex = startIndex-NUM_ALBUMS_PER_REQUEST;
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
						Toast.makeText(ViewAlbumsActivity.this, reason, 2000).show();
						break;
					}
					case ManagerThread.CALLBACK_PROCESS_WSRESPONSE_ERROR:{
						mProgressDialog.dismiss();
						FBWSResponse fbResponse = (FBWSResponse) msg.obj;					
						Toast.makeText(ViewAlbumsActivity.this,fbResponse.errorMessage, 1000).show();									
						break;
					}			
					case ManagerThread.CALLBACK_TIMEOUT_ERROR:{
						mProgressDialog.dismiss();
						Toast.makeText(ViewAlbumsActivity.this, "Processing streams timeout", 2000).show();
						//mProcessStreamsTask.cancel(true);
						break;
					}		
					case ManagerThread.MESSAGE_UPDATE_ALBUMS_GRID:{
						Logger.l(Logger.DEBUG,LOG_TAG," mUIHandler: "+this.toString());
						updatePagingButtons();
						Toast.makeText(ViewAlbumsActivity.this,"UPDATING", 3000).show();
						mAlbumsAdapter.setData(mAlbums, mCoversMap);		
						mAlbumsAdapter.notifyDataSetChanged();
						hideLoadingIndicator();
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
			};
		};
		Logger.l(Logger.DEBUG,"debug3","mUIHandler:"+mUIHandler.toString());
		mWorkerThread.mOutHandler = mUIHandler;
				
	}

	protected void initViews() {
			mAlbumsGrid.setAdapter(mAlbumsAdapter);
			mAlbumsGrid.setSelector(mResources.getDrawable(R.drawable.thumbnail_box_selector));
			mAlbumsGrid.setDrawSelectorOnTop(true);	
			mAlbumsGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long duration) {
					/*
					Logger.l(Logger.DEBUG,LOG_TAG,"position: "+position);
					Intent i = new Intent(ViewAlbumsActivity.this,ViewPhotosActivity.class);
					i.putExtra(Gallery.XTRA_STARTINDEX,position);
					i.putExtra(Gallery.XTRA_PHOTOS,mPhotos.toString());
					i.putExtra(Gallery.XTRA_USERS,mUsers.toString());
					startActivityForResult(i,REQUEST_PHOTOVIEW);
					*/			
				}
				
			});
			mProgressDialog = createProgressDialog();					
			mTopHeaderText.setFactory(new ViewFactory() {
				@Override
				public View makeView() {
					TextView t = new TextView(ViewAlbumsActivity.this);
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
	
	private void initData(){
		Logger.l(Logger.DEBUG, LOG_TAG, "[initData()]");
		Bundle extras = getIntent().getExtras();
		long uid  = extras.getLong(XTRA_FACEBOOKUSERID);
		String data = extras.getString(XTRA_DATA);
		Logger.l(Logger.DEBUG, LOG_TAG, "UID: "+uid);
		if(uid != mFbUserId){
			clearPagingInfo();
			clearData();
		}	
		if(data != null){
			parseData(data);		
		}	
		
		mFbUserId = uid;
	}

	private void clearData(){
		mAlbums = null;		
		mCovers = null;
		mCoversMap = null;
	}

	private void clearPagingInfo(){
		startIndex = 0;
		nextStartIndex = 0;
		hasNext = true;
		hasPrev = false;
	}

	/**
	 * Parse albums and photos data contained in the JSON string
	 * @param jsonString data containing album JSONArray and photos JSONArray
	 * @return the number of albums in the data
	 */
	@Override
	public int parseData(String jsonString) {
		Logger.l(Logger.DEBUG, LOG_TAG, "[parseData()] ");
		mProgressDialog.dismiss();
		
		if(jsonString == null){
			return -1;
		}
		
		JSONArray data;
		
		try{
			data = new JSONArray(jsonString);			
			mAlbums = data.getJSONObject(0).getJSONArray("fql_result_set");
			mCovers = data.getJSONObject(1).getJSONArray("fql_result_set");
		}catch(JSONException e){
			return -1;
		}			
	
		int numAlbums = 0; 
	
		try
		{
			numAlbums = mAlbums.length();
			
		}
		catch(Exception e){
			
		}
		
		processCovers();
		
		Logger.l(Logger.DEBUG, LOG_TAG, "[parseAlbumsson()] num albums: "+numAlbums);
		
		return numAlbums;
		
	}

	private void processCovers(){
		try{		
			mCoversMap = new HashMap<String, JSONObject>();
			for(int i=0,N=mCovers.length();i<N;i++){
				JSONObject photo = mCovers.getJSONObject(i);
				String pid = photo.getString(FBPhoto.fields_pid);
				mCoversMap.put(pid,photo);
			}
		}catch(JSONException e){			
		}
	}

	private void getNextBatch(){
		getAlbums();
	}
	
	private void getPrevBatch(){
		getAlbums();
	}
	

	private void getAlbums(){
		assert mFbUserId != 0;
		Logger.l(Logger.DEBUG, LOG_TAG, "FbUserId: "+mFbUserId);
		Bundle callbackData = new Bundle();
		callbackData.putString(BaseManagerThread.XTRA_CALLBACK_INTENT_ACTION, App.INTENT_GET_ALBUMS);
		mFacebook.getAlbums(R.id.outhandler_activity_viewalbums, callbackData, mFbUserId, NUM_ALBUMS_PER_REQUEST, nextStartIndex , ManagerThread.CALLBACK_GET_ALBUMS, BaseManagerThread.CALLBACK_SERVERCALL_ERROR,  BaseManagerThread.CALLBACK_TIMEOUT_ERROR, 0);		
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

	public void updatePaging(int numPhotos){
		startIndex  = nextStartIndex;
		Util.boundIndex(0, startIndex, -1);
		if(startIndex > 0){
			hasPrev = true;
		}else{
			hasPrev = false;
		}
				
		if(numPhotos > 0){
			if(numPhotos < NUM_ALBUMS_PER_REQUEST){
				hasNext = false;
			}else{
				hasNext = true;
			}
		}else{
			hasNext = false;
		}
	}
	
	private void updatePagingButtons(){
		mPrevBtn.setClickable(hasPrev);
		mNextBtn.setClickable(hasNext);
		mPrevBtn.setEnabled(hasPrev);
		mNextBtn.setEnabled(hasNext);
		mPrevBtn.setVisibility(hasPrev?View.VISIBLE:View.INVISIBLE);
		mNextBtn.setVisibility(hasNext?View.VISIBLE:View.INVISIBLE);
		
	}
	

	
	public static class WorkerManagerThread extends BaseManagerThread {
		
		public Handler mInHandler = new Handler();
		public Handler mOutHandler = new Handler();
		IAlbums mActivity;
		
		public WorkerManagerThread(CountDownLatch cdl, IAlbums act) {
			super(cdl);
			mActivity = act;
		}
		
		public void setActivity(ViewAlbumsActivity act){
			mActivity = act;
		}
		
		@Override
		public void doBusiness(Bundle data, int code, FBWSResponse fbresponse) {
		
					switch(code){
					
					case ManagerThread.CALLBACK_GET_ALBUMS:{
						String parsed;
						try {							
							parsed = fbresponse.jsonArray.toString(2);
							Logger.l(Logger.DEBUG, LOG_TAG,"[WorkerManagerThread] [handleMessage()] [callback_get_albums] json data: "+ parsed);	
							if(fbresponse.hasErrorCode){								
							}else{
								int numPhotos = mActivity.parseData(fbresponse.data);
								
								mActivity.updatePaging(numPhotos);								
								Logger.l(Logger.DEBUG,LOG_TAG,"[WorkerManagerThread] [handleMessage()] [callback_get_albums] sending message to ui handler.");
								if(waitOutHandler()){
									mOutHandler.sendEmptyMessage(ManagerThread.MESSAGE_UPDATE_ALBUMS_GRID);
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

	
	 public class AlbumsAdapter extends BaseAdapter {
		 
		 JSONArray albums;
		 HashMap<String,JSONObject> covers;
		 
		 static final String LOG_TAG = "AlbumsGridAdapter";
		 public void setData(JSONArray data, HashMap<String,JSONObject> covers){
			 this.albums = data;
			 this.covers = covers;
		 }
		 
		 public String IMAGESRCFIELD = FBPhoto.fields_src;
		 
	        public AlbumsAdapter() {
	        }

	        public class Holder {		
	    		public ImageView thumbnailImage;
	    		public String thumbnailImageUrl;	    		
	    		public int position; //cancel asynctask if the recycled row is used as a new row
	    	}
	        
	        public View getView(int position, View convertView, ViewGroup parent) {	            
	        	Logger.l(Logger.DEBUG,LOG_TAG,"[getView()] position:"+position+", thread:"+Thread.currentThread().getName());
	            if (convertView == null) {
	            	convertView = mLayoutInflater.inflate(R.layout.t_album,parent, false);
	            	
	            	GridView.LayoutParams layout = new GridView.LayoutParams(GridView.LayoutParams.WRAP_CONTENT, GridView.LayoutParams.WRAP_CONTENT);
	                convertView.setLayoutParams(layout);
	            } 
	            
	        	Holder tag = (Holder) convertView.getTag();

	    		//bind child views and store in tag
	    		if (tag == null) {
	    			tag = new Holder();
	    			tag.thumbnailImage = (ImageView) convertView.findViewById(R.id.thumbnail);	    				
	    		}	    		
	    		
	    		
	            String thumbnailImageSrc = "";
	            try {
					JSONObject album = albums.getJSONObject(position);
					String cover_pid = album.getString(FBAlbum.fields_cover_pid);
					JSONObject photo = covers.get(cover_pid);
					if(photo!=null){
						thumbnailImageSrc = photo.getString(FBPhoto.fields_src);
						tag.thumbnailImageUrl = thumbnailImageSrc;
					}else{
						tag.thumbnailImageUrl = null;
					}					
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
				
				Logger.l(Logger.DEBUG,LOG_TAG,"[getView()] loading: "+thumbnailImageSrc);
				
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
	        	if(albums == null){
	        		return 0 ;
	        	}
	        	return albums.length();
	        }

	        public final Object getItem(int position) {
	        	if(albums == null){
	        		return 0 ;
	        	}
	        	try {
					return albums.getJSONObject(position);
				} catch (JSONException e) {
				
				}
				return null;
	        }

	        public final long getItemId(int position) {
	        	if(albums == null){
	        		return 0 ;
	        	}
	        	return position;
	        }
	    }
	
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
				mAlbumsAdapter.notifyDataSetChanged();
			}			
		}
		
		@Override
		public void onCancelled() {
						
		}
	}; 
	
}
