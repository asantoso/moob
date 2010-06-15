package com.neusou.moobook.activity;

import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.FeatureInfo;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ViewSwitcher.ViewFactory;

import com.admob.android.ads.AdView;
import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.AppMenu;
import com.neusou.moobook.FBApp;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;

import com.neusou.moobook.adapters.CommentsAdapter;
import com.neusou.moobook.adapters.GenericPageableAdapter;
import com.neusou.moobook.adapters.IPageableListener;
import com.neusou.moobook.data.PageableJsonData;
import com.neusou.moobook.model.RemoteCallResult;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;
import com.neusou.moobook.view.ProfileOnMenuItemClickListener;
import com.neusou.web.IntelligentPagingInfo;
import com.neusou.web.PagingInfo;

public class ViewCommentsActivity extends BaseActivity{
	
	public static Intent getIntent(Context caller){
		Intent i = new Intent(caller, ViewCommentsActivity.class);
		return i;
	}
	
	static final String LOG_TAG = "ViewCommentsActivity";
	public static final String XTRA_OBJECTID = "xtra.oid";
	public static final String XTRA_POSTID = "xtra.pid";
	public static final String XTRA_CLEARDATA = "xtra.cleardata";
	
	static final int UIMETHOD_RELOADCOMMENTS = 2;
	
	static final int UICALLBACK_ADMOB_ONRECEIVE = 50;
	static final int UICALLBACK_ADMOB_ONFAILRECEIVE = 51;
	
	static final byte DIALOG_POSTINGCOMMENT = 2;
	static final byte DIALOG_DELETINGCOMMENT = 3;
	
	static final byte MENUITEM_REFRESH = 0;
	static final byte MENUITEM_DELETE = 1;
	static final byte MENUITEM_ACTORNAME = 2;

	static final byte MAX_LINKS = 5;
	static final int mToastLength = 4000;	
	
	//static final int MESSAGE_COMMENTS_POSTED = 2;
	//static final int MESSAGE_COMMENTS_DELETED = 3;
		
	
	static final int numThreadInitializations = 1;	
	
	String LABEL_HEADER_COMMENTS;
	ManagerThread mWorkerThread;	
	
	//Temporary data stores
	static PageableJsonData data_comments = null;
	static JSONArray data_users = null;
	static JSONArray data_comments_info = null;
	//
	
	static volatile PagingInfo mPagingInfo = new PagingInfo(0);
	static String mLblCommentPosted ;
	static String mLblCommentDeleted ;	
	
	static CountDownLatch threadsInitCountDown;	
	
	View mLoadingIndicator;
	
	String mLblRefresh;
	String mLblDelete;
	String mLblActorName;
	
	String mLblLoadingComments;
	String mLblPostingComment;
	String mLblDeletingComment;	

	ApplicationDBHelper mDBHelper;
	SQLiteDatabase mDB;	
	Facebook mFacebook;
	
	CommentsAdapter.DataTag mLongItemClickData;	
	int commentStartIndex = 0;
	
	//Views
	EditText mComment;
	Button mPostBtn;
	AdView mAdView;
	ListView mListView;
	CommentsAdapter mListAdapter;
	//PageableAdapter mListAdapter;
	View mStub;			
	TextSwitcher mTopHeaderText;
	ProgressDialog mProgressDialog;
	View.OnClickListener mPostOnClickLst;
	IPageableListener mAdapterListener;
	
	//used to check if the arriving batch of data is different than the previous batch.
	//2 comments from the fresh batch is compared against the 2 comments from the old batch
	//to determine whether the fresh batch data is different.
	int lrctime0; //posted time of 1st comment retrieved from last call 
	int lrctime1; //posted time of 2nd comment retrieved from last call
	
	boolean mIsAsyncLoadingFinished = true;
		
	public static final int DEFAULT_PAGING_WINDOW_SIZE = 5;


	static boolean mHasObjectId ; //true if EXTRA contains objectId data  
	static boolean mHasPostId ;//true if EXTRA contains postId data
	static String mPostId = "";
	static String mObjectId = "";
	boolean isClearData;
	String xtraObjectId = "";
	String xtraPostId = "";		
	
	LayoutAnimationController mListAnimation; 
	
	BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			String action = intent.getAction();
			if(action.equals(App.INTENT_DELETECOMMENT)){
				Bundle b = intent.getExtras();
				RemoteCallResult remoteCallResult = (RemoteCallResult) b.getParcelable(RemoteCallResult.XTRA_PARCELABLE_OBJECT);
				if(remoteCallResult.status){
					getCommentsFromCloud(0);				
				}
				dismissDialog(DIALOG_DELETINGCOMMENT);
				hideLoadingIndicator();
				resetHeaderText();
			}			
			else if(action.equals(App.INTENT_POSTCOMMENT)){
				Bundle b = intent.getExtras();
				RemoteCallResult remoteCallResult = (RemoteCallResult) b.getParcelable(RemoteCallResult.XTRA_PARCELABLE_OBJECT);
				if(remoteCallResult.status){
					getCommentsFromCloud(0);				
				}				
				dismissDialog(DIALOG_POSTINGCOMMENT);
				hideLoadingIndicator();
				resetHeaderText();
			}
			else if(action.equals(App.INTENT_GET_TAGGED_PHOTOS)){
				Bundle extras = intent.getExtras();
				FBWSResponse fbresponse = (FBWSResponse) extras.getParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT);										
				try{
					String parsed = fbresponse.jsonArray.toString(2);
					Logger.l(Logger.DEBUG,  LOG_TAG,"[callback_get_tagged_photos]: "+ parsed); 
					Intent i = new Intent(App.INSTANCE,ViewPhotosActivity.class);
					i.putExtra(ViewPhotosActivity.XTRA_PHOTOS, fbresponse.data);						
					i.putExtra(ViewPhotosActivity.XTRA_FACEBOOKUSERID, extras.getLong(Facebook.param_uid,0));
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT); 
					mUIHandler.sendEmptyMessage(ManagerThread.MESSAGE_DISMISS_DIALOG);
					mProgressDialog.dismiss();
					App.INSTANCE.startActivity(i);
				}catch(JSONException e){
				}
				
			}
		}
		
	};
	
	ManagerThread.IManagerResult mManagerListener = new ManagerThread.IManagerResult() {
		
		@Override
		public void setUsersData(JSONArray data) {
			data_users = data;			
		}
		
		@Override
		public void setPagingInfoData(PagingInfo data) {
			mPagingInfo = data;		
		}
		
		@Override
		public void setCommentsInfoData(JSONArray data) {
			data_comments_info = data;			
		}
		
		@Override
		public void setCommentsData(JSONArray data) {
			data_comments.set(data);			
		}
		
		public PagingInfo getPagingInfoData(){
			return mPagingInfo;
		}
		
		public boolean hasObjectId(){
			return mHasObjectId;
		}
		
		public boolean hasPostId(){
			return mHasPostId;
		}
		
		public String getObjectId(){
			return mObjectId;
		}
		
		public String getPostId(){
			return mPostId;
		}
		
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setBackgroundDrawable(mResources.getDrawable(R.drawable.metal));
		setContentView(R.layout.comments_activity);		
		bindViews();
		getExtras();
		initObjects();
		initViews();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDB.close();
		mDBHelper.close();		
		mFacebook.purgeInactiveOutHandlers(false);
		mFacebook = null;	
		System.gc();	
	}

	@Override
	protected void onStart() {
		super.onStart();		
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		mFacebook.registerOutHandler(R.id.outhandler_activity_viewcomments, mWorkerThread.getInHandler());
		mWorkerThread.setListener(mManagerListener);
		mWorkerThread.setOutHandler(mUIHandler);		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(App.INTENT_DELETECOMMENT);		
		intentFilter.addAction(App.INTENT_POSTCOMMENT);
		intentFilter.addAction(App.INTENT_GET_TAGGED_PHOTOS);
		registerReceiver(mBroadcastReceiver, intentFilter);		
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(mBroadcastReceiver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		getCommentsFromCloud(PagingInfo.CURRENT);
		updateList();
	}

	@Override
	protected void onRestart() {
		super.onRestart();		
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	protected void bindViews(){
		mAdView = (AdView) findViewById(R.id.ad);
		mLoadingIndicator = findViewById(R.id.loadingindicator);
		App.initAdView(mAdView, mAdViewHandler);		
		
		mStub = findViewById(R.id.stub);
		mListView = (ListView) findViewById(R.id.list);		
		mTopHeaderText = (TextSwitcher) findViewById(R.id.topheader);
		mPostBtn = (Button) findViewById(R.id.post);
		mComment = (EditText) findViewById(R.id.comment_input);		
	}
		
	
	
	protected void getExtras(){		
		//get intent extra parameters
		Intent i = getIntent();
		mHasObjectId = i.hasExtra(XTRA_OBJECTID);
		mHasPostId = i.hasExtra(XTRA_POSTID);
		isClearData = getIntent().getBooleanExtra(XTRA_CLEARDATA, false);
		
		if(mHasObjectId){
			xtraObjectId= i.getStringExtra(XTRA_OBJECTID);
		}else if(mHasPostId){
			xtraPostId = i.getStringExtra(XTRA_POSTID);			
		}		
			
		Logger.l(Logger.DEBUG,LOG_TAG,"[initObjects()] clearData?:"+isClearData+" postId:"+mPostId);
		
	}
	
	protected void initObjects(){
		super.initObjects();
	
		LABEL_HEADER_COMMENTS = mResources.getString(R.string.comments);
		
		try{			
			if(mHasObjectId){		
				if(isClearData){
					if(xtraObjectId != null && !xtraObjectId.equals(mObjectId)){						
						clearAllData();
					}
				}
					
			}else if(mHasPostId){				
				if(isClearData){
					if(xtraPostId != null && !xtraPostId.equals(mPostId)){
						clearAllData();
					}						
				}					
			}
			Logger.l(Logger.DEBUG,LOG_TAG,"[initObjects()] different postId, clearing comments data.");
		}catch(Exception e){	
		}		
		
		mObjectId = xtraObjectId;
		mPostId = xtraPostId;
		
		if(data_comments == null){
			Logger.l(Logger.DEBUG,LOG_TAG,"[initObjects()] data comments is null. creating new.");
			data_comments = new PageableJsonData();
		}
		
		mWorkerThread = App.INSTANCE.mManagerThread;
		
		//end of static initializations
		
		mListAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_row_left_slide);
		
		mFacebook = Facebook.getInstance();
		
		mDBHelper = new ApplicationDBHelper(this);
		mDB = mDBHelper.getReadableDatabase();
	
		mListAdapter = new CommentsAdapter(this);
		mListAdapter.setListener(mAdapterListener);		
		
		// Initialize listeners
		
		mPostOnClickLst = new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				postComment();								
			}
		};
		
		mProgressDialog = new ProgressDialog(this);				
		
		mLblRefresh = mResources.getString(R.string.refresh);
		mLblDelete = mResources.getString(R.string.delete);
		mLblPostingComment = mResources.getString(R.string.postingcomment);
		mLblDeletingComment = mResources.getString(R.string.deletingcomment);
		mLblLoadingComments = mResources.getString(R.string.loadingcomments);
				
		mLblCommentPosted = mResources.getString(R.string.commentposted);
		mLblCommentDeleted = mResources.getString(R.string.commentdeleted);
		
		mAdapterListener = new IPageableListener() {			
			@Override
			public void onHasNext() {
				Logger.l(Logger.DEBUG, LOG_TAG, "[CommentsAdapter.Listener] onHasMoreData()");
				//getCommentsFromCloud(-1);
			}
			
			@Override
			public void onHasPrev() {
				Logger.l(Logger.DEBUG, LOG_TAG, "[CommentsAdapter.Listener] onHasLessData()");
				//getCommentsFromCloud(1);
			}

			@Override
			public void onGetNext() {
				Logger.l(Logger.DEBUG, LOG_TAG, "[CommentsAdapter.Listener] onGetNext()");
				mListAdapter.onStartLoadingNext();
				getCommentsFromCloud(PagingInfo.NEXT);				
			}

			@Override
			public void onGetPrev() {
				Logger.l(Logger.DEBUG, LOG_TAG, "[CommentsAdapter.Listener] onGetPrev()");
				mListAdapter.onStartLoadingPrev();
				getCommentsFromCloud(PagingInfo.PREV);
			}
			
		};
	
		
	}	
	

	protected void initViews(){
		mPostBtn.setOnClickListener(mPostOnClickLst);
		//init comments list
		
		mListAdapter.setPagingInfo(mPagingInfo);
		
		mListView.setLayoutAnimation(mListAnimation);
		mListView.setAdapter(mListAdapter);		
		registerForContextMenu(mListView);
		mListView.setFocusableInTouchMode(true);
		mListView.setFocusable(true);
		mListView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
		mListView.setDrawingCacheBackgroundColor(Color.TRANSPARENT);		
		mListView.setAnimationCacheEnabled(true);
		mListView.setAlwaysDrawnWithCacheEnabled(true);
		mListView.setBackgroundColor(Color.TRANSPARENT);
		mListView.setCacheColorHint(Color.TRANSPARENT);
		mListView.setSmoothScrollbarEnabled(false);
		mListView.setDrawSelectorOnTop(false);
		mListView.setSmoothScrollbarEnabled(true);
		mListView.setScrollContainer(false);
		mListView.setClickable(true);
		Drawable stateListDrawable = mListView.getSelector();
		stateListDrawable.setColorFilter(App.mColorFilterBlueish);	
		try {
			mListView.setDivider(mResources	.getDrawable(android.R.drawable.divider_horizontal_bright));		
		} catch (Exception e) {

		}		
		//mListView.setItemsCanFocus(true);
		mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
		{
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0,
				View v, int position, long arg3) {
			Log.d("debug","onItemLongClick "+position);
			Log.d("debug","onItemLongClick "+v.getClass().getCanonicalName());			
			mLongItemClickData = (CommentsAdapter.DataTag) v.getTag(CommentsAdapter.TAG_ITEM_DATA);			
			return false;//return false to pass on the event, so that context menu gets displayed
		}	
		
		});
		
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				GenericPageableAdapter.InternalTag internalTag = (GenericPageableAdapter.InternalTag) arg1.getTag(GenericPageableAdapter.TAG_INTERNAL);
				if(internalTag != null){
					if(internalTag.viewType == CommentsAdapter.VIEWTYPE_LOADER){
						int direction = internalTag.loadDirection;
						if(direction == PagingInfo.NEXT ){
							if(mIsAsyncLoadingFinished){
								mListAdapter.onStartLoadingNext(); 
								getCommentsFromCloud(PagingInfo.NEXT);
							}
						}
						else if(direction == PagingInfo.PREV){
							if(mIsAsyncLoadingFinished){
								mListAdapter.onStartLoadingPrev();
								getCommentsFromCloud(PagingInfo.PREV);
							}
						}
					}
				}
			}
			
		});
		
		
		
		mTopHeaderText.setFactory(new ViewFactory() {
			@Override
			public View makeView() {				
				TextView t;// = new TextView(ViewCommentsActivity.this);
				t = (TextView) (mLayoutInflater.inflate(R.layout.t_topheadertext, null));
				return t;
			}
		});

	}
		
	
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
	
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.clearHeader();
		if(mLongItemClickData == null){
			return;
		}
		String comment = mLongItemClickData.comment;
		Matcher matcher = App.mUrlPattern.matcher(comment);	
		String[] links = new String[MAX_LINKS];
		byte numLinks = 0;
		
		while(matcher.find()){
			try{
			numLinks++;
			 if(numLinks >= MAX_LINKS){
				 break;
			 }
			 
			String url = matcher.group();
			links[numLinks] = url; 
			MenuItem urlMI = menu.add("View "+url);
			Intent viewUrl = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
			urlMI.setIntent(viewUrl);
			urlMI.setOnMenuItemClickListener(
					new OnMenuItemClickListener(){

						@Override
						public boolean onMenuItemClick(MenuItem item) {
							item.getIntent();
							try{
								startActivity(item.getIntent());
							}catch(ActivityNotFoundException e){
								Toast.makeText(ViewCommentsActivity.this, "Invalid link", 2000).show();
							}
							return true;
						}
						
			});
			Log.d("debug","matched group: "+url);	
			}
			catch(Exception e){
				
			}
							
		}
				
		
		if(mLongItemClickData.fromid > 0 && mLongItemClickData.fromid == mFacebook.getSession().uid){
			MenuItem deleteComment = menu.add(0,MENUITEM_DELETE,0,mLblDelete);
			deleteComment.setOnMenuItemClickListener(mMenuItemClickListener);	
		}
		
		//mLblActorName = mLongItemClickData.name; 
		//MenuItem actorMenuItem = menu.add(0, MENUITEM_ACTORNAME, 0, mLblActorName);
		//actorMenuItem.setOnMenuItemClickListener(mMenuItemClickListener);
			
		mProgressDialog.setTitle(null);
		mProgressDialog.setMessage(App.INSTANCE.mResources.getString(R.string.getting_tagged_photos));
		
		OnMenuItemClickListener mContextMenuItemClickListener = 
			new ProfileOnMenuItemClickListener(
				mLblActorName,
				mLongItemClickData.imageUri, 
				R.id.outhandler_activity_streams, 
				mLongItemClickData.fromid, 
				mProgressDialog, 
				ViewCommentsActivity.this);
		
		AppMenu.createActorMenu(
				menu,
				mContextMenuItemClickListener,
				mLongItemClickData.name,
				mLongItemClickData.imageUri,
				R.id.outhandler_activity_viewcomments, 
				mLongItemClickData.fromid, 
				ViewCommentsActivity.this);
		
	}
	
	OnMenuItemClickListener mMenuItemClickListener = new OnMenuItemClickListener() {
		
		@Override
		public boolean onMenuItemClick(MenuItem item) {
			int id = item.getItemId();
			switch(id){
				case MENUITEM_ACTORNAME:{
					//Intent i = ViewProfileActivity.getIntent(ViewCommentsActivity.this);
					//startActivity(i);
					break;
				}
				case MENUITEM_DELETE:{
					String comment_id = mLongItemClickData.comment_id;
					deleteComment(comment_id);
					return false;
				}
			}
			return false;
		}
		
	};
	
	@Override
	protected Dialog onCreateDialog(int id) {		
		return mProgressDialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);	
		ProgressDialog d = (ProgressDialog)dialog;
		
		switch(id){
	
		case DIALOG_DELETINGCOMMENT:{
			d.setMessage(mLblDeletingComment);    	
			d.setCancelable(true);
			break;
		}
		case DIALOG_POSTINGCOMMENT:{
			d.setMessage(mLblPostingComment);
			d.setCancelable(true);
			break;
		}
		}
			    
	}

	private void updateList(){
		Logger.l(Logger.DEBUG,LOG_TAG,"[updateList()]");
		
		mListAdapter.onFinishedLoading();
		mListAdapter.parseUsersJsonData(data_users);
		mListAdapter.setPagingInfo(mPagingInfo);					
		mListAdapter.setData(data_comments);									
		mListAdapter.notifyDataSetChanged();
	}

	private void clearAllData(){
		Logger.l(Logger.DEBUG,LOG_TAG,"[clearAllData()]");
		data_users = null;
		data_comments.clear();
		data_comments_info = null;
		mPagingInfo.clear();
	}
		
	private void showLoadingIndicator(){	
		mLoadingIndicator.setVisibility(View.VISIBLE);
		setProgressBarIndeterminateVisibility(true);
	}
	
	private void hideLoadingIndicator(){
		mLoadingIndicator.setVisibility(View.INVISIBLE);
		setProgressBarIndeterminateVisibility(false);
	}
	
	private void onStartFetchingCommentsFromCloud(){
		mIsAsyncLoadingFinished = false;
		showLoadingIndicator();
		setTitle("moobook");
		mTopHeaderText.setText("Loading comments from cloud..");	 
	}

	private void onFinishFetchingCommentsFromCloud(){
		mIsAsyncLoadingFinished = true;
		hideLoadingIndicator();
		resetHeaderText();
	}

	private void setHeaderText(String text){
		mTopHeaderText.setText(text);
	}
	
	private void resetHeaderText(){
		
		if(mPagingInfo != null && mPagingInfo.totalCount >= 0){
			setHeaderText(LABEL_HEADER_COMMENTS+" " +
				" ("+mPagingInfo.totalCount+")");
		}
		else{
			setHeaderText(LABEL_HEADER_COMMENTS);		
		}
	}
		
	private void getCommentsFromCloud(int direction){
		if(!mIsAsyncLoadingFinished){
			return;
		}
		Logger.l(Logger.DEBUG,LOG_TAG,"getCommentsFromCloud() "+direction);
		onStartFetchingCommentsFromCloud();
		/*
		onStartUpdatingComments();		
		int numComments = 0;				
		if(data_comments != null){
			numComments = data_comments.size();
		}		
		//Logger.l(Logger.DEBUG,LOG_TAG,"getCommentsFromCloud() current local comments count: "+numComments);
		*/
		mPagingInfo.compute(direction); 
		Logger.l(Logger.DEBUG,LOG_TAG,"getCommentsFromCloud() paging window size: "+ mPagingInfo.windowSize+", start:"+mPagingInfo.getNextStart());		
		Logger.l(Logger.DEBUG,LOG_TAG,"getCommentsFromCloud() hasObjectId?"+mHasObjectId+" object_id: "+mObjectId+", hasPostId?"+mHasPostId+" post_id:"+mPostId);
				
		mFacebook.getPostComments(R.id.outhandler_activity_viewcomments, mHasPostId?Facebook.COMMENT_TYPE_STREAMPOSTS:Facebook.COMMENT_TYPE_OTHERS, mHasObjectId?mObjectId:null, mHasPostId?mPostId:null, ManagerThread.CALLBACK_GET_COMMENTS_MULTIQUERY, BaseManagerThread.CALLBACK_SERVERCALL_ERROR,  BaseManagerThread.CALLBACK_TIMEOUT_ERROR, mPagingInfo.windowSize,mPagingInfo.getNextStart());		
	}
	

	private void postComment(){
		showDialog(DIALOG_POSTINGCOMMENT);
		String comment = mComment.getText().toString(); 
		Bundle callbackDescriptor = new Bundle();
		callbackDescriptor.putString(BaseManagerThread.XTRA_CALLBACK_INTENT_ACTION, App.INTENT_POSTCOMMENT);
		mFacebook.postComment(R.id.outhandler_activity_viewcomments, callbackDescriptor, comment, mPostId, ManagerThread.CALLBACK_POSTCOMMENT, BaseManagerThread.CALLBACK_SERVERCALL_ERROR, BaseManagerThread.CALLBACK_TIMEOUT_ERROR);
	}
		
	private void deleteComment(String comment_id){
		showDialog(DIALOG_DELETINGCOMMENT);
		Bundle callbackData = new Bundle();
		callbackData.putString(BaseManagerThread.XTRA_CALLBACK_INTENT_ACTION, App.INTENT_DELETECOMMENT);
		mFacebook.deleteComment(R.id.outhandler_activity_viewcomments, callbackData, comment_id, ManagerThread.CALLBACK_DELETECOMMENT, BaseManagerThread.CALLBACK_SERVERCALL_ERROR, BaseManagerThread.CALLBACK_TIMEOUT_ERROR);
	}

	/*
	private boolean getCanPostComment(JSONObject data){
		try {
			return data.getBoolean("can_post");
		} catch (JSONException e) {
			return false;
		}			
	}

	private boolean getCanRemoveComment(JSONObject data){
		 try {
				return data.getBoolean("can_remove");
		} catch (JSONException e) {
			return false;
		}
	}
	*/

	
