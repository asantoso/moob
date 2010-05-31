package com.neusou.moobook.controller;

import android.app.Activity;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import com.neusou.moobook.model.database.ApplicationDBHelper;

public class MyExpandableListAdapter extends BaseExpandableListAdapter{
        
		static final String LOG_TAG = "MyExpandableListAdapter";	
        ApplicationDBHelper mDBHelper;    	
        Cursor groupDatastore;
        Cursor childDatastore; 
        LayoutInflater mLayoutInflater;        
        Activity ctx;        
        int mGroupIdCursorColumnIndex;
        BaseExpandableListViewFactory<Cursor,?> mViewFactory;
        
        DataSetObserver mObserver = new DataSetObserver() {
        	public void onChanged() {
        		notifyDataSetChanged();
        	};
        	public void onInvalidated(){
        		notifyDataSetInvalidated();        		
        	}
		};
        
        public DataSetObserver getObserver(){
        	return mObserver; 
        }
        
        public void setViewFactory(BaseExpandableListViewFactory<Cursor,?> factory){
        	mViewFactory = factory;
        	
        }
        
        public BaseExpandableListViewFactory<Cursor,?> getViewFactory(){
        	return mViewFactory;
        }
        
        public MyExpandableListAdapter(Activity ctx) {
			this.ctx = ctx;			
			mLayoutInflater = ctx.getLayoutInflater();
			mDBHelper = new ApplicationDBHelper(ctx);
		
		}
                
        public void setData(Cursor groupDatastore){
        	if(this.groupDatastore!=null)this.groupDatastore.close();
        	this.groupDatastore = groupDatastore;
        }
                
        public Object getChild(int groupPosition, int childPosition) {        	
        	groupDatastore.moveToPosition(groupPosition);
        	String post_id = groupDatastore.getString(mGroupIdCursorColumnIndex);
        	updateChildDataStore(post_id);
            childDatastore.moveToPosition(childPosition);
            return null;      
        }

        public long getChildId(int groupPosition, int childPosition) {
        	return (groupPosition*childPosition);  
        }
        
        public void setGroupIdCursorColumnIndex(int i){       
        	mGroupIdCursorColumnIndex = i;
        }
        
        public void updateChildDataStore(String groupId){
        	if(childDatastore != null){
        		childDatastore.deactivate();
        		childDatastore.close();
        	}
        	childDatastore = mDBHelper.getPostComments(groupId);
        }
        
        public int getChildrenCount(int groupPosition) {
        	//Log.d(LOG_TAG,"getChildrenCount "+groupPosition);
        	        	
        	if(groupDatastore == null || groupDatastore.isClosed()){
        		return 0;
        	}       	
        	
        	groupDatastore.moveToPosition(groupPosition);
        	String post_id = groupDatastore.getString(mGroupIdCursorColumnIndex);
        	updateChildDataStore(post_id);
        	
        	if(childDatastore == null || childDatastore.isClosed()){
        		return 0;
        	}
        	
        	return childDatastore.getCount();    
        	    	
        }
        
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
        	groupDatastore.moveToPosition(groupPosition);        	
        	String group_id = groupDatastore.getString(mGroupIdCursorColumnIndex);
        	updateChildDataStore(group_id);          	
            childDatastore.moveToPosition(childPosition);            
            return mViewFactory.createChildView(childDatastore, groupPosition, childPosition, convertView, parent);
        }
        
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        	return mViewFactory.createGroupView(groupDatastore, groupPosition, convertView, parent);            
        }

        public Object getGroup(int groupPosition) {        	
            return null;
        }

        public int getGroupCount() {
        	if(groupDatastore == null || groupDatastore.isClosed()){
        		return 0;
        	}
            return groupDatastore.getCount();
        }

        public long getGroupId(int groupPosition) {
        	
            return groupPosition;
        }
      
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        public boolean hasStableIds() {
            return true;
        }

        
        
    }