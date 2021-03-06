package com.neusou.moobook.activity;

import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.PendingIntent.CanceledException;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.FilterQueryProvider;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ViewSwitcher.ViewFactory;

import com.admob.android.ads.AdView;
import com.neusou.Logger;
import com.neusou.Utility;
import com.neusou.moobook.App;
import com.neusou.moobook.AppMenu;
import com.neusou.moobook.FBComment;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.FQL;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;
import com.neusou.moobook.adapters.CommentsAdapter;
import com.neusou.moobook.adapters.ContactsAdapter;
import com.neusou.moobook.adapters.GenericPageableAdapter;
import com.neusou.moobook.adapters.IPageableListener;
import com.neusou.moobook.adapters.PageableDataStore;
import com.neusou.moobook.controller.BaseListViewFactory;
import com.neusou.moobook.controller.ContactsListViewFactory;
import com.neusou.moobook.controller.GetTaggedPhotosReceiver;
import com.neusou.moobook.controller.StandardUiHandler;
import com.neusou.moobook.controller.StreamListViewFactory;
import com.neusou.moobook.data.Comment;
import com.neusou.moobook.data.ContextProfileData;
import com.neusou.moobook.data.Stream;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.moobook.task.DeniedTaskCreationException;
import com.neusou.moobook.task.ProcessTaskFactory;
import com.neusou.moobook.task.ProcessUsersTask;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;
import com.neusou.moobook.view.ActionBar;
import com.neusou.web.IntelligentPagingInfo;
import com.neusou.web.PagingInfo;

public class ViewContactsActivity extends BaseActivity {

	static final String LOG_TAG = Logger
			.registerLog(ViewContactsActivity.class);

	static int numThreadInitializations = 1;
	static CountDownLatch threadsInitCountDown;

	View mLoadingIndicator;

	static final byte MESSAGE_UPDATE = 0;
	static final byte MESSAGE_PROGRESS = 1;
	static final byte MESSAGE_TIMEOUT = 2;
	static final byte MESSAGE_FINISH = 3;
	static final byte MESSAGE_START = 4;

	Facebook mFacebook;

	Cursor c;
	int commentStartIndex = 0;

	// Views
	AdView mAdView;
	ListView mListView;
	ContactsAdapter mListAdapter;
	ContactsListViewFactory mListViewFactory;
	TextSwitcher mTopHeaderText;
	ProgressDialog mProgressDialog;
	View.OnClickListener mPostOnClickLst;
	IPageableListener mAdapterListener;

	ContactsListViewFactory.Data longClickedItemData;

	boolean mIsAsyncLoadingFinished = true;

	public static final int DEFAULT_PAGING_WINDOW_SIZE = 5;
	ManagerThread mWorkerThread;
	JSONArray data_comments = null;
	PagingInfo mPagingInfo;
	static String LABEL_HEADER_COMMENTS = "Contacts";

	static final byte SELECTEDCOLUMNS_USER_DISPLAY = 0;
	static final byte SELECTEDCOLUMNS_USER_FETCH = 1;

	static short[][] mSelectedUserColumns = new short[][] {
			{ User.col_name, User.col_pic, User.col_timezone,
					User.col_pic_square, User.col_uid,
					User.col_hometown_location, User.col_current_location,
					User.col_birthday, User.col_birthday_date,
					User.col_proxied_email },
			{ User.col_name, User.col_pic, User.col_timezone,
					User.col_pic_square, User.col_uid,
					User.col_hometown_location, User.col_current_location,
					User.col_birthday, User.col_birthday_date,
					User.col_proxied_email } };

