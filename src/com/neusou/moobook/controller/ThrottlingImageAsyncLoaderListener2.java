//
//package com.neusou.moobook.controller;
//
//import android.database.DataSetObservable;
//import android.database.DataSetObserver;
//import android.os.Handler;
//import android.os.Message;
//import android.os.SystemClock;
//
//import com.neusou.Logger;
//import com.neusou.web.ImageUrlLoader2;
//import com.neusou.web.ImageUrlLoader2.AsyncLoaderResult;
//
//public abstract class ThrottlingImageAsyncLoaderListener2 extends Handler implements ImageUrlLoader2.AsyncListener {
//		long mLastUiUpdateRequest = 0;
//		long mFreezeUiRequestWindowSizeMillis; 
//		long mExtraDelayMillis;
//		boolean mIsNotifyObserver = false;
//		Handler h = new Handler();
//
//		DataSetObservable mDataSetObservable = new DataSetObservable();
//		
//		public static final String LOG_TAG = "ThrottlingImageAsyncLoaderListener";
//		
//		/**
//		 * Creates a ThrottlingImageAsyncLoaderListener
//		 * 
//		 * @param observer The data observer
//		 * @param extraDelayMillis the extra delay in milliseconds before launching notify message.
//		 * @param windowSizeMillis the size of the time window in milliseconds where notify requests are dropped, to prevent spamming the observer with "notify data changed" requests.
//		 * @param isNotifyObserver if set to true will automatically notify data observer when the result is successful
//		 */
//		public ThrottlingImageAsyncLoaderListener2(DataSetObserver observer, long extraDelayMillis, int windowSizeMillis, boolean isNotifyObserver) {
//			mFreezeUiRequestWindowSizeMillis = windowSizeMillis;
//			mExtraDelayMillis = extraDelayMillis;	
//			mIsNotifyObserver = isNotifyObserver;	
//			setObserver(observer);
//		}
//		
//		public void setObserver(DataSetObserver observer){
//			mDataSetObservable.unregisterAll();
//			try{
//				mDataSetObservable.registerObserver(observer);
//			}catch(IllegalStateException e){				
//			}catch(IllegalArgumentException e){				
//			}			
//		}
//		
//		@Override
//		public void handleMessage(Message msg) {
//			super.handleMessage(msg);
//			
//		}
//		
//		@Override
//		public void onCancelled() {
//			mDataSetObservable.unregisterAll();			
//		}
//		
//		@Override
//		public void onPreExecute() {
//
//		}
//		
//		@Override
//		public void onPostExecute(final AsyncLoaderResult result) {
//		
//			//schedule an update execution at time now+windowsize in the future.
//			//the update flag is set to true to ignore other subsequent threads that enter the code late 
//			//i.e: all threads inside the window will be ignored
//
//			if(mIsNotifyObserver){
//				if(result != null && result.status == ImageUrlLoader2.AsyncLoaderResult.SUCCESS){
//					scheduleNotify();
//				}
//			}
//			
//		} 	
//		
//		protected void scheduleNotify(){
//			long now = SystemClock.currentThreadTimeMillis();
//			if (mLastUiUpdateRequest + mFreezeUiRequestWindowSizeMillis < now) {				
//					mLastUiUpdateRequest = now + mFreezeUiRequestWindowSizeMillis;
//					h.postDelayed(new Runnable() {
//						@Override
//						public void run() {				
//							if(mDataSetObservable != null){
//								mDataSetObservable.notifyChanged();
//							}else{
//								Logger.l(Logger.ERROR, LOG_TAG, "[scheduleNotify()] adapter is null");
//							}
//						}
//					}, mFreezeUiRequestWindowSizeMillis + mExtraDelayMillis);
//			}
//		}
//
//
//	
//}