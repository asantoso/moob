package com.neusou.moobook.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.EditText;

import com.neusou.moobook.R;

public class CommentBar{
	
	Context mContext;
	int mDefStyle;
	Resources mResources;
	
	Button mCbPostButton;
	EditText mCbCommentInput;
	View mCommentBarWrapper;
	
	public CommentBar() {
		
	}
	
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
	
	public void bindViews(Activity act){
		mCbPostButton = (Button) act.findViewById(R.id.post);
		mCbCommentInput = (EditText) act.findViewById(R.id.comment_input);
		//mCommentBarWrapper = act.findViewById(R.id.commentbox_wrapper);
	}

	public void animate(boolean show){
		ViewAnimationListener mViewAnimListener = new ViewAnimationListener(ViewAnimationListener.SHOW, mCommentBarWrapper);
		mViewAnimListener.mType = ViewAnimationListener.SHOW;		
		Animation anim;
		if(show){
			anim = AnimationUtils.loadAnimation(mContext, R.anim.slide_to_bottom_from_current);
		}else{
			anim = AnimationUtils.loadAnimation(mContext, R.anim.slide_to_current_from_bottom);
		}		
		anim.setAnimationListener(mViewAnimListener);		
	}
	
	public void setPostCommentClick(View.OnClickListener listener){
		mCbPostButton.setOnClickListener(listener);
	}
		
}