package com.neusou.moobook.activity;

import android.content.Context;
import android.content.Intent;

public class PreferenceActivity extends BaseActivity{

	public static Intent getIntent(Context ctx) {		
		return new Intent(ctx, PreferenceActivity.class);
	}
	
	
}