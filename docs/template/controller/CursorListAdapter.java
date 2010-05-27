package com.neusou.moobook.controller;

import android.app.Activity;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class CursorListAdapter extends BaseAdapter {
	
	Cursor mData;
	BaseListViewFactory<Cursor> mItemViewFactory;
	int mPrimaryKeyIndex; 
	
	public CursorListAdapter(Activity ctx, BaseListViewFactory<Cursor> factory,
		  int primaryKeyIndex
	) {
		this.mItemViewFactory = factory;
		this.mPrimaryKeyIndex = primaryKeyIndex;
	}

	public void setData(Cursor c){
		mData = c;
	}
	
	@Override
	public int getCount() {
	
		if (mData == null || mData.isClosed()) {
			return 0;
		}
		
		return mData.getCount();
		
	}

	@Override
	public Object getItem(int position) {
		
		if (mData == null || mData.isClosed()) {
			return null;
		}
		
		return null;
		
	}

	@Override
	public long getItemId(int position) {
		
		if(mData == null || mData.isClosed()){
			return -1;
		}
		
		mData.moveToPosition(position);
		return mData.getLong(mPrimaryKeyIndex);
		
	}

	@Override
	public View getView(int position, View convertView, final ViewGroup parent) {
				
		return mItemViewFactory.createView(mData, position, convertView, parent);
		
	}

}
