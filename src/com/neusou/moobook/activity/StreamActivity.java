package com.neusou.moobook.activity;

import static com.neusou.moobook.App.MENUITEM_CLEAR_STREAM;
import static com.neusou.moobook.App.MENUITEM_REFRESH_STREAM;
import static com.neusou.moobook.App.MENUITEM_TOGGLE_MODE;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;

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
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
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

import com.admob.android.ads.AdView;
import com.neusou.Logger;
import com.neusou.async.UserTask.Status;
import com.neusou.moobook.App;
import com.neusou.moobook.FBSession;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.Prefs;
import com.neusou.moobook.R;
import com.neusou.moobook.controller.GetTaggedPhotosReceiver;
import com.neusou.moobook.controller.MyExpandableListCursorAdapter;
import com.neusou.moobook.controller.StandardUiHandler;
import com.neusou.moobook.controller.StreamListViewFactory;
import com.neusou.moobook.controller.StreamListViewFactory.GroupData;
import com.neusou.moobook.data.Attachment;
import com.neusou.moobook.data.AttachmentMedia;
import com.neusou.moobook.data.ContextMenuDescriptor;
import com.neusou.moobook.data.ContextProfileData;
import com.neusou.moobook.data.ProcessedData;
import com.neusou.moobook.data.Stream;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.moobook.task.ProcessStreamMultiQueryTask;
import com.neusou.moobook.thread.ManagerThread;
import com.neusou.moobook.view.ActionBar;
import com.neusou.moobook.view.TitleBar;

public class StreamActivity extends BaseActivity {

	static final String LOG_TAG = Logger.registerLog(StreamActivity.class);

