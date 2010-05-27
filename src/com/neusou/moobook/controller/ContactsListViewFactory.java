package com.neusou.moobook.controller;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.IntentSender.SendIntentException;
import android.database.Cursor;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.database.StaleDataException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.data.User;
import com.neusou.web.ImageUrlLoader2;
import com.neusou.web.ImageUrlLoader2.AsyncListener;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderProgress;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderResult;

public class ContactsListViewFactory extends BaseListViewFactory<Cursor> {

	static final String LOG_TAG = "ContactsListViewFactory";
	static final int INTERNAL_TAG = R.id.tag_contactsadapter_item;
	static final int DATAVIEW_RESID = R.layout.t_contact;
	static final int DEFAULT_PROFILE_IMAGE_RESID = R.drawable.defaultprofileimage;
	final Bitmap mDefaultProfileImage;
	User user = new User(); // cached object for parsing cursor
	short[] mDisplayColumns;

	volatile long mLastUiUpdateRequest = 0l; // to keep track when the last UI
												// update request was sent.
	volatile long mFreezeUiRequestWindowSize = 20l; // milliseconds
	// 
	boolean pendingUpdate = false;

	Handler h = new Handler();
	DataSetObserver mDataSetObserver;

	public void setDataSetObserver(DataSetObserver observer){
		 mDataSetObserver = observer;
		 mAsyncLoaderListener.setObserver(observer);
	}
	
	ThrottlingImageAsyncLoaderListener mAsyncLoaderListener = new ThrottlingImageAsyncLoaderListener(mFreezeUiRequestWindowSize, 0, true) {	
		
		@Override
		public void onPublishProgress(AsyncLoaderProgress progress) {
			
			
		}
		
	}; 
	
	public void setDisplayColumns(short[] displayColumns) {
		mDisplayColumns = displayColumns;
	}

	public ContactsListViewFactory(Activity ctx) {
		super(ctx);
		mDefaultProfileImage = BitmapFactory.decodeResource(mResources,
				DEFAULT_PROFILE_IMAGE_RESID);
	}

	class Holder {
		TextView info0;
		TextView name;
		ImageView picture;
	}

	private String generateLocationString(String city, String state,
			String zip, String country) {
		String ret = "";
		String nu = "null";
		ret += ((city == null || city.length() == 0 || city.equals(null)) ? ""
				: city);
		ret += ((state == null || state.length() == 0) || city.equals(null) ? ""
				: ", " + state);
		ret += ((zip == null || zip.length() == 0 || city.equals(null)) ? ""
				: ", " + zip);
		ret += ((country == null || country.length() == 0 || city.equals(null)) ? ""
				: ", " + country);
		return ret;
	}

	public View createView(Cursor ds, int position, View convertView,
			final ViewGroup parent) {

		try {
			ds.moveToPosition(position);
		} catch (Exception e) {
			return mLayoutInflater.inflate(R.layout.empty, parent, false);
		}

		Holder tag;

		if (convertView != null) {
			tag = (Holder) convertView.getTag(INTERNAL_TAG);
		} else {

			convertView = mLayoutInflater
					.inflate(DATAVIEW_RESID, parent, false);

			tag = new Holder();
			tag.name = (TextView) convertView.findViewById(R.id.name);
			tag.picture = (ImageView) convertView.findViewById(R.id.profilepic);
			tag.info0 = (TextView) convertView.findViewById(R.id.info0);
			convertView.setTag(INTERNAL_TAG, tag);
		}

		// parse one row of data
		try {
			user.parseCursor(ds, mDisplayColumns);
		} catch (StaleDataException e) {
			return convertView;
		} catch (IllegalStateException e) {
			return convertView;
		}

		// fill in data

		if (user != null) {
			// Logger.l(Logger.DEBUG, LOG_TAG, "num users : "+ds.getCount());

			String state = null;
			String zip = null;
			String country = null;
			String city = null;
			JSONObject htl;

			try {
				htl = new JSONObject(user.hometown_location);
				try {
					state = htl.getString("state");
				} catch (JSONException e) {
				} catch (NullPointerException e) {

				}

				try {
					zip = htl.getString("zip");
				} catch (JSONException e) {

				} catch (NullPointerException e) {

				}
				try {
					country = htl.getString("country");
				} catch (JSONException e) {
				} catch (NullPointerException e) {

				}
				try {
					city = htl.getString("city");
				} catch (JSONException e) {
				} catch (NullPointerException e) {

				}
			} catch (JSONException e) {
			} catch (NullPointerException e) {

			}

			String location = generateLocationString(city, state, zip, country);
			tag.info0.setText(
					location
					+ "\n" + 
					user.birthday_date
					+"\n"+
					user.proxied_email
					+"\n"+
					user.timezone);
			tag.name.setText(user.name);
			String imageUrl = user.pic_square;
			Util.fetchImage(App.INSTANCE.mExecScopeImageLoaderTask, tag.picture, mDefaultProfileImage, imageUrl, mAsyncLoaderListener);
		
		}

		return convertView;
	}

}
