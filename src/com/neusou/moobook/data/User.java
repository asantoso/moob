package com.neusou.moobook.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.StaleDataException;

/**
 * Represents Facebook User and Facebook Page
 * @author asantoso
 *
 */
public class User {

	public static final short col_rowid = 0;
	public static final short col_uid = 1 ;
	public static final short col_about_me = 2 ;
	public static final short col_activities = 3 ;
	public static final short col_affiliations = 4 ;
	public static final short col_birthday = 5 ;
	public static final short col_birthday_date = 6 ; 
	public static final short col_books  = 7 ;
	public static final short col_current_location = 8 ; 
	public static final short col_education_history  = 9 ;
	public static final short col_first_name  = 10 ;
	
	public static final short col_has_added_app = 11 ;
	public static final short col_hometown_location = 12 ;
	public static final short col_hs_info = 13 ;
	public static final short col_interests = 14 ;
	public static final short col_is_app_user = 15 ;
	public static final short col_is_blocked = 16 ;
	public static final short col_last_name = 17 ;
	public static final short col_locale = 18 ;
	public static final short col_meeting_for = 19 ;
	public static final short col_meeting_sex = 20 ;
	
	public static final short col_movies = 21 ;
	public static final short col_music = 22 ;
	public static final short col_name = 23 ;
	public static final short col_notes_count = 24 ; 
	public static final short col_pic = 25 ;
	public static final short col_pic_with_logo = 26 ;
	public static final short col_pic_big = 27 ;
	public static final short col_pic_big_with_logo = 28 ; 
	public static final short col_pic_small = 29 ;
	public static final short col_pic_small_with_logo = 30 ;
	
	public static final short col_pic_square = 31 ;
	public static final short col_pic_square_with_logo = 32 ;
	public static final short col_political = 33 ;
	public static final short col_profile_blurb = 34 ; 
	public static final short col_profile_update_time = 35 ;
	public static final short col_proxied_email = 36 ;
	public static final short col_quotes = 37 ;
	public static final short col_relationship_status = 38 ; 
	public static final short col_religion = 39 ;	
	public static final short col_sex = 40 ;
	
	public static final short col_significant_other_id = 41 ;
	public static final short col_status = 42 ;
	public static final short col_timezone = 43 ;
	public static final short col_tv = 44 ;
	public static final short col_wall_count = 45 ;
	public static final short col_website = 46 ;	
	public static final short col_type = 47 ;
	
	public static final short TOTAL_COLUMNS = 48; 
	
	//
	public static final byte TYPE_USER = 0;
	public static final byte TYPE_PAGE = 1;
	//
	
	public long uid ;
	public String about_me ;
	public String activities ;
	public String affiliations ;
	public String birthday ;
	public String birthday_date ; 
	public String books ;
	public String current_location ; 
	public String education_history ;
	public String first_name ;
	public String has_added_app ;
	public String hometown_location ;
	public String hs_info ;
	public String interests ; 
	public String is_app_user ;
	public String is_blocked ;
	public String last_name ;
	public String locale ;
	public String meeting_for ; 
	public String meeting_sex ;
	public String movies ;
	public String music ;
	public String name ;
	public String notes_count ; 
	public String online_presence; //not saved
	public String pic ;
	public String pic_with_logo ;
	public String pic_big ;
	public String pic_big_with_logo ; 
	public String pic_small ;
	public String pic_small_with_logo ;
	public String pic_square ;
	public String pic_square_with_logo ;
	public String political ;
	public String profile_blurb ; 
	public long profile_update_time ;
	public String proxied_email ;
	public String quotes ;
	public String relationship_status ; 
	public String religion ;
	public String sex ;
	public long significant_other_id ;
	public String status ;
	public String timezone ;
	public String tv ;
	public String wall_count ;
	public String website ;
	
	// application specific data
	public byte type;
	
