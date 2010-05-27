package com.neusou.moobook.controller;

import com.neusou.moobook.R;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class BaseListViewFactory<DataStore> {
	
	protected Resources mResources;
	protected LayoutInflater mLayoutInflater;
	
	public BaseListViewFactory(Activity ctx){
		mResources = ctx.getResources();
		mLayoutInflater = ctx.getLayoutInflater();
		
	}
	
	public abstract View createView(
			DataStore ds,			
			int position, 
			View convertView, 
			final ViewGroup parent
	);
	 
}