	Handler mUIHandler; /*
						 * = new StandardHandler(){ public void
						 * handleMessage(android.os.Message msg) {
						 * Logger.l(Logger.DEBUG, LOG_TAG,
						 * "[handleMessage()] "+msg.what); int code = msg.what;
						 * Bundle data = msg.getData();
						 * 
						 * switch(code){
						 * 
						 * 
						 * case ManagerThread.MESSAGE_DISMISS_DIALOG:{
						 * mProgressDialog.dismiss(); break; }
						 * 
						 * case ManagerThread.MESSAGE_UPDATELIST:{
						 * Logger.l(Logger.DEBUG,LOG_TAG,
						 * "[Handler()] [handleMessage()] update list");
						 * mListAdapter.notifyDataSetChanged();
						 * onFinishUpdatingContacts(); break; } case
						 * ManagerThread.CALLBACK_ADMOB_ONFAILRECEIVE:{
						 * mAdView.setVisibility(View.GONE); break; }
						 * 
						 * case ManagerThread.CALLBACK_ADMOB_ONRECEIVE:{
						 * mAdView.setVisibility(View.VISIBLE); break; }
						 * 
						 * case BaseManagerThread.CALLBACK_TIMEOUT_ERROR:{
						 * Toast.makeText(ViewContactsActivity.this,
						 * "Request to Facebook timed out", 2000).show();
						 * onFinishUpdatingContacts();
						 * mProgressDialog.dismiss(); break; }
						 * 
						 * case BaseManagerThread.CALLBACK_SERVERCALL_ERROR:{
						 * String reason =
						 * (String)data.getString(Facebook.XTRA_SERVERCALL_ERROR_MSG
						 * ); int errorCode =
						 * data.getInt(Facebook.XTRA_SERVERCALL_ERROR_CODE);
						 * Toast.makeText(ViewContactsActivity.this,
						 * errorCode+":"+reason, 2000).show();
						 * onFinishUpdatingContacts();
						 * mProgressDialog.dismiss(); break; }
						 * 
						 * case
						 * BaseManagerThread.CALLBACK_PROCESS_WSRESPONSE_ERROR:{
						 * FBWSResponse fbResponse = (FBWSResponse) msg.obj;
						 * Toast.makeText(ViewContactsActivity.this,fbResponse.
						 * errorMessage, 1000).show();
						 * onFinishUpdatingContacts();
						 * mProgressDialog.dismiss(); break; }
						 * 
						 * } };
						 * 
						 * };
						 */

	class PageableJsonData implements PageableDataStore<JSONArray> {
		JSONArray data;

		public void set(JSONArray data) {
			this.data = data;
		}

		@Override
		public void clear() {
			data = null;
		}

		public JSONArray get(int index) {
			return data;
		}

		@Override
		public Object getAt(int index) {
			if (data == null) {
				return null;
			}
			try {
				return data.get(index);
			} catch (JSONException e) {

			}

			return null;

		}

		@Override
		public int size() {
			if (data == null) {
				return 0;
			}
			return data.length();
		}

	};

	public static Intent getIntent(Context ctx) {
		return new Intent(ctx, ViewContactsActivity.class);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setBackgroundDrawable(
				mResources.getDrawable(R.drawable.metal));
		setContentView(R.layout.contacts_activity);
		bindViews();
		initObjects();
		initBroadcastReceiver();
		getIntentExtras();
		initViews();
		// getIntentExtras();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mFacebook.purgeInactiveOutHandlers(false);
		mFacebook = null;
		c.close();
		System.gc();
	}