	public void clear(){
		uid = 0;
		about_me = null;
		activities = null ;
		affiliations = null ;
		birthday = null ;
		birthday_date = null ;
		books = null ;
		current_location = null ;
		education_history= null ;
		first_name = null ;
		has_added_app = null ;
		hometown_location = null ;
		hs_info = null ;
		interests = null ; 
		is_app_user = null ;
		is_blocked = null ;
		last_name= null ;
		locale = null ;
		meeting_for = null ;
		meeting_sex = null ;
		movies = null ;
		music = null ;
		name = null ;
		notes_count = null ;
		online_presence = null ; //this data is not saved in the disk.
		pic = null ;
		pic_with_logo = null ;
		pic_big = null ;
		pic_big_with_logo = null ; 
		pic_small = null ;
		pic_small_with_logo = null ;
		pic_square = null ;
		pic_square_with_logo= null ;
		political = null ;
		profile_blurb = null ;
		profile_update_time  = 0;
		proxied_email = null ;
		quotes = null;
		relationship_status = null ; 
		religion = null ;
		sex = null ;
		significant_other_id = 0;
		status = null ;
		timezone = null ;
		tv = null ;
		wall_count = null ;
		website = null ;
	}
		
	public static final String columnNames[] = new String[]{
	 "user_rowid",
	 
	 "uid",	
	 "about_me",
	 "activities",
	 "affiliations",
	 "birthday",
	 "birthday_date", 
	 "books",
	 "current_location", 
	 "education_history",	 
	 "first_name",
	 
	 "has_added_app",
	 "hometown_location",
	 "hs_info",
	 "interests", 
	 "is_app_user",
	 "is_blocked",
	 "last_name",
	 "locale",
	 "meeting_for",	 
	 "meeting_sex",
	 
	 "movies",
	 "music",
	 "name",
	 "notes_count",
	 "pic",
	 "pic_with_logo",
	 "pic_big",
	 "pic_big_with_logo", 
	 "pic_small",
	 "pic_small_with_logo",
	 
	 "pic_square",
	 "pic_square_with_logo",
	 "political",
	 "profile_blurb", 
	 "profile_update_time",
	 "proxied_email",
	 "quotes",
	 "relationship_status", 
	 "religion",	 
	 "sex",
	 
	 "significant_other_id",
	 "status",
	 "timezone",
	 "tv",
	 "wall_count",
	 "website",
	 "type"
	 } ;
	
	
	public static User parseCursorLight(Cursor c, User u){
		if(u == null){
			u = new User();
		}
		
		u.uid = c.getLong(col_uid);		
		
		u.birthday_date = c.getString(col_birthday_date);
		u.current_location = c.getString(col_current_location);
		u.hometown_location = c.getString(col_hometown_location);		
		u.locale = c.getString(col_locale);
		
		u.name = c.getString(col_name);
		u.pic = c.getString(col_pic); 
		u.pic_square = c.getString(col_pic_square);		
		u.profile_update_time = c.getLong(col_profile_update_time);
		u.proxied_email = c.getString(col_proxied_email);		
		u.relationship_status = c.getString(col_relationship_status);
		
		u.timezone = c.getString(col_timezone);
		
		return u;
	}
		
