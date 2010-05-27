package com.neusou.moobook.controller;

import android.app.Activity;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class BaseListViewFactory<DataStore> {
	
	protected Resources mResources;
	protected LayoutInflater mLayoutInflater;
	protected DataSetObserver mDataSetObserver;
	
	public BaseListViewFactory(Activity ctx){
		mResources = ctx.getResources();
		mLayoutInflater = ctx.getLayoutInflater();		
	}
	
	public void setDataSetObserver(DataSetObserver observer){
		 mDataSetObserver = observer;		
	}
	
	public abstract View createView(
			DataStore ds,			
			int position, 
			View convertView, 
			final ViewGroup parent
	);
	 
}
