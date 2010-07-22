package com.neusou.moobook.view;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;

import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.activity.PostActivity;
import com.neusou.moobook.data.User;

public class ActionBar{
	
	public static final String NAMESPACE = "neusou"; 
	Activity mContext;
	int mDefStyle;
	long mUserId;
	String mUserName;
	int mScreenId;
	Resources mResources;
	
	ImageButton mAbPostButton;
	ImageButton mAbReloadButton;
	ImageButton mAbWallButton;
	ImageButton mAbAlbumsButton;
	ImageButton mAbProfileButton;
	
	View.OnClickListener mAbPostButtonOnClick;
	View.OnClickListener mAbReloadButtonOnClick;
	View.OnClickListener mAbWallButtonOnClick;
	View.OnClickListener mAbAlbumsButtonOnClick;
	View.OnClickListener mAbProfileButtonOnClick;
	
	public static final byte BUTTON_POST = 0;
	public static final byte BUTTON_RELOAD = 1;
	public static final byte BUTTON_WALL = 2;
	public static final byte BUTTON_GALLERY = 3;
	public static final byte BUTTON_PROFILE = 4;
		
	public ActionBar() {
		initObjects();
	}
	
	public void setUserId(long userId){
		mUserId = userId;
		updateViews();
	}
	
	public void setUserName(String username){
		mUserName = username;
		updateViews();
	}
	
	public void setActivity(int activityLayoutResId){
		mScreenId = activityLayoutResId;		
	}
	
	public void enableAllButtons(){
		mAbReloadButton.setEnabled(true);
		mAbPostButton.setEnabled(true);
	}
	
	public void disableAllButtons(){
		mAbReloadButton.setEnabled(false);
		mAbPostButton.setEnabled(false);
	}
	
	public void setEnabledButton(byte which, boolean state){
		switch(which){
			case BUTTON_GALLERY:{
				mAbAlbumsButton.setEnabled(state);
				break;
			}
			case BUTTON_POST:{
				mAbPostButton.setEnabled(state);
				break;
			}
			case BUTTON_RELOAD:{
				mAbReloadButton.setEnabled(state);
				break;
			}
			case BUTTON_WALL:{
				mAbWallButton.setEnabled(state);
				break;
			}
			case BUTTON_PROFILE:{
				mAbProfileButton.setEnabled(state);
				break;
			}
		}
	}
	
	
	public void animateReload(){		
		RotateAnimation anim = new RotateAnimation(0,360);
		anim.setRepeatMode(Animation.INFINITE);
		mAbReloadButton.startAnimation(anim);
	}
	
	public void bindViews(Activity act){
		mContext = act;
		mAbReloadButton = (ImageButton) act.findViewById(R.id.ab_refresh);
		mAbPostButton = (ImageButton) act.findViewById(R.id.ab_post);		
		mAbWallButton = (ImageButton) act.findViewById(R.id.ab_wall);
		mAbAlbumsButton = (ImageButton) act.findViewById(R.id.ab_albums);
		mAbProfileButton = (ImageButton) act.findViewById(R.id.ab_profile);
	}
	
	public void initObjects(){
		mAbPostButtonOnClick = new View.OnClickListener() {			
			@Override
			public void onClick(View v) {				
				Intent i = PostActivity.getIntent(mContext);				
				if(App.isSessionUser(mUserId)){
					if(mScreenId == R.layout.stream_activity){						
					}else if(mScreenId == R.layout.comments_activity){
						
					}
					
				}else{
					if(mScreenId == R.layout.comments_activity){
						
					}
					
				}
				mContext.startActivity(i);
			}
		};
		
		mAbReloadButtonOnClick = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
			}
			
		};
		
		mAbWallButtonOnClick = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				App.showUserWall(mContext, mUserId, mUserName);
			}
			
		};
		
		mAbAlbumsButtonOnClick = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
			}
			
		};
		
		mAbProfileButtonOnClick = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
			}
			
		};
		
	}
	
	public void initViews(){
	
		mAbAlbumsButton.setOnClickListener(mAbAlbumsButtonOnClick);
		mAbPostButton.setOnClickListener(mAbPostButtonOnClick);
		mAbReloadButton.setOnClickListener(mAbReloadButtonOnClick);
		mAbProfileButton.setOnClickListener(mAbProfileButtonOnClick);
		mAbWallButton.setOnClickListener(mAbWallButtonOnClick);
		
	//	App.showUserWall(HomeActivity.this, Facebook.INSTANCE.getSession().uid, mUserInSession.name);	
	}
	
	public void updateViews(){
		
	}
	
	public void setOnReloadClick(View.OnClickListener listener){
		mAbReloadButton.setOnClickListener(listener);
	}
	
	public void setOnAddClick(View.OnClickListener listener){
		mAbPostButton.setOnClickListener(listener);
	}
	
	

	
}