package com.neusou.android.core.controller;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.neusou.android.core.model.IAdvancedListViewData;
import com.neusou.android.core.view.IAdvancedListViewItemFactory;

/**
 * Simple generic list view adapter which accesses a homogeneous data store.
 * 
 * @since  December 2008
 * @version 2.0
 * 
 * @param <DataItem> Data item
 * @param <DataStore> A homogeneous data store which stores one or more data items.
 * @param <ViewFactory> An factory that creates a view for the list.
 */
public class AdvancedListViewAdapter<DataItem,DataStore extends IAdvancedListViewData<DataItem>,ViewFactory extends IAdvancedListViewItemFactory<DataStore>> extends BaseAdapter{
	
	public DataStore mDataStore; 
	public ViewFactory mViewFactory; 
	
	public AdvancedListViewAdapter(DataStore data, ViewFactory viewFactory){
		super();
		this.mDataStore = data;
		this.mViewFactory = viewFactory;
	}
	
	public void updateData(DataStore dataStore){
		this.mDataStore = dataStore;
		this.notifyDataSetChanged();
		this.notifyDataSetInvalidated();
	}
	
	@Override
	public int getCount() {
		if(this.mDataStore==null){
			return 0;			
		}
		return this.mDataStore.getCount();
	}

	@Override
	public DataItem getItem(int position) {		
		if(this.mDataStore==null){
			return null;			
		}
		return this.mDataStore.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		if(this.mDataStore==null){
			return -1;			
		}
		return this.mDataStore.getItemId(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(this.mViewFactory == null){
			return null;
		}else{
			return this.mViewFactory.createView(this.mDataStore, position,convertView, parent);
		}
	}
}