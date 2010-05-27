package com.neusou.moobook.data;

import android.content.ContentValues;
import android.database.Cursor;

public class Event {
	
	public enum RSVPStatus{
		NOT_REPLIED("not_replied"),
		ATTENDING("attending"),
		DECLINED("declined"),
		UNSURE("unsure");
		String rsvp;
		
		RSVPStatus(String rsvp){
			this.rsvp = rsvp;
		}
		
		public static RSVPStatus fromString(String text){
			
			if("not_replied".compareTo(text) == 0){
				return RSVPStatus.NOT_REPLIED;
			}
			
			if("attending".compareTo(text) == 0){
				return RSVPStatus.ATTENDING;
			}
			
			if("declined".compareTo(text) == 0){
				return RSVPStatus.DECLINED;
			}
			
			if("unsure".compareTo(text) == 0){
				return RSVPStatus.UNSURE;
			}
			
			return RSVPStatus.UNSURE;			
		}
		
		@Override
		public String toString() {		
			return rsvp;
		}
		
		public boolean match(String rsvp){
			return this.rsvp.compareTo(rsvp) == 0;
		}
	}
	
	public static final int col_rowid = 0;
	public static final int col_eid = 1; 
	public static final int col_name = 2;	  	
	public static final int col_tagline = 3;  	 	
	public static final int col_nid = 4;	 	 	
	public static final int col_pic_small = 5;  	
	public static final int col_pic_big = 6;	 	
	public static final int col_pic = 7;	 	 	
	public static final int col_host = 8;  	
	public static final int col_description = 9; 	 	
	public static final int col_event_type= 10;   	
	public static final int col_event_subtype= 11; 	 	
	public static final int col_start_time = 12;	  	
	public static final int col_end_time = 13;	  	
	public static final int col_creator = 14;  	
	public static final int col_update_time = 15; 	
	public static final int col_location = 16; 	 	
	public static final int col_venue = 17;	 	 	
	public static final int col_privacy = 18; 	
	public static final int col_hide_guest_list = 19;
	public static final int col_rsvp_status = 20;
	
	public long eid ;
	public String name ;  	
	public String tagline ; 	 	
	public int nid ; 	
	public String pic_small ;  	
	public String pic_big ; 	
	public String pic ;  	 	
	public String host ;  	
	public String description ; 	 	
	public String event_type ;  	
	public String event_subtype ; 	 	
	public long start_time ; 	
	public long end_time ;  	
	public String creator ;  	
	public long update_time ; 	
	public String location ; 	 	
	public String venue ; 	 	
	public String privacy ; 	
	public boolean hide_guest_list ;
	public String rsvp_status;
	
	public static final String cn_rowid = "event_rowid"; 
	public static final String cn_eid = "eid"; 
	public static final String cn_name = "name";	  	
	public static final String cn_tagline = "tagline";  	 	
	public static final String cn_nid = "nid";	 	 	
	public static final String cn_pic_small = "pic_small";  	
	public static final String cn_pic_big = "pic_big";	 	
	public static final String cn_pic = "pic";	 	 	
	public static final String cn_host = "host";  	
	public static final String cn_description = "description"; 	 	
	public static final String cn_event_type= "event_type";   	
	public static final String cn_event_subtype= "event_subtype"; 	 	
	public static final String cn_start_time = "start_time";	  	
	public static final String cn_end_time = "end_time";	  	
	public static final String cn_creator = "creator";  	
	public static final String cn_update_time = "update_time"; 	
	public static final String cn_location = "location"; 	 	
	public static final String cn_venue = "venue";	 	 	
	public static final String cn_privacy = "privacy"; 	
	public static final String cn_hide_guest_list = "hide_guest_list";
	public static final String cn_rsvp_status = "rsvp_status";
	
	/*
	public static final String RSVP_ATTENDING = "attending";
	public static final String RSVP_DECLINED = "declined";
	public static final String RSVP_UNSURE = "unsure";
	public static final String RSVP_NOTREPLIED = "not_replied";
	*/
	
	public static Event parseCursor(Cursor c, Event s){	
				
		if(c == null){
			return null;
		}
		
		if(s == null){
			s = new Event();
		}
		
		s.eid = c.getLong(col_eid);
		s.name = c.getString(col_name);
		s.tagline = c.getString(col_tagline);
		s.nid = c.getInt(col_nid);
		s.pic_small = c.getString(col_pic_small);
		s.pic_big = c.getString(col_pic_big);
		s.pic = c.getString(col_pic);
		s.host = c.getString(col_host);
		s.description = c.getString(col_description);
		s.event_type = c.getString(col_event_type);
		s.event_subtype = c.getString(col_event_subtype);
		s.start_time = c.getLong(col_start_time);
		s.end_time = c.getLong(col_end_time);
		s.creator = c.getString(col_creator);
		s.update_time = c.getLong(col_update_time);
		s.location = c.getString(col_location);
		s.venue = c.getString(col_venue);
		s.privacy = c.getString(col_privacy);
		s.hide_guest_list = c.getInt(col_hide_guest_list) == 1?true:false;
		s.rsvp_status = c.getString(col_rsvp_status);
		
		return s;
	}
	
	public ContentValues toContentValues(ContentValues cv){
		
		if(cv == null){
			cv = new ContentValues();
		}
		
		cv.put(cn_creator, creator);
		cv.put(cn_description , description);
		cv.put(cn_eid , eid);
		cv.put(cn_end_time , end_time);
		cv.put(cn_event_subtype , event_subtype);
		cv.put(cn_event_type , event_type);
		cv.put(cn_hide_guest_list , hide_guest_list?1:0);
		cv.put(cn_host, host);
		cv.put(cn_location , location);
		cv.put(cn_name, name);
		cv.put(cn_nid, nid);
		cv.put(cn_pic, pic);
		cv.put(cn_pic_big, pic_big);
		cv.put(cn_pic_small, pic_small);
		cv.put(cn_privacy, privacy);
		cv.put(cn_start_time, start_time);
		cv.put(cn_tagline, tagline);
		cv.put(cn_update_time, update_time);
		cv.put(cn_venue, venue);
		cv.put(cn_rsvp_status, rsvp_status);
			
		return cv;
	}
	
}