	public static User parseCursor(Cursor c, User u){
		
		if(u == null){
			u = new User();
		}
				
		u.uid = c.getLong(col_uid);
		u.about_me = c.getString(col_about_me);
		u.activities = c.getString(col_activities);
		u.affiliations = c.getString(col_affiliations);
		u.birthday = c.getString(col_birthday);
		u.birthday_date = c.getString(col_birthday_date);
		u.books = c.getString(col_books);
		u.current_location = c.getString(col_current_location);
		u.education_history = c.getString(col_education_history);
		u.first_name = c.getString(col_first_name);
		
		u.has_added_app = c.getString(col_has_added_app);		
		u.hometown_location = c.getString(col_hometown_location);
		u.hs_info = c.getString(col_hs_info);
		u.interests = c.getString(col_interests);
		u.is_app_user = c.getString(col_is_app_user);
		u.is_blocked = c.getString(col_is_blocked);		
		u.last_name = c.getString(col_last_name);
		u.locale = c.getString(col_locale);
		u.meeting_for = c.getString(col_meeting_for);
		u.meeting_sex = c.getString(col_meeting_sex);
		
		u.movies = c.getString(col_movies);
		u.music = c.getString(col_music);
		u.name = c.getString(col_name);
		u.notes_count = c.getString(col_notes_count);	
		u.pic = c.getString(col_pic); 
		u.pic_with_logo = c.getString(col_pic_with_logo);
		u.pic_big = c.getString(col_pic_big);
		u.pic_big_with_logo = c.getString(col_pic_big_with_logo);
		u.pic_small = c.getString(col_pic_small);
		u.pic_small_with_logo = c.getString(col_pic_small_with_logo);
		u.pic_square = c.getString(col_pic_square);
		
		u.pic_square_with_logo = c.getString(col_pic_square_with_logo);		
		u.political = c.getString(col_political);		
		u.profile_blurb = c.getString(col_profile_blurb);
		u.profile_update_time = c.getLong(col_profile_update_time);
		u.proxied_email = c.getString(col_proxied_email);		
		u.quotes = c.getString(col_quotes);
		u.relationship_status = c.getString(col_relationship_status);
		u.religion = c.getString(col_religion);
		u.sex = c.getString(col_sex);
		u.significant_other_id = c.getLong(col_significant_other_id);
		
		u.status = c.getString(col_status);
		u.timezone = c.getString(col_timezone);
		u.tv = c.getString(col_tv);
		u.wall_count = c.getString(col_wall_count);
		u.website = c.getString(col_website);
		u.type = (byte) c.getInt(col_type);		
	
		return u;
	}
	
	
	public void parseCursor(Cursor c, short[] selection) throws StaleDataException, IllegalStateException{
		for(int i=0,N=selection.length;i<N;i++){
			doInsertValuesToInstance(selection[i], i, c);			
		}	
	}

	public ContentValues toContentValues(ContentValues cache, short selection[]){
		if(cache == null){
			cache = new ContentValues();
		}
		
		for(int i=0,num=selection.length;i<num;i++){
			doInsertValuesToCV(cache, selection[i]);
		}
		
		return cache;
	}
	
	private void doInsertValuesToCV(ContentValues cv, short var){
			
		switch(var){
			case col_uid:{cv.put(columnNames[var],uid);break;}
			case col_about_me:{cv.put(columnNames[var],about_me);break;}
			case col_activities:{cv.put(columnNames[var],activities);break;}
			case col_affiliations:{cv.put(columnNames[var],affiliations);break;}
			case col_birthday:{cv.put(columnNames[var],birthday);break;}
			case col_birthday_date:{cv.put(columnNames[var],birthday_date);break;}
			case col_books:{cv.put(columnNames[var],books);break;}
			case col_current_location:{cv.put(columnNames[var],current_location);break;}
			case col_education_history:{cv.put(columnNames[var],education_history);break;}
			case col_first_name:{cv.put(columnNames[var],first_name);break;}
			case col_has_added_app:{cv.put(columnNames[var],has_added_app);break;}
			case col_hometown_location:{cv.put(columnNames[var],hometown_location);break;}
			case col_hs_info:{cv.put(columnNames[var],hs_info);break;}
			case col_interests:{cv.put(columnNames[var],interests);break;}
			case col_is_app_user:{cv.put(columnNames[var],is_app_user);break;}
			case col_is_blocked:{cv.put(columnNames[var],is_blocked);break;}
			case col_last_name:{cv.put(columnNames[var],last_name);break;}
			case col_locale:{cv.put(columnNames[var],locale);break;}
			case col_meeting_for:{cv.put(columnNames[var],meeting_for);break;}
			case col_meeting_sex:{cv.put(columnNames[var],meeting_sex);break;}
			case col_movies:{cv.put(columnNames[var],movies);break;}
			case col_music:{cv.put(columnNames[var],music);break;}
			case col_name:{cv.put(columnNames[var],name);break;}
			case col_notes_count:{cv.put(columnNames[var],notes_count);break;}
			case col_pic:{cv.put(columnNames[var],pic);break;}
			case col_pic_big:{cv.put(columnNames[var],pic_big);break;}
			case col_pic_big_with_logo:{cv.put(columnNames[var],pic_big_with_logo);break;}
			case col_pic_small:{cv.put(columnNames[var],pic_small);break;}
			case col_pic_small_with_logo:{cv.put(columnNames[var],pic_small_with_logo);break;}
			case col_pic_square:{cv.put(columnNames[var],pic_square);break;}
			case col_pic_square_with_logo:{cv.put(columnNames[var],pic_square_with_logo);break;}
			case col_pic_with_logo:{cv.put(columnNames[var],pic_with_logo);break;}
			case col_political:{cv.put(columnNames[var],political);break;}
			case col_profile_blurb:{cv.put(columnNames[var],profile_blurb);break;}
			case col_profile_update_time:{cv.put(columnNames[var],profile_update_time);break;}
			case col_proxied_email:{cv.put(columnNames[var],proxied_email);break;}
			case col_quotes:{cv.put(columnNames[var],quotes);break;}
			case col_relationship_status:{cv.put(columnNames[var],relationship_status);break;}
			case col_religion:{cv.put(columnNames[var],religion);break;}
			case col_sex:{cv.put(columnNames[var],sex);break;}
			case col_significant_other_id:{cv.put(columnNames[var],significant_other_id);break;}
			case col_status:{cv.put(columnNames[var],status);break;}
			case col_timezone:{cv.put(columnNames[var],timezone);break;}
			case col_tv:{cv.put(columnNames[var],tv);break;}
			case col_wall_count:{cv.put(columnNames[var],wall_count);break;}
			case col_website:{cv.put(columnNames[var],website);break;}
			case col_type:{cv.put(columnNames[var],type);break;}
		}
		
	}
	
