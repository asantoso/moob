package com.neusou.moobook.controller;

import org.json.JSONObject;

import android.app.Activity;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.StaleDataException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.data.BaseRowViewHolder;
import com.neusou.moobook.data.User;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderInput;

public class ContactsListViewFactory extends BaseListViewFactory<Cursor> {

	static final String LOG_TAG = "ContactsListViewFactory";
	//static final int ID_VIEW_TAG = R.id.tag_contactsadapter_item_view;
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
	
	StandardImageAsyncLoadListener mAsyncLoaderListener;

	public void setDataSetObserver(DataSetObserver observer){
		 mDataSetObserver = observer;
		 mAsyncLoaderListener.setObserver(observer);
	}
	

	
	public void setDisplayColumns(short[] displayColumns) {
		mDisplayColumns = displayColumns;
	}

	public ContactsListViewFactory(Activity ctx) {
		super(ctx, R.id.tag_contactsadapter_item_data, R.id.tag_contactsadapter_item_view);
		mDefaultProfileImage = BitmapFactory.decodeResource(mResources,
				DEFAULT_PROFILE_IMAGE_RESID);
		
		mAsyncLoaderListener = new StandardImageAsyncLoadListener(
				ctx, 
				mCreateViewLock, 
				(IStatefulListView) this, 
				200l, 
				100, 
				false);
	}

	public class Holder extends BaseRowViewHolder{
		TextView info0 = null;
		TextView name = null;
		ImageView picture = null;
	}
	
	public class Data implements BaseListViewFactory.IBaseListViewData {
		public String actorname;
		public long uid;
		public byte dataState;
		public String profileImageUri;
		
		@Override
		public void setDataState(byte state) {
			dataState = state;
		}
		
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
		
		super.createView(ds, position, convertView, parent);
		
		try {
			ds.moveToPosition(position);
		} catch (Exception e) {
			return mLayoutInflater.inflate(R.layout.empty, parent, false);
		}

		Holder tag = null;
		Data data = null;
		
		if (convertView != null) {
			tag = (Holder) convertView.getTag(mTagViewId);
			data = (Data) convertView.getTag(mTagDataId);
		} else {
			convertView = mLayoutInflater
					.inflate(DATAVIEW_RESID, parent, false);

			tag = new Holder();
			tag.name = (TextView) convertView.findViewById(R.id.name);
			tag.picture = (ImageView) convertView.findViewById(R.id.profilepic);
			convertView.setTag(mTagViewId, tag);			
		}
		
		tag.setPosition(position);
		
		if(data == null){
			data = new Data();
		}

		// parse one row of data
		try {
			user.parseCursor(ds, mDisplayColumns);
		} catch (StaleDataException e) {
			return convertView;
		} catch (IllegalStateException e) {
			return convertView;
		}

		data.actorname = user.name;
		data.uid = user.uid;
		data.profileImageUri = user.pic_square;
		
		convertView.setTag(mTagDataId, data);
		// fill in data

		if (user != null) {
			// Logger.l(Logger.DEBUG, LOG_TAG, "num users : "+ds.getCount());

			String state = null;
			String zip = null;
			String country = null;
			String city = null;
			JSONObject htl;

			tag.name.setText(user.name);
			
			String imageUrl = user.pic_square;
			
			Bitmap profileImageBitmap = App.mImageUrlLoader2.loadImage(imageUrl, true);
			if(profileImageBitmap == null){
				//TODO Object pool AsyncLoaderInput
				profileImageBitmap = mDefaultProfileImage;
				AsyncLoaderInput input = new AsyncLoaderInput();
				input.imageUri = imageUrl;
				input.imageView = tag.picture;
				input.groupCode = position;
				input.code = position;
				App.mImageUrlLoader2.loadImageAsync(App.INSTANCE.mExecScopeImageLoaderTask, input, mAsyncLoaderListener);
			}
			
			tag.picture.setImageBitmap(profileImageBitmap);
			
		}

		return convertView;
	}

}
