package com.neusou.moobook.activity;

import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.neusou.Logger;
import com.neusou.Utility;
import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.data.Event;
import com.neusou.moobook.view.TitleBar;
import com.neusou.web.ImageUrlLoader2.AsyncListener;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderInput;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderProgress;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderResult;

public class EventDetailsActivity extends BaseActivity {

	public static final String LOG_TAG = Logger.registerLog(EventDetailsActivity.class);
	String mEventId;
		
	Cursor c;
	
	public static Intent getIntent(Context ctx){
		return new Intent(ctx, EventDetailsActivity.class);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setBackgroundDrawable(mResources.getDrawable(R.drawable.metal));
		setContentView(R.layout.eventsdetail_activity);
		bindViews();
		getIntentExtras();
		initObjects();		
		initViews();
	}
	
	protected void onDestroy() {
		super.onDestroy();
		
	};
	
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		if(c!=null){
			c.deactivate();
		}
		
	}
	
	@Override
	protected void onPostResume() {
		super.onPostResume();
	}
	
	Event mEvent;
	
	TextView mTagline;
	TextView mName;
	TextView mDesc;
	TextView mTime;
	TextView mHost;
	TextView mVenue;
	TextView mLocation;
	TextView mType;
	ImageView mPicture;
	
	Button mGetComments;
		
	public void getIntentExtras(){		
		Intent i = getIntent();
		Bundle b =i.getExtras();
		mEvent = b.getParcelable(Event.XTRA_PARCELABLE_OBJECT);		
	}
		
	public void bindViews(){
		mTagline = (TextView) findViewById(R.id.tagline);		
		mName = (TextView) findViewById(R.id.name);
		mDesc = (TextView) findViewById(R.id.description);
		mTime = (TextView) findViewById(R.id.time);
		mHost = (TextView) findViewById(R.id.host);
		mVenue = (TextView) findViewById(R.id.venue);
		mLocation = (TextView) findViewById(R.id.location);
		mType = (TextView) findViewById(R.id.type);
		mPicture = (ImageView) findViewById(R.id.image);
		mGetComments = (Button) findViewById(R.id.getcomments);
	}
	TitleBar mTitleBar; 
	public void initViews(){
		mTitleBar = new TitleBar(this);
		mTitleBar.bind(this);
		mTitleBar.mHeaderText.setText(mEvent.name);
		
		Date mDate = new Date(mEvent.start_time * 1000);
		String eventDate = DateFormat.format(App.DATEFORMAT_EVENT_DATE, mDate).toString();		
		String eventStartTime = DateFormat.format(App.DATEFORMAT_EVENT_TIME, mDate).toString();
		mDate.setTime(mEvent.end_time * 1000);
		String eventEndTime = DateFormat.format(App.DATEFORMAT_EVENT_TIME, mDate).toString();
		
		mName.setText(mEvent.name);
		mTagline.setText(mEvent.tagline);		
		mDesc.setText(mEvent.description);
		mTime.setText("Date:"+eventDate+"\nStart:"+eventStartTime+"\nEnd:"+eventEndTime);
		mHost.setText("Host:"+mEvent.host);		
		mVenue.setText("Venue:"+mEvent.location_street+" "+mEvent.location_city+" "+mEvent.location_state+" "+mEvent.location_country);
		mLocation.setText("Location:"+mEvent.location);
		mType.setText("Type:"+Util.getNotNullString(mEvent.event_type)+" "+Util.getNotNullString(mEvent.event_subtype));
		
		AsyncLoaderInput input = new AsyncLoaderInput();
		if(mEvent.pic != null){
			input.imageUri = mEvent.pic;	
		}else if(mEvent.pic_big != null){
			input.imageUri = mEvent.pic_big;
		}else if(mEvent.pic_small != null){
			input.imageUri = mEvent.pic_small;
		}
		input.imageUri = mEvent.pic_big;
		input.imageView = mPicture;			
		
		AsyncListener listener = new AsyncListener() {
			
			@Override
			public void onPublishProgress(AsyncLoaderProgress progress) {
				progress.imageView.setImageBitmap(progress.bitmap);
			}
			
			@Override
			public void onPreExecute() {
				
			}
			
			@Override
			public void onPostExecute(AsyncLoaderResult result) {
				
			}
			
			@Override
			public void onCancelled() {
				
			}
		};
		App.INSTANCE.mImageUrlLoader2.loadImageAsync(App.INSTANCE.mExecScopeImageLoaderTask, input, listener);
		
		
		mGetComments.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = ViewCommentsActivity.getIntent(EventDetailsActivity.this);
				i.putExtra(ViewCommentsActivity.XTRA_OBJECTID, String.valueOf(mEvent.eid));
				i.putExtra(ViewCommentsActivity.XTRA_CLEARDATA, true);
				startActivity(i);
			}
			
		});
		
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {	
		super.onSaveInstanceState(outState);		
		outState = getIntent().getExtras();
	}
		
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {	
		super.onRestoreInstanceState(savedInstanceState);	
		
	}
	
}