package com.neusou.moobook.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.res.Resources;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.neusou.DataState;
import com.neusou.SoftHashMap;
import com.neusou.moobook.controller.BaseListViewFactory.IBaseListViewData;
import com.neusou.moobook.controller.BaseListViewFactory.IBaseListViewHolder;

public abstract class BaseExpandableListViewFactory<DataStore, CacheItemType>
implements android.widget.AbsListView.RecyclerListener, IStatefulListView{
	
	protected Resources mResources;
	protected LayoutInflater mLayoutInflater;
	protected volatile Object mCreateViewLock = new Object();
	protected volatile List<Long> mAsyncLoadingState = Collections.synchronizedList(new ArrayList<Long>(10));
	protected int mTagDataId;
	protected int mTagViewId;
	
	public BaseExpandableListViewFactory(Activity ctx, int tagDataId, int tagViewId){
		mResources = ctx.getResources();
		mLayoutInflater = ctx.getLayoutInflater();	
		mTagDataId = tagDataId;
		mTagViewId = tagViewId;
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
		
		synchronized(mCreateViewLock){
			IBaseListViewData gdata = (IBaseListViewData) view.getTag(mTagDataId);
			IBaseListViewHolder gholder = (IBaseListViewHolder) view.getTag(mTagViewId);			
			if(gdata != null){
				gdata.setDataState(DataState.INVALID);
			}
			if(gholder != null){
				long pos = Long.valueOf(gholder.getPosition());
				if(mAsyncLoadingState.contains(pos)){
					removeAsyncLoadState(pos);				
				}			
			}
			gholder.onMovedToScapHeap();
		}
		
	}

	
	public void clearAsyncLoadStates(){
		synchronized(mCreateViewLock){
			mAsyncLoadingState.clear();
		}
	}
	
	public void addAsyncLoadState(long groupCode){
		synchronized(mCreateViewLock){
			long lgc = Long.valueOf(groupCode);
			boolean isContained = mAsyncLoadingState.contains(lgc);
			if(!isContained){
				mAsyncLoadingState.add(lgc);
			}			
		}
	}
	
	public void removeAsyncLoadState(long groupCode){
		synchronized(mCreateViewLock){			
			long lgc = Long.valueOf(groupCode);
				if(mAsyncLoadingState.contains(lgc)){						
					mAsyncLoadingState.remove(mAsyncLoadingState.indexOf(lgc));		
				}
		}
	}
	
	public boolean getAsyncLoadState(long groupCode){
		synchronized(mCreateViewLock){
			return mAsyncLoadingState.contains(Long.valueOf(groupCode));			
		}	
	}
		
}