	@Override
	protected void onStart() {
		super.onStart();
		mFacebook.registerOutHandler(R.id.outhandler_activity_viewcontacts,
				mWorkerThread.getInHandler());
		mWorkerThread.setOutHandler(mUIHandler);
		assert (mFacebook != null);
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	protected void onPause() {
		super.onPause();

		if (c != null) {
			c.deactivate();
		}

		try {
			unregisterReceiver(mGetTaggedPhotosBroadcastReceiver);
		} catch (IllegalArgumentException e) {
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		resolveCursor();
		updateList();

		if (c == null || c.getCount() == 0) {
			getContactsFromCloud(PagingInfo.CURRENT);
		} else {
		}

		registerReceiver(mGetTaggedPhotosBroadcastReceiver,
				GetTaggedPhotosReceiver.INTENT_FILTER);
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

	public void getIntentExtras() {
		Intent i = getIntent();
		Bundle b = i.getExtras();
		mContextProfileData = b
				.getParcelable(ContextProfileData.XTRA_PARCELABLE_OBJECT);
	}

	protected void bindViews() {
		mAdView = (AdView) findViewById(R.id.ad);
		mLoadingIndicator = findViewById(R.id.loadingindicator);

		mListView = (ListView) findViewById(R.id.list);
		mTopHeaderText = (TextSwitcher) findViewById(R.id.topheader);
	}

	ActionBar mActionBar;

	protected void initObjects() {
		super.initObjects();

		mActionBar = new ActionBar();
		mActionBar.bindViews(this);
		mActionBar.hideButton(ActionBar.BUTTON_POST);
		mActionBar.setOnReloadClick(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				getContactsFromCloud(PagingInfo.CURRENT);

			}

		});

		mProgressDialog = new ProgressDialog(this);

		// Initialize static variables
		// check if any of the required static threads died, recreate countdown
		// latch and initialize all threads
		boolean initStatics = false;
		if (mWorkerThread == null || !mWorkerThread.isAlive()) {
			initStatics = true;
		}
		if (initStatics) {
			threadsInitCountDown = new CountDownLatch(numThreadInitializations);
			mWorkerThread = new ManagerThread(threadsInitCountDown);
			mWorkerThread.start();

			try {
				Logger
						.l(Logger.DEBUG, LOG_TAG,
								"[initObjects()] waiting thread initializations to complete");
				threadsInitCountDown.await();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			Logger.l(Logger.DEBUG, LOG_TAG,
					"[initObjects()] thread initializations completed.");

		}

		// end of static initializations

		mFacebook = Facebook.getInstance();

		// get intent extra parameters

		mListViewFactory = new ContactsListViewFactory(this);
		mListAdapter = new ContactsAdapter(this, mListViewFactory, 0);
		mListViewFactory.setDataSetObserver(mListAdapter.getObserver());
		mListViewFactory
				.setDisplayColumns(mSelectedUserColumns[SELECTEDCOLUMNS_USER_DISPLAY]);

		mFilterQueryProvider = new FilterQueryProvider() {
			@Override
			public Cursor runQuery(CharSequence constraint) {
				return App.INSTANCE.mDBHelper.getAllUsers(App.INSTANCE.mDB,
						mSelectedUserColumns[SELECTEDCOLUMNS_USER_DISPLAY], 1,
						"name like " + constraint.toString() + "%",
						getProcessFlag());
			}
		};

		// Initialize listeners

		mAdapterListener = new IPageableListener() {
			@Override
			public void onHasNext() {
				Logger.l(Logger.DEBUG, LOG_TAG,
						"[CommentsAdapter.Listener] onHasMoreData()");
				// getCommentsFromCloud(-1);
			}

			@Override
			public void onHasPrev() {
				Logger.l(Logger.DEBUG, LOG_TAG,
						"[CommentsAdapter.Listener] onHasLessData()");
				// getCommentsFromCloud(1);
			}

			@Override
			public void onGetNext() {
				Logger.l(Logger.DEBUG, LOG_TAG,
						"[CommentsAdapter.Listener] onGetNext()");
				// mListAdapter.onStartLoadingNext();
				getContactsFromCloud(PagingInfo.NEXT);
			}

			@Override
			public void onGetPrev() {
				Logger.l(Logger.DEBUG, LOG_TAG,
						"[CommentsAdapter.Listener] onGetPrev()");
				// mListAdapter.onStartLoadingPrev();
				getContactsFromCloud(PagingInfo.PREV);
			}

		};

		mPagingInfo = new PagingInfo(0);

		mUIHandler = new StandardUiHandler(this, mProgressDialog,
				findViewById(R.id.adview_wrapper),
				findViewById(R.id.bottomheader)) {
			@Override
			public void handleMessage(Message msg) {

				super.handleMessage(msg);

				int code = msg.what;

				switch (code) {

				case ManagerThread.MESSAGE_DISMISS_DIALOG: {
					mProgressDialog.dismiss();
					break;
				}

				case ManagerThread.MESSAGE_UPDATELIST: {
					Logger.l(Logger.DEBUG, LOG_TAG,
							"[Handler()] [handleMessage()] update list");
					mListAdapter.notifyDataSetChanged();
					onFinishUpdatingContacts();
					break;
				}
				}
			}
		};
	}

	protected void initViews() {

		mListAdapter.setFilterQueryProvider(mFilterQueryProvider);

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
		mListView.setRecyclerListener(mListViewFactory);

		Drawable stateListDrawable = mListView.getSelector();
		stateListDrawable.setColorFilter(App.mColorFilterBlueish);
		try {
			mListView.setDivider(mResources
					.getDrawable(android.R.drawable.divider_horizontal_bright));
		} catch (Exception e) {

		}
		// mListView.setItemsCanFocus(true);
		mListView
				.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> arg0, View v,
							int position, long arg3) {
						Log.d("debug", "onItemLongClick " + position);
						Log.d("debug", "onItemLongClick "
								+ v.getClass().getCanonicalName());
						longClickedItemData = (ContactsListViewFactory.Data) v
								.getTag(R.id.tag_contactsadapter_item_data);
						return false;// return false to pass on the event, so
										// that context menu gets displayed
					}

				});

		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				GenericPageableAdapter.InternalTag internalTag = (GenericPageableAdapter.InternalTag) arg1
						.getTag(GenericPageableAdapter.TAG_INTERNAL);
				if (internalTag != null) {
					if (internalTag.viewType == CommentsAdapter.VIEWTYPE_LOADER) {
						int direction = internalTag.loadDirection;
						if (direction == PagingInfo.NEXT) {
							if (mIsAsyncLoadingFinished) {
								// mListAdapter.onStartLoadingNext();
								// getContactsFromCloud(PagingInfo.NEXT);
							}
						} else if (direction == PagingInfo.PREV) {
							if (mIsAsyncLoadingFinished) {
								// mListAdapter.onStartLoadingPrev();
								// getContactsFromCloud(PagingInfo.PREV);
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
				t = (TextView) (mLayoutInflater.inflate(
						R.layout.t_topheadertext, null));
				return t;
			}
		});

		App.initAdMob(mAdView, mUIHandler);
	}

