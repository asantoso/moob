package com.neusou.moobook.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class PhotoAlbumsActivity extends BaseActivity{
	
	public static Intent getIntent(Context ctx) {		
		return new Intent(ctx, PhotoAlbumsActivity.class);
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void bindViews() {
		super.bindViews();
	}
	
	@Override
	protected void initObjects() {
		super.initObjects();
	}
	
	@Override
	protected void initViews() {
		super.initViews();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {	
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	
	
	
}