/*
	private void processCommentsInfo(){
		if(data_comments_info != null){
			try{								
				JSONObject obj = data_comments_info.getJSONObject(0);								
				JSONObject commentsObj = obj.getJSONObject("comments");
				
				Stream stream = new Stream();
				if (commentsObj != null) {					
					stream.comments_can_post = getCanPostComment(commentsObj);
					stream.comments_can_remove = getCanRemoveComment(commentsObj);
					stream.comments_count = getReturnedCommentsCount(commentsObj);
					stream.post_id = mPostId;
				}
				
				if(mDB.isOpen()){
					Log.d(LOG_TAG,"updating stream comments info");
					mDBHelper.updateStreamCommentsInfo(stream, mDB);	
				}
				
			}
			catch(JSONException e){
				
			}
			}
	}*/

	

	Handler mUIHandler = new BaseUiHandler(this){
		
		public void handleMessage(android.os.Message msg) {
			super.handleMessage(msg);
			
			//Log.d(LOG_TAG,"ui hander handle message what:"+msg.what);
			int code = msg.what;
			Bundle data = msg.getData();
		
			switch(code){
				case ManagerThread.MESSAGE_DISMISS_DIALOG:{
					mProgressDialog.dismiss();
					break;
				}
			
				case ManagerThread.MESSAGE_UPDATELIST:{
					Logger.l(Logger.DEBUG,LOG_TAG,"[Handler()] [handleMessage()] update list");
					mListAdapter.onFinishedLoading();
					mListAdapter.parseUsersJsonData(data_users);
					mListAdapter.setPagingInfo(mPagingInfo);					
					mListAdapter.setData(data_comments);	
					mListAdapter.notifyDataSetChanged();
					onFinishFetchingCommentsFromCloud();	
					break;
				}
				
				case UICALLBACK_ADMOB_ONFAILRECEIVE:{					
					mAdView.setVisibility(View.GONE);
					break;
				}
				
				case UICALLBACK_ADMOB_ONRECEIVE:{
					mAdView.setVisibility(View.VISIBLE);
					break;
				}
			/*
				case BaseManagerThread.CALLBACK_TIMEOUT_ERROR:{
					Toast.makeText(ViewCommentsActivity.this, "Request to Facebook timed out", 2000).show();
		
					break;
				}
				
				case BaseManagerThread.CALLBACK_SERVERCALL_ERROR:{					
					String reason = (String)data.getString(Facebook.XTRA_SERVERCALL_ERROR_MSG);
					String errorCode = ""; 
					if(data.containsKey(Facebook.XTRA_SERVERCALL_ERROR_CODE)){
						errorCode = Integer.toString(data.getInt(Facebook.XTRA_SERVERCALL_ERROR_CODE));
						errorCode+=":";
					}
					Toast.makeText(ViewCommentsActivity.this, errorCode+reason, 2000).show();			
				
					break;
				}
				*/
				case UIMETHOD_RELOADCOMMENTS:{					
					getCommentsFromCloud(1);
					break;
				}
				
				case BaseManagerThread.CALLBACK_PROCESS_WSRESPONSE_ERROR:{					
					FBWSResponse fbResponse = (FBWSResponse) data.getParcelable(Facebook.XTRA_RESPONSE);					
					Toast.makeText(ViewCommentsActivity.this,fbResponse.errorMessage, 1000).show();
					onFinishFetchingCommentsFromCloud();
					mProgressDialog.dismiss();
					mListAdapter.onFinishedLoading();
					break;
				}			
					
			}
		}

		@Override
		public void onServerCallError() {
			onFinishFetchingCommentsFromCloud();
			mProgressDialog.dismiss();
			mListAdapter.onFinishedLoading();
		}

		@Override
		public void onTimeoutError() {
			onFinishFetchingCommentsFromCloud();
			mListAdapter.onFinishedLoading();
			mProgressDialog.dismiss();
		};
	};	
		
	Handler mAdViewHandler = new Handler(){
		public void handleMessage(Message msg) {
			int code = msg.what;
			switch(code){
				case UICALLBACK_ADMOB_ONRECEIVE:{
					mAdView.setVisibility(View.VISIBLE);
					break;
				}
				case UICALLBACK_ADMOB_ONFAILRECEIVE:{
					mAdView.setVisibility(View.INVISIBLE);	
					break;
				}
			}
			
		};
	};
	
	
		
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {				
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENUITEM_REFRESH, 0, mLblRefresh);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		byte id = (byte) item.getItemId();
		switch(id){
			case MENUITEM_REFRESH:{				
				mPagingInfo.clear();
				getCommentsFromCloud(PagingInfo.CURRENT);
				break;
			}			
		}
		return super.onOptionsItemSelected(item);
	}
	
	Runnable mShowCommentsPosted = new Runnable(){
		public void run() {
			Toast.makeText(ViewCommentsActivity.this, mLblCommentPosted, mToastLength).show();
		}
	};
	
	Runnable mShowToastCommentsDeleted = new Runnable(){
		public void run() {
			Toast.makeText(ViewCommentsActivity.this, mLblCommentDeleted, mToastLength).show();	
		}
	};
	
}
