/*
package com.neusou.moobook.data;


import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;

public class Profile {

	public static final short col_id = 1 ;
	public static final short col_name = 2 ;
	public static final short col_pic = 3 ;
	public static final short col_pic_square = 4;

	public long id ;
	public String name;
	public String pic;
	public String pic_square;

	static final short TOTAL = 5;
	public static final String columnNames[] = new String[]{
	 "rowid",
	 "id",	
	 "name",
	 "pic",
	 "pic_square",	 
	 } ;
	
	
	public static Profile parseCursor(Cursor c, Profile u){
		
		if(u == null){
			u = new Profile();
		}
		
		u.id = c.getLong(col_id);
		u.name = c.getString(col_name);
		u.pic = c.getString(col_pic);
		u.pic_square = c.getString(col_pic_square);
		
		return u;
	}
	
	
	public ContentValues toContentValues(ContentValues cache){
		if(cache == null){
			cache = new ContentValues();
		}
		
		for(int i=0;i<TOTAL;i++){
			doInsertValuesToCV(cache, (short) i);
		}
		
		return cache;
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
			case col_id:{cv.put(columnNames[var],id);break;}
			case col_name:{cv.put(columnNames[var],name);break;}
			case col_pic:{cv.put(columnNames[var],pic);break;}
			case col_pic_square:{cv.put(columnNames[var],pic_square);break;}
		
		}
	}
	
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
	
	public void parse(JSONObject data){
		try{id = data.getLong("id");}catch(JSONException e){}
		try{name = data.getString("name");}catch(JSONException e){}		
		try{pic = data.getString("pic");}catch(JSONException e){}
		try{pic_square = data.getString("pic_square");}catch(JSONException e){}
	}
	
	public void parse(JSONObject data, short selection[]){		
		for(int i=0,num=selection.length;i<num;i++){
			short column = selection[i];
			doParse(column,data);
		}
	}
	
	private void doParse(short columnIndex, JSONObject data){
		case col_id:{	
			id = data.getLong(columnNames[columnIndex]);					
			break;
		}
		case col_name:{
			name = data.getString(columnNames[columnIndex]);
			break;
		}
		case col_pic:{
			pic = data.getString(columnNames[columnIndex]);					
			break;
		}		
		case col_pic_square:{
			pic_square = data.getString(columnNames[columnIndex]);
			break;
		}
		}
		}
		catch(JSONException e){			
		}	
		catch(IndexOutOfBoundsException e){			
		}
	}
	
}
*/