	/**
	 * 
	 * @param var The code enumeration value of the field name (without any reference to the supplied cursor)
	 * @param i the position of the field name in the supplied cursor
	 * @param c the cursor
	 */
	private void doInsertValuesToInstance(short var, int i, Cursor c) throws StaleDataException, IllegalStateException{
		
		switch(var){
			case col_uid:{uid=c.getLong(i);break;}
			case col_about_me:{about_me=c.getString(i);break;}
			case col_activities:{activities=c.getString(i);break;}
			case col_affiliations:{affiliations=c.getString(i);break;}
			case col_birthday:{birthday=c.getString(i);break;}
			case col_birthday_date:{birthday_date=c.getString(i);break;}
			case col_books:{books=c.getString(i);break;}
			case col_current_location:{current_location=c.getString(i);break;}
			case col_education_history:{education_history=c.getString(i);break;}
			case col_first_name:{first_name=c.getString(i);break;}
			case col_has_added_app:{has_added_app=c.getString(i);break;}
			case col_hometown_location:{hometown_location=c.getString(i);break;}
			case col_hs_info:{hs_info=c.getString(i);break;}
			case col_interests:{interests=c.getString(i);break;}
			case col_is_app_user:{is_app_user=c.getString(i);break;}
			case col_is_blocked:{is_blocked=c.getString(i);break;}
			case col_last_name:{last_name=c.getString(i);break;}
			case col_locale:{locale=c.getString(i);break;}
			case col_meeting_for:{meeting_for=c.getString(i);break;}
			case col_meeting_sex:{meeting_sex=c.getString(i);break;}
			case col_movies:{movies=c.getString(i);break;}
			case col_music:{music=c.getString(i);break;}
			case col_name:{name=c.getString(i);break;}
			case col_notes_count:{notes_count=c.getString(i);break;}
			case col_pic:{pic=(String)c.getString(i);break;}
			case col_pic_big:{pic_big=c.getString(i);break;}
			case col_pic_big_with_logo:{pic_big_with_logo=c.getString(i);break;}
			case col_pic_small:{pic_small=c.getString(i);break;}
			case col_pic_small_with_logo:{pic_small_with_logo=c.getString(i);break;}
			case col_pic_square:{pic_square=(String)c.getString(i);break;}
			case col_pic_square_with_logo:{pic_square_with_logo=c.getString(i);break;}
			case col_pic_with_logo:{pic_with_logo=c.getString(i);break;}
			case col_political:{political=c.getString(i);break;}
			case col_profile_blurb:{profile_blurb=c.getString(i);break;}
			case col_profile_update_time:{profile_update_time=c.getLong(i);break;}
			case col_proxied_email:{proxied_email=c.getString(i);break;}
			case col_quotes:{quotes=(String)c.getString(i);break;}
			case col_relationship_status:{relationship_status=c.getString(i);break;}
			case col_religion:{religion=c.getString(i);break;}
			case col_sex:{sex=c.getString(i);break;}
			case col_significant_other_id:{significant_other_id=c.getInt(i);break;}
			case col_status:{status=c.getString(i);break;}
			case col_timezone:{timezone=c.getString(i);break;}
			case col_tv:{tv=c.getString(i);break;}
			case col_wall_count:{wall_count=c.getString(i);break;}
			case col_website:{website=c.getString(i);break;}
			case col_type:{type=(byte)c.getShort(i);break;}
		}
	}
	
	
	/**
	 * 
	 * @param selection array of code values of the field names we want to retrieve from the table.
	 * @return a string of field names separated by a comma
	 */
	public static String createColumnNames(short[] selection){
		StringBuilder sb = new StringBuilder();
		for(int i=0,num=selection.length;i<num;i++){
			sb.append(columnNames[selection[i]]);
			if(i<num-1){
				sb.append(",");
			}
		}
		return sb.toString();		
	}
	
