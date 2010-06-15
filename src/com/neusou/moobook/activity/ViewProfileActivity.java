package com.neusou.moobook.activity;

import com.neusou.moobook.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class ViewProfileActivity extends BaseActivity{
	
	public static Intent getIntent(Context caller){
		Intent i = new Intent(caller, ViewProfileActivity.class);
		return i;
	}
	
	public static Bundle getCallDescriptor(String name){
		Bundle b = new Bundle();
		b.putString("name", name);
		return b;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewprofile_activity);
	}
	
}