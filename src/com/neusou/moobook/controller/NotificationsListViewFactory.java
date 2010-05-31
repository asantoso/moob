package com.neusou.moobook.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.data.FBApplication;
import com.neusou.moobook.data.FBNotification;
import com.neusou.moobook.data.Stream;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.web.ImageUrlLoader2;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderProgress;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderResult;

public class NotificationsListViewFactory extends BaseListViewFactory<Cursor> {
	static final String LOG_TAG = "NotificationsListViewFactory";
	
	Context ctx;
	ApplicationDBHelper mDBHelper;
	SQLiteDatabase mDB;
	
	public View.OnTouchListener mItemTouchListener;
	static int DATA_TAGID = R.id.tag_notificationsadapter_item;
	
	ArrayList<Long> mAsyncLoadState = new ArrayList<Long>();
	
	ImageUrlLoader2.AsyncListener  mAsyncLoaderListener = new ImageUrlLoader2.AsyncListener() {		
		@Override
		public void onPreExecute() {
			
			
		}		
		@Override
		public void onPostExecute(AsyncLoaderResult result) {
			if(result.status == ImageUrlLoader2.AsyncLoaderResult.SUCCESS){
				if(mDataSetObserver != null){
					//Logger.l(Logger.DEBUG, LOG_TAG, "calling dataSetObserver onChanged()");
					//mDataSetObserver.onChanged();
				}
			}			
		}		
		@Override
		public void onCancelled() {
		}
		
		@Override
		public void onPublishProgress(AsyncLoaderProgress progress) {
			Logger.l(Logger.DEBUG, LOG_TAG, "[onPublishProgress()] progress:"+progress.code);
			
			
			int foundIndex = Collections.binarySearch(mAsyncLoadState, progress.code);
			
			if(foundIndex!=-1){
//				Logger.l(Logger.DEBUG, LOG_TAG, "[onPublishProgress()] progress:"+progress.code+". code is valid");
				if(progress.success){
	//				Logger.l(Logger.DEBUG, LOG_TAG, "[onPublishProgress()] progress:"+progress.code+" updating..");
					progress.imageView.setImageBitmap(progress.bitmap);
				}else{
		//			Logger.l(Logger.DEBUG, LOG_TAG, "[onPublishProgress()] progress:"+progress.code+". progress failed.");
					progress.imageView.setImageBitmap(null);
				}
			}
		}
	};
	
	public NotificationsListViewFactory(Activity app, ListView listView) {
		super(app);
		this.ctx = app.getApplicationContext();
		Resources res = ctx.getResources();
		
		
	}

	public class Holder {				
		public TextView title;
		public TextView name;
		public TextView message;
		public ImageView pic;
		public ImageView appicon;
		public TextView since;
		public long position;
	}
	
	public void destroy() {
		if(this.mDB != null){
			this.mDB.close();
		}
		if(this.mDBHelper != null){
			this.mDBHelper.close();
		}
	}
	
	synchronized public View createView(Cursor ds, int position,
			View convertView, final ViewGroup parent) {
		
		
		//Logger.l(Logger.DEBUG, "debug", "[createView()] "+position);
		
		mAsyncLoadState.add((long)position);
		
		try {
			// check if the cursor is still open
			// cursor can be closed when the corresponding sqlitedb is closed
			// caused by orientation screen changes.
			//
			ds.moveToPosition(position);
		} catch (Exception e) {
			return mLayoutInflater.inflate(R.layout.empty, parent, false);
		}
		
		Holder tag;

		if (convertView != null) {
			tag = (Holder) convertView.getTag(DATA_TAGID);
			if(tag.position != position){
				//mAsyncLoadState.remove(tag.position);
			}			
		} else {
			convertView = mLayoutInflater.inflate(R.layout.t_notification,parent,false);
			tag = new Holder();
			tag.title = (TextView) convertView.findViewById(R.id.title);
			tag.name = (TextView) convertView.findViewById(R.id.name);
			tag.since = (TextView) convertView.findViewById(R.id.since);
			tag.pic = (ImageView) convertView.findViewById(R.id.pic);
			tag.message = (TextView) convertView.findViewById(R.id.message);
			tag.appicon = (ImageView) convertView.findViewById(R.id.appicon);
		}
		
		if(ds != null){ 
			long nowMilliseconds = (new Date()).getTime();
			long createdSecs = ApplicationDBHelper.getCursorLongData(R.string.sql_notifications_complete, R.id.table_notifications, FBNotification.col_created_time, ds);			
			long elapsedSecs = nowMilliseconds / 1000 - createdSecs;
			
			//Util.dumpCursor(ds);
			
			String since = Util.createElapsedString(elapsedSecs, nowMilliseconds, createdSecs);
			String name = ApplicationDBHelper.getCursorStringData(R.string.sql_notifications_complete, R.id.table_users, User.col_name, ds);
			String title = ApplicationDBHelper.getCursorStringData(R.string.sql_notifications_complete, R.id.table_notifications, FBNotification.col_title_text, ds);
			String body = ApplicationDBHelper.getCursorStringData(R.string.sql_notifications_complete, R.id.table_notifications, FBNotification.col_body_text, ds);
			String appiconuri = ApplicationDBHelper.getCursorStringData(R.string.sql_notifications_complete, R.id.table_applications, FBApplication.col_icon_url, ds);
			String senderpicuri = ApplicationDBHelper.getCursorStringData(R.string.sql_notifications_complete, R.id.table_users, User.col_pic_square,  ds);
			
			int numColumns = ds.getColumnCount();
			//Logger.l(Logger.DEBUG, LOG_TAG, "numColumns:"+numColumns+" name: "+name+" appicon:"+appiconuri+" pic:"+senderpicuri);
			
			tag.since.setText(since);
			tag.title.setText(title);
			tag.name.setText(name);
			tag.message.setText(body);
			
			App.mImageUrlLoader2.fetch(
					App.INSTANCE.mExecScopeImageLoaderTask, 
					0, 
					position,
					senderpicuri,
					tag.pic, 
					null, 
					mAsyncLoaderListener);
						
			App.mImageUrlLoader2.fetch(
					App.INSTANCE.mExecScopeImageLoaderTask, 
					0, 
					position,
					appiconuri,
					tag.appicon, 
					null, 
					mAsyncLoaderListener);
				
		}
								
		convertView.setTag(DATA_TAGID, tag);
		
		return convertView; 
	}
	
}
