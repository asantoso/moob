package com.neusou.moobook.activity;

import com.neusou.moobook.R;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class WriteCommentActivity extends BaseActivity{
	public static final String LOG_TAG = "WriteCommentActivity";
	public static final String XTRA_MESSAGE = "xtra.message";
	public static final String XTRA_POST_ID = "xtra.post_id";
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.writecomment_activity);
		bindViews();
		initObjects();
		initViews();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
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
	protected void onDestroy() {
		super.onDestroy();
	
	}
	
	protected void bindViews(){
		comment = (TextView) findViewById(R.id.comment);
		message = (TextView) findViewById(R.id.message);
		post = (Button) findViewById(R.id.post);
	}
	
	protected void initObjects(){
		Bundle b = getIntent().getExtras();
		messageText = b.getString(XTRA_MESSAGE);
		post_id = b.getString(XTRA_POST_ID);
		mPostOnClickLst = new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				
			}
		};
	}
	
	protected void initViews(){
		message.setText(messageText);
		post.setOnClickListener(mPostOnClickLst);
	}
	
	View.OnClickListener mPostOnClickLst;
	TextView comment;
	TextView message;
	Button post;
	
	String messageText;
	String post_id;
	
}