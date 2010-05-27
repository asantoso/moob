package com.neusou.moobook.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;

public class FriendList {

	public static final short  col_rowid = 0;
	public static final short  col_flid = 1;
	public static final short  col_uid = 2;
	public static final short  col_name = 3;
	
	public static final String  cn_rowid = "friendlist_rowid";
	public static final String  cn_flid = "flid";
	public static final String  cn_uid = "uid";
	public static final String  cn_name = "name";
	
	
	public String flid ;
	public long uid;
	public String name;
	
	static final short TOTAL = 4;
	
	
	public static FriendList parseCursor(Cursor c, FriendList u){
		
		if(u == null){
			u = new FriendList();
		}
		
		u.flid = c.getString(col_flid);
		u.name = c.getString(col_name);
		u.uid = c.getLong(col_uid);
		return u;
	}
	
	
	public ContentValues toContentValues(ContentValues cache){
		if(cache == null){
			cache = new ContentValues();
		}
	
		
		cache.put(cn_flid, flid);
		cache.put(cn_uid, uid);
		cache.put(cn_name, name);

		
		return cache;
	}
	
	public void parse(JSONObject data){		
		try{name = data.getString("name");}catch(JSONException e){}		
		try{uid= data.getLong("uid");}catch(JSONException e){}
		try{flid = data.getString("flid");}catch(JSONException e){}
	}
	
}