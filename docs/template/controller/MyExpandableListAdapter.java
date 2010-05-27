package com.neusou.moobook.controller;

import android.app.Activity;
import android.database.Cursor;

//import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;


import com.neusou.moobook.R;

import com.neusou.moobook.data.Comment;
import com.neusou.moobook.data.Stream;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;

public class MyExpandableListAdapter extends BaseExpandableListAdapter {
        
        ApplicationDBHelper mDBHelper;
    	
        public Cursor groupDatastore;
        Cursor childDatastore; 
        public StreamListViewFactory mGroupViewFactory ;
        LayoutInflater mLayoutInflater;
        
        static final String LOG_TAG = "MyExpandableListAdapter";
        Activity ctx;
        
        private int mGroupIdCursorColumnIndex;
        
        public class Holder{
        	public int groupPosition;
        	public TextView name;
        	public TextView text;
        }
        
        public MyExpandableListAdapter(Activity ctx, ExpandableListView listView) {
			this.ctx = ctx;			
			mGroupViewFactory = new StreamListViewFactory(ctx, listView);
			mLayoutInflater = ctx.getLayoutInflater();
			mDBHelper = new ApplicationDBHelper(ctx);
		}
        
        
        public void setData(Cursor groupDatastore){
        	this.groupDatastore = groupDatastore;
        }
                
        public Object getChild(int groupPosition, int childPosition) {        	
        	groupDatastore.moveToPosition(groupPosition);
        	String post_id = groupDatastore.getString(mGroupIdCursorColumnIndex);
        	updateChildDataStore(post_id);
            childDatastore.moveToPosition(childPosition);
            return childDatastore.getString(Comment.col_comment);            
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
        	
        	return 0;
        	/*
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
        	*/    	
        }
        
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
        	//Log.d(LOG_TAG,"getChildView "+childPosition);
        	groupDatastore.moveToPosition(groupPosition);
        	
        	String post_id = groupDatastore.getString(mGroupIdCursorColumnIndex);        
        	
        	updateChildDataStore(post_id);  
        	
            childDatastore.moveToPosition(childPosition);
            
            
            Holder tag;
            if(convertView == null){
            	convertView = mLayoutInflater.inflate(R.layout.t_streamcomment, parent, false);
            	tag = new Holder();
            	tag.name = (TextView) convertView.findViewById(R.id.name);
            	tag.text = (TextView) convertView.findViewById(R.id.text);
            }else{
            	tag = (Holder) convertView.getTag();
            }
            
            
            String name = childDatastore.getString(Comment.TOTAL_PROPERTY_COLUMNS + User.col_name );
            String message = childDatastore.getString(Comment.col_comment);
            if(message != null){
            	tag.text.setText(message.trim());
            }
            
            
            tag.name.setText(name);
            tag.groupPosition = groupPosition;
            
            convertView.setTag(tag);
            
            return convertView;    
        }
        
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
        	return mGroupViewFactory.createView(groupDatastore, groupPosition, convertView, parent);            
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