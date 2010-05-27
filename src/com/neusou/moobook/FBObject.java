package com.neusou.moobook;

public class FBObject {
	public byte type = type_unknown;
	public String id = null; 
	
	public static final String event_idname = "eid";
	public static final byte type_event = 1;
	public static final byte type_unknown = 0;		

	public FBObject(byte type, String id){
		this.type = type;
		this.id = id;
	}
	
	public static FBObject parseFromIdName(String idName, String idValue){
		if(event_idname.equals(idName)){
			return new FBObject(type_event, idValue);
		}
		return null;
	}
	
}