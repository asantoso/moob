package com.neusou.moobook.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;

public class DisplayImageActivity extends BaseActivity{
	
	//public static final String XTRA_IMAGE = "xtra.image";
	public static final String XTRA_IMAGE_URL = "xtra.image.url";
	public static final String XTRA_PHOTO_ID = "xtra.photo.id";

	public static Intent getIntent(Context ctx) {
		return new Intent(ctx, DisplayImageActivity.class);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	
		super.onCreate(savedInstanceState);
		getWindow().setBackgroundDrawable(null);
		setContentView(R.layout.viewimage_activity);
		bindViews();
		initObjects();
		initViews();
		
	}
	
	@Override
	protected void onResume() {
	
		super.onResume();
		
		//Drawable d = (Drawable) getIntent().getParcelableExtra(XTRA_IMAGE);
		String imageUrl =  getIntent().getStringExtra(XTRA_IMAGE_URL);
		long photo_id =  getIntent().getLongExtra(XTRA_PHOTO_ID,0);
		
		Toast.makeText(this, imageUrl, 1000).show();
		Util util = new Util();
		//TODO Logger remove
		Log.d("debug","loading image... url: "+imageUrl);
		Drawable d = App.mImageUrlLoader.loadImage(imageUrl, false);
		Log.d("debug","loading image... done.");
		mImageView.setImageDrawable(d);
		
	}
	
	@Override
	protected void bindViews() {
	
		super.bindViews();
		
		mImageView = (ImageView) findViewById(R.id.image);
	}
	
	@Override
	protected void initViews() {
	
		super.initViews();
	}
	
	@Override
	protected void initObjects() {

		super.initObjects();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
	
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
	
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	ImageView mImageView;
	
}