package com.neusou.moobook.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;
import android.widget.Toast;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;
import com.neusou.moobook.data.MediaImageTag;
import com.neusou.web.ImageUrlLoader2;
import com.neusou.web.ImageUrlLoader2.AsyncListener;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderProgress;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderResult;

public class DisplayImageActivity extends BaseActivity{
	
	public static final String LOG_TAG = Logger.registerLog(DisplayImageActivity.class);
	
	public static final String XTRA_MEDIAIMAGETAGS = "xtra.mediaimagetags";
		
	DisplayImageActivityInvocationData mInvocationData;
	ImageView mImageView;
	int mCurrentIndex;
	int mNumMedia;
			

	AsyncListener mImgAsyncListener = new AsyncListener() {
		
		@Override
		public void onPublishProgress(final AsyncLoaderProgress progress) {
			runOnUiThread(new Runnable() {				
				@Override
				public void run() {
					progress.imageView.setImageBitmap(progress.bitmap);					
				}
			});			
		};
		
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
	
	public static Intent getIntent(Context ctx) {
		return new Intent(ctx, DisplayImageActivity.class);
	}
	
	public static class DisplayImageActivityInvocationData implements Parcelable{			
		
		int mNumMediaImageTags;
		MediaImageTag[] mMediaImageTags;
		public int mSelectedIndex; //the index selected in the media tag array.
		
		public void setMediaImageTags(MediaImageTag[] data){
			mMediaImageTags = data;
			mNumMediaImageTags = data.length;
		}		
		
		public static final String XTRA_PARCELABLE_OBJECT = DisplayImageActivityInvocationData.class.getCanonicalName();
			
		public static Creator CREATOR = new DisplayImageActivityInvocationData.Creator();
		
		static class Creator implements Parcelable.Creator<DisplayImageActivityInvocationData>{

			@Override
			public DisplayImageActivityInvocationData createFromParcel(Parcel source) {
				int numMediaImageTags = source.readInt();
				DisplayImageActivityInvocationData d = new DisplayImageActivityInvocationData();
				d.mNumMediaImageTags = numMediaImageTags;
				d.mMediaImageTags = new MediaImageTag[numMediaImageTags];
				for(int i=0;i<numMediaImageTags;i++){
					d.mMediaImageTags[i] = MediaImageTag.CREATOR.createFromParcel(source);
				}			
				d.mSelectedIndex = source.readInt();
				return d;
			}

			@Override
			public DisplayImageActivityInvocationData[] newArray(int size) {
				return null;
			}
			
		}
		
		@Override
		public int describeContents() {
			return 1212;
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			if(mNumMediaImageTags == 0){
				return;
			}
			dest.writeInt(mNumMediaImageTags);
			for(int i=0;i<mNumMediaImageTags;i++){		
				mMediaImageTags[i].writeToParcel(dest, flags);
			}	
			dest.writeInt(mSelectedIndex);
		}
		
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {	
		super.onCreate(savedInstanceState);
		getWindow().setBackgroundDrawable(null);
		setContentView(R.layout.viewimage_activity);
		bindViews();
		getIntentExtras();
		initObjects();		
		initViews();		
	}
	
	@Override
	protected void onResume() {	
		super.onResume();
		mNumMedia = mInvocationData.mMediaImageTags.length;
		Toast.makeText(this, "numMedias:"+mNumMedia, 1000).show();
		loadImage(mInvocationData.mSelectedIndex);
	}
	
	@Override
	protected void bindViews() {	
		super.bindViews();		
		mImageView = (ImageView) findViewById(R.id.image);
	}

	@Override
	protected void initViews() {	
		super.initViews();
	}
	
	@Override
	protected void initObjects() {
		super.initObjects();
		Facebook.getInstance().registerOutHandler(R.id.outhandler_activity_displayimage, mUiHandler);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {	
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {	
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	public void getIntentExtras() {
		Intent i = getIntent();
		mInvocationData = i.getParcelableExtra(DisplayImageActivityInvocationData.XTRA_PARCELABLE_OBJECT);
	//	Log.d("getIntentExtras. ", " number of media tags: "+mInvocationData.mNumMediaImageTags);
	//	Log.d("getIntentExtras. ", " length of media tags: "+mInvocationData.mMediaImageTags.length);
	}
	
	

	public void loadImage(int index){
		if(index < mNumMedia && index >= 0) {
		ImageUrlLoader2.AsyncLoaderInput input = new ImageUrlLoader2.AsyncLoaderInput();
		
		//Log.d("DisplayImage","num medias? "+mInvocationData.mNumMediaImageTags);
		//Log.d("DisplayImage","medias null? "+(mInvocationData.mMediaImageTags == null));
		
		input.imageUri = mInvocationData.mMediaImageTags[index].imageSrc;
		input.imageView = mImageView;
		App.INSTANCE.mImageUrlLoader2.loadImageAsync(
				App.INSTANCE.mExecScopeImageLoaderTask, 
				input, mImgAsyncListener
				);
		}
	}
	
	public void getFullImages(){
		//mInvocationData.mMediaImageTags[0].
		
	//	Facebook.getInstance().getPhoto(R.id.outhandler_activity_displayimage, extraData, photoIds, limit, offset, cbSuccessOp, cbErrorOp, cbTimeoutOp, timeoutMillisecs);
	}
	
	Handler mUiHandler = new Handler(){
		
	};
}