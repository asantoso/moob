package com.neusou.moobook.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ExpandableListView;

import com.neusou.Logger;

public class MyExpandableList extends ExpandableListView {

	public static final String LOG_TAG = Logger.registerLog(MyExpandableList.class);
	
	public MyExpandableList(Context context, AttributeSet attrSet) {
		super(context, attrSet);
	}

}