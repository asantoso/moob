package com.neusou.moobook.controller;

import com.neusou.moobook.adapters.CursorRowMapper;

import android.app.Activity;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.StaleDataException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class CursorListAdapter<D> extends BaseAdapter {
	
	protected Cursor mData;
	protected BaseListViewFactory<Cursor> mItemViewFactory;
	protected int mPrimaryKeyIndex; 
	protected LayoutInflater mLayoutInflater;
	protected CursorRowMapper<D> mRowMapper;
	
	public CursorListAdapter(Activity ctx, BaseListViewFactory<Cursor> factory,
			  int primaryKeyIndex
		) {
			mItemViewFactory = factory;
			mPrimaryKeyIndex = primaryKeyIndex;
			mLayoutInflater = ctx.getLayoutInflater();
		}

	public CursorListAdapter(Activity ctx, BaseListViewFactory<Cursor> factory,
		  int primaryKeyIndex, CursorRowMapper<D> rowMapper
	) {
		mItemViewFactory = factory;
		mPrimaryKeyIndex = primaryKeyIndex;
		mLayoutInflater = ctx.getLayoutInflater();
		mRowMapper = rowMapper;
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
	public D getItem(int position) {
		
		if (mData == null || mData.isClosed()) {
			return null;
		}
		
		if(mRowMapper != null){
			mData.moveToPosition(position);
			return mRowMapper.map(mData);
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
