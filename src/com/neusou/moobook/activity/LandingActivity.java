package com.neusou.moobook.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.neusou.moobook.App;
import com.neusou.moobook.FBSession;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.thread.BaseManagerThread;

public class LandingActivity extends BaseActivity {
	
	Facebook mFacebook;
	
	public static Intent getIntent(Context ctx) {		
		return new Intent(ctx, LandingActivity.class);
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initObjects();
		
		
		
	}
	
	@Override
	protected void initObjects() {	
		super.initObjects();
		mFacebook = Facebook.getInstance();
	}

	@Override	
	protected void onResume() {
		super.onResume();
		
	
	}	

	View.OnClickListener onClick;
	
	private void gotoHome(){
		Intent i = new Intent(LandingActivity.this, HomeActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
		finish();
	}
	
	private void gotoLogin(){
		Intent i = new Intent(LandingActivity.this, LoginActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(i);
		finish();
	}
	
	@Override
	protected void onPostResume() {
		super.onPostResume();
		
		FBSession session = App.INSTANCE.getSessionInfo();
		mFacebook.setSession(session);
		
		boolean isSessionValid = mFacebook.quickCheckSession(false);
		if(!isSessionValid){
			gotoLogin();
		}else{
			gotoHome();
		}			
		
	}
}
