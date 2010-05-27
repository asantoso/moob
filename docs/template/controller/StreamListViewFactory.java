package com.neusou.moobook.controller;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message; //import android.util.Log;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast; //import android.widget.ProgressBar;
import android.widget.TextView;

import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.activity.DisplayImageActivity;
import com.neusou.moobook.core.SoftHashMap;
import com.neusou.moobook.data.Attachment;
import com.neusou.moobook.data.MediaImageTag;
import com.neusou.moobook.data.Stream;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.moobook.model.database.DBHelper;
import com.neusou.moobook.thread.UserTask;
import com.neusou.moobook.thread.UserTask2;

public class StreamListViewFactory extends BaseListViewFactory<Cursor> {
	Util util = new Util();
	Drawable mLoadingDrawable;
	StringBuilder mStringBuilder;
	ColorMatrix mColorMatrix = new ColorMatrix(new float[] { 0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.1f, 0.4f, 0.1f, 0.0f, 0.0f, 0.2f, 0.2f, 0.1f, 0.0f,
			0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f });

	ColorMatrixColorFilter mColorMatrixColorFilter = new ColorMatrixColorFilter(
			mColorMatrix);

	byte MAX_ATTACHMENTS = 5;

	public SoftHashMap<String, PostQuickInfo> mPQI_Registry = new SoftHashMap<String, PostQuickInfo>(
			100, 1.5f);
	
	public void setDirty(String[] ids){
		if(ids == null){
			return;
		}
		for(int i=0,num=ids.length; i<num; i++){
			if(ids[i]!=null){
				PostQuickInfo data = mPQI_Registry.get(ids[i]);
				if(data != null){
					data.isDirty = true;
				}	
			}			
		}
	}
	
	SoftHashMap<String, View> mProgressBar_Registry = new SoftHashMap<String, View>(
			100, 1.5f);

	public long getPQICount() {
		if (mPQI_Registry == null) {
			return 0;
		}
		return mPQI_Registry.size();
	}

	Drawable mEmptyDrawable;
	Context ctx;
	byte loadCount = 0;

	ExpandableListView mListView;

	public StreamListViewFactory(Activity app, ExpandableListView listView) {
		super(app);
		this.ctx = app.getApplicationContext();
		Resources res = ctx.getResources();
		mLoadingDrawable = res.getDrawable(R.drawable.mobook2_exclaimation_64);
		mStringBuilder = new StringBuilder();
		mEmptyDrawable = res.getDrawable(R.drawable.empty);
		mListView = listView;
	}

	public class PostQuickInfo {
		public boolean isDirty = true;
		public boolean hasValidAttachment;
		public boolean hasMediaAttachment;
		public byte numMediaImages;
		
		
		public String name;// both actor + target
		public String profilePictureUrl;
		public long updateTtime;
		public String message;
		public long numLikes;
		public String actorName;
		public String targetName;
		
		public String videoSourceUrl;
		public byte attachmentType;
		public String attachmentJsonData;
		public String attachmentName;
		public String attachmentCaption;
		public String attachmentDescription;
		public String attachmentAttribution;
		public String attachmentIcon;
		public String attachmentHref;
		public long comments_count;
		public boolean comments_can_post;

		public boolean likes_canlike;
		public boolean likes_user_likes;
		
		public String post_id;
		public long target_id;
		public long actor_id;

		public String mMediaMp3Artist[] = new String[MAX_ATTACHMENTS];
		public String mMediaMp3Title[] = new String[MAX_ATTACHMENTS];
		public String mMediaMp3Album[] = new String[MAX_ATTACHMENTS];
		
		public String mediaImagesSrcs[] = new String[MAX_ATTACHMENTS];
		public String mediaImagesHref[] = new String[MAX_ATTACHMENTS];

	}

	public class Holder {
		public TextView name;
		public TextView targetName;
		public TextView message;
		public TextView comments;
		public TextView likes;

		// public ImageView attachmentPreviewImage;
		public TextView attachmentDescription;
		public TextView attachmentName;
		public View attachmentContainer;
		public View mediaImagesContainer;
		public ImageView profileImage;

		public static final byte TOTAL_MEDIAIMAGES = 5;
		public ImageView mediaimages[] = new ImageView[TOTAL_MEDIAIMAGES];
		public ImageView mediaimage0;
		public ImageView mediaimage1;
		public ImageView mediaimage2;
		public ImageView mediaimage3;
		public ImageView mediaimage4;

		public TextView since;
		public BGLoader loader;

		public ImageView mProgressBar;
		public int position; // cancel asynctask if the recycled row is used as
		// new row
		public String post_id;

		public boolean isProgressBarShown = false;

