package com.neusou.moobook.adapters;

import java.util.Date;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.FBUser;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.data.User;
import com.neusou.web.ImageUrlLoader2;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderInput;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderProgress;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderResult;

public class CommentsAdapter extends GenericPageableAdapter<JSONArray, PageableDataStore<JSONArray> >{

	static final String LOG_TAG = "CommentsAdapter";
	HashMap<Long, User> userMap = new HashMap<Long, User>(25);		
	HashMap<Long, Long> mSkipCount = new HashMap<Long,Long>();	
	int[] mJumpCount;
	//public static final int TAG_INTERNAL = R.id.tag_commentsadapter_item_internal;
	public static final int TAG_ITEM_VIEW = R.id.tag_commentsadapter_item_view;
	public static final int TAG_ITEM_DATA = R.id.tag_commentsadapter_item_data;
	public static final int DEFAULT_PICTURE_RESID = R.drawable.defaultprofileimage;
	Bitmap mDefaultProfileImage;
	
	public CommentsAdapter(Activity ctx) {
		super(ctx);		
		mDefaultProfileImage = App.INSTANCE.mDefaultProfileBitmap;
	}

	public class ItemTag{
		public TextView comment;
		public TextView name;
		public TextView time;
		public String comment_id;
		public ImageView picture;
		public long fromid;
		public int internalPosition;		
	}
	
	public class DataTag{
		public String name;
		public String imageUri;
		public String comment;
		public long fromid;
		public String comment_id;		
	}
		
	public void clearUsersData(){
		userMap.clear();	
	}
	
	public void setData(PageableDataStore<JSONArray> data){
		datastores = data;
	}
	
	public void clearData(){
		datastores.clear();
		datastores = null;
		userMap.clear();
	}
	
	public void parseUsersJsonData(JSONArray data){
		Logger.l(Logger.DEBUG,LOG_TAG, "[parseUsersJsonData()]");
		
		if(data == null){
			Logger.l(Logger.DEBUG,LOG_TAG, "[parseUsersJsonData()] data is null");
			return;
		}
		
		String fqlResultSet;
		
		try {
			fqlResultSet = data.toString(2);
			Logger.l(Logger.DEBUG,LOG_TAG, "[parseUsersJsonData()] fql result set: "+fqlResultSet);
		} catch (JSONException e1) {
			e1.printStackTrace();
		}

				
		for(int i=0,num=data.length();i<num;i++){
		try{
			User user = new User();
			JSONObject userObj = data.getJSONObject(i);
			user.uid = userObj.getLong("uid");
			if(userMap.get(user.uid) == null || true){				
				user.name = userObj.getString(FBUser.fields_name);
				user.name = user.name.equals("null")?"":user.name;
				user.pic_square = userObj.getString(FBUser.fields_pic_square);
				try{
				user.timezone = userObj.getString(FBUser.fields_timezone);
				}catch(JSONException e){					
				}
				try{
				user.current_location = userObj.getString(FBUser.fields_current_location);
				}catch(JSONException e){					
				}
				try{
				user.online_presence = userObj.getString(FBUser.fields_online_presence);								
				}catch(JSONException e){					
				}
				userMap.put(user.uid, user);
				Logger.l(Logger.DEBUG, LOG_TAG,"[parseUsersJsonData()] name:"+ user.name+", location:"+user.current_location+" "+user.online_presence+" "+user.status);
			}			
			
		}catch(JSONException e){	
			e.printStackTrace();
		}
		}
	}
	
	java.util.Locale mLocale = new java.util.Locale("en_US");
	
