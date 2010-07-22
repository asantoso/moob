package com.neusou.moobook.controller;

import android.app.Activity;
import android.app.ProgressDialog;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;

import com.neusou.moobook.R;
import com.neusou.moobook.activity.BaseUiHandler;
import com.neusou.moobook.thread.ManagerThread;

public class StandardUiHandler extends BaseUiHandler {
	ProgressDialog mProgressDialog;
	View mAdViewWrapper;
	View mBottomHeader;

	ViewAnimationListener mAdViewAnimListener;

	class ViewAnimationListener implements AnimationListener {
		public byte mType;
		View mAdView;
		public static final byte SHOW = 0;
		public static final byte HIDE = 1;

		public ViewAnimationListener(byte type, View adView) {
			mType = type;
			mAdView = adView;
		}

		@Override
		public void onAnimationStart(Animation animation) {
			mAdView.setVisibility(View.VISIBLE);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (mType == SHOW) {
				mAdView.setVisibility(View.VISIBLE);
			} else if (mType == HIDE) {
				mAdView.setVisibility(View.GONE);
			}
		}
	};

	public StandardUiHandler(Activity ctx, ProgressDialog pd, View adViewWrapper, View bottomHeader) {
		super(ctx);
		mBottomHeader = bottomHeader;
		mProgressDialog = pd;
		mAdViewWrapper = adViewWrapper;
		mAdViewAnimListener = new ViewAnimationListener(ViewAnimationListener.SHOW, adViewWrapper);
	}

	public void handleMessage(android.os.Message msg) {
		super.handleMessage(msg);

		int code = msg.what;
		switch (code) {
		
		/*
		case BaseManagerThread.CALLBACK_SERVERCALL_ERROR:{
			try{
			FBWSResponse fbresponse = (FBWSResponse) msg.getData().getParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT);
			Toast.makeText(mContext,fbresponse.errorMessage , 2000).show();
			}catch(Exception e){
			
			}
			break;
		}
		
		case BaseManagerThread.CALLBACK_TIMEOUT_ERROR:{
			try{
			FBWSResponse fbresponse = (FBWSResponse) msg.getData().getParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT);
			Toast.makeText(mContext,fbresponse.errorMessage , 2000).show();
			}catch(Exception e){
				
			}
			break;
		}
		
		case BaseManagerThread.CALLBACK_PROCESS_WSRESPONSE_ERROR:{
			FBWSResponse fbresponse = (FBWSResponse) msg.getData().getParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT);
			Toast.makeText(mContext,fbresponse.errorMessage , 2000).show();
			break;
		}
		*/
		
		case ManagerThread.MESSAGE_DISMISS_DIALOG: {
			mProgressDialog.dismiss();
			break;
		}

		case ManagerThread.CALLBACK_ADMOB_ONFAILRECEIVE: {
			Animation anim = AnimationUtils.loadAnimation(mContext,
					R.anim.slide_to_bottom_from_current);
			
			if(mAdViewWrapper.getVisibility() == View.VISIBLE){
				mAdViewAnimListener.mType = ViewAnimationListener.HIDE;
				anim.setAnimationListener(mAdViewAnimListener);
				mBottomHeader.startAnimation(anim);
			}
			
			//Toast.makeText(mContext, "AdMob Fail Receive", 1000).show();
			break;
		}

		case ManagerThread.CALLBACK_ADMOB_ONRECEIVE: {
			Animation anim = AnimationUtils.loadAnimation(mContext,
					R.anim.slide_to_current_from_bottom);
			if(mAdViewWrapper.getVisibility() != View.VISIBLE){
				mAdViewWrapper.setVisibility(View.VISIBLE);
				int height = mAdViewWrapper.getHeight();
				mAdViewAnimListener.mType = ViewAnimationListener.SHOW;				
				anim.setAnimationListener(mAdViewAnimListener);
				mBottomHeader.startAnimation(anim);
			}
			//Toast.makeText(mContext, "AdMob Receive", 1000).show();
			break;
		}
		}
	}

	@Override
	public void onServerCallError() {
		try{
			mProgressDialog.dismiss();
		}catch(Exception e){
			
		}
	}

	@Override
	public void onTimeoutError() {
		try{
			mProgressDialog.dismiss();
		}catch(Exception e){			
		}
	}

	@Override
	public void onWsResponseError() {
		try{
			mProgressDialog.dismiss();
		}catch(Exception e){			
		}		
	}

}