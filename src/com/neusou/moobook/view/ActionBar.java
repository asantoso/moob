package com.neusou.moobook.view;

import com.neusou.moobook.R;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

public class ActionBar{
	
	Context mContext;
	int mDefStyle;
	Resources mResources;
	public static final String NAMESPACE = "neusou"; 
	ImageButton mAbReloadButton;
	ImageButton mAbPostButton;
	
	public ActionBar() {
		
	}
	
	private void init(){	
	}
	
	
	
	public void animateReload(){		
		RotateAnimation anim = new RotateAnimation(0,360);
		anim.setRepeatMode(Animation.INFINITE);
		mAbReloadButton.startAnimation(anim);
	}
	
	public void bindViews(Activity act){
		mAbReloadButton = (ImageButton) act.findViewById(R.id.ab_refresh);
		mAbPostButton = (ImageButton) act.findViewById(R.id.ab_post);
	}
	
	public void setOnReloadClick(View.OnClickListener listener){
		mAbReloadButton.setOnClickListener(listener);
	}
	
	public void setOnAddClick(View.OnClickListener listener){
		mAbPostButton.setOnClickListener(listener);
	}

	
}