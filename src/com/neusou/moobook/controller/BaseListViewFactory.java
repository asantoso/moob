package com.neusou.moobook.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.neusou.DataState;

public abstract class BaseListViewFactory<DataStore> 
implements android.widget.AbsListView.RecyclerListener, IStatefulListView

{
	static interface IBaseListViewData {
		public void setDataState(byte state);		
	}
	public static interface IBaseListViewHolder {
		public void setPosition(int position);
		public int getPosition();
		public void onMovedToScapHeap();
		public void onMovedToForeground();
	}
	
	protected volatile List<Long> mAsyncLoadingState = Collections.synchronizedList(new ArrayList<Long>(10));
	protected volatile Object mCreateViewLock = new Object();
	protected DataSetObserver mDataSetObserver;
	protected LayoutInflater mLayoutInflater;
	protected Resources mResources;
	protected int mTagDataId;
	protected int mTagViewId;	
	
	public BaseListViewFactory(Activity ctx, int tagDataId, int tagViewId){
		//mActivity = ctx;
		mResources = ctx.getResources();
		mLayoutInflater = ctx.getLayoutInflater();		
		mTagDataId = tagDataId;
		mTagViewId = tagViewId;
	}
	
	public void setDataSetObserver(DataSetObserver observer){
		 mDataSetObserver = observer;		
	}
	
	public View createView(
			DataStore ds,			
			int position, 
			View convertView, 
			final ViewGroup parent
	){		
		addAsyncLoadState((long)(position));
		return null;
	}

	public void addAsyncLoadState(long groupCode){
		synchronized(mCreateViewLock){
			long longGroupCode = Long.valueOf(groupCode);
			boolean isContained = mAsyncLoadingState.contains(longGroupCode);
			if(!isContained){
				mAsyncLoadingState.add(longGroupCode);
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
				removeAsyncLoadState(pos);
			}
			gholder.onMovedToScapHeap();
		}
	}
	
	@Override
	public void clearAsyncLoadStates() {
		synchronized(mCreateViewLock){
			mAsyncLoadingState.clear();
		}		
	}
	
}
