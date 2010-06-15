package com.neusou.moobook.controller;

import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.data.BaseRowViewHolder;
import com.neusou.moobook.data.Event;

public class EventsListViewFactory extends BaseListViewFactory<Cursor> {

	static final String LOG_TAG = "EventsListViewFactory";
	static final String dateFormat = "EEEE',' d MMMM yyyy 'at' k:mm z";
	LayoutInflater mLayoutInflater;
	Resources mResources;
	Context mContext;
	Util mUtil;
	BaseAdapter mAdapter;
	Date mDate;	
	StandardImageAsyncLoadListener mAsyncLoaderListener;
	
	public EventsListViewFactory(Activity act) {
		super(act,R.id.tag_eventsadapter_item_data, R.id.tag_eventsadapter_item_view);
		mContext = act.getApplicationContext();
		mLayoutInflater = act.getLayoutInflater();
		mResources = act.getResources();
		mUtil = new Util();

		mDate = new Date();
		
		mAsyncLoaderListener = new StandardImageAsyncLoadListener(
				act, 
				mCreateViewLock, 
				(IStatefulListView) this, 
				50l, 
				50, 
				false);
	}

	public void setAdapter(BaseAdapter adapter) {
		mAdapter = adapter;
	}

	public class Holder extends BaseRowViewHolder {
		public TextView title;
		public TextView description;
		public ImageView pic;
		public TextView host;
		public TextView rsvp_status;
		public TextView start_date;
		public long eid;
		public int position;
	
	}

	public void destroy() {
	}

	public View createView(Cursor ds, int position, View convertView,
			final ViewGroup parent) {
		super.createView(ds, position, convertView, parent);
		
		
		try {
			ds.moveToPosition(position);
		} catch (Exception e) {
			return mLayoutInflater.inflate(R.layout.empty, parent, false);
		}

		Holder tag;

		if (convertView != null) {
			tag = (Holder) convertView.getTag();
		} else {

			convertView = mLayoutInflater.inflate(R.layout.t_event, parent,
					false);

			tag = new Holder();
			tag.title = (TextView) convertView.findViewById(R.id.title);
			tag.pic = (ImageView) convertView.findViewById(R.id.pic);
			tag.description = (TextView) convertView
					.findViewById(R.id.description);
			tag.start_date = (TextView) convertView
					.findViewById(R.id.start_date);
			tag.rsvp_status = (TextView) convertView
					.findViewById(R.id.rsvp_status);
			// tag.host = (TextView) convertView.findViewById(R.id.host);
			convertView.setTag(mTagViewId,tag);
		}
		Event event = null;
		event = Event.parseCursor(ds, event);

		if (event != null) {
			tag.title.setText(event.name);
			String desc = null;

			tag.eid = event.eid;
			
			if (event.description.length() > 140) {
				desc = event.description.substring(0, 140) + "..";
			} else {
				desc = event.description;
			}

			if (desc == null || desc.length() == 0) {
				tag.description.setVisibility(View.GONE);
			} else {
				tag.description.setText(desc);
				tag.description.setVisibility(View.VISIBLE);
			}

			String imageUrl = event.pic_small;

			if (imageUrl != null) {
				
				Drawable picDrawable = App.mImageUrlLoader.loadImage(imageUrl, true);
				if (picDrawable == null) {
					new AsyncTask<String, Void, Boolean>() {
						@Override
						protected Boolean doInBackground(String... params) {
							String url = params[0];
							if (url != null) {
								Drawable d = App.mImageUrlLoader.loadImage(url, false);
								if (d != null) {
									return true;
								}
							}
							return false;
						}

						protected void onPostExecute(Boolean result) {
							if (result) {
								if (mAdapter != null) {
									mAdapter.notifyDataSetChanged();
								}
							}
						};
					}.execute(imageUrl);
					tag.pic.setVisibility(View.VISIBLE);
				
				} else {
					
				}
				
				// set loading drawable
				if (picDrawable == null) {
					tag.pic.setVisibility(View.GONE);
				} else {
					tag.pic.setVisibility(View.VISIBLE);
					tag.pic.setImageDrawable(picDrawable);
				}
				
			} else {
				tag.pic.setVisibility(View.GONE);
			}

			tag.rsvp_status.setText(event.rsvp_status);

			if (Event.RSVPStatus.ATTENDING.match(event.rsvp_status)) {
				tag.rsvp_status
						.setBackgroundResource(R.drawable.green_message_box);
			} else if (Event.RSVPStatus.DECLINED.match(event.rsvp_status)) {
				tag.rsvp_status
						.setBackgroundResource(R.drawable.red_message_box);
			} else if (
					(Event.RSVPStatus.NOT_REPLIED.match(event.rsvp_status))
				) {
				tag.rsvp_status
						.setBackgroundResource(R.drawable.grey_message_box);
			} else if (
					(Event.RSVPStatus.UNSURE.match(event.rsvp_status))
					
				
			) {
				tag.rsvp_status
						.setBackgroundResource(R.drawable.orange_message_box);
			}

			mDate.setTime(event.start_time * 1000);
			String formattedDate = DateFormat.format(dateFormat, mDate)
					.toString();
			tag.start_date.setText(formattedDate);
		}

		return convertView;
	}

	
}
