package com.neusou.moobook.activity;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;

import org.json.JSONException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import com.admob.android.ads.AdManager;
import com.admob.android.ads.AdView;
import com.neusou.Logger;
import com.neusou.async.UserTask.Status;
import com.neusou.moobook.App;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.Prefs;
import com.neusou.moobook.R;
import com.neusou.moobook.controller.MyExpandableListAdapter;
import com.neusou.moobook.controller.StreamListViewFactory;
import com.neusou.moobook.controller.StreamListViewFactory.GroupData;
import com.neusou.moobook.data.Attachment;
import com.neusou.moobook.data.AttachmentMedia;
import com.neusou.moobook.data.ProcessedData;
import com.neusou.moobook.data.Stream;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.moobook.task.ProcessStreamMultiQueryTask;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;

public class StreamActivity extends BaseActivity {

	static final String LOG_TAG = "StreamActivity"; 

	static CountDownLatch mThreadsInitCountdown;
	static final int mNumThreadsInitialization = 1;

	static final byte CONTEXT_MENUITEM_DO_NOTHING = -1;
	static final byte CONTEXT_MENUITEM_LIKE = 0;
	static final byte CONTEXT_MENUITEM_UNLIKE = 1;
	static final byte CONTEXT_MENUITEM_VIEW_LINKS = 6;
	static final byte CONTEXT_MENUITEM_VIEW_COMMENTS = 3;
	static final byte CONTEXT_MENUITEM_VIEW_PROFILE = 4;
	static final byte CONTEXT_MENUITEM_VIEW_PICTURES = 5;
	static final byte CONTEXT_MENUITEM_POST_COMMENTS = 7;
	static final byte CONTEXT_MENUITEM_VIEW_TAGGED_PHOTOS = 8;
	static final byte CONTEXT_MENUITEM_BAN_APP = 9;

	static final byte MENUITEM_TEST_GETPROFILE = 1;
	static final byte MENUITEM_REFRESH_STREAM = 2;
	static final byte MENUITEM_CLEAR_STREAM = 3;
	static final byte MENUITEM_CHAT = 4;
	static final byte MENUITEM_CLEARALL = 5;
	static final byte MENUITEM_TOGGLE_MODE = 6;

	static final int CALLBACK_ADMOB_ONRECEIVE = 50;
	static final int CALLBACK_ADMOB_ONFAILRECEIVE = 51;

	static final int CALLBACK_PROCESS_WSRESPONSE_HAS_ERRORCODE = 130;

	static final String XTRA_ONRESUME_FORCESTREAMSUPDATE = App.packageprefix
			+ ".xtra.onresume.autorefresh";
	static final String XTRA_DIRTYPOSTS = App.packageprefix
			+ ".xtra.dirty.posts.ids";
	static final String XTRA_STREAMMODE = App.packageprefix
			+ ".xtra.stream.mode";

	static final byte NUM_TASKS = 2;
	static final byte TASK_GETSTREAMS = 0;
	static final byte TASK_GETFRIENDS = 1;
	boolean tasks[] = new boolean[NUM_TASKS];
	short tasksMax[] = new short[] { 1, 1 };

	byte mStreamMode = Facebook.STREAMMODE_LIVEFEED;
	Facebook mFacebook;
	ExpandableListView mListView;
	StreamListViewFactory mListViewFactory;
	MyExpandableListAdapter mListViewAdapter;
	OnMenuItemClickListener mContextMenuItemListener;
	TextSwitcher mTopHeaderText;
	View mLoadingIndicator;
	AdView mAdView;
	boolean isStreamFiltered = false;
	byte mFilterType = 0;
	final byte FILTER_FRIENDLIST = 1;
	final byte FILTER_NAME = 2;
	Handler mThreadUIHandler;
	static Cursor mStreamsCursor;
	StreamListViewFactory.GroupData longClickedItemData;
	BroadcastReceiver mBroadcastReceiver;
	PendingIntent mAutoUpdateStreamsPendingIntent;
	Intent mAutoUpdateStreamsIntent;

	static boolean mIsStreamsUpdateFinished = true;
	static final String INTENT_ACTION_SET_DIRTY_POSTS = "intent.action.set.dirty.posts";
	static final IntentFilter mUpdateDirtyRowsIF = new IntentFilter(INTENT_ACTION_SET_DIRTY_POSTS);
	static ProcessStreamMultiQueryTask mProcessStreamMultiQueryDataTask;
	static ManagerThread mWorkerThread;

	/**
	 * To store header text info during orientation change
	 */
	static String bgProcessingName = "";

