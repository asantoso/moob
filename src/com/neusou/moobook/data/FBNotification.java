package com.neusou.moobook.data;

import android.content.ContentValues;
import android.database.Cursor;

public class FBNotification {
		
	public static final short col_rowid = 0;
	public static final short col_notification_id = 1;
	public static final short col_app_id = 2;  	
	public static final short col_sender_id = 3; 	
	public static final short col_recipient_id = 4; 	
	public static final short col_created_time = 5;  	
	public static final short col_updated_time = 6; 	
	public static final short col_title_html = 7; 	
	public static final short col_title_text = 8; 	
	public static final short col_body_html = 9; 	
	public static final short col_body_text = 10; 	
	public static final short col_href = 11;	
	public static final short col_is_unread = 12;
		
	public static final short TOTAL_COLUMNS = 13;
	
	public long rowid; 
	public long notification_id;
	public long app_id;  
	public long sender_id; 	
	public long recipient_id; 	
	public long created_time;  	
	public long updated_time; 	
	public String title_html; 	
	public String title_text; 	
	public String body_html; 	
	public String body_text; 	
	public String href;	
	public boolean is_unread;	

	
	public static final String cn_rowid = "notif_rowid";
	public static final String cn_notification_id = "notification_id";
	public static final String cn_app_id = "notif_app_id";
	public static final String cn_sender_id = "sender_id"; 	
	public static final String cn_recipient_id = "recipient_id"; 	
	public static final String cn_created_time = "created_time";  	
	public static final String cn_updated_time = "updated_time"; 	
	public static final String cn_title_html = "title_html"; 	
	public static final String cn_title_text = "title_text"; 	
	public static final String cn_body_html = "body_html"; 	
	public static final String cn_body_text = "body_text"; 	
	public static final String cn_href = "href";	
	public static final String cn_is_unread = "is_unread";

	public static final String fields_rowid = "rowid";
	public static final String fields_notification_id = "notification_id";
	public static final String fields_app_id = "app_id";
	public static final String fields_sender_id = "sender_id"; 	
	public static final String fields_recipient_id = "recipient_id"; 	
	public static final String fields_created_time = "created_time";  	
	public static final String fields_updated_time = "updated_time"; 	
	public static final String fields_title_html = "title_html"; 	
	public static final String fields_title_text = "title_text"; 	
	public static final String fields_body_html = "body_html"; 	
	public static final String fields_body_text = "body_text"; 	
	public static final String fields_href = "href";	
	public static final String fields_is_unread = "is_unread";
	
	public static FBNotification parseCursor(Cursor c, FBNotification s){
		
		if(c == null){
			return null;
		}
		
		if(s == null){
			s = new FBNotification();
		}
		
		s.notification_id = c.getLong(col_notification_id);
		s.app_id = c.getLong(col_app_id);
		
		s.sender_id = c.getLong(col_sender_id);
		s.recipient_id = c.getLong(col_recipient_id);
		s.created_time = c.getLong(col_created_time);
		s.updated_time = c.getLong(col_updated_time);
		
		s.title_html = c.getString(col_title_html);
		s.title_text = c.getString(col_title_text);
		s.body_html = c.getString(col_body_html);
		s.body_text = c.getString(col_body_text);
		
		s.href = c.getString(col_href);
		s.is_unread = c.getLong(col_is_unread) == 1?true:false;
		
		
		return s;
	}
	
	public ContentValues toContentValues(ContentValues cv){
		
		if(cv == null){
			cv = new ContentValues();
		}
		
		cv.put(cn_notification_id, notification_id);
		cv.put(cn_app_id, app_id);
		cv.put(cn_sender_id, sender_id);
		cv.put(cn_recipient_id, recipient_id);
		cv.put(cn_created_time, created_time);
		cv.put(cn_updated_time, updated_time);
		
		cv.put(cn_title_html, title_html);
		cv.put(cn_title_text, title_text);
		cv.put(cn_body_html, body_html);
		cv.put(cn_body_text, body_text);
		
		cv.put(cn_href, href);
		cv.put(cn_is_unread, is_unread);	
		
		return cv;
	}
			
}