		public void reset() {
			isProgressBarShown = false;
			post_id = null;
			attachmentDescription = null;
		}
	}

	public class AsyncLoadData {
		// public ProgressBar loadingProgress;
		public String profileImageUrl;
		public String attachmentJsonData;
		public boolean isLoadProfileImage;
		public boolean isProcessAttachment;
		public boolean isLoadMediaImages;
		public PostQuickInfo mPQI;
		public Holder mHolder;
	}

	public void destroy() {
		// this.mDB.close();
		// this.mDBHelper.close();
	}

	String attachmentJsonData = null;
	String profilePictureURL = null;
	String actorPicSquare = null;
	String actorPic = null;
	String actorPicSmall = null;
	String actorPicBig = null;
	String pic_small = null;
	String pic_big = null;
	String pic = null;
	String actorName = null;
	String actorFirstname = null;
	String actorLastname = null;
	String targetFirstname = null;
	String targetLastname = null;
	String targetName = null;
	String message = null;
	String post_id;

	long comments_count;
	boolean comments_can_post;
	int numLikes;
	boolean likes_canlike = false;
	boolean likes_userlikes = false;
	
	final int colindex_targetname = 0;
	final int colindex_targetfirstname = 1;
	final int colindex_targetlastname = 2;
	final int colindex_actorname = 3;
	final int colindex_actorpicsquare = 4;
	final int colindex_actorpic = 5;
	final int colindex_actorpicsmall = 6;
	final int colindex_actorpicbig = 7;
	final int colindex_actorfirstname = 8;
	final int colindex_actorlastname = 9;
	// @see ApplicationDBHelper
	final int startStreamTableIndex = ApplicationDBHelper.START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC;

	View.OnClickListener mediaImageOnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			// Toast.makeText(ctx, (String)(v.getTag()), 2000).show();
			Intent i = new Intent(ctx, DisplayImageActivity.class);
			MediaImageTag tag = (MediaImageTag) v.getTag();

