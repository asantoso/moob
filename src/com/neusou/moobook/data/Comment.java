package com.neusou.moobook.data;


public class Comment {
	
	public static final short col_rowid = 0;  
	public static final short col_from_id = 1;  
	public static final short col_time = 2;
	public static final short col_comment = 3;
	public static final short col_post_id = 4;
	public static final short col_comment_id = 5;
	
	public static final short TOTAL_PROPERTY_COLUMNS = 6;
		
	public long from_id;
	public long time;
	public String comment;
	public String post_id;
	public String comment_id; //corresponds to id attribute in the FQL comment table 
	
	public static final String cn_comment_id = "comment_id";
	public static final String cn_from_id = "from_id";  
	public static final String cn_time = "time";
	public static final String cn_comment = "comment";
	public static final String cn_post_id = "post_id";
		/*
	
	public static Comment parseCursor(Cursor c, Comment s){
		
		boolean valid = c.moveToNext();
		if(!valid){
			return null;
		}
		
		if(s == null){
			s = new Comment();
		}
		
		s.from_id = c.getLong(col_from_id);
		s.comment = c.getString(col_comment);
		s.time = c.getLong(col_time);
		s.post_id = c.getString(col_post_id);
		s.comment_id = c.getString(col_comment_id);		
		
		c.close();
		return s;
	}
	
	public ContentValues toContentValues(ContentValues cv){
		
		if(cv == null){
			cv = new ContentValues();
		}
		
		cv.put(cn_from_id, from_id);
		cv.put(cn_comment, comment);
		cv.put(cn_time, time);
		cv.put(cn_post_id, post_id);
		cv.put(cn_comment_id, comment_id);
		
		return cv;
	}
	*/
		
}