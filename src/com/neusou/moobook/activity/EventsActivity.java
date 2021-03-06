package com.neusou.moobook.activity;

import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;

import org.json.JSONArray;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SyncAdapterType;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import com.admob.android.ads.AdView;
import com.neusou.moobook.App;
import com.neusou.moobook.FBConnectionException;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;
import com.neusou.moobook.controller.CursorListAdapter;
import com.neusou.moobook.controller.EventsListViewFactory;
import com.neusou.moobook.data.Event;
import com.neusou.moobook.data.Event.RSVPStatus;
import com.neusou.moobook.task.ResponseProcessor;
import com.neusou.moobook.thread.ManagerThread;
import com.neusou.async.UserTask;

public class EventsActivity extends BaseActivity {

	static final int UIMETHOD_RELOADEVENTS = 2;
	
	static final byte MENUITEM_REFRESH = 0;
	static final byte MENUITEM_DELETE = 1;

	String mLblRefresh;
	String mLblDelete;
	String mLblLoadingComments;
	String mLblPostingComment;
	String mLblDeletingComment;

	View.OnClickListener mPostOnClickLst;
	View mLoadingIndicator;

	EventsListViewFactory mListViewFactory;	
	ProgressDialog mProgressDialog;

	static String mLblCommentPosted;
	static String mLblCommentDeleted;

	Cursor c;
	Facebook mFacebook;

	EditText mComment;
	
	AdView mAdView;
	String post_id;
	ListView mListView;
	CursorListAdapter<Event> mListAdapter;
	TextSwitcher mTopHeaderText;

	
	long numMaxEvents;
	EventsListViewFactory.Holder mLongItemClickData;
	FBWSResponse commentsData;
	GetEventsTask mGetEventsTask;

	Handler mUIHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			// Log.d(LOG_TAG,"ui hander handle message what:"+msg.what);
			int code = msg.what;
			Bundle data = msg.getData();

