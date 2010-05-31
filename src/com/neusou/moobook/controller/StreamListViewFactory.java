package com.neusou.moobook.controller;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.DataSetObserver;
import android.database.StaleDataException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.neusou.DataState;
import com.neusou.Logger;
import com.neusou.SoftHashMap;
import com.neusou.async.UserTask;
import com.neusou.async.UserTaskExecutionScope;
import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.activity.DisplayImageActivity;
import com.neusou.moobook.data.Attachment;
import com.neusou.moobook.data.AttachmentMedia;
import com.neusou.moobook.data.Comment;
import com.neusou.moobook.data.MediaImageTag;
import com.neusou.moobook.data.ProcessedData;
import com.neusou.moobook.data.Stream;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.web.ImageUrlLoader2;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderInput;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderProgress;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderResult;

public class StreamListViewFactory extends BaseExpandableListViewFactory<Cursor,ProcessedData>

{
	
	static final String LOG_TAG = "StreamListViewFactory";
	
	static class GroupImageKey {
		public String uri;
		public Bitmap bitmap;
		
		public static GroupImageKey getInstance(){
			return new GroupImageKey();
		}
		
		public static Comparator<GroupImageKey> COMPARATOR = new Comparator<GroupImageKey>() {

			@Override
			public int compare(GroupImageKey object1, GroupImageKey object2) {
				return (object1.uri.compareTo(object2.uri));
			}

		};
	}

	private int mPreemptiveFetchSize = 5;
	private int mSolidCacheSize = 5;
	
	static final int MIN_BITMAP_WIDTH = 5;
	static final int MIN_BITMAP_HEIGHT = 5;
	
	Bitmap mProfileLoadingBitmap;
	Bitmap mProfileDefaultBitmap;
	Bitmap mAttachmentDefaultBitmap;
	Bitmap mAttachmentLoadingBitmap;
	StringBuilder mStringBuilder;	
	static byte MAX_ATTACHMENTS = 5;
	//SoftHashMap<String, ProcessedAttachmentData> mProcessedDataCache = new SoftHashMap<String, ProcessedAttachmentData>(100, 1.5f);
	//Drawable mEmptyDrawable;
	Activity ctx;
	//byte loadCount = 0;
			
	HashMap<Long, ArrayList<GroupImageKey>> mImagesCache = new HashMap<Long, ArrayList<GroupImageKey>>(mSolidCacheSize,1.7f);
		
	int app_id = -1;
	String attachmentJsonData = null;
	String actorPicSquare = null;
	String actorPic = null;
	String actorPicSmall = null;
	String actorPicBig = null;
	String pic_small = null;
	String pic_big = null;
	String pic = null;
	long actorId = 0;
	String actorName = null;
	String actorFirstname = null;
	String actorLastname = null;
	long targetId = -0;
	String targetFirstname = null;
	String targetLastname = null;
	String targetName = null;
	String message = null;
	String post_id;
	long updated_time; //milliseconds since epoch

	long comments_count;
	boolean comments_can_post;
	boolean comments_can_remove;
	int numLikes;
	boolean likes_canlike = false;
	boolean likes_userlikes = false;
	
	final int colindex_targetid = 0; 
	final int colindex_targetname = 1;
	final int colindex_targetfirstname = 2;
	final int colindex_targetlastname = 3;
	final int colindex_actorid = 4;
	final int colindex_actorname = 5;
	final int colindex_actorpicsquare = 6;
	final int colindex_actorpic = 7;
	final int colindex_actorpicsmall = 8;
	final int colindex_actorpicbig = 9;
	final int colindex_actorfirstname = 10;
	final int colindex_actorlastname = 11;
	
	// @see ApplicationDBHelper
	final int startStreamTableIndex = ApplicationDBHelper.START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC;

	Object mCreateViewLock = new Object();
	Object mCursorLock = new Object();
	Cursor mDataStore;
	
	ArrayList<String> loadAsyncImageUris = new ArrayList<String>(5);
	ArrayList<Integer> imageViewIndices = new ArrayList<Integer>(5);

