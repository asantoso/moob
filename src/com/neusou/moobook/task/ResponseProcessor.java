package com.neusou.moobook.task;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.data.Event;
import com.neusou.moobook.data.FBApplication;
import com.neusou.moobook.data.FBNotification;
import com.neusou.moobook.data.User;
import com.neusou.moobook.model.database.ApplicationDBHelper;

public class ResponseProcessor {

	static final String LOG_TAG = "ResponseProcessor";

	public static Notification showNotificaton(int total, FBNotification note,
			NotificationManager nm, Context ctx) {
		Logger.l(Logger.DEBUG, LOG_TAG, "[showNotications()]");
		Notification notif = App.INSTANCE.createNotification("Moobook", note.title_text, total);
		nm.notify(R.string.alarm_service_started, notif);
		return notif;
		//Vibrator vibrator = (Vibrator) (App.INSTANCE.getSystemService(Context.VIBRATOR_SERVICE));
		//vibrator.vibrate(new long[]{0,500,500,500,500}, 2);
		//vibrator.cancel();		
	}

	public static HashMap<Long, FBApplication> processApplications(JSONArray data, HashMap<Long,FBApplication> appMaps) {
		if(appMaps == null){
			appMaps = new HashMap<Long, FBApplication>();
		}else{
			appMaps.clear();
		}
		if (data == null) {
			return null;
		}
		int count = data.length();
		if (count == 0) {
			return null;
		}
		for (int i = 0; i < count; i++) {
			try {
				JSONObject user = data.getJSONObject(i);
				FBApplication fbApp = new FBApplication();
				Long app_id = user.getLong(FBApplication.cn_app_id);
				fbApp.app_id = app_id;
				fbApp.icon_url= user.getString(FBApplication.cn_icon_url);
				fbApp.logo_url= user.getString(FBApplication.cn_logo_url);
				fbApp.display_name = user.getString(FBApplication.cn_display_name);
				appMaps.put(app_id, fbApp);
			} catch (JSONException e) {
			}
		}
		return appMaps;
	}

	public static void processUsers(JSONArray data, short[] selection, SQLiteDatabase db,
			ApplicationDBHelper dbHelper) {
				
		if (data == null) {
			return;
		}
		int numUsers = data.length();
		if (numUsers == 0) {
			return;
		}
		
		User user = new User();		
		db.beginTransaction();		
		for (int i = 0; i < numUsers; i++) {
			try {
				JSONObject userJson = data.getJSONObject(i);
				user.clear();
				user.parse(userJson, selection);
				dbHelper.insertUser(user, selection , db);
			} catch (JSONException e) {
			}
		}
		db.setTransactionSuccessful();
		db.endTransaction();		
	}

	public static void processEvents(
			JSONArray events_member,
			JSONArray events,
			SQLiteDatabase db,
			ApplicationDBHelper dbHelper,
			Context ctx
		){
		
		if (events_member == null || events == null) {
			return;
		}
		
		int attendanceCount = events.length();
		HashMap<Long,String> rsvp = new HashMap<Long,String>();
		
		for(int i=0;i<attendanceCount;i++){
			JSONObject att;
			try {
				att = events.getJSONObject(i);
				String rsvp_status = att.getString(Event.cn_rsvp_status);
				long eid = att.getLong("eid");
				rsvp.put(eid, rsvp_status);
			} catch (JSONException e) {
				e.printStackTrace();
			}	
		}
				
		int num = events_member.length();
		Event event = null;
		
		db.beginTransaction();
		
		for (int i = 0; i < num; i++) {
			JSONObject itemJson;

			try {
				itemJson = events_member.getJSONObject(i);
				long event_id = itemJson.getLong(Event.cn_eid);
				if (db != null && db.isOpen()) {					
					event = dbHelper.getEvent(db, event_id, event);
					if (event == null)
					{
						event = new Event();						
						event.creator = itemJson.getString(Event.cn_creator);
						event.description = itemJson.getString(Event.cn_description);
						event.eid = itemJson.getLong(Event.cn_eid);
						event.start_time = itemJson.getLong(Event.cn_start_time);
						event.end_time = itemJson.getLong(Event.cn_end_time);
						event.event_subtype = itemJson.getString(Event.cn_event_subtype);
						event.event_type = itemJson.getString(Event.cn_event_type);
						event.hide_guest_list = Boolean.parseBoolean(itemJson.getString(Event.cn_hide_guest_list));
						event.host = itemJson.getString(Event.cn_host);
						event.location = itemJson.getString(Event.cn_location);
						event.name = itemJson.getString(Event.cn_name);
						event.pic = itemJson.getString(Event.cn_pic);
						event.pic_big = itemJson.getString(Event.cn_pic_big);
						event.pic_small = itemJson.getString(Event.cn_pic_small);
						event.tagline = itemJson.getString(Event.cn_tagline);
						event.venue = itemJson.getString(Event.cn_venue);	
						event.rsvp_status = rsvp.get(event.eid);
						dbHelper.insertEvent(event, db);
					}
				} else {
					db.endTransaction();
					return;
				}

			} catch (JSONException e) {
				e.printStackTrace();
			} finally {
			}
		}	
		
		db.setTransactionSuccessful();
		db.endTransaction();		
	}
	
