package com.neusou.moobook.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;

import com.neusou.Logger;
import static com.neusou.moobook.activity.LifecycleFlags.*;

public abstract class BaseActivity extends Activity {
	protected static String lcloctag;
	Resources mResources;
	LayoutInflater mLayoutInflater;
	protected boolean onCreate;
	protected boolean onDestroy;
	protected boolean onRestart;
	protected boolean onResume;
	protected boolean onStart;
	protected boolean onPause;
	
	protected LifecycleFlags mLifecycleFlags = new LifecycleFlags();
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		lcloctag = this.getClass().getSimpleName();
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onRetainNonConfiguration");
		return super.onRetainNonConfigurationInstance();
	}
	
	@Override
	public Object getLastNonConfigurationInstance() {
		lcloctag = this.getClass().getSimpleName();
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->getLastNonConfigurationInstance");
		return super.getLastNonConfigurationInstance();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		lcloctag = this.getClass().getSimpleName();
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onConfigurationChanged");
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {	
		super.onActivityResult(requestCode, resultCode, data);
		mLifecycleFlags.set(ACTIVITYRESULT);
		lcloctag = this.getClass().getSimpleName();
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onActivityResult");
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mLifecycleFlags.set(LC_CREATE);
		onCreate = true;
		lcloctag = this.getClass().getSimpleName();
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onCreate");
		mResources = getResources();
		mLayoutInflater = getLayoutInflater();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onSaveInstanceState");
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onRestoreInstanceState");
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		mLifecycleFlags.set(START);
		onStart = true;
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onStart");
	}
	@Override
	protected void onRestart() {
		super.onRestart();
		mLifecycleFlags.set(RESTART);
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onRestart");
		onRestart = true;		
	}
	@Override
	protected void onResume() {
		super.onResume();		
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onResume");
		mLifecycleFlags.set(LC_RESUME);
	}
	@Override
	protected void onPostResume() {
		super.onPostResume();
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onPostResume");
		mLifecycleFlags.clearAndSet(POSTRESUME);
		onResume = true;		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mLifecycleFlags.clearAndSet(DESTROY);
		onDestroy = true;
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onDestroy, isFinishing?"+isFinishing());		
	}
	
	@Override
	protected void onPause() {	
		super.onPause();
		mLifecycleFlags.set(PAUSE);
		onPause = true;		
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onPause, isFinishing?"+isFinishing());
	}
	
	protected void bindViews(){
		Logger.l(Logger.DEBUG,lcloctag,"bindViews()");
	}
	
	protected void initObjects(){
		Logger.l(Logger.DEBUG,lcloctag,"initObjects()");
	}
	
	protected void initViews(){
		Logger.l(Logger.DEBUG,lcloctag,"initViews()");
	}
}