			switch (code) {

			case ManagerThread.CALLBACK_ADMOB_ONFAILRECEIVE: {
				mAdView.setVisibility(View.GONE);
				break;
			}

			case ManagerThread.CALLBACK_ADMOB_ONRECEIVE: {
				mAdView.setVisibility(View.VISIBLE);
				break;
			}

			

			}
		};
	};

	public static Intent getIntent(Context ctx) {
		return new Intent(ctx, EventsActivity.class);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setBackgroundDrawableResource(R.drawable.metal);		
		setContentView(R.layout.events_activity);
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
		resolveCursor();
		updateListView();
		getEventsFromCloud();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if(c!=null){
			c.deactivate();
		}
		if(mGetEventsTask!=null){
			mGetEventsTask.cancel(true);
		}
	};
	
	@Override
	protected void onPause() {	
		super.onPause();
	
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		return mProgressDialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		ProgressDialog d = (ProgressDialog) dialog;
/*
		switch (id) {

		case DIALOG_DELETINGCOMMENT: {
			d.setMessage(mLblDeletingComment);
			d.setCancelable(false);
			break;
		}
		case DIALOG_POSTINGCOMMENT: {
			d.setMessage(mLblPostingComment);
			d.setCancelable(false);
			break;
		}
		}
		*/
	}

	protected void bindViews() {
		mAdView = (AdView) findViewById(R.id.ad);
		App.initAdMob(mAdView, mUIHandler);
		mListView = (ListView) findViewById(R.id.list);
		mLoadingIndicator = findViewById(R.id.loadingindicator);
		mTopHeaderText = (TextSwitcher) findViewById(R.id.topheader);
	}

	protected void initObjects() {
		mFacebook = Facebook.getInstance();
		
		//App.INSTANCE.restoreSessionInfo();
		
		mProgressDialog = new ProgressDialog(this);

		mLblRefresh = mResources.getString(R.string.refresh);
		mLblDelete = mResources.getString(R.string.delete);
		mLblPostingComment = mResources.getString(R.string.postingcomment);
		mLblDeletingComment = mResources.getString(R.string.deletingcomment);
		mLblLoadingComments = mResources.getString(R.string.loadingcomments);

		mLblCommentPosted = mResources.getString(R.string.commentposted);
		mLblCommentDeleted = mResources.getString(R.string.commentdeleted);

	}

	
	protected void initViews() {
		
		mListViewFactory = new EventsListViewFactory(this);
			
		mListAdapter = new CursorListAdapter<Event>(this, mListViewFactory, Event.col_eid, Event.RowMapper);
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
		mListView.setRecyclerListener(mListViewFactory);
		
		Drawable stateListDrawable = mListView.getSelector();
		stateListDrawable.setColorFilter(App.mColorFilterBlueish);
		try {
			mListView.setDivider(mResources
					.getDrawable(android.R.drawable.divider_horizontal_bright));
		} catch (Exception e) {

		}
		
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, View v, int position,
					long arg3) {
								
				Event e = mListAdapter.getItem(position);
				Intent i = EventDetailsActivity.getIntent(EventsActivity.this);
				i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				i.putExtra(Event.XTRA_PARCELABLE_OBJECT, e);
				startActivity(i);
			}			
			
		});
		
		mListView
				.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
					@Override
					public boolean onItemLongClick(AdapterView<?> arg0, View v,
							int position, long arg3) {
					//	Log.d("debug", "onItemLongClick " + position);
					//	Log.d("debug", "onItemLongClick "
						//		+ v.getClass().getCanonicalName());
						mLongItemClickData = (EventsListViewFactory.Holder) v.getTag(R.id.tag_eventsadapter_item_view);
						return false;// return false to pass on the event, so
										// that context menu gets displayed
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

	}

	public void resolveCursor(){
		if(c!=null && !c.isClosed()){
			c.requery();
		}else{			
			c = App.INSTANCE.mDBHelper.getEvents(App.INSTANCE.mDB);
		}
	}

	private void onStartUpdatingEvents() {
		mIsCommentsUpdateFinished = false;
		setTitle("moobook");
		mTopHeaderText.setText("Loading events from cloud..");
	}

	private void onFinishUpdatingEvents() {
		mIsCommentsUpdateFinished = true;
		hideLoadingIndicator();
		resetHeaderText();
	}

	private void showLoadingIndicator(){	
		mLoadingIndicator.setVisibility(View.VISIBLE);
		setProgressBarIndeterminateVisibility(true);
	}
	
	private void hideLoadingIndicator(){
		mLoadingIndicator.setVisibility(View.INVISIBLE);
		setProgressBarIndeterminateVisibility(false);
	}

	private void resetHeaderText() {
		mTopHeaderText.setText("Events");
	}

	class GetEventsTask extends UserTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			doGetEventsFromCloud();	
			return null;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			onStartUpdatingEvents();
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					resolveCursor();
					updateListView();
					onFinishUpdatingEvents();		
				}
				
			});
			
		}
	}
	
	private void updateListView(){	
		mListAdapter.setData(c);
		mListAdapter.notifyDataSetChanged();	
	}
	
	private void getEventsFromCloud() {
		mGetEventsTask = new GetEventsTask();
		mGetEventsTask.execute();
	}
	
	private void doGetEventsFromCloud() {
		long start_time = new Date().getTime() / 1000;
		FBWSResponse fbwsresponse = null;
		
		try{
		 fbwsresponse = mFacebook.getEvents(mFacebook.getCurrentSession().uid, start_time);
		}catch(FBConnectionException e){
			return;
		}
		
		if(fbwsresponse == null){
			return;
		}
		if (fbwsresponse.hasErrorCode) {
			return;
		}
		HashMap<String, JSONArray> data = Facebook.parseMultiFQLResult(
				fbwsresponse, null);
		JSONArray events_member = data.get("events_member");
		JSONArray events = data.get("events");

		ResponseProcessor.processEvents(events, events_member, App.INSTANCE.mDB, App.INSTANCE.mDBHelper,
				this);
	}

	static final String LOG_TAG = "EventsActivity";

	boolean mIsCommentsUpdateFinished = false;

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENUITEM_REFRESH, 0, mLblRefresh);
		return super.onCreateOptionsMenu(menu);
	}

	static final int CONTEXTMENU_ATTEND = 0;
	static final int CONTEXTMENU_UNSURE = 1;
	static final int CONTEXTMENU_DECLINE = 2;
		
	MenuItem cmi_attend;
	MenuItem cmi_unsure;
	MenuItem cmi_decline;
	
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		String rsvp_text = mLongItemClickData.rsvp_status.getText().toString();
		if(! Event.RSVPStatus.ATTENDING.match(rsvp_text)){
			menu.add(0,CONTEXTMENU_ATTEND,0,"Attend");	
		}
		if(! Event.RSVPStatus.UNSURE.match(rsvp_text)){
			menu.add(0,CONTEXTMENU_UNSURE,0,"Unsure");	
		}
		
		if(! Event.RSVPStatus.DECLINED.match(rsvp_text)){
			menu.add(0,CONTEXTMENU_DECLINE,0,"Decline");	
		}
				
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	AsyncTask mChangeEventRSVPTask;
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Toast.makeText(this, mLongItemClickData.description.getText()+":"+mLongItemClickData.eid , 2000).show();
		long eid = mLongItemClickData.eid;
		int itemId = item.getItemId();
		RSVPStatus newRSVP = RSVPStatus.UNSURE;
		switch(itemId){
			case CONTEXTMENU_DECLINE:{
				newRSVP = RSVPStatus.DECLINED;
				break;
			}
			case CONTEXTMENU_ATTEND:{
				newRSVP = RSVPStatus.ATTENDING;
				break;
			}
			case CONTEXTMENU_UNSURE:{
				newRSVP = RSVPStatus.UNSURE;
				break;
			}
		}
		
		if(mChangeEventRSVPTask != null && mChangeEventRSVPTask.getStatus() != AsyncTask.Status.FINISHED){
			return true;
		}
		
			TreeMap<String,Object> params = new TreeMap<String,Object>();
			params.put("eid", eid);
			params.put("rsvp_status",newRSVP);
			
			mChangeEventRSVPTask = new AsyncTask<TreeMap<String,Object>, Void, Boolean>(){
				
				FBWSResponse fbresponse;
				long eid;
				RSVPStatus rsvp_status;
				
				@Override
				protected Boolean doInBackground(TreeMap<String,Object>... params) {
					TreeMap<String, Object> param = params[0];
					eid = (Long)param.get("eid");
					rsvp_status = (RSVPStatus) param.get("rsvp_status");
					
					try{
						fbresponse = mFacebook.events_rsvp(eid,rsvp_status);
						//Log.d("debug",fbresponse.data);
						if(fbresponse.hasErrorCode){
							return false;
						}
						return Boolean.parseBoolean(fbresponse.data);
					}catch(FBConnectionException e){
						return false;
					}					
				}
				
				protected void onPostExecute(Boolean result) {
					if(result){
						Toast.makeText(EventsActivity.this, "RSVP Changed.", 3000).show();
						long rows_updated = App.INSTANCE.mDBHelper.updateEventRSVP(App.INSTANCE.mDB, eid, rsvp_status);
						//Log.d("debug","rows updated: "+rows_updated);
						updateListView();
					}else{
						Toast.makeText(EventsActivity.this, "Unable to change RSVP status. " +
								"(Error "+fbresponse.errorCode+":"+fbresponse.errorMessage+")", 3000).show();
					}
				};
			}.execute(params);			
			
		
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		byte id = (byte) item.getItemId();
		switch (id) {
		case MENUITEM_REFRESH: {
			getEventsFromCloud();
			break;
		}
		}
		return super.onOptionsItemSelected(item);
	}

	abstract class QuickTask extends AsyncTask{
		int message;
		Handler handler;
		
		public QuickTask(int message, Handler handler) {
			this.message = message;
			this.handler = handler;
		}
		
		@Override
		protected Object doInBackground(Object... params) {
			handler.sendEmptyMessage(message);
			return null;
		}
		
	};
	
	
}