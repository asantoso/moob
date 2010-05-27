package com.neusou.moobook.adapters;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.FBUser;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.data.User;
import com.neusou.web.ImageUrlLoader;
import com.neusou.web.ImageUrlLoader.AsyncLoaderResult;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.sax.RootElement;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class CommentsAdapter extends GenericPageableAdapter<JSONArray, PageableDataStore<JSONArray> >{

	static final String LOG_TAG = "CommentsAdapter";
	HashMap<Long, User> userMap = new HashMap<Long, User>(25);		
	HashMap<Long, Long> mSkipCount = new HashMap<Long,Long>();	
	int[] mJumpCount;
	//public static final int TAG_INTERNAL = R.id.tag_commentsadapter_item_internal;
	public static final int TAG_ITEM = R.id.tag_commentsadapter_item;
	public static final int DEFAULT_PICTURE_RESID = R.drawable.defaultprofileimage;
	Drawable mDefaultProfileImage;
	

	
	public CommentsAdapter(Activity ctx) {
		super(ctx);		
		mDefaultProfileImage = ctx.getResources().getDrawable(DEFAULT_PICTURE_RESID);
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
			itemTag = (ItemTag) convertView.getTag(TAG_ITEM);
		}catch(Exception e){
		}
		
		if(itemTag == null){
			itemTag = new ItemTag();
			itemTag.comment = (TextView) convertView.findViewById(R.id.comment);
			itemTag.name = (TextView) convertView.findViewById(R.id.name);
			itemTag.time = (TextView) convertView.findViewById(R.id.time);
			itemTag.picture = (ImageView) convertView.findViewById(R.id.profile_pic);
			
			convertView.setTag(TAG_ITEM,itemTag);
		}
									
		// bind data to UI views
		
		String text = null,fromName = null, comment_id = null, pictureUrl = null;
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
			text = comment.getString("text");
			timePostedInSecs = comment.getLong("time");
			fromid = comment.getLong("fromid");
			comment_id = comment.getString("id");
			
			User user = userMap.get(fromid);
			if(user != null){
				fromName = user.name==null?"":user.name;	
				pictureUrl = user.pic_square;
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
		
		try{
			itemTag.comment.setText(text==null?"-":text.trim());
		}catch(NullPointerException e){				
		}
		
		//mystery bug: sometimes the reference to the ui views gone.
		String inferredCountry = "";
		try{
			inferredCountry = new java.util.Locale(locale).getCountry();
		}catch(Exception e){
			
		}
		
		
		try{
		itemTag.name.setText(fromName+"\n"+inferredCountry);			
		itemTag.time.setText(since);				
		}catch(Exception e){			
		}
			
		itemTag.comment_id = comment_id;
		itemTag.fromid = fromid;
		itemTag.internalPosition = requestedPosition; 
			
		Drawable d; 
		d = App.mImageUrlLoader.loadImage(pictureUrl, true);
		if(d == null){
			App.mImageUrlLoader.loadImageAsync(pictureUrl, mImageAsyncLoaderListener);
			d = mDefaultProfileImage;
		}
		try{
		itemTag.picture.setImageDrawable(d);
		}catch(Exception e){			
		}
		
		Logger.l(Logger.DEBUG, LOG_TAG, "[createDataView()] time:"+timePostedInSecs+" name:"+fromName);
		return convertView;		
	}
	
	
	ImageUrlLoader.AsyncListener mImageAsyncLoaderListener = new ImageUrlLoader.AsyncListener() {
		
		@Override
		public void onPreExecute() {								
		}
		
		@Override
		public void onPostExecute(AsyncLoaderResult result) {
			
			if(result!= null && result.status == AsyncLoaderResult.SUCCESS){
				notifyDataSetChanged();
			}
		}
		
		@Override
		public void onCancelled() {
			
		}
	};
	
	
	
}