	/**
	 * Dirty streampost rows
	 */
	protected static String[] sDirtyRows;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setBackgroundDrawable(
				mResources.getDrawable(R.drawable.metal));
		setContentView(R.layout.stream_activity);
		bindViews();
		initObjects();
		initBroadcastReceiver();
		initViews();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			mStreamsCursor.close();
			mFacebook.purgeInactiveOutHandlers(false);
			mFacebook = null;
			mUIHandler = null;
			mListViewFactory.destroy();
			mWorkerThread.informWaitOut();
		} catch (Exception e) {
		}
		System.gc();
	}

	@Override
	protected void onStart() {
		super.onStart();

		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		mFacebook.registerOutHandler(R.id.outhandler_activity_streams, mWorkerThread.getInHandler());
		mWorkerThread.setOutHandler(mUIHandler);

		if (!mIsStreamsUpdateFinished) {
			showLoadingIndicator();
			if (mProcessStreamMultiQueryDataTask != null) {
				// mProcessStreamMultiQueryDataTask.connectUiHandler(mUIHandler,
				// mUIHandlerSignature);
			}
		}

		try {
			mTopHeaderText.setText(bgProcessingName);
		} catch (Exception e) {

		}

		
		startAlarm();

		
	}

	@Override
	protected void onStop() {
		super.onStop();

		try {
			bgProcessingName = ((TextView) mTopHeaderText.getCurrentView())
					.getText().toString();
			if (mProcessStreamMultiQueryDataTask != null) {
				// mProcessStreamMultiQueryDataTask.disconnectUiHandler();
			}
			// mProcessStreamMultiQueryDataTask.isLocked = true;

		} catch (Exception e) {
		}
		
		hideLoadingIndicator();

	}

	@Override
	protected void onPause() {
		super.onPause();
		System.gc();
		if (mStreamsCursor != null) {

		}

		try {
			unregisterReceiver(mBroadcastReceiver);
		} catch (IllegalArgumentException e) {
		}
		try {
			unregisterReceiver(mUpdateDirtyRows);
		} catch (IllegalArgumentException e) {
		}
		stopAlarm();
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		hideLoadingIndicator();
		fetchIsCurrentlyIncrementing = false;
		startReceivingAutoUpdateIntent();
		updateHeaderTextBasedOnMode();
		updateHeaderBackgroundBasedOnMode();
		if (mStreamsCursor != null) {

		} else {
			resolveCursor(null);
		}
		updateListView();
		if (isTimeToUpdate()) {
			fetchStreamsFromCloud(false);
		}
		registerReceiver(mUpdateDirtyRows, mUpdateDirtyRowsIF);
		
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	/**
	 * Note that onSaveInstanceState is invoked when the Activity is first
	 * created.
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putByte(XTRA_STREAMMODE, mStreamMode);

	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mStreamMode = savedInstanceState.getByte(XTRA_STREAMMODE);
	}

	@Override
	protected void bindViews() {
		super.bindViews();
		mLoadingIndicator = findViewById(R.id.loadingindicator);
		mListView = (ExpandableListView) findViewById(R.id.list);
		AdManager.setInTestMode(false);
		mAdView = (AdView) findViewById(R.id.ad);
		mAdView.setVisibility(View.VISIBLE);
		mAdView.setKeywords("gift");

		mAdView.setBackgroundColor(0xFFaaaaaa);
		mAdView.setTextColor(0xFF111111);
		mAdView.setGoneWithoutAd(false);
		mAdView.setRequestInterval(60);
		mAdView.setListener(new AdView.AdListener() {
			@Override
			public void onReceiveAd(AdView adView) {
				mUIHandler.sendEmptyMessage(CALLBACK_ADMOB_ONRECEIVE);
				// adView.setVisibility(View.VISIBLE);
			}

			@Override
			public void onNewAd() {

			}

			@Override
			public void onFailedToReceiveAd(AdView adView) {
				mUIHandler.sendEmptyMessage(CALLBACK_ADMOB_ONFAILRECEIVE);
				// adView.setVisibility(View.GONE);
			}
		});

		mTopHeaderText = (TextSwitcher) findViewById(R.id.topheader);
		mListView = (ExpandableListView) findViewById(R.id.list);
	}

	LayoutAnimationController mListAnimation;

	protected void initObjects() {

		// initialize threads
		if (mWorkerThread == null) {
			mThreadsInitCountdown = new CountDownLatch(
					mNumThreadsInitialization);
			mWorkerThread = new ManagerThread(mThreadsInitCountdown);
			mWorkerThread.start();
			// wait for threads to initialize
			try {
				mThreadsInitCountdown.await();
			} catch (InterruptedException e) {
			}
		}

		mFacebook = Facebook.getInstance();
		mFacebook.registerOutHandler(R.id.outhandler_activity_streams, mWorkerThread.getInHandler());
		//mFacebook.setOutHandler(mWorkerThread.getInHandler());

		mListAnimation = AnimationUtils.loadLayoutAnimation(this,
				R.anim.layout_animation_row_left_slide);

		mListViewAdapter = new MyExpandableListAdapter(this);
		mListViewAdapter.setGroupIdCursorColumnIndex(Stream.col_post_id	+ ApplicationDBHelper.START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC);
		mListViewFactory = new StreamListViewFactory(this);
		mListViewFactory.setDataSetObserver(mListViewAdapter.getObserver());
		mListViewAdapter.setViewFactory(mListViewFactory);

		mContextMenuItemListener = new MenuItem.OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				int id = item.getItemId();
				Intent i = item.getIntent();
				switch (id) {
					case CONTEXT_MENUITEM_VIEW_LINKS: {
						break;
					}
					case CONTEXT_MENUITEM_POST_COMMENTS: {
						startActivity(i);
						break;
					}
					case CONTEXT_MENUITEM_VIEW_COMMENTS: {
						
						break;
					}
					case CONTEXT_MENUITEM_BAN_APP:{
						//String text = longClickedItemData.processedData.mAttachment.longClickedItemData.app_id;
						Toast.makeText(StreamActivity.this,"ignored:"+longClickedItemData.app_id, 3000).show();
						//App.INSTANCE.addAppToIgnoreList(name, id);
						break;
					}
				}
				return false;
			}

		};

	}

	protected void initViews() {
		mProgressDialog = createProgressDialog();

		mTopHeaderText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switchMode();
			}
		});

		mListView.setLayoutAnimation(mListAnimation);
		mListView.setOnScrollListener(new AbsListView.OnScrollListener() {

			int state;

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				state = scrollState;
				switch (scrollState) {

				case AbsListView.OnScrollListener.SCROLL_STATE_IDLE: {
					state = scrollState;
					break;
				}
				case AbsListView.OnScrollListener.SCROLL_STATE_FLING: {
					break;

				}
				case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL: {
					break;
				}
				}
			}

			@Override
			public void onScroll(AbsListView view, final int firstVisibleItem,
					final int visibleItemCount, int totalItemCount) {
				if (state == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
						|| state == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {

				}
			}
		});

		registerForContextMenu(mListView);
		mListView.setFocusableInTouchMode(true);
		mListView.setFocusable(true);
		mListView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
		mListView.setDrawingCacheBackgroundColor(Color.TRANSPARENT);
		mListView.setAnimationCacheEnabled(true);
		mListView.setAlwaysDrawnWithCacheEnabled(true);
		mListView.setAdapter(mListViewAdapter);
		mListView.setBackgroundColor(Color.TRANSPARENT);
		mListView.setCacheColorHint(Color.TRANSPARENT);
		mListView.setSmoothScrollbarEnabled(true);
		mListView.setDrawSelectorOnTop(false);
		mListView.setGroupIndicator(null);
		mListView.setChildIndicator(null);
		// mListView.setChoiceMode(ExpandableListView.CHOICE_MODE_SINGLE);
		Drawable stateListDrawable = mListView.getSelector();
		stateListDrawable.setColorFilter(App.mColorFilterBlueish);
		try {
			mListView.setDivider(mResources
					.getDrawable(android.R.drawable.divider_horizontal_bright));
			mListView.setChildDivider(mResources
					.getDrawable(android.R.drawable.divider_horizontal_bright));
		} catch (Exception e) {

		}

		mListView
				.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
					@Override
					public boolean onGroupClick(ExpandableListView parent,
							View v, int groupPosition, long id) {
						Logger.l(Logger.DEBUG, LOG_TAG,"[onGroupClick()] groupPosition:"+groupPosition);

						GroupData groupData = (GroupData) v.getTag(R.id.tag_streamsadapter_item_data);
						
						int numMedias0 = -1;
						int numMedias1 = -1;
						
						if(groupData == null){
							Logger.l(Logger.DEBUG, LOG_TAG, "groupdata null");
						}else if(groupData.processedData == null){
							Logger.l(Logger.DEBUG, LOG_TAG, "processedAttachmentData null");
						}
						
						try{
							Logger.l(Logger.DEBUG, LOG_TAG, "groupdata message: "+groupData.message.length());
						}catch(Exception e){
							
						}
						
						try{
							numMedias0 = groupData.processedData.mAttachment.mNumMedias;
						}catch(Exception e){
							Logger.l(Logger.DEBUG, LOG_TAG, "error: "+e.getMessage());
						}
						
						try{
							ArrayList<AttachmentMedia> mediaList = (ArrayList<AttachmentMedia>) groupData.processedData.mAttachment.mAttachmentMediaList;
							if(mediaList == null){
								Logger.l(Logger.DEBUG, LOG_TAG, "mediaList null");
							}
							
							numMedias1 = mediaList.size();							
						}catch(Exception e){
							Logger.l(Logger.DEBUG, LOG_TAG, "error: "+e.getMessage());
						}
						
						Logger.l(Logger.DEBUG, LOG_TAG, "num0: "+numMedias0+", num1:"+numMedias1);
												
						Attachment att = new Attachment();
						att.parseJson(groupData.attachmentJson);
						
						Logger.l(Logger.DEBUG, LOG_TAG, "attachment mediaType: "+att.mMediaType);
						Logger.l(Logger.DEBUG, LOG_TAG, "attachment name: "+att.mName);
						Logger.l(Logger.DEBUG, LOG_TAG, "raw attachment json: "+groupData.attachmentJson);
						
						//Logger.l(Logger.DEBUG, LOG_TAG, "attachment json: "+groupData.attachmentJson);
						
						ArrayList<AttachmentMedia> mediaList;
						try{
							mediaList = (ArrayList<AttachmentMedia>) groupData.processedData.mAttachment.mAttachmentMediaList;
							int numMedias = mediaList.size();
							for (int i = 0; i < numMedias; i++) {
								Logger.l(Logger.DEBUG, LOG_TAG, "media #" +i+ ", src:" + mediaList.get(i).src);
							}
						}catch(Exception e){							
						}

												
						showPost(groupPosition);
						return true;// returning false won't consume the event
									// completely hence this will produce a bug
									// where viewcommentsactivity won't be
									// launched everytime
					}

				});

		mListView.setOnScrollListener(mListViewFactory);
		mListView.setRecyclerListener(mListViewFactory);
		

		/*
		 * mListView.setOnItemClickListener(new
		 * AdapterView.OnItemClickListener() {
		 * 
		 * @Override public void onItemClick(AdapterView<?> adapter, View view,
		 * int position, long arg3) {
		 * 
		 * Intent i = new Intent(StreamActivity.this,
		 * StreamPostActionsDialogActivity.class);
		 * 
		 * startActivityForResult(i, ACTCODE_SHOW_POSTACTIONS);
		 * 
		 * StreamListViewFactory.Holder h =
		 * (StreamListViewFactory.Holder)view.getTag(); String comment =
		 * h.message.getText().toString(); String from =
		 * h.name.getText().toString(); String since =
		 * h.since.getText().toString(); String say = since +".. "+ from +
		 * ".. Says: "+comment;
		 * 
		 * mTts.speak(say, TextToSpeech.QUEUE_FLUSH, null);
		 * 
		 * } });
		 */

		mListView
				.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> arg0, View v,
							int position, long arg3) {
						Logger.l(Logger.DEBUG, "debug", "onItemLongClick " + position);
						Logger.l(Logger.DEBUG, "debug", "onItemLongClick "
								+ v.getClass().getCanonicalName());

						StreamListViewFactory.GroupData tag = (StreamListViewFactory.GroupData) v.getTag(R.id.tag_streamsadapter_item_data);
						
						longClickedItemData = tag;
						
						//longClickedItemData =
						// ((StreamListViewFactory)mListViewAdapter.getViewFactory()).getMemoryCache().get(tag.post_id);

						return false;// return false to pass on the event, so
										// that context menu gets displayed
					}

				});

		mTopHeaderText.setFactory(new ViewFactory() {
			@Override
			public View makeView() {

				TextView t = new TextView(StreamActivity.this);
				t = (TextView) mLayoutInflater.inflate(
						R.layout.t_topheadertext, null);
				return t;
			}
		});
		/*
		 * mTestBtn.setOnClickListener(new View.OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { test(); } });
		 * 
		 * mTest2Btn.setOnClickListener(new View.OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { test2(); } });
		 */
	}

	private void switchMode() {
		if (mStreamMode == Facebook.STREAMMODE_LIVEFEED) {
			mStreamMode = Facebook.STREAMMODE_NEWSFEED;
		} else {
			mStreamMode = Facebook.STREAMMODE_LIVEFEED;
		}
		updateListView();
		updateHeaderTextBasedOnMode();
		updateHeaderBackgroundBasedOnMode();
	}

	private void updateHeaderTextBasedOnMode() {
		if (mStreamMode == Facebook.STREAMMODE_LIVEFEED) {
			mTopHeaderText.setText("Live Feed");
		} else {
			mTopHeaderText.setText("News Feed");
		}
	}

	private void updateHeaderBackgroundBasedOnMode(){
		if (mStreamMode == Facebook.STREAMMODE_LIVEFEED) {
			mTopHeaderText.setBackgroundDrawable(mResources.getDrawable(R.drawable.title_bg));
		} else {
			mTopHeaderText.setBackgroundDrawable(mResources.getDrawable(R.drawable.stream_header_bg_newsfeed));			
		}
	}
	
	ProgressDialog mProgressDialog;

	private ProgressDialog createProgressDialog() {
		ProgressDialog pd;
		pd = new ProgressDialog(this);
		pd.setMessage("Loading..");
		pd.setCancelable(true);
		pd.setIndeterminate(true);
		pd.setCustomTitle(null);
		return pd;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);
		menu.clearHeader();

		if (longClickedItemData == null) {
			return;
		}
		
		Matcher matcher = App.mUrlPattern.matcher(longClickedItemData.message);
		String[] links = new String[5];
		byte numLinks = 0;
		if (matcher.find()) {
			try {
				numLinks++;
				String url = matcher.group();
				links[numLinks] = url;
				MenuItem urlMI = menu.add("View " + url);
				Intent viewUrl = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				urlMI.setIntent(viewUrl);
				urlMI.setOnMenuItemClickListener(new OnMenuItemClickListener() {

					@Override
					public boolean onMenuItemClick(MenuItem item) {
						item.getIntent();
						try {
							startActivity(item.getIntent());
						} catch (ActivityNotFoundException e) {
							Toast.makeText(StreamActivity.this, "Invalid link",
									2000).show();
						}
						return true;
					}

				});
				Log.d("debug", "matched group: " + url);
			} catch (Exception e) {

			}

		}

		ProcessedData processedAttachmentData = longClickedItemData.processedData;

		if (processedAttachmentData != null) {
			
			//String iconUri = longClickedItemData.processedAttachmentData.mAttachment.mIcon;
			//Logger.l(Logger.DEBUG, LOG_TAG, "iconUri: " + iconUri);

			Logger.l(Logger.DEBUG, LOG_TAG, longClickedItemData.post_id
					+ ", processed attachment data null? "
					+ (processedAttachmentData == null));
			Logger.l(Logger.DEBUG, LOG_TAG,	"processed attachment data mattachment null? "
							+ (processedAttachmentData.mAttachment == null));
			
			byte attachmentType = processedAttachmentData.mAttachment.mMediaType;

			// menu.setHeaderTitle(message);
			// menu.setHeaderIcon(android.R.drawable.ic_menu_info_details);
			// menu.setHeaderView(null);

			if (attachmentType == AttachmentMedia.PHOTO) {
				MenuItem viewPicturesMI = menu.add("View pictures");
			}

			// menu.setHeaderTitle(mResources.getString(R.string.possible_actions));
			if (attachmentType == AttachmentMedia.VIDEO

			) {
				/*
				 * if(longClickedItemData.videoSourceUrl != null){
				 * 
				 * MenuItem watchVideoMI = menu.add("Watch video");
				 * 
				 * Intent watchVideoIntent = new Intent(Intent.ACTION_VIEW,
				 * Uri.parse(longClickedItemData.videoSourceUrl));
				 * watchVideoMI.setIntent(watchVideoIntent);
				 * watchVideoMI.setOnMenuItemClickListener( new
				 * OnMenuItemClickListener(){
				 * 
				 * @Override public boolean onMenuItemClick(MenuItem item) {
				 * item.getIntent(); startActivity(item.getIntent()); return
				 * true; }
				 * 
				 * }); }
				 */
			}

		}

		MenuItem viewCommentsMI;
		MenuItem unlikeMI;
		MenuItem likeMI;
		MenuItem postCommentsMI;
		
		/*
		MenuItem banAppMenuItem;
		if(longClickedItemData.app_id != -1){
			banAppMenuItem = menu.add(0, CONTEXT_MENUITEM_BAN_APP, 0, "Ignore app");
			banAppMenuItem.setOnMenuItemClickListener(mContextMenuItemListener);				
		}	
		*/	
		
		/*
		postCommentsMI = menu.add(0, CONTEXT_MENUITEM_POST_COMMENTS, 0, 	"Write a comment");		
		Intent postCommentIntent = new Intent(StreamActivity.this, WriteCommentActivity.class);
		postCommentIntent.putExtra(WriteCommentActivity.XTRA_MESSAGE, longClickedItemData.message);
		postCommentIntent.putExtra(WriteCommentActivity.XTRA_POST_ID, longClickedItemData.post_id);
		postCommentIntent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
				| Intent.FLAG_ACTIVITY_NO_HISTORY);
		postCommentsMI.setIntent(postCommentIntent);
		postCommentsMI.setOnMenuItemClickListener(mContextMenuItemListener);
		

		if (longClickedItemData.comments_can_post) {
		} else {
			postCommentsMI.setEnabled(false);
		}
		 */
		
		/*
		if (longClickedItemData.comments_count == 0) {
			viewCommentsMI = menu.add(0, CONTEXT_MENUITEM_DO_NOTHING, 0,
					"Read comments (0)");
			viewCommentsMI.setEnabled(false);
		} else {
			viewCommentsMI = menu.add(0, CONTEXT_MENUITEM_VIEW_COMMENTS, 0,
					"Read comments (" + longClickedItemData.comments_count
							+ ")");
			viewCommentsMI.setEnabled(true);
		}
		
		viewCommentsMI.setOnMenuItemClickListener(mContextMenuItemListener);
		 */
		
		if (longClickedItemData.likes_user_likes) {
			unlikeMI = menu.add(0, CONTEXT_MENUITEM_UNLIKE, 0, "Unlike!");
			unlikeMI.setOnMenuItemClickListener(mContextMenuItemListener);
		} else {
			if (longClickedItemData.likes_canlike) {
				likeMI = menu.add(0, CONTEXT_MENUITEM_UNLIKE, 0, "Like!");
				likeMI.setOnMenuItemClickListener(mContextMenuItemListener);
			}
		}

		Bitmap headerIconBmp = App.mImageUrlLoader2.loadImage(longClickedItemData.processedData.mProfileImageUri, true);
		Drawable actorProfileDrawable = new BitmapDrawable(headerIconBmp);
		SubMenu actorNameSM = menu.addSubMenu(longClickedItemData.actorName);
		actorNameSM.setHeaderIcon(actorProfileDrawable);
		actorNameSM.setHeaderTitle(longClickedItemData.actorName);
		//actorNameSM.setHeaderIcon(actorProfileDrawable);
		actorNameSM.setIcon(actorProfileDrawable);
		
		//actorNameSM.add("Send message on wall");
		//actorNameSM.add("Send images on wall");
		//actorNameSM.add("Send videos on wall");
		actorNameSM.add("View wall");
		actorNameSM.add("View profile");
		actorNameSM.add("View photos");
		MenuItem mi = actorNameSM.add(0,CONTEXT_MENUITEM_VIEW_TAGGED_PHOTOS,0,"View tagged photos");
		mi.setOnMenuItemClickListener(mGetTaggedPhotosMenuItemOnClick);
		//actorNameSM.add("Add to favorites");
		//actorNameSM.add("Un-favorite");

		if (longClickedItemData.targetName != null
				&& longClickedItemData.targetName.length() > 0) {
			SubMenu targetNameSM = menu
					.addSubMenu(longClickedItemData.targetName);
			targetNameSM.setHeaderTitle(longClickedItemData.targetName);
			targetNameSM.setHeaderIcon(android.R.drawable.presence_online);
			// actorNameSM.setHeaderIcon(util.loadImage(longClickedItemData.profilePictureUrl,true));

			targetNameSM.add("Write on Wall");
			targetNameSM.add("View Wall");
			targetNameSM.add("View Profile");
			targetNameSM.add("View Photos");

		}

	}

	OnMenuItemClickListener mGetTaggedPhotosMenuItemOnClick = 
	new OnMenuItemClickListener() {

		@Override
		public boolean onMenuItemClick(MenuItem item) {
		mProgressDialog = new ProgressDialog(
				StreamActivity.this);
		mProgressDialog = createProgressDialog();
		mProgressDialog.setMessage("Getting photos..");
		Bundle data = new Bundle();
		data.putString(ManagerThread.XTRA_CALLBACK_INTENT_ACTION, App.INTENT_GET_TAGGED_PHOTOS);
		data.putLong(Facebook.param_uid, longClickedItemData.actor_id);
		mFacebook.getTaggedPhotos(
				R.id.outhandler_app,
				data,
				longClickedItemData.actor_id,
				ViewPhotosActivity.NUM_PHOTOS_PER_REQUEST, 0,
				ManagerThread.CALLBACK_GET_TAGGED_PHOTOS,
				BaseManagerThread.CALLBACK_SERVERCALL_ERROR,
				BaseManagerThread.CALLBACK_TIMEOUT_ERROR, 0);
		mProgressDialog.show();
		return false;
		}
	};
	
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		SubMenu toggleMode;
		if (mStreamMode == Facebook.STREAMMODE_LIVEFEED) {
			toggleMode = menu
					.addSubMenu(0, MENUITEM_TOGGLE_MODE, 0, "Newsfeed");
		} else {
			toggleMode = menu
					.addSubMenu(0, MENUITEM_TOGGLE_MODE, 0, "Livefeed");
		}
		SubMenu clearAll = menu.addSubMenu(0, MENUITEM_CLEARALL, 0, "ClearAll");

		SubMenu refresh = menu.addSubMenu(0, MENUITEM_REFRESH_STREAM, 0,
				mResources.getString(R.string.menu_update));
		refresh.setIcon(android.R.drawable.ic_popup_sync);

		SubMenu clearDB = menu.addSubMenu(0, MENUITEM_CLEAR_STREAM, 0,
				"Clear stream");
		clearDB.setIcon(android.R.drawable.ic_popup_disk_full);

		SubMenu filter = menu.addSubMenu("Filter");
		filter.setIcon(android.R.drawable.ic_menu_manage);

		filter.setHeaderTitle("Filter streams using:");
		filter.setHeaderIcon(android.R.drawable.ic_menu_search);
		MenuItem filterNameMI = filter.add(0, MENUITEM_TEST_GETPROFILE, 0,
				"Name");
		MenuItem filterMessageMI = filter.add(0, MENUITEM_TEST_GETPROFILE, 0,
				"Message");
		MenuItem filterFriendlistMI = filter.add(0, MENUITEM_TEST_GETPROFILE,
				0, "Friendlist");
		filterNameMI.setIcon(android.R.drawable.ic_dialog_info);

		SubMenu actions = menu.addSubMenu("Actions");
		actions.add("Refresh");
		actions.add("Post photo");
		actions.add("Send live message");
		actions.add("Send an email");
		actions.add("Show notifications");
		actions.add("Quick status update");

		/*
		 * MenuItem photos = menu.add(0, 0, 0, "Photos"); photos.setIntent(new
		 * Intent(StreamActivity.this, ViewPhotosActivity.class));
		 */

		SubMenu friends = menu.addSubMenu("Friends");
		friends.add("All");
		friends.add("Starred");
		friends.add("Events");
		friends.add("B-Days");

		SubMenu my = menu.addSubMenu("My");
		my.add("Profile");
		my.add("Mailbox");
		my.add("Friend list");
		my.add("Hidden feeds list");
		my.add("Events");
		my.add("Photos");

		SubMenu favn = menu.addSubMenu(0, 0, 0, "Fav-n");
		favn.add("Show my fav-5");
		favn.add("Settings");

		SubMenu sessions = menu.addSubMenu("Sessions");
		sessions.add("Lock and go standby mode");
		sessions.add("Change password");
		sessions.add("Sign off");

		SubMenu options = menu.addSubMenu("Options");
		MenuItem preference = options.add("Preferences");
		preference.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent i = new Intent(StreamActivity.this, AppSettings.class);
				startActivity(i);
				return true;
			}
		});

		SubMenu help = menu.addSubMenu("Help");
		SubMenu about = menu.addSubMenu("About");
		about.add("About");
		about.add("Send feedback");

		if (mFacebook.getSession() != null) {
			MenuItem chat = menu.add(0, MENUITEM_CHAT, 0, "chat");
			Intent chatIntent = new Intent(StreamActivity.this,
					ChatActivity.class);
			chatIntent.putExtra(ChatActivity.XTRA_UID,
					mFacebook.getSession().uid);
			chat.setIntent(chatIntent);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent i = item.getIntent();
		int itemId = item.getItemId();

		switch (itemId) {
		case MENUITEM_TOGGLE_MODE: {
			switchMode();
			break;
		}
		case MENUITEM_REFRESH_STREAM: {
			fetchStreamsFromCloud(false);
			return true;
		}
		case MENUITEM_CLEAR_STREAM: {
			// SQLiteDatabase db = mDB.getWritableDatabase();
			// mDB.deleteAllStreams(db);
			App.INSTANCE.mDBHelper.deleteAllStreams(App.INSTANCE.mDB);
			// db.close();
			return true;
		}
		case MENUITEM_CLEARALL: {
			try {
				// SQLiteDatabase db = mDB.getWritableDatabase();
				// mDB.deleteAllStreams(mSQLite);
				mFacebook.setSession(null);
			} catch (Exception e) {

			}
			return true;
		}
		case MENUITEM_CHAT: {
			startActivity(i);
			return true;
		}
		}

		return super.onMenuItemSelected(featureId, item);
	}

	static long mUIHandlerSignature = 1000l;
	BroadcastReceiver mUpdateDirtyRows = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(LOG_TAG, "onReceiveIntent: " + intent.getAction());
			String rows[] = intent.getStringArrayExtra(XTRA_DIRTYPOSTS);
			mListViewAdapter.getViewFactory().markGroupAsDirty(rows);
		}
	};

	Handler mUIHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			// Log.d(LOG_TAG,"ui hander handle message what:"+msg.what);
			int code = msg.what;
			Bundle data = msg.getData();
			switch (code) {
			case ManagerThread.MESSAGE_DISMISS_DIALOG: {
				mProgressDialog.dismiss();
				break;
			}

			case CALLBACK_ADMOB_ONFAILRECEIVE: {
				mAdView.setVisibility(View.GONE);
				break;
			}
			
			case CALLBACK_ADMOB_ONRECEIVE: {
				mAdView.setVisibility(View.VISIBLE);
				break;
			}

			case BaseManagerThread.CALLBACK_TIMEOUT_ERROR: {
				mProgressDialog.dismiss();
				Toast.makeText(StreamActivity.this,
						"Request to Facebook timed out", 2000).show();
				Logger
						.l(Logger.WARN, LOG_TAG,
								"[UIHandler] [handleMessage()] remote request timed out.");
				onFinishUpdatingStreams();
				break;
			}
			
			case BaseManagerThread.CALLBACK_SERVERCALL_ERROR: {
				mProgressDialog.dismiss();
				String reason = (String) data
						.getString(Facebook.XTRA_SERVERCALL_ERROR_MSG);
				int errorCode = data
						.getInt(Facebook.XTRA_SERVERCALL_ERROR_CODE);
				Logger.l(Logger.ERROR, LOG_TAG,
						"[UIHandler] [handleMessage()] failed invoking remote request. error: "
								+ errorCode + ", reason:" + reason);
				Toast.makeText(StreamActivity.this, errorCode + ":" + reason,
						2000).show();
				
				
				// hideLoadingIndicator();
				// resetHeaderText();
				onFinishUpdatingStreams();
				break;
			}

				/**
				 * case CALLBACK_PROCESS_WSRESPONSE_HAS_ERRORCODE:{
				 * mProgressDialog.dismiss(); FBWSResponse fbResponse =
				 * (FBWSResponse) msg.obj; Logger.l(Logger.ERROR,LOG_TAG,
				 * "[UIHandler] [handleMessage()] failed processing response.");
				 * Toast.makeText(StreamActivity.this,fbResponse.errorDesc,
				 * 1000).show(); onFinishUpdatingStreams(); break; }
				 **/

			case ManagerThread.CALLBACK_PROCESS_STREAMS_FINISH: {
				Logger
						.l(Logger.DEBUG, LOG_TAG,
								"[UiHandler][handleMessage()] finished processing streams.");

				updateHeaderTextBasedOnMode();
				updateHeaderBackgroundBasedOnMode();
				try {
					updateListView();

				} catch (Exception e) {
				}
				onFinishUpdatingStreams();

				// incremental fetch
				long waitTime = (long) (3000+1000*Math.random());	
								
					postDelayed(
							new Runnable() {								
								@Override
								public void run() {
									fetchStreamsFromCloud(true);									
								}
							}, 							
							waitTime);
					
				// end of incremental fetch
				
				break;
			}
			}
		};
	};

	private void resolveCursor(long[] uids) {

		boolean isDBLocked = App.INSTANCE.mDB.isDbLockedByCurrentThread();
		boolean isDBOpen = App.INSTANCE.mDB.isOpen();

		Logger.l(Logger.DEBUG, LOG_TAG, "[resolveCursor()] is db locked: "
				+ isDBLocked + ", is db open: " + isDBOpen
				+ " , sqlitedb null?: " + (App.INSTANCE.mDB == null));

		if (mStreamsCursor != null) {
			mStreamsCursor.close();
		}

		mStreamsCursor = App.INSTANCE.mDBHelper.getStreams(App.INSTANCE.mDB, mStreamMode, -1,
				uids, 300l, 0l);
		startManagingCursor(mStreamsCursor);

		try {
			int size = mStreamsCursor.getCount();
			Logger.l(Logger.DEBUG, LOG_TAG,
					"[resolveCursor()] streams cursor size: " + size);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Logger.l(Logger.DEBUG, LOG_TAG, "[resolveCursor()] finished.");
	}

	private void initBroadcastReceiver() {
		mAutoUpdateStreamsIntent = new Intent(App.INTENT_AUTOUPDATE_STREAMS);
		mAutoUpdateStreamsPendingIntent = PendingIntent.getBroadcast(this, 0,
				mAutoUpdateStreamsIntent, 0);

		mBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				Log.d("StreamActivity", "BroadcastReceiver->onReceive, action:"+action);
				// PowerManager mgr = (PowerManager)
				// context.getSystemService(Context.POWER_SERVICE);
				// WakeLock lockStatic =
				// mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"mylock");
				
				if(action.equals(App.INTENT_AUTOUPDATE_STREAMS)){
					if (mIsStreamsUpdateFinished) {
						Log.d("StreamActivity","BroadcastReceiver->onReceive: start updating streams from the cloud");
						fetchStreamsFromCloud(false);
					} else {
						Log.d("StreamActivity",	"BroadcastReceiver->onReceive: not done updating streams");
						Toast.makeText(context, "Not done updating.", 2000).show();
					}
				}else if(action.equals(App.INTENT_GET_TAGGED_PHOTOS)){
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
						App.INSTANCE.startActivity(i);			  
					}catch(JSONException e){
					}
					
				}
			}
		};

	}

	private void startReceivingAutoUpdateIntent() {
		IntentFilter autoUpdateStreamsIntentFilter = new IntentFilter(
				App.INTENT_AUTOUPDATE_STREAMS);
		autoUpdateStreamsIntentFilter.addAction(
				App.INTENT_GET_TAGGED_PHOTOS
		);
		registerReceiver(mBroadcastReceiver,
				autoUpdateStreamsIntentFilter);
	}

	private void stopAlarm() {
		AlarmManager am = (AlarmManager) this
				.getSystemService(Service.ALARM_SERVICE);
		am.cancel(mAutoUpdateStreamsPendingIntent);
	}

	private void startAlarm() {
		SharedPreferences sp = this.getSharedPreferences(Prefs.PREF_MAIN, 0);
		boolean isPeriodicCheck = sp.getBoolean(
				Prefs.KEY_PERIODIC_CHECK_ENABLED,
				Prefs.DEFAULT_PERIODIC_CHECK_ENABLED);

		if (isPeriodicCheck) {
			AlarmManager am = (AlarmManager) this
					.getSystemService(Service.ALARM_SERVICE);
			long waitInterval = sp.getLong(Prefs.KEY_PERIODIC_CHECK_INTERVAL,
					Prefs.DEFAULT_PERIODIC_CHECK_INTERVAL);
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + waitInterval, waitInterval,
					mAutoUpdateStreamsPendingIntent);
		}

	}

	private void onStartUpdatingStreams() {
		mIsStreamsUpdateFinished = false;
		mTopHeaderText.setText("Updating streams from the cloud..");
	}

	private void onFinishUpdatingStreams() {
		mIsStreamsUpdateFinished = true;
		hideLoadingIndicator();
		SharedPreferences sp = getSharedPreferences(Prefs.PREF_MAIN, 0);
		Editor editor = sp.edit();
		editor.putLong(Prefs.KEY_STREAMS_LASTUPDATED, new Date().getTime());
		editor.commit();
	}

	private boolean isTimeToUpdate() {
		SharedPreferences sp = getSharedPreferences(Prefs.PREF_MAIN, 0);
		long lastTime = sp.getLong(Prefs.KEY_STREAMS_LASTUPDATED, 0);
		long now = new Date().getTime();
		long checkInterval = sp.getLong(Prefs.KEY_PERIODIC_CHECK_INTERVAL,
				Prefs.DEFAULT_PERIODIC_CHECK_INTERVAL);
		if (checkInterval + lastTime < now) {
			return true;
		}
		return false;
	}

	private void stopAllBackgroundTasks() {

		if (mProcessStreamMultiQueryDataTask != null) {
			mProcessStreamMultiQueryDataTask.cancel(true);
		}

		mIsStreamsUpdateFinished = true;
	}

	private void showPost(int groupPosition) {
		mStreamsCursor.moveToPosition(groupPosition);
		String post_id = mStreamsCursor
				.getString(Stream.col_post_id
						+ ApplicationDBHelper.START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC);
		String message = mStreamsCursor
				.getString(Stream.col_message
						+ ApplicationDBHelper.START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC);
		String attachment = mStreamsCursor
				.getString(Stream.col_attachment
						+ ApplicationDBHelper.START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC);

		Logger.l(Logger.VERBOSE, LOG_TAG, "message: " + message);
		Logger.l(Logger.VERBOSE, LOG_TAG, "post_id: " + post_id);
		Logger.l(Logger.VERBOSE, LOG_TAG, "attachment: " + attachment);

		Intent showComments = new Intent(StreamActivity.this,
				ViewCommentsActivity.class);
		showComments.putExtra(ViewCommentsActivity.XTRA_POSTID, post_id);
		showComments.putExtra(ViewCommentsActivity.XTRA_CLEARDATA, true);
		showComments.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
				| Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
				| Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(showComments);
	}

	int fetchMaxStreams = 120;
	int fetchSizePerFetch = 20;
	int fetchNumTotalFetches = (int)Math.ceil(fetchMaxStreams/fetchSizePerFetch);
	int fetchCurrentFetch = 0;
	boolean fetchIsCurrentlyIncrementing = false;
	boolean blockAutoUpdate = true;
	boolean fetchIsRefreshAll = true;
	
	private void fetchStreamsFromCloud(boolean isIncremental) {
		Logger.l(Logger.DEBUG, "crucial", "totalFetches: "+fetchNumTotalFetches+", fetchCurrentFetch: "+fetchCurrentFetch);
		if (mFacebook == null) {
			Toast.makeText(this, "Facebook null", 1000).show();
			return;
		}

		if (mFacebook.getSession() == null) {
			Toast.makeText(this, "Session is null", 1000).show();
		}

		if (!mFacebook.quickCheckSession(true)) {
			return;
		}

		// abort if not done completing last invoked process
		if (mProcessStreamMultiQueryDataTask != null
				&& mProcessStreamMultiQueryDataTask.getStatus() == Status.RUNNING) {
			Toast.makeText(this, "I'm not done processing previous request.",
					3000).show();
			return;
		}

		// incremental fetch section 
		
		if(!isIncremental && fetchIsCurrentlyIncrementing){
			return;
		}
		
		fetchCurrentFetch++;
		
		if(fetchCurrentFetch > fetchNumTotalFetches){
			fetchCurrentFetch = 0;
			fetchIsCurrentlyIncrementing = false;
			return;
		}
				
		if(fetchCurrentFetch == 1){
			fetchIsCurrentlyIncrementing = true;
		}
				
		//end of incremental fetch section
		
		showLoadingIndicator();
		onStartUpdatingStreams();
		long numMaxPosts = Prefs.getNumFetchStreams();
		long lastStreamPostUpdateTime;
		
		
		int limit = fetchSizePerFetch;
		int offset = fetchCurrentFetch * fetchSizePerFetch;
		
		if(fetchIsRefreshAll){
			lastStreamPostUpdateTime = 0;
		}else{
			lastStreamPostUpdateTime = App.INSTANCE.mDBHelper.getStreamLastUpdatedTime(App.INSTANCE.mDB);
		}
		
		mFacebook.getStreamsComplete(				
				R.id.outhandler_activity_streams,
				Long.toString(mFacebook.getSession().uid),
				null, 
				lastStreamPostUpdateTime,
				ManagerThread.CALLBACK_GET_STREAMS_MQ,
				ManagerThread.CALLBACK_SERVERCALL_ERROR,
				ManagerThread.CALLBACK_TIMEOUT_ERROR,
				1, 
				offset, //using offset as the limit and 0 as the offset, the behaviour is every fetching increases the fetch size 
				0,
				10, 0);
		
	
	}

	private void updateListView() {
		try {
			resolveCursor(null);
			mListViewAdapter.setData(mStreamsCursor);
			mListViewAdapter.notifyDataSetChanged();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showLoadingIndicator() {
		mLoadingIndicator.setVisibility(View.VISIBLE);
		setProgressBarIndeterminateVisibility(true);
	}

	private void hideLoadingIndicator() {
		mLoadingIndicator.setVisibility(View.INVISIBLE);
		setProgressBarIndeterminateVisibility(false);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	}

}