	View.OnClickListener mMediaImageOnClickListener = new View.OnClickListener() {

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

	ImageUrlLoader2.AsyncListener mPrefetchImageLoaderListener = new ImageUrlLoader2.AsyncListener() {
		
		@Override
		public void onPublishProgress(AsyncLoaderProgress progress) {
			int a = firstVisibleItem + visibleItemCount;
			
			int lb0 = firstVisibleItem - mPreemptiveFetchSize;
			int ub0 = firstVisibleItem;
			
			int lb1 = a;
			int ub1 = a + mPreemptiveFetchSize; 
			
			if((lb1 < progress.groupCode && progress.groupCode < ub1) ||
				lb0 < progress.groupCode && progress.groupCode < ub0		
			){
				//addBitmapToCache(progress.groupCode, progress.imageUri, progress.bitmap);
			}
			
		}
		
		@Override
		public void onPreExecute() {
				
		}
		
		@Override
		public void onPostExecute(AsyncLoaderResult result) {
			
		}
		
		@Override
		public void onCancelled() {
	
			
		}
	};
	ThrottlingImageAsyncLoaderListener mImageAsyncLoaderListener = new ThrottlingImageAsyncLoaderListener(10l, 20, false){
	
		@Override
		public void onPublishProgress(AsyncLoaderProgress progress) { 
			//Logger.l(Logger.DEBUG, LOG_TAG, "onPublishProgress() message:"+progress.message);
			
			boolean isGroupStateValid = true;
			
			synchronized (mCreateViewLock) {
							
				isGroupStateValid = getAsyncLoadState(progress.groupCode);
				
				Logger.l(Logger.DEBUG, "crucial", "onPublishProgress() groupCode: "+progress.groupCode+" valid? "+isGroupStateValid);
				
				if(isGroupStateValid){
					if(progress.imageView != null){
						if(progress.success){		
							
		//					Logger.l(Logger.DEBUG, "bmp", "w:"+w+", h:"+h);
			
							if(!filterImageByDimension(progress.bitmap)){
								progress.imageView.setVisibility(View.GONE);
							}else{							
								progress.imageView.setImageBitmap(progress.bitmap);
								progress.imageView.setVisibility(View.VISIBLE);
								addBitmapToCache(progress.groupCode, progress.imageUri, progress.bitmap);
							}
						}else{
							if(progress.code == R.id.asyncimageloader_profileimage){						
								progress.imageView.setImageBitmap(mProfileLoadingBitmap);							
							}
							else{
								progress.imageView.setImageBitmap(mAttachmentDefaultBitmap);
							}
							progress.imageView.setVisibility(View.VISIBLE);
						}						
					}
					//Logger.l(Logger.DEBUG, LOG_TAG, "[onPublishProgress()] progress groupCode:"+progress.groupCode+", success?:"+progress.success);
				}else{
					//Logger.l(Logger.DEBUG, LOG_TAG, "[onPublishProgress()] progress groupCode:"+progress.groupCode+"   INVALID INVALID INvALID");
				}
				
			}
	
		}	
		
	};
	StringBuffer mStringBuffer = new StringBuffer(10);
	
	public View.OnTouchListener mItemTouchListener;

	
	private void clearGroupFromCache(long id){
		mImagesCache.remove(Long.valueOf(id));
	}
	
	private Bitmap getBitmapFromCache(long id, String uri){
		Logger.l(Logger.DEBUG,"prefetch", "[getBitmapFromCache()] id:"+id+", uri:"+uri);
		ArrayList<GroupImageKey> images = mImagesCache.get(Long.valueOf(id));
		if(images == null){
			return null;
		}
		try{
		GroupImageKey key = GroupImageKey.getInstance();
		key.uri = uri;
		int indexFound = java.util.Collections.binarySearch(images,key,GroupImageKey.COMPARATOR);
		if(indexFound >= 0){
			Logger.l(Logger.DEBUG, "prefetch", "found.");	
			return images.get(indexFound).bitmap;
		}else{
			return null;
		}
		}catch(Exception e){
			return null;
		}
	}
	
	private void addBitmapToCache(final long id, final String uri,final Bitmap bmp){
		Logger.l(Logger.DEBUG,"prefetch",  "[addBitmapToCache()]  id:"+id+", uri:"+uri);
		ArrayList<GroupImageKey> images = mImagesCache.get(id);
		if(images == null){
			images = new ArrayList<GroupImageKey>(1);
			mImagesCache.put(Long.valueOf(id), images);
		}
		GroupImageKey gim = GroupImageKey.getInstance();
		gim.uri = uri;
		try{
		int indexFound = 
			java.util.Collections.binarySearch(images,gim,GroupImageKey.COMPARATOR);
		if(indexFound < 0){
			gim.bitmap = bmp;
			Logger.l(Logger.DEBUG, "prefetch", "[addBitmapToCache()] added. id:"+id+", uri:"+uri);
			images.add(gim);
			
		}}
		catch(Exception e){
			return;
		}
	}
	
	private void doPreemptiveFetch(int side){
		try{
		if(mAsyncLoadingState == null){
			return;
		}
		
		java.util.Collections.sort(mAsyncLoadingState);
		
		
		int size = mAsyncLoadingState.size();
		long lb=0,ub=0;
		
		if(size > 0){
			lb = mAsyncLoadingState.get(0);
			ub = mAsyncLoadingState.get(mAsyncLoadingState.size()-1);				
		}
		
		if(mDataStore != null && !mDataStore.isClosed()){
		
			
			for(int i=0;i<this.mPreemptiveFetchSize;i++){
			synchronized(mCursorLock){
				int pos = (int)(ub+i);
				try{
					mDataStore.moveToPosition(pos);
					String attachmentJsonData = mDataStore.getString(Stream.col_attachment	+ startStreamTableIndex);
				}catch(Exception e){
					return;
				}
				
				// TODO Object pool: ProcessedAttachmentData
				Attachment att = processAttachment( attachmentJsonData);
				ProcessedData pData = ProcessedData.process(att);
				prefetchImages(pData, pos, mPrefetchImageLoaderListener);
			}
			
		}
		
		}
		}catch(Throwable e){
			return;
		}
	}	
	
		
	private boolean filterImageByDimension(Bitmap bmp){		
		int w = bmp.getWidth();
		int h = bmp.getHeight();
		if(w < MIN_BITMAP_WIDTH || h < MIN_BITMAP_HEIGHT){
			return false;
		}
		return true;
	}
	public void setDataSetObserver(DataSetObserver observer){
		mImageAsyncLoaderListener.setObserver(observer);
	}
	
	@Override
	public SoftHashMap<String, ProcessedData> getMemoryCache() {
		//return mProcessedDataCache;
		return null;
	}	
	
	
	//SoftHashMap<String, View> mProgressBar_Registry = new SoftHashMap<String, View>(100, 1.5f);

	

	public StreamListViewFactory(Activity app) {
		super(app);
		this.ctx = app;
		Resources res = ctx.getResources();
		WindowManager wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
		
		mAttachmentDefaultBitmap = BitmapFactory.decodeResource(res, R.drawable.empty);
		mAttachmentLoadingBitmap = BitmapFactory.decodeResource(res, R.drawable.mobook2_exclaimation_64);
		mProfileLoadingBitmap = BitmapFactory.decodeResource(res, R.drawable.defaultprofileimage);
		mProfileDefaultBitmap = BitmapFactory.decodeResource(res, R.drawable.defaultprofileimage);
		mStringBuilder = new StringBuilder();
		//mEmptyDrawable = res.getDrawable(R.drawable.empty);
	}
	
	
	public class GroupData {
		public String post_id;
		public long actor_id;
		public long target_id;
		public int app_id;
		public ProcessedData processedData;
		public String message;
		public boolean comments_can_post;
		public boolean comments_can_remove;
		public long comments_count;
		public boolean likes_user_likes;
		public boolean likes_canlike;
		public String actorName;
		public String targetName;
		public String attachmentJson;		
		
		public byte mDataState = DataState.VALID;		
	}
	
	public class GroupViewHolder {
		public TextView name;
		public TextView targetName;
		public TextView message;
		public TextView comments;
		public TextView likes;

		// public ImageView attachmentPreviewImage;
		public TextView attachmentDescription;
		public TextView attachmentName;
		public TextView attachmentCaption;
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

		public ImageView icon;
		
		public TextView since;
		public ImageView mProgressImage;
		public int position; // cancel asynctask if the recycled row is used as
		
		public GroupData groupData;
	
	}

	public class AsyncProcessDescriptor {
		
		public String post_id;
		public int mViewPosition;
		public GroupViewHolder mGroupViewHolder;
		public ProcessedData mProcessedAttachmentData;
		
		public void clear(){
			mGroupViewHolder = null;			
			post_id = null;
		}		
	}

	public void destroy() {
		mProfileLoadingBitmap.recycle();
		mProfileDefaultBitmap.recycle();
		mAttachmentDefaultBitmap.recycle();
		mAttachmentLoadingBitmap.recycle();	
		
		mProfileLoadingBitmap = null;
		mProfileDefaultBitmap = null;
		mAttachmentDefaultBitmap = null;
		mAttachmentLoadingBitmap = null;
	}

	/*
	synchronized private void showMediaImages(int numMediaImages, Drawable mMediaPhotosDrawables[], ImageView[] views, String[] hrefs) {
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
*/
	
	synchronized private void showHideViews(	
			GroupData groupData,
			ProcessedData processedAttachmentData,			
			GroupViewHolder groupViewHolder
			) {
				
		View attachmentContainer = groupViewHolder.attachmentContainer;
		View mediaImagesContainer = groupViewHolder.mediaImagesContainer; 
		View message = groupViewHolder.message;
		
		
		if(groupData.message == null || groupData.message.trim().length() == 0){
			message.setVisibility(View.GONE);
		}else{
			String _messageText;
			try{
				_messageText = URLDecoder.decode(groupData.message);
			}catch(Exception e){
				_messageText = groupData.message;
			}
			groupViewHolder.message.setText(_messageText);
			message.setVisibility(View.VISIBLE);
		}
		
		int mediaType = -1;
		String attachmentName = null;
		boolean hide = false;
				
		try{
			mediaType = processedAttachmentData.mAttachment.mMediaType;
			attachmentName = processedAttachmentData.mAttachment.mName;
		}catch(NullPointerException e){
			hide = true;
		}
				
		//Logger.l(Logger.ERROR, "debug", "[showHideAttachmentFields()] mediaType: "+mediaType);
		
		if(attachmentName == null || attachmentName.length() == 0){
			groupViewHolder.attachmentName.setVisibility(View.GONE);
		}else{
			groupViewHolder.attachmentName.setVisibility(View.VISIBLE);
		}
		
		if(mediaType == AttachmentMedia.PHOTO ||
			mediaType == AttachmentMedia.VIDEO ||
			mediaType == AttachmentMedia.LINK || 
			mediaType == AttachmentMedia.MUSIC ||
			attachmentName != null && !attachmentName.equals("null")
			){			
			attachmentContainer.setVisibility(View.VISIBLE);
			mediaImagesContainer.setVisibility(View.VISIBLE);
		}else{
			hide = true;						
		}	
		
		if(hide){
			attachmentContainer.setVisibility(View.GONE);
			mediaImagesContainer.setVisibility(View.GONE);
		}
	}
	
	
	
	public View createGroupView(
			Cursor ds, 
			int position,
			View convertView, 
			final ViewGroup parent) {		
		super.createGroupView(ds, position, convertView, parent);
		
		Logger.l(Logger.DEBUG, "crucial", "createGroupView() position: "+position);
		
		this.mDataStore = ds;
				
		GroupViewHolder groupViewHolder;
		GroupData groupData;
		
		if (convertView != null) {
			groupViewHolder = (GroupViewHolder) convertView.getTag(R.id.tag_streamsadapter_item);
			groupData = (GroupData) convertView.getTag(R.id.tag_streamsadapter_item_data);
			
			if (position != groupViewHolder.position) {
				//cancel asyncTask of the row if the row is a previous row.
				//groupViewHolder.mLoadImagesAsync.cancel(true);
			}
			
		} else {
			convertView = mLayoutInflater.inflate(R.layout.t_streampost,parent, false);
	
			groupData = new GroupData();
			
			groupViewHolder = new GroupViewHolder();
			groupViewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
			groupViewHolder.name = (TextView) convertView.findViewById(R.id.name);
			groupViewHolder.message = (TextView) convertView.findViewById(R.id.message);
			groupViewHolder.comments = (TextView) convertView.findViewById(R.id.comments);
			groupViewHolder.likes = (TextView) convertView.findViewById(R.id.likes);
			groupViewHolder.since = (TextView) convertView.findViewById(R.id.since);
			groupViewHolder.profileImage = (ImageView) convertView.findViewById(R.id.profile_pic);
			groupViewHolder.targetName = (TextView) convertView.findViewById(R.id.targetname);
			groupViewHolder.attachmentDescription = (TextView) convertView.findViewById(R.id.attachmentdescription);
			groupViewHolder.attachmentName = (TextView) convertView.findViewById(R.id.attachmentname);
			groupViewHolder.attachmentCaption = (TextView) convertView.findViewById(R.id.attachmentcaption);
			groupViewHolder.mediaimages[0] = (ImageView) convertView.findViewById(R.id.mediaimage0);
			groupViewHolder.mediaimages[1] = (ImageView) convertView.findViewById(R.id.mediaimage1);
			groupViewHolder.mediaimages[2] = (ImageView) convertView.findViewById(R.id.mediaimage2);
			groupViewHolder.mediaimages[3] = (ImageView) convertView.findViewById(R.id.mediaimage3);
			groupViewHolder.mediaimages[4] = (ImageView) convertView.findViewById(R.id.mediaimage4);
			groupViewHolder.attachmentContainer = convertView.findViewById(R.id.attachmentcontainer);
			groupViewHolder.mediaImagesContainer = convertView.findViewById(R.id.mediaimagescontainer);
			groupViewHolder.mProgressImage = (ImageView) convertView.findViewById(R.id.progress);
	
			for (int i = 0; i < 3; i++) {
				groupViewHolder.mediaimages[i].setOnClickListener(mMediaImageOnClickListener);
			}	
			
		}
	
		// hide the attachment and media images containers
		//groupViewHolder.attachmentContainer.setVisibility(View.INVISIBLE);
		//groupViewHolder.mediaImagesContainer.setVisibility(View.INVISIBLE);
				
		if(mItemTouchListener != null){
			convertView.setOnTouchListener(mItemTouchListener);
		}
		
		if (groupViewHolder.mProgressImage != null) {
			groupViewHolder.mProgressImage.setVisibility(View.INVISIBLE);
		}
		
		groupViewHolder.message.setText("");
		groupViewHolder.attachmentName.setText("");
		groupViewHolder.attachmentCaption.setText("");
		groupViewHolder.attachmentDescription.setText("");
		groupViewHolder.icon.setImageBitmap(App.INSTANCE.mEmptyBitmap);
		groupViewHolder.profileImage.setImageBitmap( mProfileLoadingBitmap);
		
		
		synchronized(mCursorLock){
		try {
			// check if the cursor is still open
			// cursor can be closed when the corresponding sqlitedb is closed
			// caused by orientation screen changes.
			//
			ds.moveToPosition(position);
		} catch (Exception e) {			
			return mLayoutInflater.inflate(R.layout.empty, parent, false);
		}
		
		if(ds.isClosed()){
			return null;
		}
	
		// start read from cursor
		try{
			post_id = ds.getString(Stream.col_post_id + startStreamTableIndex);
			updated_time = ds.getLong(Stream.col_updated_time+ startStreamTableIndex);
			actorPicSquare = ds.getString(colindex_actorpicsquare);
			actorPic = ds.getString(colindex_actorpic);
			actorPicBig = ds.getString(colindex_actorpicbig);
			actorPicSmall = ds.getString(colindex_actorpicsmall);
			targetId = ds.getLong(colindex_targetid);
			targetName = ds.getString(colindex_targetname);
			targetFirstname = ds.getString(colindex_targetfirstname);
			targetLastname = ds.getString(colindex_targetlastname);		
			actorId = ds.getLong(colindex_actorid);
			actorName = ds.getString(colindex_actorname);
			actorFirstname = ds.getString(colindex_actorfirstname);
			actorLastname = ds.getString(colindex_actorfirstname);		
			likes_canlike = ds.getLong(Stream.col_likes_canlike + startStreamTableIndex) == 0?false:true;
			likes_userlikes = ds.getLong(Stream.col_likes_userlikes + startStreamTableIndex) == 0?false:true;	
			comments_can_post = ds.getLong(Stream.col_comments_can_post + startStreamTableIndex) == 0?false:true;
			comments_can_remove = ds.getLong(Stream.col_comments_can_remove + startStreamTableIndex) == 0?false:true;
			comments_count = ds.getLong(Stream.col_comments_count + startStreamTableIndex);
			message = ds.getString(Stream.col_message + startStreamTableIndex);			
			numLikes = ds.getInt(Stream.col_likes_count + startStreamTableIndex);			
			attachmentJsonData = ds.getString(Stream.col_attachment	+ startStreamTableIndex);
			app_id = ds.getInt(Stream.col_app_id+startStreamTableIndex);
		}
		catch(IllegalStateException e){
			return convertView;
		}catch(CursorIndexOutOfBoundsException e){
			return convertView;
		}catch(StaleDataException e){
			return convertView;
		}
		}
		
		//end read from cursor
	
		//TODO Object pool Attachment
		Attachment att = processAttachment( attachmentJsonData);
		// TODO Object pool: ProcessedAttachmentData
		ProcessedData processedData = null;
		processedData = ProcessedData.process(att);		
		
		if (actorPicSquare != null && actorPicSquare.length() > 0) {
			processedData.mProfileImageUri = actorPicSquare;
		} else if (actorPicSmall != null && actorPicSmall.length() > 0) {
			processedData.mProfileImageUri  = actorPicSmall;
		} else if (actorPic != null && actorPic.length() > 0) {
			processedData.mProfileImageUri = actorPic;
		} else if (actorPicBig != null && actorPicBig.length() > 0) {
			processedData.mProfileImageUri = actorPicBig;
		}
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
		
		long nowMilliseconds = (new Date()).getTime();
		long elapsedSecs = nowMilliseconds / 1000 - updated_time;
		String since = Util.createElapsedString(elapsedSecs, nowMilliseconds, updated_time);
		
		mStringBuffer.append(numLikes).append(" ").append((numLikes > 1 ? "likes" : "like"));
		String likesText = mStringBuffer.toString();
		Util.clearStringBuffer(mStringBuffer);
				
		// retrieve processed attachment data from cache.
		
		loadAttachment(position, groupViewHolder, processedData);		
			
		AsyncProcessDescriptor asyncProcessDescriptor = new AsyncProcessDescriptor();
		asyncProcessDescriptor.mProcessedAttachmentData = processedData;
		asyncProcessDescriptor.mGroupViewHolder = groupViewHolder;
		
		// bind values to UI views
						
		groupViewHolder.since.setText(since);
		groupViewHolder.likes.setText(likesText);
		mStringBuffer.append(actorName).append(targetName!=null?" > ":"").append(targetName!=null?targetName:"");
		groupViewHolder.name.setText(mStringBuffer.toString());	
		Util.clearStringBuffer(mStringBuffer);
		mStringBuffer.append(comments_count).append(" comments");
		groupViewHolder.comments.setText(mStringBuffer.toString());
		Util.clearStringBuffer(mStringBuffer);
			
		// when no image for profile is available, use default profile image
		
		if (processedData.mProfileImageUri == null || processedData.mProfileImageUri.length() == 0) {				
			groupViewHolder.profileImage.setImageBitmap(mProfileDefaultBitmap);
		}
		else{						
			Bitmap profileImageBitmap = App.mImageUrlLoader2.loadImage(processedData.mProfileImageUri, true);
			if(profileImageBitmap != null){
				groupViewHolder.profileImage.setImageBitmap(profileImageBitmap);
			}
		}		
		
		
		groupData.post_id = post_id;
		groupData.actor_id = actorId;
		groupData.target_id = targetId;
		groupData.message = message;		
		groupData.comments_can_post = comments_can_post;
		groupData.comments_can_remove = comments_can_remove;
		groupData.comments_count = comments_count;
		groupData.likes_user_likes = likes_userlikes;
		groupData.likes_canlike = likes_canlike;
		groupData.actorName = actorName;
		groupData.targetName = targetName;
		//groupData.profilePictureUrl = processedData.mProfileImageUri;
		groupData.processedData = processedData;
		groupData.attachmentJson = attachmentJsonData;
		groupData.app_id = app_id;
						
		convertView.setTag(R.id.tag_streamsadapter_item_data, groupData);
		convertView.setTag(R.id.tag_streamsadapter_item, groupViewHolder);
			
		groupViewHolder.position = position;
		groupViewHolder.groupData = groupData;
				
		showHideViews(
				groupData,
				processedData,
				groupViewHolder				
		);				
				
		loadImages(processedData, groupViewHolder, position, mImageAsyncLoaderListener);
		
		return convertView;
	}

	private void prefetchImages(
			final ProcessedData processedData,
			long position,
			ImageUrlLoader2.AsyncListener asyncListener
			
	){
	
		if(processedData == null){
			return;
		}
		
		if(processedData.mAttachment == null){
			return;
		}
		
		// load profile image
		
		
		Bitmap profileImageBitmap;
		
		
		AsyncLoaderInput input = AsyncLoaderInput.getInstance();
		input.imageUri = processedData.mProfileImageUri;
		input.code = position;
		input.groupCode = position;
		App.mImageUrlLoader2.loadImageAsync(
				App.INSTANCE.mExecScopeImageLoaderTask,
				input
				, asyncListener);
		
		// load icon image
		String iconUri = processedData.mAttachment.mIcon;
		
		AsyncLoaderInput input2 = AsyncLoaderInput.getInstance();
		input.imageUri = iconUri;			
		App.mImageUrlLoader2.loadImageAsync(
				App.INSTANCE.mExecScopeImageLoaderTask,
				input2
				, asyncListener);
		
		
		// load attachment images
				
		int numShownImages = processedData.numMediaImages;
		
		ArrayList<String> loadAsyncImageUris = new ArrayList<String>();
		ArrayList<Long> imageViewIndices = new ArrayList<Long>();
		
		for (int i = 0; i < Math.min(MAX_ATTACHMENTS,numShownImages); i++) {
			String imageUri = processedData.mediaImagesSrcs[i];
			loadAsyncImageUris.add(imageUri);			
		}			
		
		// fetch images asynchronously from the caches..
		AsyncLoaderInput[] inputs = new AsyncLoaderInput[loadAsyncImageUris.size()];			
		Iterator<String> loadAsyncImageUrisIterator = loadAsyncImageUris.iterator();
		
		for(int j=0;loadAsyncImageUrisIterator.hasNext();j++){
			inputs[j] = AsyncLoaderInput.getInstance();
			inputs[j].groupCode = position;
			inputs[j].code = position;
			inputs[j].imageUri = loadAsyncImageUrisIterator.next();						
		}			
		
		App.mImageUrlLoader2.loadImageAsync(App.INSTANCE.mExecScopeImageLoaderTask, inputs, asyncListener);			
		
		Logger.l(Logger.DEBUG, LOG_TAG, "loading attachment images DONE. count: "+loadAsyncImageUris.size());		
	}

	
	private void loadImages(
			final ProcessedData processedData,
			final GroupViewHolder groupViewHolder,
			long position,
			ImageUrlLoader2.AsyncListener asyncListener			
	){
		if(groupViewHolder == null){
			return;
		}
		
		if(processedData == null){
			return;
		}
		
		if(processedData.mAttachment == null){
			return;
		}
		
		// load profile image
		
		Bitmap profileImageBitmap = getBitmapFromCache(position, processedData.mProfileImageUri);
		boolean isLoadProfileImageAsync = false;
		if(profileImageBitmap == null){
			profileImageBitmap = App.mImageUrlLoader2.loadImage(processedData.mProfileImageUri, true);
			if (profileImageBitmap == null) {
				profileImageBitmap = App.INSTANCE.mDefaultProfileBitmap;
				isLoadProfileImageAsync = true;				
			}
		}
		if(isLoadProfileImageAsync){
			AsyncLoaderInput input = AsyncLoaderInput.getInstance();
			input.imageUri = processedData.mProfileImageUri;
			input.imageView = groupViewHolder.profileImage;
			input.code = position;
			input.groupCode = position;
			App.mImageUrlLoader2.loadImageAsync(
				App.INSTANCE.mExecScopeImageLoaderTask,
				input
				, asyncListener);
	
		}else{
			groupViewHolder.profileImage.setImageBitmap(profileImageBitmap);
		}
		
		// load icon image
		String iconUri = processedData.mAttachment.mIcon;
		Bitmap iconBmp;
		boolean isLoadIconAsync = false;
		iconBmp = getBitmapFromCache(position, iconUri);
		if(iconBmp == null){
			iconBmp = App.mImageUrlLoader2.loadImage(iconUri, true);
			if (iconBmp == null) {
				isLoadIconAsync = true;
				iconBmp = App.INSTANCE.mEmptyBitmap;
			}
		}
		if(isLoadIconAsync){
			groupViewHolder.icon.setImageBitmap(iconBmp);
			AsyncLoaderInput input = AsyncLoaderInput.getInstance();
			input.imageUri = iconUri;
			input.imageView = groupViewHolder.icon;
			input.code = position;
			input.groupCode = position;
			App.mImageUrlLoader2.loadImageAsync(
				App.INSTANCE.mExecScopeImageLoaderTask,
				input
				, asyncListener);
		}else{
			groupViewHolder.icon.setImageBitmap(iconBmp);
		}		
		
		// load attachment images
				
		int numShownImages = processedData.numMediaImages;
		Logger.l(Logger.DEBUG,LOG_TAG,"[loadAttachmentImages()] numShownImages: "+numShownImages);
		loadAsyncImageUris.clear();
		imageViewIndices.clear();
				
		Logger.l(Logger.DEBUG, LOG_TAG,"[loadAttachmentImages()] position:"+position+". Loading attachment image. numImages:"+numShownImages);
		
		int numOfInvalidImageUris = 0;
		
		//TODO Attachment # images
		for (int i = 0; i < Math.min(MAX_ATTACHMENTS,numShownImages); i++) {
					
			//Logger.l(Logger.DEBUG, LOG_TAG, "loading attachment image "+i);
			groupViewHolder.mediaimages[i].setVisibility(View.VISIBLE);
			
			String imageUri = processedData.mediaImagesSrcs[i];
			
			Logger.l(Logger.DEBUG, LOG_TAG, "[loadAttachmentImages()] Loading attachment image. i:"+i+", uri:"+imageUri);
			
			//set tag
			MediaImageTag mediaTag = (MediaImageTag) groupViewHolder.mediaimages[i].getTag();
			if (mediaTag == null) {
				mediaTag = new MediaImageTag();
			}
			mediaTag.imageSrc = imageUri;
			groupViewHolder.mediaimages[i].setTag(mediaTag);
			//end of set tag
	
			//Logger.l(Logger.DEBUG, "crucial" ,"[loadAttachmentImages()] position:"+position+", loading image#:"+i+", imageUri:"+imageUri);
			Bitmap bmp;			
			bmp = getBitmapFromCache(position, imageUri);
			if(bmp == null){
				bmp = App.mImageUrlLoader2.loadImage(imageUri, true);			
				if (bmp == null) {
					// we don't have the bitmap in the cache, so add to a list of uris to be fetched asynchronously.
					loadAsyncImageUris.add(imageUri);
					imageViewIndices.add(i);
					bmp = mAttachmentLoadingBitmap;
				}else{
					if(!filterImageByDimension(bmp)){
						groupViewHolder.mediaimages[i].setVisibility(View.GONE);
					}else{
						groupViewHolder.mediaimages[i].setImageBitmap(bmp);		
					}
				}
			}else{
				groupViewHolder.mediaimages[i].setImageBitmap(bmp);
			}
			
		}			
		
		assert(imageViewIndices.size() == loadAsyncImageUris.size());
		
		Iterator<Integer> unloadedImageViewIndicesIterator = imageViewIndices.iterator();
	
		Logger.l(Logger.DEBUG, LOG_TAG,"[loadAttachmentImages()] total async load:"+loadAsyncImageUris.size());
		Logger.l(Logger.DEBUG, LOG_TAG,"[loadAttachmentImages()] total image indices:"+imageViewIndices.size());
		
		Iterator<Integer> imageViewIndicesIterator = imageViewIndices.iterator();
		
		// set loading image for image views
		
		while(unloadedImageViewIndicesIterator.hasNext()){
			int j = unloadedImageViewIndicesIterator.next();
			groupViewHolder.mediaimages[j].setImageBitmap(mAttachmentLoadingBitmap);
		}
			
		// hide un-needed imageViews
				
		
		for (int i = numShownImages - numOfInvalidImageUris; i < GroupViewHolder.TOTAL_MEDIAIMAGES;	i ++) {			
			groupViewHolder.mediaimages[i].setVisibility(View.GONE);
		}
		
		
		// fetch images asynchronously from the web
		AsyncLoaderInput[] inputs = new AsyncLoaderInput[loadAsyncImageUris.size()];			
		Iterator<String> loadAsyncImageUrisIterator = loadAsyncImageUris.iterator();
				
		
			for(int j=0;imageViewIndicesIterator.hasNext();j++){
				int imageViewIndex = imageViewIndicesIterator.next();
				inputs[j] = AsyncLoaderInput.getInstance();
				inputs[j].groupCode = position;
				inputs[j].code = position;
				inputs[j].imageUri = loadAsyncImageUrisIterator.next();	
				inputs[j].imageView = groupViewHolder.mediaimages[imageViewIndex];				
			}			
			App.mImageUrlLoader2.loadImageAsync(App.INSTANCE.mExecScopeImageLoaderTask, inputs, asyncListener);
			
		
		Logger.l(Logger.DEBUG, LOG_TAG, "loading attachment images DONE. count: "+loadAsyncImageUris.size());
		
	}

	private void loadAttachment(
			long position,
			GroupViewHolder groupViewHolder, 
			ProcessedData processedAttachmentData){
		
		if(processedAttachmentData.mAttachment != null){
			App.mImageUrlLoader2.fetch(
				App.INSTANCE.mExecScopeImageLoaderTask,
				0,
				position,
				processedAttachmentData.mAttachment.mIcon,
				groupViewHolder.icon, 
				App.INSTANCE.mEmptyBitmap, 
				mImageAsyncLoaderListener);				
		}
		
		String attachmentCaption = Util.getNotNullString(processedAttachmentData.mAttachment.mCaption);		
		String attachmentName = Util.getNotNullString(processedAttachmentData.mAttachment.mName);
		String displayAttName = "";
		if(attachmentCaption == null){
			attachmentName =  attachmentName==null?"":attachmentName + "\n" + processedAttachmentData.mAttachment.mHref;
		}		 
		displayAttName = attachmentName;
		
		groupViewHolder.attachmentName.setText(displayAttName);
		groupViewHolder.attachmentCaption.setText(attachmentCaption);
		groupViewHolder.attachmentDescription.setText(processedAttachmentData.mAttachment.mDescription);
		
			
		//Logger.l(Logger.DEBUG, LOG_TAG, "loading attachment image? "+numShownImages);		
	}
	
	private Attachment processAttachment(String attachmentJsonData) {
		if (attachmentJsonData == null) {
			return null;
		}
		
		//TODO [Object Pools] acquire attachment 
		Attachment att = new Attachment();
		att.parseJson(attachmentJsonData);
		
		return att;
	}
	
	
	public class ChildHolder{
       	public int groupPosition;
       	public TextView name;
       	public TextView text;
    }

	@Override
	public View createChildView(Cursor childDatastore, int groupPosition, int childPosition, View convertView,
			ViewGroup parent) {
		       
        ChildHolder tag;
        if(convertView == null){
        	convertView = mLayoutInflater.inflate(R.layout.t_streamcomment, parent, false);
        	tag = new ChildHolder();
        	tag.name = (TextView) convertView.findViewById(R.id.name);
        	tag.text = (TextView) convertView.findViewById(R.id.text);
        }else{
        	tag = (ChildHolder) convertView.getTag();
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

	
	class PrefetchAsync extends UserTask<Void, Void, Void>{

		public PrefetchAsync(UserTaskExecutionScope scope) {
			super(scope);
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			Logger.l(Logger.DEBUG, "prefetch", "doInBackground()");
			doPreemptiveFetch(1);
			doPreemptiveFetch(-1);
			return null;
		}
		
	}
	
	volatile private int visibleItemCount ;
	volatile private int totalItemCount ;
	volatile private int dVisible;
	volatile private int firstVisibleItem;
	volatile private int mScrollDirection;
	
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		
		//new PrefetchAsync(App.INSTANCE.mExecScopePrefetcherTask).execute((Void[])null);
		
		//determine the direction of scroll
		
		/*
		if(this.visibleItemCount != visibleItemCount || this.totalItemCount != totalItemCount){
			this.dVisible = this.firstVisibleItem - firstVisibleItem;
			this.visibleItemCount = visibleItemCount;
			this.totalItemCount = totalItemCount;
			this.mScrollDirection = this.dVisible<0?-1:1;
		}else{
			return;
		}
		*/
		
		
		
		/*
		synchronized(mCreateViewLock){
			
			if(this.visibleItemCount != visibleItemCount || this.totalItemCount != totalItemCount){
				this.visibleItemCount = visibleItemCount;
				this.totalItemCount = totalItemCount;
			}else{
				return;
			}
		
			Logger.l(Logger.DEBUG, "crucial", "[onScroll()] fvi:"+firstVisibleItem+", ic:"+visibleItemCount);
		
			clearAsyncLoadStates();
		
			for(int i=0;i<visibleItemCount;i++){
				addAsyncLoadState((long)(i+firstVisibleItem));
			}
		*/		
			/*
			Iterator<Long> loadStates = mAsyncLoadingState.iterator();
			for(int i=0;loadStates.hasNext();i++){
				long position = loadStates.next();
				Logger.l(Logger.DEBUG, LOG_TAG, "[onScroll()] states position #"+i+": "+position);			
			}
			*/
			
		//}
		
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// Logger.l(Logger.DEBUG, LOG_TAG, "[onScrollStateChanged()] state:"+scrollState);
		switch(scrollState){
			case SCROLL_STATE_IDLE:{
				mScrollDirection = 0;
				break;
			}
		}
	}

	
	@Override
	public void markGroupAsDirty(String[] groupIds) {
		// TODO Auto-generated method stub
			
	}

}
