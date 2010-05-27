package com.neusou.moobook.data;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;

public class Page{

	public static final short col_page_id = 1 ;
	public static final short col_name = 2 ;
	public static final short col_pic_small = 3 ;
	public static final short col_pic_square = 4 ;
	
	public long page_id ;
	public String name ;
	public String pic_small ;	
	public String pic_square ;
	
	
	public static final String columnNames[] = new String[]{
	 "rowid",
	 "page_id",	
	 "name",
	 "pic_small",
	 "pic_square"
	 
	 } ;
	
	
	public static Page parseCursor(Cursor c, Page u){
		
		if(u == null){
			u = new Page();
		}
		
		u.page_id = c.getLong(col_page_id);
		u.name = c.getString(col_name);
		u.pic_square = c.getString(col_pic_square);
		
		return u;
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
			case col_page_id:{cv.put(columnNames[var], page_id);break;}
			case col_name:{cv.put(columnNames[var], name);break;}
			case col_pic_small:{cv.put(columnNames[var], pic_small);break;}
			case col_pic_square:{cv.put(columnNames[var], pic_square);break;}
			
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
	
	public void parse(JSONObject data, short selection[]){		
		for(int i=0,num=selection.length;i<num;i++){
			short column = selection[i];
			doParse(column,data);
		}
	}
	
	private void doParse(short columnIndex, JSONObject data){
		try{
		switch(columnIndex){
		case col_page_id:{	
			page_id = data.getLong(columnNames[columnIndex]);					
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