	@Override
	public View createDataView(int requestedPosition, View convertView, ViewGroup parent) {
		//bind views to itemtag if itemtag does not exist
		Logger.l(Logger.DEBUG, LOG_TAG, "[createDataView()] pos:"+requestedPosition);
		ItemTag itemTag = null;
		DataTag dataTag = null;
		InternalTag internalTag = null;
		boolean isInflateNewView = false;
		
		//create a brand new view if the recycled convertView is null. 
		if(convertView == null){
			isInflateNewView = true;		
		}
		else{
		//otherwise check the viewtype in the internal tag object to determine the class of the View.		
			try{
				internalTag = (InternalTag) convertView.getTag(TAG_INTERNAL);
			}catch(Exception e){
				e.printStackTrace();
				isInflateNewView = true;
			}
			if(internalTag == null || internalTag.viewType != VIEWTYPE_DATA){
				isInflateNewView = true;
			}
		}
		
		if(isInflateNewView){
			convertView = mLayoutInflater.inflate(R.layout.t_comment, parent, false);
		}		
		
		// get reference to UI views
		try{
			itemTag = (CommentsAdapter.ItemTag) convertView.getTag(TAG_ITEM_VIEW);
			dataTag = (CommentsAdapter.DataTag) convertView.getTag(TAG_ITEM_DATA);
		}catch(Exception e){
		}
		
		if(itemTag == null){
			itemTag = new ItemTag();
			itemTag.comment = (TextView) convertView.findViewById(R.id.comment);
			itemTag.name = (TextView) convertView.findViewById(R.id.name);
			itemTag.time = (TextView) convertView.findViewById(R.id.time);
			itemTag.picture = (ImageView) convertView.findViewById(R.id.profile_pic);
			
			convertView.setTag(TAG_ITEM_VIEW,itemTag);
		}
		
		if(dataTag == null){
			dataTag = new DataTag();			
			convertView.setTag(TAG_ITEM_DATA,dataTag);
		}
								
		
		
		String commentText = null,senderUserName = null, comment_id = null, pictureImageUri = null;
		String online_presence = "";
		String current_location = ""; 
		String status = "";
		String locale = "en_US";
		long fromid = -1;
		String since = "";
		long timePostedInSecs = 0;
		
		try{
			//int validNonDeletedPos = findValidPos(requestedPosition);
			//assert(validNonDeletedPos >= 0 && validNonDeletedPos < commentsJsonData.length());
			//JSONObject comment = commentsJsonData.getJSONObject(validNonDeletedPos);

			JSONObject comment = (JSONObject) datastores.getAt(requestedPosition);
			commentText = comment.getString("text");
			timePostedInSecs = comment.getLong("time");
			fromid = comment.getLong("fromid");
			comment_id = comment.getString("id");
			
			User user = userMap.get(fromid);
			if(user != null){
				senderUserName = user.name==null?"":user.name;	
				pictureImageUri = user.pic_square;
				status = user.status;
				locale = user.locale;
				current_location = user.current_location;
				online_presence = user.online_presence;
				long nowInSecs = new Date().getTime()/1000;
				long elapsedSecs = nowInSecs - timePostedInSecs;
				since = Util.createElapsedString(elapsedSecs, new Date().getTime(), timePostedInSecs);
			}
		}catch(JSONException e){
			e.printStackTrace();
		}			
		
		// assign datatag variables
		
		dataTag.name = senderUserName;
		dataTag.imageUri = pictureImageUri;
		dataTag.comment = commentText;
		dataTag.fromid = fromid;
		dataTag.comment_id = comment_id; 
			
		// bind data to UI views		
			
		
		try{
			itemTag.comment.setText(commentText==null?"-":commentText.trim());
		}catch(NullPointerException e){				
		}
		
		//mystery bug: sometimes the reference to the ui views gone.
		String inferredCountry = "";
		try{
			inferredCountry = new java.util.Locale(locale).getCountry();
		}catch(Exception e){			
		}		
		
		try{
		itemTag.name.setText(senderUserName+"\n"+inferredCountry);			
		itemTag.time.setText(since);				
		}catch(Exception e){			
		}
			
		itemTag.comment_id = comment_id;
		itemTag.fromid = fromid;
		itemTag.internalPosition = requestedPosition; 
			
		Bitmap bmp;
		bmp = App.mImageUrlLoader2.loadImage(pictureImageUri, true);
		if(bmp == null){
			AsyncLoaderInput input = new AsyncLoaderInput();
			input.imageUri = pictureImageUri;
			App.mImageUrlLoader2.loadImageAsync(
					App.INSTANCE.mExecScopeImageLoaderTask,input, mImageAsyncLoaderListener);
			bmp = mDefaultProfileImage;
		}
		try{
			itemTag.picture.setImageBitmap(bmp);
		}catch(Exception e){			
		}
		
		Logger.l(Logger.DEBUG, LOG_TAG, "[createDataView()] time:"+timePostedInSecs+" name:"+senderUserName);
		return convertView;		
	}
	
	
	ImageUrlLoader2.AsyncListener mImageAsyncLoaderListener = new ImageUrlLoader2.AsyncListener() {
		
		@Override
		public void onPreExecute() {								
		}
		
		@Override
		public void onPostExecute(final AsyncLoaderResult result) {
			
			
		}
		
		@Override
		public void onCancelled() {
			
		}

		@Override
		public void onPublishProgress(final AsyncLoaderProgress progress) {
			Activity ctx = mActivityWeakRef.get();
			
			ctx.runOnUiThread(new Runnable() {				
				@Override
				public void run() {
					if(progress!= null && progress.success){
						notifyDataSetChanged();
					}					
				}
			});
			
		}
	};
	
	
	
}