package com.neusou.moobook.activity;

import com.neusou.Logger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;

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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {	
		super.onActivityResult(requestCode, resultCode, data);
		Logger.l(Logger.DEBUG,lcloctag,"onActivityResult");
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		onStart = true;
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onStart");
	}
	@Override
	protected void onRestart() {
		super.onRestart();
		onRestart = true;
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onRestart");
	}
	@Override
	protected void onResume() {
		super.onResume();
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onResume");
	}
	@Override
	protected void onPostResume() {
		super.onPostResume();
		onResume = true;
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onPostResume");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		onDestroy = true;
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onDestroy");
	}
	
	@Override
	protected void onPause() {	
		super.onPause();
		onPause = true;
		Logger.l(Logger.DEBUG,lcloctag,"#lifecycle->onPause()");
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