	public void parse(JSONObject data, short selection[]){		
		for(int i=0,num=selection.length;i<num;i++){
			short column = selection[i];
			doParse(column,data);
		}
	}
	
	private void doParse(short columnIndex, JSONObject data){
		try{
		switch(columnIndex){
		case col_uid:{	
			uid = data.getLong(columnNames[columnIndex]);					
			break;
		}
		case col_name:{
			name = data.getString(columnNames[columnIndex]);
			break;
		}
		case col_first_name:{
			first_name = data.getString(columnNames[columnIndex]);
			break;
		}
		case col_last_name:{
			last_name = data.getString(columnNames[columnIndex]);
			break;
		}
		case col_profile_update_time:{
			profile_update_time = data.getLong(columnNames[columnIndex]);					
			break;
		}
		case col_hometown_location:{
			hometown_location = data.getString(columnNames[columnIndex]);					
			break;
		}
		case col_current_location:{
			current_location = data.getString(columnNames[columnIndex]);					
			break;
		}
		case col_relationship_status:{
			relationship_status = data.getString(columnNames[columnIndex]);					
			break;
		}
		case col_birthday_date:{
			birthday_date = data.getString(columnNames[columnIndex]);
			break;
		}
		case col_profile_blurb:{
			profile_blurb = data.getString(columnNames[columnIndex]);
			break;
		}
		case col_pic:{
			pic = data.getString(columnNames[columnIndex]);
			pic = nonullstring(pic);
			break;
		}
		case col_pic_square:{
			pic_square = data.getString(columnNames[columnIndex]);
			pic_square = nonullstring(pic_square);
			break;
		}
		case col_pic_small:{
			pic_small = data.getString(columnNames[columnIndex]);
			pic_small = nonullstring(pic_small);
			break;
		}
		case col_pic_big:{
			pic_big = data.getString(columnNames[columnIndex]);
			pic_big = nonullstring(pic_big);
			break;
		}
		case col_timezone:{
			timezone = data.getString(columnNames[columnIndex]);
			break;
		}
		case col_type:{
			type = (byte) data.getInt(columnNames[columnIndex]);
			break;
		}
		
		}
		}
		catch(JSONException e){			
		}	
		catch(IndexOutOfBoundsException e){			
		}
	}
	
	private String nonullstring(String d){
		if(d == null){
			return null;
		}
		if(d.compareTo("null")==0){
			return "";
		}
		return d;
	}
	
}