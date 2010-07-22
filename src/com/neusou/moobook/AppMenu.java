package com.neusou.moobook;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

import com.neusou.moobook.activity.ViewPhotosActivity;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;

public class AppMenu {
	

	
	
	private static ProgressDialog createProgressDialog(Context ctx) {
		ProgressDialog pd;
		pd = new ProgressDialog(ctx);
		return pd;
	}
	
	public static OnMenuItemClickListener createGetTaggedMenuItemOnClick(final ProgressDialog pd, final int outHandlerKey, final long actor_id, final Context ctx){
		OnMenuItemClickListener mGetTaggedPhotosMenuItemOnClick = 
			new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					
					Bundle data = new Bundle();
					data.putString(ManagerThread.XTRA_CALLBACK_INTENT_ACTION, App.INTENT_GET_TAGGED_PHOTOS);
					data.putLong(Facebook.param_uid, actor_id);
					Facebook.INSTANCE.getTaggedPhotos(
						outHandlerKey,
						data,
						actor_id,
						ViewPhotosActivity.NUM_PHOTOS_PER_REQUEST, 0,
						ManagerThread.CALLBACK_GET_TAGGED_PHOTOS,
						BaseManagerThread.CALLBACK_SERVERCALL_ERROR,
						BaseManagerThread.CALLBACK_TIMEOUT_ERROR, 0);		
					
					pd.show();
					
					return true;
				}
			};
		return mGetTaggedPhotosMenuItemOnClick;
	}
	
	
	
}