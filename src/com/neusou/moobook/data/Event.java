package com.neusou.moobook.data;

import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.Logger;
import com.neusou.moobook.adapters.CursorRowMapper;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Event implements Parcelable{
	
	public static final ROWMAPPER RowMapper = new ROWMAPPER();
	
	public static final String LOG_TAG = Logger.registerLog(Event.class);
	
	static final class ROWMAPPER implements CursorRowMapper<Event>{
		@Override
		public Event map(Cursor c) {
			return Event.parseCursor(c, null);
		}		
	}

	
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
			Log.d("Event", "Event rsvp match: "+rsvp);
			try{
				return rsvp.compareTo(rsvp) == 0;
			}catch(NullPointerException e){
				return false;
			}
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
	
	//this section below are not to be serialized
	public String location_latitude;
	public String location_longitude;
	public String location_state;
	public String location_country;
	public String location_city;
	public String location_street;
	//
	
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
		
		parseVenue(s);
		
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
	
	
	private static void parseVenue(Event evt){
		try {
			JSONObject loc = new JSONObject(evt.venue);
		
			try{
				evt.location_city = loc.getString("longitude");
				evt.location_city = loc.getString("latitude");
			}catch(JSONException e){
				Logger.l(Logger.DEBUG,LOG_TAG, e.getMessage());
			}
			try{
				evt.location_city = loc.getString("city");
				evt.location_state = loc.getString("state");
				evt.location_country = loc.getString("country");
				evt.location_street = loc.getString("street");
			}catch(JSONException e){
				Logger.l(Logger.DEBUG,LOG_TAG, e.getMessage());
			}
			
		} catch (JSONException e) {		
			e.printStackTrace();
		}
		
	}
	
	
	// PARCELABLE IMPLEMENTATION
		
	public static final Creator CREATOR = new Creator();

	public static final String XTRA_PARCELABLE_OBJECT = Event.class.getCanonicalName();


	static class Creator implements Parcelable.Creator<Event> {

		@Override
		public Event createFromParcel(Parcel source) {
			Event e = new Event();

			e.eid = source.readLong();
			e.name = source.readString();
			e.tagline = source.readString();
			e.nid = source.readInt();
			e.pic_small = source.readString();
			e.pic_big = source.readString();
			e.pic = source.readString();
			e.host = source.readString();
			e.description = source.readString();
			e.event_type = source.readString();
			e.event_subtype = source.readString();
			e.start_time = source.readLong();
			e.end_time = source.readLong();
			e.creator = source.readString();
			e.update_time = source.readLong();
			e.location = source.readString();
			e.venue = source.readString();
			e.privacy = source.readString();
			e.hide_guest_list = source.readByte() == 1;
			e.rsvp_status = source.readString();

			Event.parseVenue(e);
			
			return e;
		}

		@Override
		public Event[] newArray(int size) {
			return null;
		}

	};

	@Override
	public int describeContents() {	
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {

		dest.writeLong(eid);
		dest.writeString(name);
		dest.writeString(tagline);
		dest.writeInt(nid);
		dest.writeString(pic_small);
		dest.writeString(pic_big);
		dest.writeString(pic);
		dest.writeString(host);
		dest.writeString(description);
		dest.writeString(event_type);
		dest.writeString(event_subtype);
		dest.writeLong(start_time);
		dest.writeLong(end_time);
		dest.writeString(creator);
		dest.writeLong(update_time);
		dest.writeString(location);
		dest.writeString(venue);
		dest.writeString(privacy);
		dest.writeByte(hide_guest_list ? (byte) 1 : (byte) 0);
		dest.writeString(rsvp_status);

	}

	
	public String toString(){
		
		
		return  
		", eid:"+eid+
		", name: "+name+
		", tagline: "+tagline+
		", nid: "+nid+
		", pic_small: "+pic_small+
		", pic_big: "+pic_big+
		", pic: "+pic+
		", host: "+host+
		", description: "+description+
		", event_type: "+event_type+
		", event_subtype: "+event_subtype+
		", start_time: "+start_time+
		", end_time: "+end_time+
		", creator: "+creator+
		", update_time: "+update_time+
		
		", location:"+location+
		", venue:"+venue+
		", privacy:"+privacy+
		", rsvp: "+rsvp_status;
		

	
		
	}
	
	
	
}