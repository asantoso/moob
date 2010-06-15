package com.neusou.moobook.controller;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderProgress;

public class StandardImageAsyncLoadListener extends
		ThrottlingImageAsyncLoaderListener {

	Activity mActivity;
	volatile Object mCreateViewLock;
	IStatefulListView mListView;
	Bitmap mProfileLoadingBitmap;
	Bitmap mAttachmentDefaultBitmap;

	public StandardImageAsyncLoadListener(Activity ctx, Object createViewLock,
			IStatefulListView listView, long extraDelayMillis,
			int windowSizeMillis, boolean isNotifyObserver) {
		super(extraDelayMillis, windowSizeMillis, isNotifyObserver);
		mActivity = ctx;
		mCreateViewLock = createViewLock;
		mListView = listView;
	}

	@Override
	public void onPublishProgress(final AsyncLoaderProgress progress) {		
		mActivity.runOnUiThread(new Runnable() {

			@Override
			public void run() {

				// Logger.l(Logger.DEBUG, LOG_TAG,
				// "onPublishProgress() message:"+progress.message);

				boolean isGroupStateValid = true;
				
				synchronized (mCreateViewLock) {
					isGroupStateValid = mListView.getAsyncLoadState(progress.groupCode);
					Logger.l(Logger.DEBUG, "staterow", progress.groupCode+":"+isGroupStateValid);
					
					if (isGroupStateValid) {
						if (progress.imageView != null) {
							if (progress.success) {
								if (!Util.filterImageByDimension(
										progress.bitmap,
										App.MINIMUM_IMAGE_WIDTH,
										App.MINIMUM_IMAGE_HEIGHT)) {
									progress.imageView.setVisibility(View.GONE);
								} else {
									progress.imageView.setImageBitmap(progress.bitmap);
									progress.imageView.setVisibility(View.VISIBLE);
								}
							} else {
								if (progress.code == R.id.asyncimageloader_profileimage) {
									progress.imageView.setImageBitmap(mProfileLoadingBitmap);
								} else {
									progress.imageView.setImageBitmap(mAttachmentDefaultBitmap);
								}
								progress.imageView.setVisibility(View.VISIBLE);
							}
						}
					} 
					
					else {
					}
					
					progress.imageUri = null;
					progress.imageView = null;

				}// end of synchronized

			}
		});

	}

};