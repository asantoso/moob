//package com.neusou.moobook.adapters;
//
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Set;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import android.app.Activity;
//import android.content.res.Resources;
//import android.util.Log;
//import android.view.KeyEvent;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.BaseAdapter;
//import android.widget.TextView;
//
//import com.neusou.Logger;
//import com.neusou.moobook.R;
//import com.neusou.moobook.Util;
//import com.neusou.moobook.data.User;
//import com.neusou.web.PagingInfo;
//
//public class PageableAdapter extends BaseAdapter implements IPageableAdapter{
//		JSONArray commentsJsonData;
//		
//		Activity ctx;
//		Resources mResources;
//		LayoutInflater mLayoutInflater; 
//		
//		View.OnClickListener mGetNextOnClick = new View.OnClickListener() {			
//			@Override
//			public void onClick(View v) {
//				mPageableListener.onGetNext();				
//			}
//		};
//		
//		View.OnClickListener mGetPrevOnClick = new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				mPageableListener.onGetPrev();				
//			}
//		};
//		
//		View.OnKeyListener mGetPrevOnKey = new View.OnKeyListener() {
//			
//			@Override
//			public boolean onKey(View v, int keyCode, KeyEvent event) {
//				mPageableListener.onGetPrev();	
//				return true;
//			}
//		};
//		
//		View.OnKeyListener mGetNextOnKey = new View.OnKeyListener() {
//			
//			@Override
//			public boolean onKey(View v, int keyCode, KeyEvent event) {
//				mPageableListener.onGetNext();
//				return true;
//			}
//		};
//		
//		
//		HashMap<Long, User> userMap = new HashMap<Long, User>(25);
//		HashMap<String,Boolean> mDeletedComments = new HashMap<String, Boolean>(5);			
//		Object mDeletedCommentsLock = new Object();			
//		HashMap<Long, Long> mSkipCount = new HashMap<Long,Long>();
//
//		int[] mJumpCount;
//
//		PagingInfo mPagingInfo;
//		
//		static final String LOG_TAG = "debug";
//		
//		IPageableListener mPageableListener ;
//		
//		public static final byte VIEWTYPE_LOADER = 0;
//		public static final byte VIEWTYPE_COMMENT = 1;
//		public static final byte TOTAL_VIEWTYPES = 2;
//		
//		View nextRowView;
//		View prevRowView;
//		public static final int mInternalTag = R.id.tag_pageable_internalTag;
//		public static final int mItemTag = R.id.tag_pageable_itemTag;
//		
//		
//		public void setListener(IPageableListener listener){
//			mPageableListener = listener;
//		}
//		
//		public void onStartLoadingNext(){
//			View v = nextRowView.findViewById(R.id.progressbar);
//			v.setVisibility(View.VISIBLE);
//		}
//		public void onFinishedLoadingNext(){
//			View v = nextRowView.findViewById(R.id.progressbar);
//			v.setVisibility(View.GONE);
//		}
//				
//		public void onStartLoadingPrev(){
//			View v = prevRowView.findViewById(R.id.progressbar);
//			v.setVisibility(View.VISIBLE);
//		}
//		
//		public void onFinishedLoadingPrev(){
//			View v = prevRowView.findViewById(R.id.progressbar);
//			v.setVisibility(View.GONE);
//		}
//		
//		public void onFinishedLoading(){
//			onFinishedLoadingNext();
//			onFinishedLoadingPrev();
//		}
//		
//		public void setPagingInfo(PagingInfo pagingInfo){
//			mPagingInfo = pagingInfo;
//		}
//
//		private int findValidPos(final int reqpos){			
//			
//			//Log.d(LOG_TAG,"findValidPos: "+reqpos+", jps:"+mJumpCount.length);
//			
//			int jumpcount = mJumpCount[reqpos];
//			int runningpos = reqpos;
//			if(jumpcount == 0){
//				return reqpos;
//			}
//			
//			synchronized (mDeletedCommentsLock) {
//			while(true){
//				if(jumpcount==0){
//					break;
//				}
//				runningpos++;
//				try{
//					Log.d(LOG_TAG, "running pos: "+runningpos);
//					String comment_id = commentsJsonData.getJSONObject(runningpos).getString("id");
//					//Log.d(LOG_TAG, "comment id: "+comment_id);					
//					boolean isdeleted = false;
//					
//					if(mDeletedComments != null && !mDeletedComments.isEmpty()){
//						if(comment_id != null){
//							
//							try{								
//								isdeleted = mDeletedComments.get(comment_id);
//							}catch(NullPointerException e){
//								return reqpos;
//								//TODO figure this out, very interesting code
//								//e.printStackTrace();
//							}	
//							
//						}
//					}
//					
//					if(!isdeleted){
//						jumpcount--;
//					}
//				}catch(JSONException e){	
//					return -1;
//				}				
//			}
//			
//			}
//			return runningpos;
//			
//		}
//		
//		private void updateJumpCount(final int deletedPos){
//			for(int i=deletedPos,end=commentsJsonData.length()-mDeletedComments.size();
//				i<end;i++){
//				mJumpCount[i]++;
//			}			
//		}
//		
//		public void setData(JSONArray commentsJsonData){
//			this.commentsJsonData = commentsJsonData;			
//			clearDeletedList();
//		}
//		
//		public String[] getDirtyRows(){		
//			synchronized (mDeletedCommentsLock) {
//				Set<String> ids = mDeletedComments.keySet();			
//				String ret[] = new String[ids.size()];
//				ids.toArray(ret);
//				return ret;
//			}
//		}
//		
//		private void clearDeletedList(){
//			synchronized (mDeletedCommentsLock) {
//				if(mDeletedComments != null){
//					mDeletedComments.clear();				
//				}	
//			}
//			
//			int numcomments = 0;
//			if(commentsJsonData != null){
//				numcomments = commentsJsonData.length();
//			}
//			mJumpCount = new int[numcomments];			
//		}
//		
//		public void markDataAsDeleted(String comment_id, int position){
//			synchronized (mDeletedCommentsLock) {
//				mDeletedComments.put(comment_id, true);
//				updateJumpCount(position);
//			}			
//		}
//
//		public void clearData(){
//			commentsJsonData = null;
//			userMap.clear();
//		}
//		
//		public PageableAdapter(Activity ctx) {
//			this.ctx = ctx;
//			mResources = ctx.getResources();
//			mLayoutInflater = ctx.getLayoutInflater();			
//			nextRowView = mLayoutInflater.inflate(R.layout.asyncloader, null, false);
//			prevRowView = mLayoutInflater.inflate(R.layout.asyncloader, null, false);
//		}
//		
//		long sessionUid;
//		public void setSessionUser(long uid){
//			this.sessionUid = uid;
//		}
//		
//		public void clearUsersData(){
//			userMap.clear();	
//		}
//		
//		public void parseUsersJsonData(JSONArray data){
//				
//			if(data == null){
//				return;
//			}
//			
//			for(int i=0,num=data.length();i<num;i++){
//			try{
//				User user = new User();
//				JSONObject userObj = data.getJSONObject(i);
//				user.uid = userObj.getLong("id");
//				if(userMap.get(user.uid) == null){
//					user.name = userObj.getString("name");
//					user.pic_square = userObj.getString("pic_square");
//					userMap.put(user.uid, user);
//				}				
//			}catch(JSONException e){				
//			}
//			}
//		}
//		
//		@Override
//		public int getCount() {
//			if(commentsJsonData == null){
//				return 0;
//			}
//			int len = commentsJsonData.length() - mDeletedComments.size();
//			int add = 0;
//			if(mPagingInfo.isRemoteSiteHasNext){
//				add = 1;
//			}
//			if(mPagingInfo.isRemoteSiteHasPrev){
//				add++;
//			}
//			return len + add; 
//		}
//			
//		
//
//		@Override
//		public Object getItem(int position) {
//			return null;
//		}
//
//		@Override
//		public long getItemId(int position) {
//			return 0;
//		}
//		
//		@Override
//		public int getViewTypeCount() {
//			return TOTAL_VIEWTYPES;
//		}
//		
//		@Override
//		public int getItemViewType(int position) {
//			if(mPagingInfo.isRemoteSiteHasPrev){
//				if(position == 0){
//					return VIEWTYPE_LOADER;	
//				}
//			}
//			if(mPagingInfo.isRemoteSiteHasNext){
//				if(position == getCount() - 1){
//					return VIEWTYPE_LOADER;
//				}	
//			}						
//			return VIEWTYPE_COMMENT;
//		}
//		
//		
//		
//		@Override
//		public View getView(int requestedPosition, View convertView, ViewGroup parent) {			
//			InternalTag internalTag = null;
//			Holder itemTag = null;
//			
//			int totalRows = getCount();
//			
//			Logger.l(Logger.DEBUG,LOG_TAG,"[getView()] " +
//					" hasPrev: "+mPagingInfo.isRemoteSiteHasNext +
//					", hasNext: "+mPagingInfo.isRemoteSiteHasPrev +
//					", start: "+mPagingInfo.getNextStart() +
//					", reqPos:"+requestedPosition+", count:"+totalRows);
//			
//			
//		
//			boolean showViewPrev = (requestedPosition == 0 && mPagingInfo.isRemoteSiteHasPrev);
//			boolean showViewNext = (requestedPosition == totalRows - 1 && mPagingInfo.isRemoteSiteHasNext);
//
//			
//			if(showViewPrev || showViewNext){
//				
//				if(showViewNext){
//					convertView = nextRowView;
//				}
//				else if(showViewPrev){
//					convertView = prevRowView;
//				}
//				internalTag = (InternalTag)convertView.getTag(mInternalTag);
//				if(internalTag == null){
//					internalTag = new InternalTag();
//					convertView.setTag(mInternalTag,internalTag);
//				}
//				internalTag.viewType = VIEWTYPE_LOADER;
//						
//				
//				/*
//				if(convertView != null){
//					internalTag = (InternalTag)convertView.getTag(mInternalTag);
//				}else{
//					convertView = mLayoutInflater.inflate(R.layout.asyncloader, parent, false);
//				}
//				
//				
//				if(internalTag == null || internalTag.viewType != VIEWTYPE_LOADER){					
//					convertView = mLayoutInflater.inflate(R.layout.asyncloader, parent, false);
//					internalTag = new InternalTag();					
//					internalTag.viewType = VIEWTYPE_LOADER;
//					convertView.setTag(mInternalTag,internalTag);					
//				}
//				*/
//				
//				
//				TextView label = (TextView) convertView.findViewById(R.id.label);
//				
//				if(showViewNext){
//					label.setText("view next");
//					internalTag.loadDirection = PagingInfo.NEXT;
//				}else{
//					label.setText("view previous");
//					internalTag.loadDirection = PagingInfo.PREV;
//				}
//				
//				
//				if(mPageableListener != null){
//					if(showViewNext){						
//						mPageableListener.onHasNext();
//					}
//					if(showViewPrev){
//						mPageableListener.onHasPrev();
//					}
//				}
//				
//				return convertView;
//			}
//			
//			//show data row			
//			int calculatedRequestedDataRowIndex = requestedPosition - (mPagingInfo.isRemoteSiteHasPrev?1:0);
//
//			
//			if(convertView != null){
//				internalTag = (InternalTag) convertView.getTag(mInternalTag);
//			}
//			
//			if(convertView == null || internalTag != null && internalTag.viewType != VIEWTYPE_COMMENT){
//				convertView = mLayoutInflater.inflate(R.layout.t_comment, parent, false);
//				internalTag = new InternalTag();	
//				internalTag.viewType = VIEWTYPE_COMMENT;
//			}
//			
//			//bind views to itemtag if itemtag does not exist
//			itemTag = (Holder) convertView.getTag(mItemTag);
//			if(itemTag == null){
//				itemTag = new Holder();
//				itemTag.comment = (TextView) convertView.findViewById(R.id.comment);
//				itemTag.name = (TextView) convertView.findViewById(R.id.name);
//				itemTag.time = (TextView) convertView.findViewById(R.id.time);
//				convertView.setTag(mItemTag,itemTag);
//			}
//										
//			String text = null,fromName = null, comment_id = null;
//			long fromid = -1;
//			String since = "";
//			long timePostedInSecs = 0;
//			
//			try{
//				//int validNonDeletedPos = findValidPos(requestedPosition);
//				//assert(validNonDeletedPos >= 0 && validNonDeletedPos < commentsJsonData.length());
//				//JSONObject comment = commentsJsonData.getJSONObject(validNonDeletedPos);
//				Logger.l(Logger.DEBUG,LOG_TAG,"[getView()] calc req pos: "+calculatedRequestedDataRowIndex);
//
//				JSONObject comment = commentsJsonData.getJSONObject(calculatedRequestedDataRowIndex);
//				text = comment.getString("text");
//				timePostedInSecs = comment.getLong("time");
//				fromid = comment.getLong("fromid");
//				comment_id = comment.getString("id");
//				
//				User user = userMap.get(fromid);
//				if(user != null){
//					fromName = user.name;				
//					long nowInSecs = new Date().getTime()/1000;
//					long elapsedSecs = nowInSecs - timePostedInSecs;
//					since = Util.createElapsedString(elapsedSecs, new Date().getTime(), timePostedInSecs);
//				}
//			}catch(JSONException e){
//				e.printStackTrace();
//			}			
//			
//			try{
//				itemTag.comment.setText(text==null?"-":text.trim());
//			}catch(NullPointerException e){				
//			}
//			
//			itemTag.name.setText(fromName);			
//			itemTag.time.setText(since);						
//			itemTag.comment_id = comment_id;
//			itemTag.fromid = fromid;
//			itemTag.internalPosition = requestedPosition; 
//				
//			return convertView;
//		}
//		
//		
//		public class InternalTag{
//			public byte viewType;
//			public byte loadDirection; // 1 or -1
//		}
//		
//		public class Holder{
//			public TextView comment;
//			public TextView name;
//			public TextView time;
//			public String comment_id;
//			public long fromid;
//			public int internalPosition;
//			
//		}
//		
//	};