	public static void processNotifications(
			JSONArray notifications,
			HashMap<Long, FBApplication> applications,
			SQLiteDatabase db, 
			ApplicationDBHelper dbHelper,
			NotificationManager nm,
			Context ctx) {
		
		Logger.l(Logger.DEBUG, LOG_TAG, "[process notifications()]");
		
		
		if (notifications == null) {
			return;
		}
				
		int num = notifications.length();
		FBNotification note = null;
		FBApplication remoteApp = null;

		boolean getFirst = true;
		boolean hasNew = false;
		
		FBNotification firstnote = null;
		
		db.beginTransaction();
		
		for (int i = 0; i < num; i++) {
			JSONObject itemJson;

			try {
				itemJson = notifications.getJSONObject(i);
				long notification_id = itemJson.getLong(FBNotification.fields_notification_id);
				if (db != null && db.isOpen()) {	
					
					note = dbHelper.getNotification(db, notification_id, note);					
					
					if (note == null) 
					{
						note = new FBNotification();						
						note.notification_id = notification_id;
						note.body_text = itemJson.getString(FBNotification.fields_body_text);
						note.title_text = itemJson.getString(FBNotification.fields_title_text);
						note.created_time = itemJson.getLong(FBNotification.fields_created_time);
						note.href = itemJson.getString(FBNotification.fields_href);
						note.sender_id = itemJson.getLong(FBNotification.fields_sender_id);
						note.app_id = itemJson.getLong(FBNotification.fields_app_id); 
							
						FBApplication existingApp = dbHelper.getApplication(db,note.app_id, null);
						remoteApp = applications.get(note.app_id);
						if(existingApp != null){
							remoteApp._isHidden = existingApp._isHidden;
						}
						
						//once the local application visibility status is set to hidden,
						//the response applications set must not have the same application  
						assert(existingApp.app_id != remoteApp.app_id && existingApp._isHidden);
						
						dbHelper.insertApplication(remoteApp, db);						
						dbHelper.insertNotification(note, db);						
						
						hasNew = true;
						
						if(getFirst){
							firstnote = note;
							getFirst = false;
						}
						
					}
					
				}
				
			} catch (JSONException e) {
				e.printStackTrace();				
			} finally {							
			}			
		
		}		
		
		db.setTransactionSuccessful();
		db.endTransaction();
		
		if(firstnote != null){
			Logger.l(Logger.DEBUG, LOG_TAG, "show notifications");
			Cursor c = dbHelper.getAllNotifications(App.INSTANCE.mDB, null);
			int totalNotifications = c.getCount();
			c.close();			
			
			if(App.INSTANCE.isNotificationEnabled()){
				App.INSTANCE.mNotification = showNotificaton(totalNotifications, firstnote, nm, ctx);	
				App.INSTANCE.playNotificationRingtone();
				Intent stopNotifIntent = new Intent(App.INTENT_STOP_NOTIFICATIONS_SOUND);
				App.INSTANCE.delayedBroadcast(stopNotifIntent,15000);
			}
			Intent i = new Intent(App.INTENT_NEW_NOTIFICATIONS);
			ctx.sendBroadcast(i);
		}
	}

}