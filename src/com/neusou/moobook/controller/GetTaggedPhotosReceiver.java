package com.neusou.moobook.controller;

import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.activity.ViewPhotosActivity;

public class GetTaggedPhotosReceiver extends BroadcastReceiver {
	public static final String LOG_TAG = Logger.registerLog(GetTaggedPhotosReceiver.class);
	public static final IntentFilter INTENT_FILTER = new IntentFilter(App.INTENT_GET_TAGGED_PHOTOS);

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Logger.l(Logger.DEBUG, LOG_TAG, "onReceive(), action:"+action);
		
		if(action.equals(App.INTENT_GET_TAGGED_PHOTOS)){
			Bundle extras = intent.getExtras();
			FBWSResponse fbresponse = (FBWSResponse) extras.getParcelable(FBWSResponse.XTRA_PARCELABLE_OBJECT);										
			try{
				String parsed = fbresponse.jsonArray.toString(2);
				Logger.l(Logger.DEBUG,  LOG_TAG,"[callback_get_tagged_photos]: "+ parsed); 
				Intent i = new Intent(App.INSTANCE,ViewPhotosActivity.class);
				i.putExtra(ViewPhotosActivity.XTRA_PHOTOS, fbresponse.data);						
				i.putExtra(ViewPhotosActivity.XTRA_FACEBOOKUSERID, extras.getLong(Facebook.param_uid,0));
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT); 
				//mUIHandler.sendEmptyMessage(ManagerThread.MESSAGE_DISMISS_DIALOG);
				//mProgressDialog.dismiss();
				App.INSTANCE.startActivity(i);
			}catch(JSONException e){
			}			
		}
	}
};

