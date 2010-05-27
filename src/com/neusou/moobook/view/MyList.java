package com.neusou.moobook.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class MyList extends ListView {

	public static final String LOG_TAG = "MyList";
	
	public MyList(Context context, AttributeSet attrSet) {
		super(context, attrSet);
		
		setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
			
			@Override
			public void onChildViewRemoved(View parent, View child) {
				//Log.d(LOG_TAG,"onChildViewRemoved");
				
			}
			 
			@Override
			public void onChildViewAdded(View parent, View child) {
				//Log.d(LOG_TAG,"onChildViewAdded");							
			}
				
		});
		
	}

}