	public static Intent getIntent(Context caller) {
		Intent i = new Intent(caller, StreamActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		return i;
	}

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

	
	

	//static final int CALLBACK_ADMOB_ONRECEIVE = 50;
	//static final int CALLBACK_ADMOB_ONFAILRECEIVE = 51;

	static final int CALLBACK_PROCESS_WSRESPONSE_HAS_ERRORCODE = 130;

	static final String XTRA_ONRESUME_FORCESTREAMSUPDATE = App.packageprefix
			+ ".xtra.onresume.autorefresh";
	static final String XTRA_DIRTYPOSTS = App.packageprefix
			+ ".xtra.dirty.posts.ids";
	public static final String XTRA_STREAMMODE = App.packageprefix
			+ ".xtra.stream.mode";
	public static final String XTRA_USERID = App.packageprefix
			+ ".xtra.stream.uid";
	/*
	public static final String XTRA_USERNAME = App.packageprefix
	+ ".xtra.stream.username";
*/
	
	static final byte NUM_TASKS = 2;
	static final byte TASK_GETSTREAMS = 0;
	static final byte TASK_GETFRIENDS = 1;
	boolean tasks[] = new boolean[NUM_TASKS];
	short tasksMax[] = new short[] { 1, 1 };

	byte mStreamMode = Facebook.STREAMMODE_LIVEFEED;
	//long mUserId = -1; // -1 refers to current session user.
	//String mUsername = "";
	ContextProfileData mContextProfileData = null;
	
	Facebook mFacebook;
	ExpandableListView mListView;
	StreamListViewFactory mListViewFactory;
	MyExpandableListCursorAdapter mListViewAdapter;
	OnMenuItemClickListener mContextMenuItemListener;
	TextSwitcher mTopHeaderText;
	View mLoadingIndicator;
	AdView mAdView;
	ActionBar mActionBar;

	boolean isStreamFiltered = false;
	byte mFilterType = 0;
	final byte FILTER_FRIENDLIST = 1;
	final byte FILTER_NAME = 2;
	Handler mThreadUIHandler;
	Cursor mStreamsCursor;
	StreamListViewFactory.GroupData longClickedItemData;

	BroadcastReceiver mAutoStreamsUpdateBroadcastReceiver;
	BroadcastReceiver mGetTaggedPhotosBroadcastReceiver;

	PendingIntent mAutoUpdateStreamsPendingIntent;
	Intent mAutoUpdateStreamsIntent;

	LayoutAnimationController mListAnimation;
	ProgressDialog mProgressDialog;
	Handler mUIHandler;

	static boolean mIsStreamsUpdateFinished = true;
	static final String INTENT_ACTION_SET_DIRTY_POSTS = "intent.action.set.dirty.posts";
	static final IntentFilter mUpdateDirtyRowsIF = new IntentFilter(
			INTENT_ACTION_SET_DIRTY_POSTS);
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

	TitleBar mTitleBar;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setBackgroundDrawable(mResources.getDrawable(R.drawable.metal));
		setContentView(R.layout.stream_activity);
		bindViews();
		initObjects();
		initBroadcastReceiver();
		getIntentExtras();
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

		mFacebook.registerOutHandler(R.id.outhandler_activity_streams,
				mWorkerThread.getInHandler());
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

		// startAlarm();

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

	boolean mIsFinishing = false;
	@Override
	protected void onPause() {
		super.onPause();
		if (mStreamsCursor != null) {
			
		}
		
		//if not truly a destructive pause, we assume that's when we switch the activity mode
		mIsFinishing = isFinishing();
		
		
		doUnregisterReceivers();
		stopAlarm();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if(!mIsFinishing){ 
			getIntentExtras();
		}
				
		hideLoadingIndicator();
		fetchIsCurrentlyIncrementing = false;
		startReceivingAutoUpdateIntent();
		updateHeaderTextBasedOnMode();
		updateColorsBasedOnMode();
		if (mStreamsCursor != null) {
			resolveCursor(null);
		} else {
			resolveCursor(null);
		}
		updateListView();

		if (isTimeToUpdate()) {
			fetchStreamsFromCloud(false);
		}

		updateColorsBasedOnMode();

		doRegisterReceivers();
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
//		outState.putLong(XTRA_USERID, mUserId);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);		
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mStreamMode = savedInstanceState.getByte(XTRA_STREAMMODE);
		//mUserId = savedInstanceState.getLong(XTRA_USERID);
	}

	private void getIntentExtras() {
		Logger.l(Logger.DEBUG, LOG_TAG,"getIntentExtras()");
		Intent i = getIntent();
		try{
			FBSession fbSession = Facebook.getInstance().getSession();
			//mUserId = i.getLongExtra(StreamActivity.XTRA_USERID,fbSession.uid);
		}catch(NullPointerException e){			
		}
		
		if(i.hasExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT)){
			mContextProfileData = (ContextProfileData) i.getParcelableExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT);
			
		}
		mStreamMode = i.getByteExtra(XTRA_STREAMMODE, mStreamMode);
		//mUsername = i.getStringExtra(XTRA_USERNAME);
		//Logger.l(Logger.DEBUG, LOG_TAG,"mUserId:"+mUserId);
	}

	private void doRegisterReceivers() {
		registerReceiver(mGetTaggedPhotosBroadcastReceiver,
				GetTaggedPhotosReceiver.INTENT_FILTER);
		IntentFilter autoUpdateStreamsIntentFilter = new IntentFilter();
		autoUpdateStreamsIntentFilter.addAction(App.INTENT_GET_TAGGED_PHOTOS);
		registerReceiver(mAutoStreamsUpdateBroadcastReceiver,
				autoUpdateStreamsIntentFilter);
	}

	private void doUnregisterReceivers() {
		try {
			unregisterReceiver(mAutoStreamsUpdateBroadcastReceiver);
		} catch (IllegalArgumentException e) {
		}
		try {
			unregisterReceiver(mGetTaggedPhotosBroadcastReceiver);
		} catch (IllegalArgumentException e) {
		}
	}

	@Override
	protected void bindViews() {
		super.bindViews();
		mLoadingIndicator = findViewById(R.id.loadingindicator);
		mListView = (ExpandableListView) findViewById(R.id.list);
		mAdView = (AdView) findViewById(R.id.ad);
		mTopHeaderText = (TextSwitcher) findViewById(R.id.topheader);
		mListView = (ExpandableListView) findViewById(R.id.list);
	}

	protected void initObjects() {

		mProgressDialog = createProgressDialog();
		
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
		mFacebook.registerOutHandler(R.id.outhandler_activity_streams,
				mWorkerThread.getInHandler());
		// mFacebook.setOutHandler(mWorkerThread.getInHandler());

		mListAnimation = AnimationUtils.loadLayoutAnimation(this,
				R.anim.layout_animation_row_left_slide);

		mListViewAdapter = new MyExpandableListCursorAdapter(this) {
			@Override
			public int getChildrenCount(int groupPosition) {
				return 1;
			}
		};

		mListViewAdapter
				.setGroupIdCursorColumnIndex(Stream.col_post_id
						+ ApplicationDBHelper.START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC);
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
				case CONTEXT_MENUITEM_BAN_APP: {
					// String text =
					// longClickedItemData.processedData.mAttachment.longClickedItemData.app_id;
					Toast.makeText(StreamActivity.this,
							"ignored:" + longClickedItemData.app_id, 3000)
							.show();
					// App.INSTANCE.addAppToIgnoreList(name, id);
					break;
				}
				}
				return false;
			}

		};

		mUIHandler = new StandardUiHandler(this, mProgressDialog, findViewById(R.id.adview_wrapper), findViewById(R.id.bottomheader)) {
			public void handleMessage(android.os.Message msg) {
				super.handleMessage(msg);
				int code = msg.what;
				Bundle data = msg.getData();
				switch (code) {
				case ManagerThread.CALLBACK_PROCESS_STREAMS_FINISH: {
					Logger.l(Logger.DEBUG, LOG_TAG,	"[UiHandler][handleMessage()] finished processing streams.");
					updateHeaderTextBasedOnMode();
					updateColorsBasedOnMode();
					try {
						updateListView();

					} catch (Exception e) {
					}
					onFinishUpdatingStreams();
					
					/*
					// incremental fetch
					long waitTime = (long) (3000 + 1000 * Math.random());

					postDelayed(new Runnable() {
						@Override
						public void run() {
							fetchStreamsFromCloud(true);
						}
					}, waitTime);

					// end of incremental fetch
					 */
					break;
				}
				}
			};
			
			@Override
			public void onServerCallError() {
				super.onServerCallError();
				hideLoadingIndicator();
				onFinishUpdatingStreams(); 
				updateHeaderTextBasedOnMode();
				updateColorsBasedOnMode();
			}

			@Override
			public void onTimeoutError() {
				super.onTimeoutError();
				hideLoadingIndicator();
				onFinishUpdatingStreams();
				updateHeaderTextBasedOnMode();
				updateColorsBasedOnMode();
			}
			
		};

		/*
		 * mUIHandler = new Handler() { public void
		 * handleMessage(android.os.Message msg) { //
		 * Log.d(LOG_TAG,"ui hander handle message what:"+msg.what); int code =
		 * msg.what; Bundle data = msg.getData(); switch (code) { case
		 * ManagerThread.MESSAGE_DISMISS_DIALOG: { mProgressDialog.dismiss();
		 * break; }
		 * 
		 * case ManagerThread.CALLBACK_ADMOB_ONFAILRECEIVE: {
		 * mAdView.setVisibility(View.GONE); break; }
		 * 
		 * case ManagerThread.CALLBACK_ADMOB_ONRECEIVE: {
		 * mAdView.setVisibility(View.VISIBLE); break; }
		 * 
		 * case BaseManagerThread.CALLBACK_TIMEOUT_ERROR: {
		 * mProgressDialog.dismiss(); Toast.makeText(StreamActivity.this,
		 * "Request to Facebook timed out", 2000).show(); Logger .l(Logger.WARN,
		 * LOG_TAG, "[UIHandler] [handleMessage()] remote request timed out.");
		 * onFinishUpdatingStreams(); break; }
		 * 
		 * case BaseManagerThread.CALLBACK_SERVERCALL_ERROR: {
		 * mProgressDialog.dismiss(); String reason = (String) data
		 * .getString(Facebook.XTRA_SERVERCALL_ERROR_MSG); int errorCode = data
		 * .getInt(Facebook.XTRA_SERVERCALL_ERROR_CODE); Logger.l(Logger.ERROR,
		 * LOG_TAG,
		 * "[UIHandler] [handleMessage()] failed invoking remote request. error: "
		 * + errorCode + ", reason:" + reason);
		 * Toast.makeText(StreamActivity.this, errorCode + ":" + reason,
		 * 2000).show();
		 * 
		 * 
		 * // hideLoadingIndicator(); // resetHeaderText();
		 * onFinishUpdatingStreams(); break; }
		 * 
		 * 
		 * 
		 * case ManagerThread.CALLBACK_PROCESS_STREAMS_FINISH: { Logger
		 * .l(Logger.DEBUG, LOG_TAG,
		 * "[UiHandler][handleMessage()] finished processing streams.");
		 * 
		 * updateHeaderTextBasedOnMode(); updateColorsBasedOnMode(); try {
		 * updateListView();
		 * 
		 * } catch (Exception e) { } onFinishUpdatingStreams();
		 * 
		 * // incremental fetch
		 * 
		 * long waitTime = (long) (3000+1000*Math.random());
		 * 
		 * postDelayed( new Runnable() {
		 * 
		 * @Override public void run() { fetchStreamsFromCloud(true); } },
		 * waitTime);
		 * 
		 * // end of incremental fetch
		 * 
		 * break; } } }; };
		 */
	}

	protected void initViews() {
		mTitleBar = new TitleBar(this);
		
		mActionBar = App.INSTANCE.mActionBar;		
		mActionBar.bindViews(this);
		mActionBar.initViews();
		mActionBar.setUserId(mContextProfileData.actorId);
		mActionBar.setUserName(mContextProfileData.name);
		
		mActionBar.setOnAddClick(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});
		
		mActionBar.setOnReloadClick(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fetchStreamsFromCloud(false);
				mActionBar.animateReload();
			}
		});

		mTopHeaderText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mStreamMode != Facebook.STREAMMODE_WALLFEED){
					switchMode();
				}
			}
		});

		//mListView.setLayoutAnimation(mListAnimation);
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
		mListView.setDividerHeight(0);
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
						Logger.l(Logger.DEBUG, LOG_TAG,
								"[onGroupClick()] groupPosition:"
										+ groupPosition);

						GroupData groupData = (GroupData) v
								.getTag(R.id.tag_streamsadapter_item_data);

						int numMedias0 = -1;
						int numMedias1 = -1;

						if (groupData == null) {
							Logger.l(Logger.DEBUG, LOG_TAG, "groupdata null");
						} else if (groupData.processedData == null) {
							Logger.l(Logger.DEBUG, LOG_TAG,
									"processedAttachmentData null");
						}

						try {
							Logger.l(Logger.DEBUG, LOG_TAG,
									"groupdata message: "
											+ groupData.message.length());
						} catch (Exception e) {

						}

						try {
							numMedias0 = groupData.processedData.mAttachment.mNumMedias;
						} catch (Exception e) {
							Logger.l(Logger.DEBUG, LOG_TAG, "error: "
									+ e.getMessage());
						}

						try {
							ArrayList<AttachmentMedia> mediaList = (ArrayList<AttachmentMedia>) groupData.processedData.mAttachment.mAttachmentMediaList;
							if (mediaList == null) {
								Logger.l(Logger.DEBUG, LOG_TAG,
										"mediaList null");
							}

							numMedias1 = mediaList.size();
						} catch (Exception e) {
							Logger.l(Logger.DEBUG, LOG_TAG, "error: "
									+ e.getMessage());
						}

						Logger.l(Logger.DEBUG, LOG_TAG, "num0: " + numMedias0
								+ ", num1:" + numMedias1);

						Attachment att = new Attachment();
						att.parseJson(groupData.attachmentJson);

						Logger.l(Logger.DEBUG, LOG_TAG,
								"attachment mediaType: " + att.mMediaType);
						Logger.l(Logger.DEBUG, LOG_TAG, "attachment name: "
								+ att.mName);
						Logger.l(Logger.DEBUG, LOG_TAG, "raw attachment json: "
								+ groupData.attachmentJson);

						// Logger.l(Logger.DEBUG, LOG_TAG,
						// "attachment json: "+groupData.attachmentJson);

						ArrayList<AttachmentMedia> mediaList;
						try {
							mediaList = (ArrayList<AttachmentMedia>) groupData.processedData.mAttachment.mAttachmentMediaList;
							int numMedias = mediaList.size();
							for (int i = 0; i < numMedias; i++) {
								Logger.l(Logger.DEBUG, LOG_TAG, "media #" + i
										+ ", src:" + mediaList.get(i).src);
							}
						} catch (Exception e) {
						}

						showPost(groupPosition);
						return true;// returning false won't consume the event
						// completely hence this will produce a bug
						// where viewcommentsactivity won't be
						// launched everytime
					}

				});

		mListView.setRecyclerListener(mListViewFactory);
		mListView
				.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

					@Override
					public boolean onItemLongClick(AdapterView<?> arg0, View v,
							int position, long arg3) {
						Logger.l(Logger.DEBUG, "debug", "onItemLongClick "
								+ position);
						Logger.l(Logger.DEBUG, "debug", "onItemLongClick "
								+ v.getClass().getCanonicalName());

						StreamListViewFactory.GroupData tag = (StreamListViewFactory.GroupData) v
								.getTag(R.id.tag_streamsadapter_item_data);

						longClickedItemData = tag;

						// longClickedItemData =
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
		
		App.initAdMob(mAdView, mUIHandler);
		

	}

	private void switchMode() {
		if (mStreamMode == Facebook.STREAMMODE_LIVEFEED) {
			mStreamMode = Facebook.STREAMMODE_NEWSFEED;
		} else {
			mStreamMode = Facebook.STREAMMODE_LIVEFEED;
		}
		updateListView();
		updateHeaderTextBasedOnMode();
		updateColorsBasedOnMode();
	}

	private void updateHeaderTextBasedOnMode() {
		String txt = getHeaderNormalText(mStreamMode);
		mTopHeaderText.setText(txt);
	}

	public String getHeaderNormalText(byte type) {
		Resources res = App.INSTANCE.getResources();
		
		switch (type) {
		case Facebook.STREAMMODE_LIVEFEED: {
			return mContextProfileData.name+" "+res.getString(R.string.live_feed);
		}
		case Facebook.STREAMMODE_NEWSFEED: {
			return mContextProfileData.name+" "+res.getString(R.string.news_feed);
		}
		case Facebook.STREAMMODE_WALLFEED: {			 
			return mContextProfileData.name+" "+res.getString(R.string.wall_feed);
		}
		default: {
			return "";
		}
		}
	}

	public String getHeaderLoadingText(byte type) {
		Resources res = App.INSTANCE.getResources();
		switch (type) {
		case Facebook.STREAMMODE_LIVEFEED: {
			return res.getString(R.string.live_feed);
		}
		case Facebook.STREAMMODE_NEWSFEED: {
			return res.getString(R.string.news_feed);
		}
		case Facebook.STREAMMODE_WALLFEED: {
			return res.getString(R.string.wall_feed);
		}
		default: {
			return "";
		}
		}
	}

	private void updateColorsBasedOnMode() {
		if (mStreamMode == Facebook.STREAMMODE_LIVEFEED) {
			
			//mTopHeaderText.setBackgroundDrawable(mResources		.getDrawable(R.drawable.title_bg));
			//Drawable stateListDrawable = mListView.getSelector();
			//stateListDrawable.setColorFilter(App.mColorFilterBlueish);

		} else {
			//mTopHeaderText.setBackgroundDrawable(mResources		.getDrawable(R.drawable.stream_header_bg_newsfeed));
			//mListView.getSelector().setColorFilter(null);
		}

	}

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
	public boolean onContextItemSelected(MenuItem item) {
		boolean isHandled = App.onContextItemSelected(this, item, mProgressDialog);
		if(!isHandled){
			return super.onContextItemSelected(item);
		}
		return isHandled;
	}
		
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.clearHeader();
		
		ContextMenuDescriptor cmd = (ContextMenuDescriptor) v.getTag(R.id.tag_contextmenu_data);		
		if(cmd != null && cmd.type == App.CONTEXT_MENU_TOPHEADER_PROFILE){	
			App.createActorMenu(menu, mContextProfileData,StreamActivity.this);
			return;
		}
		
		super.onCreateContextMenu(menu, v, menuInfo);

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

			// String iconUri =
			// longClickedItemData.processedAttachmentData.mAttachment.mIcon;
			// Logger.l(Logger.DEBUG, LOG_TAG, "iconUri: " + iconUri);

			Logger.l(Logger.DEBUG, LOG_TAG, longClickedItemData.post_id
					+ ", processed attachment data null? "
					+ (processedAttachmentData == null));
			Logger.l(Logger.DEBUG, LOG_TAG,
					"processed attachment data mattachment null? "
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
			
			}

		}

		MenuItem viewCommentsMI;
		MenuItem unlikeMI;
		MenuItem likeMI;
		MenuItem postCommentsMI;

	
		if (longClickedItemData.likes_user_likes) {
			unlikeMI = menu.add(0, CONTEXT_MENUITEM_UNLIKE, 0, "Unlike!");
			unlikeMI.setOnMenuItemClickListener(mContextMenuItemListener);
		} else {
			if (longClickedItemData.likes_canlike) {
				likeMI = menu.add(0, CONTEXT_MENUITEM_UNLIKE, 0, "Like!");
				likeMI.setOnMenuItemClickListener(mContextMenuItemListener);
			}
		}

		mProgressDialog.setTitle(null);
		mProgressDialog.setMessage(App.INSTANCE.mResources
				.getString(R.string.getting_tagged_photos));
		
		ContextProfileData cpd = new ContextProfileData();
		cpd.name = longClickedItemData.actorName;
		cpd.actorId = longClickedItemData.actor_id;
		cpd.outhandler = R.id.outhandler_activity_streams;
		cpd.profileImageUri = longClickedItemData.processedData.mProfileImageUri;
		App.createActorMenu(menu, cpd,	StreamActivity.this);
		
		
		
	}

	private int getActivityMode(){
		int act = 0;
		if(mStreamMode == Facebook.STREAMMODE_LIVEFEED || 
			mStreamMode == Facebook.STREAMMODE_NEWSFEED){
			if(App.isSessionUser(mContextProfileData.actorId)){
				act = App.ACTIVITY_VIEW_MY_FEED;				
			}
		}else {
			if(App.isSessionUser(mContextProfileData.actorId)){
				act = App.ACTIVITY_VIEW_MY_WALL;				
			}else{
				act = App.ACTIVITY_VIEW_OTHER_WALL;
			}
		}		
		return act;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {	
		
		App.prepareOptionsMenu(this, null, getActivityMode(), menu);
		
		if (mStreamMode != Facebook.STREAMMODE_WALLFEED) {
			SubMenu toggleMode;
			if (mStreamMode == Facebook.STREAMMODE_LIVEFEED) {
				toggleMode = menu.addSubMenu(0, MENUITEM_TOGGLE_MODE, 0,
						"Newsfeed");
			} else {
				toggleMode = menu.addSubMenu(0, MENUITEM_TOGGLE_MODE, 0,
						"Livefeed");
			}
		} else {

		}
			
		SubMenu clearDB = menu.addSubMenu(0, MENUITEM_CLEAR_STREAM, 0,
				"Clear stream");
		clearDB.setIcon(android.R.drawable.ic_popup_disk_full);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean isHandled = App.onOptionsItemSelected(this, mContextProfileData, getActivityMode(), item);
		
		if(isHandled){
			return true;
		}
		
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
		
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
	
		return super.onMenuItemSelected(featureId, item);
	}

	// static long mUIHandlerSignature = 1000l;
	/*
	 * BroadcastReceiver mUpdateDirtyRows = new BroadcastReceiver() {
	 * 
	 * @Override public void onReceive(Context context, Intent intent) {
	 * Log.d(LOG_TAG, "onReceiveIntent: " + intent.getAction()); String rows[] =
	 * intent.getStringArrayExtra(XTRA_DIRTYPOSTS);
	 * mListViewAdapter.getViewFactory().markGroupAsDirty(rows); } };
	 */

	private int getProcessFlag(){
		
		if(mContextProfileData.actorId == Facebook.getInstance().getSession().uid){
			Toast.makeText(this, "SessionUser", 2000).show();
			return App.PROCESS_FLAG_SESSIONUSER;
		}		
		else {
			Toast.makeText(this, "NotSessionUser", 2000).show();
			return App.PROCESS_FLAG_OTHER;		
		}
	}
	
	private void resolveCursor(long[] uids) {

		boolean isDBLocked = App.INSTANCE.mDB.isDbLockedByCurrentThread();
		boolean isDBOpen = App.INSTANCE.mDB.isOpen();

		/*
		Logger.l(Logger.DEBUG, LOG_TAG, "[resolveCursor()] is db locked: "
				+ isDBLocked + ", is db open: " + isDBOpen
				+ " , sqlitedb null?: " + (App.INSTANCE.mDB == null));
*/
		
		if (mStreamsCursor != null) {
			mStreamsCursor.close();
		}

		mStreamsCursor = App.INSTANCE.mDBHelper.getStreams(
				App.INSTANCE.mDB,			
				mStreamMode, 
				getProcessFlag(),
				mContextProfileData.actorId, uids, 300l, 0l);

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

		mGetTaggedPhotosBroadcastReceiver = new GetTaggedPhotosReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				super.onReceive(context, intent);
				mProgressDialog.dismiss();
			}
		};

		mAutoStreamsUpdateBroadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				// Log.d("StreamActivity",
				// "BroadcastReceiver->onReceive, action:"+action);

				if (action.equals(App.INTENT_AUTOUPDATE_STREAMS)) {
					onFinishUpdatingStreams();
					if (mIsStreamsUpdateFinished) {
						Logger
								.l(Logger.DEBUG, "StreamActivity",
										"BroadcastReceiver->onReceive: start updating streams from the cloud");
						fetchStreamsFromCloud(false);
					} else {
						Logger
								.l(Logger.DEBUG, "StreamActivity",
										"BroadcastReceiver->onReceive: not done updating streams");
						Toast.makeText(context, "Not done updating..", 2000)
								.show();
					}

				}

			}
		};

	}

	private void startReceivingAutoUpdateIntent() {

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
		// mTopHeaderText.setText("Updating streams from the cloud..");
		mActionBar.disableAllButtons();
	}

	private void onFinishUpdatingStreams() {
		mIsStreamsUpdateFinished = true;
		mActionBar.enableAllButtons();
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
		/*
		 * String message = mStreamsCursor .getString(Stream.col_message +
		 * ApplicationDBHelper
		 * .START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC);
		 */
		/*
		 * String attachment = mStreamsCursor .getString(Stream.col_attachment +
		 * ApplicationDBHelper
		 * .START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC);
		 */
		// Logger.l(Logger.VERBOSE, LOG_TAG, "message: " + message);
		Logger.l(Logger.VERBOSE, LOG_TAG, "post_id: " + post_id);

		// Logger.l(Logger.VERBOSE, LOG_TAG, "attachment: " + attachment);

		App.showPost(this, post_id);
	}

	int fetchMaxStreams = 50;
	int fetchSizePerFetch = 25;
	int fetchNumTotalFetches = (int) Math.ceil(fetchMaxStreams
			/ fetchSizePerFetch);
	int fetchCurrentFetch = 0;
	boolean fetchIsCurrentlyIncrementing = false;
	boolean blockAutoUpdate = true;
	boolean fetchIsRefreshAll = true;

	private void fetchStreamsFromCloud(boolean isIncremental) {
		Logger.l(Logger.DEBUG, "crucial", "totalFetches: "
				+ fetchNumTotalFetches + ", fetchCurrentFetch: "
				+ fetchCurrentFetch);
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
		
		showLoadingIndicator();
		onStartUpdatingStreams();
		long numMaxPosts = Prefs.getNumFetchStreams();
		long lastStreamPostUpdateTime;

		int limit = fetchSizePerFetch;
		int offset = fetchCurrentFetch * fetchSizePerFetch;

		if (fetchIsRefreshAll) {
			lastStreamPostUpdateTime = 0;
		} else {
			lastStreamPostUpdateTime = App.INSTANCE.mDBHelper
					.getStreamLastUpdatedTime(App.INSTANCE.mDB);
		}

		if (mStreamMode != Facebook.STREAMMODE_WALLFEED) {
			Bundle extraData = new Bundle();
			int processingFlag = getProcessFlag();
			extraData.putInt(App.FLAG_STREAM_PROCESS_FLAG, processingFlag);
			mFacebook.getStreamsComplete(R.id.outhandler_activity_streams,
					extraData,
					Long
					.toString(mContextProfileData.actorId), null, lastStreamPostUpdateTime,
					ManagerThread.CALLBACK_GET_STREAMS_MQ,
					ManagerThread.CALLBACK_SERVERCALL_ERROR,
					ManagerThread.CALLBACK_TIMEOUT_ERROR, 1, offset, // using
																		// offset
																		// as
																		// the
																		// limit
																		// and 0
																		// as
																		// the
																		// offset,
																		// the
																		// behaviour
																		// is
																		// every
																		// fetching
																		// increases
																		// the
																		// fetch
																		// size
					0, 10, 0);
		}

		else {

			Bundle callbackData = new Bundle();
			int processingFlag = getProcessFlag();
			callbackData.putInt(App.FLAG_STREAM_PROCESS_FLAG, processingFlag);
			mFacebook.getWallPosts(R.id.outhandler_activity_streams,
					callbackData, Long.toString(mContextProfileData.actorId), 0,
					ManagerThread.CALLBACK_GET_STREAMS_MQ,
					ManagerThread.CALLBACK_SERVERCALL_ERROR,
					ManagerThread.CALLBACK_TIMEOUT_ERROR, 1, 10, 0);

		}

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