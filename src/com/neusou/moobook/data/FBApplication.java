package com.neusou.moobook.data;

import android.content.ContentValues;
import android.database.Cursor;

public class FBApplication {
	
	public long app_id;
	public String display_name;
	public String company_name;	
	public String icon_url;
	public String logo_url;
	public boolean _isHidden; //internal moobook only
	
	//public static final String cn_rowid = "rowid"; //internal moobook only
	public static final String cn_app_id = "app_id"; 
	public static final String cn_display_name = "display_name";
	public static final String cn_company_name = "company_name";	
	public static final String cn_icon_url = "icon_url";
	public static final String cn_logo_url = "logo_url";
	public static final String cn_isHidden = "_isHidden";  //internal moobook only
	
	//public static final short col_rowid = 0;  //internal moobook only
	public static final short col_app_id = 0;
	public static final short col_display_name = 1;  	
	public static final short col_company_name = 2;
	public static final short col_icon_url= 3; 	
	public static final short col_logo_url= 4; 		  	
	public static final short col_isHidden = 5;  //internal moobook only
	
	public static final short TOTAL_COLUMNS = 6;

	public static FBApplication parseCursor(Cursor c, FBApplication s){
		
		if(c == null){
			return null;
		}
		
		if(s == null){
			s = new FBApplication();
		}		
		
		s.app_id = c.getLong(col_app_id);
		s.display_name= c.getString(col_display_name);
		s.company_name= c.getString(col_company_name);
		s.icon_url= c.getString(col_icon_url);
		s.logo_url= c.getString(col_logo_url);
		s._isHidden = c.getInt(col_isHidden)==1?true:false;
		
		return s;
	}
	
	public ContentValues toContentValues(ContentValues cv){
		
		if(cv == null){
			cv = new ContentValues();
		}
		
		cv.put(cn_app_id, app_id);
		cv.put(cn_display_name, display_name);
		cv.put(cn_company_name, company_name);
		cv.put(cn_icon_url, icon_url);
		cv.put(cn_logo_url, logo_url);
		cv.put(cn_isHidden, _isHidden);
		
		return cv;
	}
			
}