package com.neusou.moobook.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.neusou.DataState;
import com.neusou.Logger;
import com.neusou.SoftHashMap;
import com.neusou.moobook.R;
import com.neusou.moobook.controller.StreamListViewFactory.GroupData;
import com.neusou.moobook.controller.StreamListViewFactory.GroupViewHolder;

import android.app.Activity;
import android.content.res.Resources;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;

public abstract class BaseExpandableListViewFactory<DataStore, CacheItemType>
implements android.widget.AbsListView.RecyclerListener, 
OnScrollListener{
	
	protected Resources mResources;
	protected LayoutInflater mLayoutInflater;
	protected Object mCreateViewLock = new Object();
	volatile List<Long> mAsyncLoadingState = Collections.synchronizedList(new ArrayList<Long>(10));
	
	public BaseExpandableListViewFactory(Activity ctx){
		mResources = ctx.getResources();
		mLayoutInflater = ctx.getLayoutInflater();		
	}
	
	public View createGroupView(Cursor ds, int groupPosition, View convertView, final ViewGroup parent){
		addAsyncLoadState((long)(groupPosition));
		return null;
	}
	
	public abstract View createChildView(Cursor ds, int groupPosition, int childPosition, View convertView, final ViewGroup parent);	
	public abstract void markGroupAsDirty(String[] groupIds);
	public abstract SoftHashMap<String, CacheItemType> getMemoryCache();
	
	@Override
	public void onMovedToScrapHeap(View view) {
		
		int position = 0;
		
		synchronized(mCreateViewLock){
			GroupData gdata = (GroupData) view.getTag(R.id.tag_streamsadapter_item_data);
			GroupViewHolder gholder = (GroupViewHolder) view.getTag(R.id.tag_streamsadapter_item);
			position = gholder.position;
			
			Logger.l(Logger.DEBUG, "crucial", "[onMovedToScrapHead()] actor:"+gdata.actorName+", position:"+gholder.position+", states size: " +mAsyncLoadingState.size());
			
			gdata.mDataState = DataState.INVALID;
			
			if(mAsyncLoadingState.contains(Long.valueOf(gholder.position))){
				Logger.l(Logger.DEBUG, "crucial", "[onMovedToScrapHead()] found.");
				removeAsyncLoadState(gholder.position);				
			}
			
		}		
	}

	
	protected void clearAsyncLoadStates(){
		synchronized(mCreateViewLock){
			mAsyncLoadingState.clear();
		}
	}
	
	protected void addAsyncLoadState(long groupCode){
		synchronized(mCreateViewLock){
			boolean isContained = mAsyncLoadingState.contains(Long.valueOf(groupCode));
			Logger.l(Logger.ERROR, "HELLO",groupCode+" isContained? "+isContained);
			if(!isContained){
				mAsyncLoadingState.add(Long.valueOf(groupCode));
			}			
		}
	}
	
	protected void removeAsyncLoadState(long groupCode){
		synchronized(mCreateViewLock){			
				if(mAsyncLoadingState.contains(Long.valueOf(groupCode))){						
					mAsyncLoadingState.remove(mAsyncLoadingState.indexOf(groupCode));		
				}
		}
	}
	
	protected boolean getAsyncLoadState(long groupCode){
		synchronized(mCreateViewLock){
			return mAsyncLoadingState.contains(Long.valueOf(groupCode));			
		}	
	}
		

	
}