	public void resolveCursor() {
		if (c != null && !c.isClosed()) {
			c.requery();
		} else {
			c = App.INSTANCE.mDBHelper.getAllUsers(App.INSTANCE.mDB,
					mSelectedUserColumns[SELECTEDCOLUMNS_USER_DISPLAY], 1,
					null, getProcessFlag());
		}
	}

	BroadcastReceiver mGetTaggedPhotosBroadcastReceiver;

	private void initBroadcastReceiver() {
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		boolean isHandled = App.onContextItemSelected(this, item,
				mProgressDialog);
		if (!isHandled) {
			return super.onContextItemSelected(item);
		}
		return isHandled;
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		super.onCreateContextMenu(menu, v, menuInfo);
		menu.clearHeader();

		ContextProfileData cpd = new ContextProfileData();
		cpd.name = longClickedItemData.actorname;
		cpd.actorId = longClickedItemData.uid;
		cpd.outhandler = R.id.outhandler_activity_streams;
		cpd.profileImageUri = longClickedItemData.profileImageUri;
		App.createActorMenu(menu, cpd, ViewContactsActivity.this);

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		return mProgressDialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		ProgressDialog d = (ProgressDialog) dialog;

	}

	private void updateList() {
		Logger.l(Logger.DEBUG, LOG_TAG, "[updateList()]");
		mListAdapter.setData(c);
		mListAdapter.notifyDataSetChanged();
	}

	private void clearAllData() {
		Logger.l(Logger.DEBUG, LOG_TAG, "[clearAllData()]");

	}

	private void showLoadingIndicator() {
		mLoadingIndicator.setVisibility(View.VISIBLE);
		setProgressBarIndeterminateVisibility(true);
	}

	private void hideLoadingIndicator() {
		mLoadingIndicator.setVisibility(View.INVISIBLE);
		setProgressBarIndeterminateVisibility(false);
	}

	private void onStartUpdatingContacts() {
		mIsAsyncLoadingFinished = false;
		showLoadingIndicator();
		setTitle("moobook");
		mTopHeaderText.setText("Loading contacts from cloud..");
		mActionBar.setEnabledButton(ActionBar.BUTTON_RELOAD, false);
	}

	private void onFinishUpdatingContacts() {
		mIsAsyncLoadingFinished = true;
		hideLoadingIndicator();
		resetHeaderText();
		resolveCursor();
		updateList();
		mActionBar.setEnabledButton(ActionBar.BUTTON_RELOAD, true);
	}

	private void setHeaderText(String text) {
		mTopHeaderText.setText(text);
	}

	private void resetHeaderText() {
		if (mPagingInfo != null && mPagingInfo.totalCount >= 0) {
			setHeaderText(LABEL_HEADER_COMMENTS);
		} else {
			setHeaderText(LABEL_HEADER_COMMENTS);
		}
	}

