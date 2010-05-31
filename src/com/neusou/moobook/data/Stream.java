package com.neusou.moobook.data;

import android.content.ContentValues;
import android.database.Cursor;

public class Stream {
			
	//0-based index of the table columns
	public static final short col_rowid = 0;
	public static final short col_post_id = 1 ;
	public static final short col_actor_id = 2 ;	
	public static final short col_target_id = 3 ;
	public static final short col_viewer_id = 4 ;
	public static final short col_source_id = 5 ;
	public static final short col_type = 6 ;	
	public static final short col_message = 7 ;
	public static final short col_updated_time = 8 ;
	public static final short col_created_time = 9 ;
	public static final short col_likes_count = 10 ;
	public static final short col_likes_friends = 11 ;
	public static final short col_likes_canlike = 12 ;
	public static final short col_likes_userlikes = 13 ;
	public static final short col_attribution = 14;
	public static final short col_attachment = 15;
	public static final short col_app_id = 16;
	
	public static final short col_comments_count = 17;
	public static final short col_comments_can_post = 18;
	public static final short col_comments_can_remove = 19;
	
	public static final short TOTAL_COLUMNS = 20;
	
	public String post_id;
	public long actor_id;
	public String target_id;
	public long viewer_id;
	public long source_id;
	public String type;
	public String message;
	public long updated_time;
	public long created_time;
	public long likes_count;
	public String likes_friends;
	public boolean likes_canlike;
	public boolean likes_userlikes;
	public String attribution;
	public String attachment;
	public int app_id;
	
	public long comments_count;
	public boolean comments_can_post;
	public boolean comments_can_remove;
	//public String comments_comment_list;
	
	public static final String cn_rowid = "post_rowid";
	public static final String cn_post_id = "post_id";
	public static final String cn_attribution = "attribution";
	public static final String cn_actor_id = "actor_id";
	public static final String cn_target_id = "target_id";
	public static final String cn_viewer_id = "viewer_id";
	public static final String cn_source_id = "source_id";
	public static final String cn_type = "type";
	public static final String cn_message = "message";
	public static final String cn_updated_time = "updated_time";
	public static final String cn_created_time = "created_time";
	public static final String cn_likes_count = "likes_count";
	public static final String cn_likes_friends = "likes_friends";
	public static final String cn_likes_canlike = "likes_canlike";
	public static final String cn_likes_userlikes = "likes_userlikes";
	public static final String cn_attachment = "attachment";
	public static final String cn_appid = "stream_appid";
	
	public static final String cn_comments_count = "comments_count";
	public static final String cn_comments_can_post = "comments_can_post";
	public static final String cn_comments_can_remove = "comments_can_remove";
	
	public static final String fields_appid = "app_id";
//	public static final String cn_comments_comment_list = "comments_comment_list";
	
	public static Stream parseCursor(Cursor c, Stream s){
		
		boolean valid = !c.isClosed();
		if(!valid){
			return null;
		}
		
		if(s == null){
			s = new Stream();
		}
		
		s.post_id = c.getString(col_post_id);
		s.attribution = c.getString(col_attribution);
		s.actor_id = c.getLong(col_actor_id);
		s.target_id = c.getString(col_target_id);
		s.viewer_id = c.getLong(col_viewer_id);
		s.source_id = c.getLong(col_source_id);
		s.type = c.getString(col_type);
		s.message = c.getString(col_message);
		s.updated_time = c.getLong(col_updated_time);
		s.created_time = c.getLong(col_created_time);
		s.likes_count = c.getLong(col_likes_count);
		s.likes_friends = c.getString(col_likes_friends);
		s.likes_canlike = c.getInt(col_likes_canlike) == 1? true:false;
		s.likes_userlikes = c.getInt(col_likes_userlikes) == 1? true:false;
	
		s.attachment = c.getString(col_attachment);
		s.app_id = c.getInt(col_app_id);
		
		s.comments_count = c.getLong(col_comments_count);
		s.comments_can_post = c.getInt(col_comments_can_post) == 1?true:false;
		s.comments_can_remove = c.getInt(col_comments_can_remove) == 1?true:false;
		//s.comments_comment_list = c.getString(col_comments_comment_list);
		
		c.close();
		return s;
	}
	
	public ContentValues toContentValues(ContentValues cv){
		
		if(cv == null){
			cv = new ContentValues();
		}
		
		cv.put(cn_actor_id, actor_id);
		cv.put(cn_attribution, attribution);
		cv.put(cn_created_time, created_time);
		cv.put(cn_likes_canlike, likes_canlike);
		cv.put(cn_likes_count, likes_count);
		cv.put(cn_likes_friends, likes_friends);
		cv.put(cn_likes_userlikes, likes_userlikes);
		cv.put(cn_message, message);
		cv.put(cn_post_id, post_id);
		cv.put(cn_source_id, source_id);
		cv.put(cn_target_id, target_id==null?"":target_id);
		cv.put(cn_type, type);
		cv.put(cn_updated_time, updated_time);
		cv.put(cn_viewer_id, viewer_id);
		cv.put(cn_attachment, attachment);
		cv.put(cn_appid, app_id);
		cv.put(cn_comments_can_post, comments_can_post);
		cv.put(cn_comments_can_remove, comments_can_remove);
		cv.put(cn_comments_count, comments_count);		

		//cv.put(cn_comments_comment_list, comments_comment_list);
		
		
		return cv;
	}
	
	public ContentValues commentsToContentValues(ContentValues cv){
		
		if(cv == null){
			cv = new ContentValues();
		}
		
		cv.put(cn_comments_can_post, comments_can_post);
		cv.put(cn_comments_can_remove, comments_can_remove);
		cv.put(cn_comments_count, comments_count);		
				
		return cv;
	}
	
}