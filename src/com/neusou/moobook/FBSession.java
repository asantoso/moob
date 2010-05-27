package com.neusou.moobook;

public class FBSession {	
	public long uid = 0;
	public String sig = null;	
	public String key = null; 
	public String secret = null;
	public int expires = -1;	
	public String json = null; // the original session json object
	
	public void clear(){
		uid = 0;
		sig = null;
		key = null;
		secret = null;
		expires = -1;
		json = null;
	}
}