	private void getContactsFromCloud(int direction) {
		if (!mIsAsyncLoadingFinished) {
			return;
		}
		long uid;

		try {
			uid = Facebook.getInstance().getCurrentSession().uid;
		} catch (Exception e) {
			return;
		}

		onStartUpdatingContacts();
		Logger.l(Logger.DEBUG, LOG_TAG, "getContactsFromCloud() " + direction);
		int numComments = 0;

		if (data_comments != null) {
			numComments = data_comments.length();
		}

		mPagingInfo.compute(direction);

		Logger.l(Logger.DEBUG, LOG_TAG,
				"getCommentsFromCloud() paging window size: "
						+ mPagingInfo.windowSize + ", start:"
						+ mPagingInfo.getNextStart());

		// don't forget to pass in the processing information to process the
		// returned api call.
		Bundle extraData = new Bundle();
		int processingFlag = getProcessFlag();
		extraData.putInt(App.FLAG_USER_PROCESS_FLAG, processingFlag);

		// now call the api
		mFacebook.getContacts(R.id.outhandler_activity_viewcontacts, extraData,
				uid, ManagerThread.CALLBACK_GET_CONTACTS,
				ManagerThread.CALLBACK_SERVERCALL_ERROR,
				ManagerThread.CALLBACK_TIMEOUT_ERROR, 0,
				// mPagingInfo.windowSize,
				// mPagingInfo.getNextStart());
				500, 0);

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	FilterQueryProvider mFilterQueryProvider;

	@Override
	public boolean onSearchRequested() {
		super.onSearchRequested();

		SearchManager sm = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		//App.onOptionsItemSelected(this, null, R.layout.contacts_activity, item);
		//byte id = (byte) item.getItemId();
		/*
		 * switch(id){ case MENUITEM_REFRESH:{
		 * 
		 * break; } }
		 */
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		//ContextProfileData cpd = App.createSessionUserContextProfileData();
		//App.prepareOptionsMenu(this, cpd, R.layout.contacts_activity, menu);
		return true;
	}

	ContextProfileData mContextProfileData;

	public int getProcessFlag() {

		if (mContextProfileData.actorId == Facebook.getInstance().getCurrentSession().uid) {
			return App.PROCESS_FLAG_USER_CONNECTED;
		} else {
			return App.PROCESS_FLAG_USER_OTHER;
		}

	}

	public static class WorkerManagerThread extends BaseManagerThread {

		CountDownLatch mProcessCommentsWaitCountdown;
		ProcessUsersTask mProcessUsersTask;
		Context ctx;
		int mProcessFlag;

		public WorkerManagerThread(CountDownLatch cdl, Context ctx,
				int processFlag) {
			super(cdl);
			this.ctx = ctx;
			mProcessFlag = processFlag;
		}

		public void finalize() {
			if (mProcessUsersTask != null) {
				mProcessUsersTask.cancel(true);
				mProcessUsersTask = null;
			}
		}

		public void doBusiness(Bundle data, int code, FBWSResponse fbresponse) {

			switch (code) {

			case ManagerThread.CALLBACK_GET_CONTACTS: {
				Logger.l(Logger.DEBUG, LOG_TAG,
						"[ViewContactsAct] [callback_get_comments] "
								+ fbresponse.data);
				if (!fbresponse.hasErrorCode && fbresponse.jsonArray != null) {
					int numComments = fbresponse.jsonArray.length();
					if (numComments > 0) {
						String[] uids = App.getCommentsUids(
								fbresponse.jsonArray, null);
						Logger.l(Logger.DEBUG, LOG_TAG,
								"[ViewContactsAct] [callback_get_contacts] response: "
										+ fbresponse.data);
						data
								.putShortArray(
										Facebook.XTRA_TABLECOLUMNS_SHORTARRAY,
										mSelectedUserColumns[SELECTEDCOLUMNS_USER_FETCH]);

						ProcessUsersTask procUsersTask;
						try {
							procUsersTask = ProcessTaskFactory
									.create(ProcessUsersTask.class);
							// Toast.makeText(ctx, "Success Task Creation",
							// 2000).show();
							mProcessUsersTask.init(mOutHandler, MESSAGE_START,
									MESSAGE_UPDATE,
									ManagerThread.MESSAGE_UPDATELIST,
									MESSAGE_PROGRESS, MESSAGE_TIMEOUT,
									App.PROCESS_FLAG_USER_CONNECTED);
							mProcessUsersTask.execute(data);
						} catch (DeniedTaskCreationException e) {
							// Toast.makeText(ctx, "Denied Task Creation",
							// 2000).show();
						}

					}
				}
				break;
			}
			}
		};

	}

}