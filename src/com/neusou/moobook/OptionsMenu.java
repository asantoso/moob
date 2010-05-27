package com.neusou.moobook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

import com.neusou.moobook.activity.AppSettings;
import com.neusou.moobook.activity.StreamActivity;

public class OptionsMenu {	
	public static  MenuItem createPreference(Activity act, Menu menu, int groupId, int itemId, int order){	
		MenuItem mi = menu.add(groupId, itemId, order, "Settings");
		mi.setOnMenuItemClickListener(
			new OnMenuItemClickListener() {					
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					Intent i = new Intent(App.INSTANCE, AppSettings.class);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					App.INSTANCE.startActivity(i);
					return true;
				}
			}
		);
		return mi;
	}
	
}