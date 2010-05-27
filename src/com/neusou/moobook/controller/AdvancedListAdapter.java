/*package com.neusou.moobook.controller;

import com.neusou.android.core.model.IAdvancedListViewData;
import com.neusou.android.core.view.IAdvancedListViewItemFactory;

import android.app.Activity;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class AdvancedListAdapter<ViewFactory extends IAdvancedListViewItemFactory<IAdvancedListViewData<?>>> extends BaseAdapter {
	
	public Activity ctx;
	public LayoutInflater mLayoutInflater;	
	public Resources mResources;
	public ViewFactory mViewFactory; 
	public IAdvancedListViewData<?> mData;
	
	public AdvancedListAdapter(Activity ctx, ViewFactory viewFactory) {
		this.ctx = ctx;
		this.mViewFactory = viewFactory;
		this.mLayoutInflater = ctx.getLayoutInflater();
		this.mResources = ctx.getResources();
	}

	interface IHolder{
		public int getPosition();
		public AsyncTask getAsyncTask();
		public void setAsyncTask(AsyncTask asyncTask);
	}
	
	protected void bookkeep(int position, View convertView, final ViewGroup parent){
		if (convertView != null) {
			IHolder h = (IHolder) convertView.getTag();
			if (position != h.getPosition()) { 
				AsyncTask task = h.getAsyncTask();						
				if(task != null){
					task.cancel(true);
					h.setAsyncTask(null);
				}	
			}
		} 		
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		bookkeep(position,convertView,parent);
		
		if(this.mViewFactory == null){
			return null;
		}else{
			return this.mViewFactory.createView(mData, position,convertView, parent);
		}
		
	}
	
}
*/