			i.putExtra(DisplayImageActivity.XTRA_IMAGE_URL, tag.imageSrc);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			ctx.startActivity(i);
		}

	};

	synchronized public View createView(Cursor ds, int position,
			View convertView, final ViewGroup parent) {

		try {
			// check if the cursor is still open
			// cursor can be closed when the corresponding sqlitedb is closed
			// caused by orientation screen changes.
			//
			ds.moveToPosition(position);
		} catch (Exception e) {
			// e.printStackTrace();
			return mLayoutInflater.inflate(R.layout.empty, parent, false);
		}

		Holder tag;

		if (convertView != null) {
			tag = (Holder) convertView.getTag();

			if (position != tag.position) { // cancel asyncTask of the row if
				// the
				// row is a previous row.
				// Log.d("agus3","task cancelled");
				if (tag.loader != null) {
					tag.loader.cancel(true); // cancel task immediately. (in the
					// BGLoader code, we check for
					// Status if it is running we
					// perform UI update)
					tag.loader = null;
				}
			}

			

		} else {

			convertView = mLayoutInflater.inflate(R.layout.t_streampost,
					parent, false);

			tag = new Holder();
			tag.name = (TextView) convertView.findViewById(R.id.name);
			tag.message = (TextView) convertView.findViewById(R.id.message);
			tag.comments = (TextView) convertView.findViewById(R.id.comments);
			tag.likes = (TextView) convertView.findViewById(R.id.likes);
			tag.since = (TextView) convertView.findViewById(R.id.since);
			tag.profileImage = (ImageView) convertView
					.findViewById(R.id.profile_pic);
			tag.targetName = (TextView) convertView
					.findViewById(R.id.targetname);

			tag.attachmentDescription = (TextView) convertView
					.findViewById(R.id.attachmentdescription);
			tag.attachmentName = (TextView) convertView
					.findViewById(R.id.attachmentname);
			tag.mediaimages[0] = (ImageView) convertView
					.findViewById(R.id.mediaimage0);
			tag.mediaimages[1] = (ImageView) convertView
					.findViewById(R.id.mediaimage1);
			tag.mediaimages[2] = (ImageView) convertView
					.findViewById(R.id.mediaimage2);
			tag.mediaimages[3] = (ImageView) convertView
					.findViewById(R.id.mediaimage3);
			tag.mediaimages[4] = (ImageView) convertView
					.findViewById(R.id.mediaimage4);
			tag.attachmentContainer = convertView
					.findViewById(R.id.attachmentcontainer);
			tag.mediaImagesContainer = convertView
					.findViewById(R.id.mediaimagescontainer);

			tag.mProgressBar = (ImageView) convertView
					.findViewById(R.id.progress);

			for (int i = 0; i < 3; i++) {
				tag.mediaimages[i]
						.setOnClickListener(mediaImageOnClickListener);
			}
		}

		if (tag.mProgressBar != null) {
			tag.mProgressBar.setVisibility(View.INVISIBLE);
		}
		
		tag.message.setText("");
		tag.attachmentName.setText("");
		tag.attachmentDescription.setText("");

		post_id = ds.getString(Stream.col_post_id + startStreamTableIndex);
		PostQuickInfo existingPQI = (PostQuickInfo) mPQI_Registry.get(post_id);		
				
		long updatedSecs = ds.getLong(Stream.col_updated_time+ startStreamTableIndex);
		long nowMilliseconds = (new Date()).getTime();
		long elapsedSecs = nowMilliseconds / 1000 - updatedSecs;
		String since = Util.createElapsedString(elapsedSecs, nowMilliseconds, updatedSecs);
			
		// SET REQUIRED DESCRIPTION FOR ASYNCHRONOUS OPERATION
		AsyncLoadData asyncLoadData = new AsyncLoadData();
		asyncLoadData.isLoadProfileImage = false;
		asyncLoadData.isProcessAttachment = false;
		asyncLoadData.isLoadMediaImages = false;
		
		// PQI not null means that we still have the result of the processed
		// attachment data.
		if (existingPQI != null && !existingPQI.isDirty) {
			
			tag.attachmentName.setText(existingPQI.attachmentName);
			tag.attachmentDescription.setText(existingPQI.attachmentDescription);
			if (existingPQI.hasValidAttachment) {
				tag.attachmentContainer.setVisibility(View.VISIBLE);
			}
			
			for (int i = 0; i < existingPQI.numMediaImages; i++) {
				tag.mediaimages[i].setVisibility(View.VISIBLE);				
				Drawable d = util.loadImage(existingPQI.mediaImagesSrcs[i], true);
				
				//set tag
				MediaImageTag mediaTag = (MediaImageTag) tag.mediaimages[i].getTag();
				if (mediaTag == null) {
					mediaTag = new MediaImageTag();
				}
				mediaTag.imageSrc = existingPQI.mediaImagesSrcs[i];
				tag.mediaimages[i].setTag(mediaTag);
				//end of set tag
								
				if (d == null) {
					asyncLoadData.isLoadMediaImages = true;
					tag.mediaimages[i].setImageDrawable(App.mLoadingDrawable);
				} else {
					tag.mediaimages[i].setImageDrawable(d);
				}
			}			
						
			for (int i = existingPQI.numMediaImages;
					i < Holder.TOTAL_MEDIAIMAGES; 
					i ++) {
				tag.mediaimages[i].setVisibility(View.GONE);
			}
			
			showHideAttachmentFields(existingPQI.hasValidAttachment,
					existingPQI.hasMediaAttachment,
					existingPQI.attachmentDescription,
					existingPQI.attachmentName,
					existingPQI.attachmentAttribution, tag.attachmentName,
					tag.attachmentDescription, tag.mediaImagesContainer,
					tag.attachmentContainer);
			
		} else {
			
			for (int i = 0; i < Holder.TOTAL_MEDIAIMAGES; i++) {
				tag.mediaimages[i].setVisibility(View.GONE);
			}
			
			actorPicSquare = ds.getString(colindex_actorpicsquare);
			actorPic = ds.getString(colindex_actorpic);
			actorPicBig = ds.getString(colindex_actorpicbig);
			actorPicSmall = ds.getString(colindex_actorpicsmall);

			if (actorPicSquare != null && actorPicSquare.length() > 0) {
				profilePictureURL = actorPicSquare;
			} else if (actorPicSmall != null && actorPicSmall.length() > 0) {
				profilePictureURL = actorPicSmall;
			} else if (actorPic != null && actorPic.length() > 0) {
				profilePictureURL = actorPic;
			} else if (actorPicBig != null && actorPicBig.length() > 0) {
				profilePictureURL = actorPicBig;
			}
						
			targetName = ds.getString(colindex_targetname);
			targetFirstname = ds.getString(colindex_targetfirstname);
			targetLastname = ds.getString(colindex_targetlastname);
			// actor name
			actorName = ds.getString(colindex_actorname);
			actorFirstname = ds.getString(colindex_actorfirstname);
			actorLastname = ds.getString(colindex_actorfirstname);
			
			if (actorName == null) {
				if (actorFirstname != null && actorLastname != null) {
					mStringBuilder.append(actorFirstname);
					mStringBuilder.append(" ");
					mStringBuilder.append(actorLastname);
					actorName = mStringBuilder.toString();
					mStringBuilder.delete(0, mStringBuilder.length() - 1);
				}
			}

			if (targetName == null) {
				if (targetFirstname != null && targetLastname != null) {
					mStringBuilder.append(targetFirstname);
					mStringBuilder.append(" ");
					mStringBuilder.append(targetLastname);
					targetName = mStringBuilder.toString();
					mStringBuilder.delete(0, mStringBuilder.length() - 1);
				}
			}
					
			likes_canlike = ds.getLong(Stream.col_likes_canlike + startStreamTableIndex) == 0?false:true;
			likes_userlikes = ds.getLong(Stream.col_likes_userlikes + startStreamTableIndex) == 0?false:true;
			
			comments_can_post = ds.getLong(Stream.col_comments_can_post + startStreamTableIndex) == 0?false:true;
			comments_count = ds.getLong(Stream.col_comments_count + startStreamTableIndex);
			message = ds.getString(Stream.col_message + startStreamTableIndex);			
			numLikes = ds.getInt(Stream.col_likes_count + startStreamTableIndex);			
			attachmentJsonData = ds.getString(Stream.col_attachment	+ startStreamTableIndex);
						
			existingPQI = new PostQuickInfo();
			existingPQI.isDirty = false;
			existingPQI.profilePictureUrl = profilePictureURL;
			existingPQI.attachmentJsonData = attachmentJsonData;
			existingPQI.numLikes = numLikes;
			existingPQI.likes_canlike = likes_canlike;
			existingPQI.likes_user_likes = likes_userlikes;
			
			existingPQI.message = message;
			existingPQI.comments_count = comments_count;
			existingPQI.comments_can_post = comments_can_post;
			
			existingPQI.post_id = post_id;		
			existingPQI.name = actorName;
			existingPQI.actorName = actorName;
			existingPQI.targetName = targetName;
		
			if (targetName != null) {
				existingPQI.name += " > " +targetName;
			}
				
			asyncLoadData.isProcessAttachment = true;
			asyncLoadData.isLoadMediaImages = true;
			asyncLoadData.isLoadProfileImage = true;
		}

		String likesText = "";
		likesText += existingPQI.numLikes + " ";
		likesText += existingPQI.numLikes > 1 ? "likes" : "like";
		
		// SET VALUES TO VIEWS
		if (existingPQI.message == null || existingPQI.message.length() == 0) {
			tag.message.setVisibility(View.GONE);
		} else {
			tag.message.setText(existingPQI.message);
			tag.message.setVisibility(View.VISIBLE);
		}
		tag.since.setText(since);
		tag.likes.setText(likesText);		
		tag.name.setText(existingPQI.name);
		tag.comments.setText(existingPQI.comments_count + " comments");
							
		// when no image for profile is available, use default profile image
		if (existingPQI.profilePictureUrl == null || existingPQI.profilePictureUrl.length() == 0) {				
			Drawable img = mResources.getDrawable(R.drawable.mobook2_64);
			tag.profileImage.setImageDrawable(img);
		}
		else{
						
			// if profileimageurl is specified, try to immediately load the
			// image from cache.
			Drawable img = util.loadImage(existingPQI.profilePictureUrl, true);
			// if we can't get the drawable immediately from the caches (sdcard
			// or memory) then
			// try to load it asynchronously from the web.
			asyncLoadData.isLoadProfileImage = (img == null);	
			asyncLoadData.profileImageUrl = existingPQI.profilePictureUrl;
			// if we can get the drawable from the cache then show it
			if (img == null) {
				img = mLoadingDrawable;
			}
			tag.profileImage.setImageDrawable(img);
		}
		
		tag.post_id = post_id;
		convertView.setTag(tag);
		
		if (asyncLoadData.isLoadProfileImage
				|| asyncLoadData.isProcessAttachment
				|| asyncLoadData.isLoadMediaImages) {
			
			if (asyncLoadData.isProcessAttachment ||
				asyncLoadData.isLoadMediaImages) {			
				tag.mProgressBar.setVisibility(View.VISIBLE);
			}
			
			asyncLoadData.mPQI = existingPQI;
			asyncLoadData.mPQI.post_id = post_id;
			asyncLoadData.attachmentJsonData = attachmentJsonData;
			asyncLoadData.mHolder = tag;
			
			tag.position = position;
			
			tag.loader = new BGLoader();
			tag.loader.execute(asyncLoadData);
		}		
		
		return convertView;
	}

	synchronized private void showMediaImages(int numMediaImages,
			Drawable mMediaPhotosDrawables[], ImageView[] views, String[] hrefs) {
		for (int i = 0; i < numMediaImages; i++) {
			views[i].setVisibility(View.VISIBLE);
			MediaImageTag tag = (MediaImageTag) views[i].getTag();
			if (tag == null) {
				tag = new MediaImageTag();
			}
			tag.imageSrc = hrefs[i];
			views[i].setTag(tag);
			views[i].setImageDrawable(mMediaPhotosDrawables[i]);
		}
	}

	synchronized private void showHideAttachmentFields(
			boolean hasValidAttachment, boolean hasMediaAttachment,
			String attachmentDescription, String attachmentName,
			String attachmentAttribution, TextView nameView, TextView descView,
			View mediaImagesContainer, View attachmentContainer) {
		descView.setText(attachmentDescription);
		descView.setVisibility(View.VISIBLE);

		if (attachmentName == null || attachmentName.length() == 0) {
			if (attachmentAttribution != null) {
				nameView.setText(attachmentAttribution);
				nameView.setVisibility(View.VISIBLE);
			}
			nameView.setVisibility(View.GONE);
		} else {
			nameView.setText(attachmentName);
			nameView.setVisibility(View.VISIBLE);
		}

		if (hasMediaAttachment) {
			int visible = mediaImagesContainer.getVisibility();
			if(visible != View.VISIBLE){
				mediaImagesContainer.setVisibility(View.VISIBLE);	
			}			
		} else {
			mediaImagesContainer.setVisibility(View.GONE);
		}

		if (hasValidAttachment) {
			int visible = attachmentContainer.getVisibility();
			if(visible != View.VISIBLE){
				attachmentContainer.setVisibility(View.VISIBLE);
			}
		} else {
			attachmentContainer.setVisibility(View.GONE);
		}

	}

	public boolean process(Cursor ds, int position){
		if(ds == null || ds.isClosed()){
			return false;
		}
		if(position <= ds.getCount() && position >= 0){
			ds.moveToPosition(position);
		}else{
			return false;
		}
		String attachmentJsonData = ds.getString(Stream.col_attachment+ApplicationDBHelper.START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC);
		String post_id = ds.getString(Stream.col_post_id+ApplicationDBHelper.START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC);
		PostQuickInfo pqi = mPQI_Registry.get(post_id);
		if(pqi == null){
			pqi = new PostQuickInfo();
		}
		processAttachment(pqi, attachmentJsonData);
		mPQI_Registry.put(post_id,pqi);
		return true;
	}
	
	private boolean processAttachment(PostQuickInfo pqi,
			String attachmentJsonData) {
		if (attachmentJsonData == null) {
			return false;
		}

		JSONObject mAttachmentData = null;
		boolean isParseAttachmentSuccessful;

		try {
			mAttachmentData = new JSONObject(attachmentJsonData);
			isParseAttachmentSuccessful = true;
			// Log.d("agus3","can parse attachment "+mAttachmentData.toString());

			// now determine if there is a valid attachment
			try {
				Object descObj = mAttachmentData.get("description");
				Object mediaObj = mAttachmentData.get("media");
				if (descObj instanceof String || mediaObj instanceof JSONArray) {
					pqi.hasValidAttachment = true;
				}
				pqi.hasValidAttachment = true;
				// Log.d("agus3",
				// pqi.name+", desc: "+descObj.getClass().getCanonicalName());
				// Log.d("agus3",
				// pqi.name+", media: "+mediaObj.getClass().getCanonicalName());

				// String desc = mAttachmentData.getString("description");
				// JSONArray media = mAttachmentData.getJSONArray("media");
			} catch (JSONException e) {
				pqi.hasValidAttachment = false;
			}
			// end of valid attachment detection

		} catch (JSONException e) {
			isParseAttachmentSuccessful = false;
			return false;
			// Log.d("agus3","cant parse attachment");
		}

		if (isParseAttachmentSuccessful) {
			try {
				try {
					String attr = mAttachmentData.getString("attribution");
					// Log.d("agus3","attribution: "+attr);
				} catch (JSONException e) {
					// Log.d("agus3","no attribution data");
				}

				JSONArray mediaArray = mAttachmentData.getJSONArray("media");
				byte numMedia = (byte) mediaArray.length();
				// Log.d("agus3","num of media: "+numMedia);

				try {
					pqi.attachmentAttribution = mAttachmentData
							.getString("attribution");
				} catch (JSONException e) {
				}

				if (numMedia > 0) {
					pqi.hasMediaAttachment = true;
					JSONObject media = mediaArray.getJSONObject(0);
					String mediaType = media.getString("type");

					// Log.d("test","mediatype: "+mediaType+" name: "+mHolder.name.getText().toString());

					if (mediaType.compareTo(Attachment.MEDIATYPE_LINK) == 0) {
						pqi.attachmentType = Attachment.ATTACHMENT_TYPE_MEDIALINK;
					} else if (mediaType.compareTo(Attachment.MEDIATYPE_IMAGE) == 0) {
						pqi.attachmentType = Attachment.ATTACHMENT_TYPE_MEDIAIMAGE;
					} else if (mediaType.compareTo(Attachment.MEDIATYPE_FLASH) == 0) {
						pqi.attachmentType = Attachment.ATTACHMENT_TYPE_MEDIAVIDEO;
					} else if (mediaType.compareTo(Attachment.MEDIATYPE_MP3) == 0) {
						pqi.attachmentType = Attachment.ATTACHMENT_TYPE_MEDIAMP3;
					}
				} else {
					pqi.attachmentType = Attachment.ATTACHMENT_TYPE_NOTE;

				}

				switch (pqi.attachmentType) {
				case Attachment.ATTACHMENT_TYPE_MEDIALINK: {
					// Log.d("test","media link name: "+mHolder.name.getText().toString());

					try {
						JSONObject media = mediaArray.getJSONObject(0);
						pqi.numMediaImages = 1;
						String src = media.getString("src");
						pqi.mediaImagesSrcs[0] = src;
						String href = media.getString("href");
						pqi.mediaImagesHref[0] = href;
						util.loadImage(src, false);
					} catch (JSONException e) {
						e.printStackTrace();
					}

					break;
				}

				case Attachment.ATTACHMENT_TYPE_MEDIAIMAGE: {
					// Log.d("test","media image "+numMedia+
					// " name: "+mHolder.name.getText().toString());
					pqi.numMediaImages = (byte) numMedia;
					for (int i = 0; i < pqi.numMediaImages; i++) {
						try {
							JSONObject media;
							media = mediaArray.getJSONObject(i);
							pqi.mediaImagesHref[i] = media.getString("href");
							pqi.mediaImagesSrcs[i] = media.getString("src");
							// Log.d("test","mediaimage src: "+mMediaSrcs[i]);
							util.loadImage(pqi.mediaImagesSrcs[i], false);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
					break;
				}

				case Attachment.ATTACHMENT_TYPE_MEDIAVIDEO: {
					// Log.d("test","media video "+numMedia+
					// " name: "+mHolder.name.getText().toString());

					try {
						pqi.numMediaImages = 1;
						JSONObject media = mediaArray.getJSONObject(0);
						String src = media.getString("src");
						String href = media.getString("href");
						JSONObject videoObj = media.getJSONObject("video");						
						String display_url = videoObj.getString("display_url");
						
						pqi.mediaImagesSrcs[0] = src;
						pqi.mediaImagesHref[0] = href;
						pqi.videoSourceUrl = display_url;
						//Log.d("test","media video src: "+src);
						Log.d("test","media video href: "+display_url);
						util.loadImage(src, false);
					} catch (JSONException e) {
						//e.printStackTrace();
					}

					break;
				}

				case Attachment.ATTACHMENT_TYPE_MEDIAMP3: {
					pqi.mMediaMp3Artist = new String[MAX_ATTACHMENTS];
					pqi.mMediaMp3Title = new String[MAX_ATTACHMENTS];
					pqi.mMediaMp3Album = new String[MAX_ATTACHMENTS];
					JSONObject media;
					for (int i = 0; i < numMedia; i++) {
						media = mediaArray.getJSONObject(i);
						String src = media.getString("src");
						String title = media.getString("title");
						String artist = media.getString("artist");
						String album = media.getString("album");
						pqi.mMediaMp3Album[i] = album;
						pqi.mMediaMp3Title[i] = title;
						pqi.mMediaMp3Artist[i] = artist;
						pqi.mediaImagesSrcs[i] = src;
						util.loadImage(src, false);
					}
					break;
				}

				case Attachment.ATTACHMENT_TYPE_NOTE: {
					break;
				}

				}

			} catch (JSONException e) {
			}

			// attachment name
			try {
				pqi.attachmentName = mAttachmentData.getString("name");
			} catch (JSONException f) {
				pqi.attachmentName = null;
			}

			// attachment href
			try {
				pqi.attachmentHref = mAttachmentData.getString("href");
			} catch (JSONException f) {
				pqi.attachmentHref = null;
			}
			// attachment description
			try {
				pqi.attachmentDescription = mAttachmentData
						.getString("description");
			} catch (JSONException f) {
				pqi.attachmentDescription = null;
			}
			// attachment icon
			try {
				pqi.attachmentIcon = mAttachmentData.getString("icon");
			} catch (JSONException f) {
				pqi.attachmentIcon = null;
			}
			// attachment caption
			try {
				pqi.attachmentCaption = mAttachmentData.getString("caption");
			} catch (JSONException f) {
				pqi.attachmentCaption = null;
			}

			return true;
		}// end of if parse attachment successful
		return false;
	}

	class BGLoader extends UserTask2<AsyncLoadData, Byte, Short> {

		// this Util instance creation will run inside another thread
		Util mUtility = new Util();

		public BGLoader() {

		}

		static final byte MAX_ATTACHMENTS = 5;
		AsyncLoadData mPostData;

		String attachmentName;
		String attachmentHref;
		String attachmentIcon;
		String attachmentDescription;
		String attachmentCaption;
		String attachmentAttribution;

		byte numMediaImages;

		Drawable mProfileImageDrawable;
		Drawable mMediaLinkDrawable;
		String mMediaLinkSrc;
		String mMediaLinkHref;
		Drawable mMediaPhotosDrawables[] = new Drawable[MAX_ATTACHMENTS];
		String mMediaHrefs[] = new String[MAX_ATTACHMENTS];
		String mMediaSrcs[] = new String[MAX_ATTACHMENTS];
		String mMediaMp3Artist[] = new String[MAX_ATTACHMENTS];
		String mMediaMp3Title[] = new String[MAX_ATTACHMENTS];
		String mMediaMp3Album[] = new String[MAX_ATTACHMENTS];

		JSONObject mAttachmentData;

		boolean hasMediaAttachment = false;
		boolean hasValidAttachment = false;
		boolean isParseAttachmentSuccessful = false;
		byte mediaAttachmentType = 0;

		static final byte START_SHOW_PROGRESS = 5;
		static final byte FINISH = 4;
		static final byte FINISH_PROCESS_ATTACHMENT = 0;
		static final byte FINISH_LOAD_PROFILEIMAGE = 1;
		static final byte FINISH_PARSING_MEDIAATTACHMENT_JSON = 2;
		static final byte FINISH_PARSING_BASEATTACHMENT_JSON = 3;
		static final byte SET_MEDIAIMAGES_PLACEHOLDER = 6;
		static final byte FINISH_LOAD_MEDIA_IMAGES = 8;
		static final byte LOAD_ATTACHMENT_IMAGES = 7;

		@Override
		protected Short doInBackground(AsyncLoadData... params) {
			// Log.d("agus3","doInBg..");

			// check if task is still running
			if (mStatus != Status.RUNNING) {
				return 0;
			}

			mPostData = params[0];

			boolean isShowingProgress = false;
			if (mPostData.isLoadProfileImage) {
				isShowingProgress = true;
				publishProgress(START_SHOW_PROGRESS);
				String imgUrl = params[0].profileImageUrl;
				// Log.d("agus3","loading.. "+imgUrl);
				mProfileImageDrawable = mUtility.loadImage(imgUrl, false);
				// Log.d("agus3","loaded drawable is null? "+(mProfileImageDrawable
				// == null));
				publishProgress(FINISH_LOAD_PROFILEIMAGE);
			}

			if (mPostData.isProcessAttachment) {
				if (!isShowingProgress) {
					publishProgress(START_SHOW_PROGRESS);
				}
				if (mStatus != Status.RUNNING) {
					return 0;
				}
				publishProgress(SET_MEDIAIMAGES_PLACEHOLDER);
				processAttachment(mPostData.mPQI, mPostData.attachmentJsonData);
				
				// Log.d("agus3","doInBg.. adding pqi in to hashmap");
				mPQI_Registry.put(mPostData.mPQI.post_id, mPostData.mPQI);
				// Log.d("agus3","doInBg.. done adding. total: "+mPQI_Registry.size());
			}

			if (mPostData.isLoadMediaImages) {
				// mMediaPhotosDrawables = new
				// Drawable[mPostData.mPQI.numMediaImages];
				for (int i = 0; i < mPostData.mPQI.numMediaImages; i++) {
					mMediaPhotosDrawables[i] = mUtility.loadImage(
							mPostData.mPQI.mediaImagesSrcs[i], false);
				}

				publishProgress(FINISH_LOAD_MEDIA_IMAGES);
			}

			publishProgress(FINISH_PROCESS_ATTACHMENT);

			return 0;
		}

		@Override
		protected void onProgressUpdate(Byte... values) {
			super.onProgressUpdate(values);
			short code = values[0];

			if (code == START_SHOW_PROGRESS) {
				showProgressIndicator();
			}

			else if (code == SET_MEDIAIMAGES_PLACEHOLDER) {
				mPostData.mHolder.mediaimages[0]
						.setImageDrawable(App.mLoadingDrawable);
				mPostData.mHolder.mediaimages[1]
						.setImageDrawable(App.mLoadingDrawable);
				mPostData.mHolder.mediaimages[2]
						.setImageDrawable(App.mLoadingDrawable);
			}

			else if (code == FINISH_LOAD_MEDIA_IMAGES) {
				if (mStatus == Status.RUNNING) {
					showMediaImages(mPostData.mPQI.numMediaImages,
							mMediaPhotosDrawables,
							mPostData.mHolder.mediaimages,
							mPostData.mPQI.mediaImagesSrcs);
				}
			}

			else if (code == FINISH_PROCESS_ATTACHMENT) {

				if (mStatus == Status.RUNNING) {
					showHideAttachmentFields(mPostData.mPQI.hasValidAttachment,
							mPostData.mPQI.hasMediaAttachment,
							mPostData.mPQI.attachmentDescription,
							mPostData.mPQI.attachmentName,
							mPostData.mPQI.attachmentAttribution,
							mPostData.mHolder.attachmentName,
							mPostData.mHolder.attachmentDescription,
							mPostData.mHolder.mediaImagesContainer,
							mPostData.mHolder.attachmentContainer);
				}
			}

			else if (code == FINISH_LOAD_PROFILEIMAGE) {
				if (mStatus == Status.RUNNING) {
					mPostData.mHolder.profileImage
							.setImageDrawable(mProfileImageDrawable);
					mPostData.mHolder.profileImage.setVisibility(View.VISIBLE);
				}
			}

		}

		@Override
		protected void onPostExecute(Short result) {
			super.onPostExecute(result);
			if (isProgressIndicatorShown) {
				hideProgressIndicator();
			}
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

		}

		@Override
		protected void onCancelled() {

			super.onCancelled();
			hideProgressIndicator();

		}

		boolean isProgressIndicatorShown = false;

		private void showProgressIndicator() {
			if (isProgressIndicatorShown) {
				return;
			}

			isProgressIndicatorShown = true;

			// NOTE: This showProgressIndicator method runs on main thread.

			// String tName = Thread.currentThread().getName();
			// Log.d("agus3","tName: "+tName);

			/*
			 * 
			 * Message m = uiHandler.obtainMessage(); m.obj =
			 * mPostData.mHolder.mProgressBar; m.what = 0; Bundle b = new
			 * Bundle(); b.putString("post_id", post_id); m.setData(b);
			 * m.sendToTarget();
			 */

			try {
				mPostData.mHolder.mProgressBar.setVisibility(View.VISIBLE);

			} catch (NullPointerException e) {

			}

		}

		private void hideProgressIndicator() {

			if (!isProgressIndicatorShown) {
				return;
			}
			isProgressIndicatorShown = false;

			/*
			 * Message m = uiHandler.obtainMessage(); m.obj =
			 * mPostData.mHolder.mProgressBar; m.what = 1; Bundle b = new
			 * Bundle(); b.putString("post_id", post_id); m.setData(b);
			 * m.sendToTarget();
			 */

			try {
				mPostData.mHolder.mProgressBar.setVisibility(View.INVISIBLE);
			} catch (NullPointerException e) {
			}

		}

	};

	abstract class PGBAnimationListener implements AnimationListener {
		public String id;
		public View v;

		public PGBAnimationListener(String id, View v) {
			this.id = id;
			this.v = v;
		}
	}

	private Animation createFadeOutAnimation(View v, String post_id) {
		v.clearAnimation();
		Animation anim = AnimationUtils
				.loadAnimation(this.ctx, R.anim.fade_out);

		AnimationListener animLst0 = new PGBAnimationListener(post_id, v) {

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// ProgressBar pgb = (ProgressBar)
				// mProgressBar_Registry.get(id);
				ImageView pgb = (ImageView) this.v;
				pgb.setVisibility(View.INVISIBLE);
				this.v = null;
			}
		};
		anim.setAnimationListener(animLst0);
		return anim;
	}

	private Animation createFadeInAnimation(View v, String post_id) {
		v.clearAnimation();

		Animation anim = AnimationUtils.loadAnimation(this.ctx, R.anim.fade_in);

		PGBAnimationListener animLst0 = new PGBAnimationListener(post_id, v) {

			@Override
			public void onAnimationStart(Animation animation) {
				// mProgressBar_Registry.put(this.id, v);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				// ProgressBar pgb = (ProgressBar)
				// mProgressBar_Registry.get(this.id);
				((ImageView) (this.v)).setVisibility(View.VISIBLE);
				this.v = null;
			}
		};
		anim.setAnimationListener(animLst0);
		return anim;
	}

	Handler uiHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			View v = (View) msg.obj;
			String post_id = msg.getData().getString("post_id");

			switch (msg.what) {
			case 0: {
				try {
					v.startAnimation(createFadeInAnimation(v, post_id));
				} catch (Exception e) {
				}
				break;
			}
			case 1: {
				try {
					v.startAnimation(createFadeOutAnimation(v, post_id));
				} catch (Exception e) {
				}
				break;
			}

			}

		};
	};

}
