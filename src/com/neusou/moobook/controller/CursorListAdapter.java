package com.neusou.moobook.controller;

import android.app.Activity;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.StaleDataException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class CursorListAdapter extends BaseAdapter {
	
	protected Cursor mData;
	protected BaseListViewFactory<Cursor> mItemViewFactory;
	protected int mPrimaryKeyIndex; 
	protected LayoutInflater mLayoutInflater;

	public CursorListAdapter(Activity ctx, BaseListViewFactory<Cursor> factory,
		  int primaryKeyIndex
	) {
		this.mItemViewFactory = factory;
		this.mPrimaryKeyIndex = primaryKeyIndex;
		this.mLayoutInflater = ctx.getLayoutInflater();
		//factory.setDataSetObserver(observer);
		
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
		
		try{
			return mData.getLong(mPrimaryKeyIndex);
		}catch(StaleDataException e){
			mData.requery();			
		}catch(CursorIndexOutOfBoundsException e){
			return 0;
		}
		return 0;
	}

	@Override
	public View getView(int position, View convertView, final ViewGroup parent) {				
		return mItemViewFactory.createView(mData, position, convertView, parent);		
	}

}
