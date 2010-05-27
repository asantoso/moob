package com.neusou.moobook.data;

import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.Logger;

import android.os.Parcel;
import android.os.Parcelable;

public class AttachmentLink extends AttachmentMedia implements Parcelable{
	
	public static final byte TYPE = LINK;
	public static final String NAME = "Link";
	final static String LOG_TAG = "AttachmentLink";
	
	public static AttachmentLink parseJson(String jsonData, AttachmentLink ap)
	throws AttachmentMediaUnparsableException{
		if(jsonData == null){
			return null;
		}		
		try{
			JSONObject data = new JSONObject(jsonData);
			return parseJson(data, ap);
		}catch(JSONException e){
			throw new AttachmentMediaUnparsableException();
		}			
	}

	public static AttachmentLink parseJson(JSONObject jsonObj, AttachmentLink ap)
	throws AttachmentMediaUnparsableException{
		//Logger.l(Logger.DEBUG, LOG_TAG, jsonObj.toString());
		
		if(jsonObj == null){
			return null;
		}
		
		try{
			if(ap == null){
				ap = new AttachmentLink();
			}
			
			ap.href = jsonObj.getString(fields_href);
			ap.src = jsonObj.getString(fields_src);
			
			return ap;
		}catch(JSONException e){	
			throw new AttachmentMediaUnparsableException();
		}
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(href);
		dest.writeString(src);
	}
	
	public static Parcelable.Creator<AttachmentLink> CREATOR = new Creator<AttachmentLink>() {
		
		@Override
		public AttachmentLink[] newArray(int size) {
			return new AttachmentLink[size];
		}
		
		@Override
		public AttachmentLink createFromParcel(Parcel source) {
			AttachmentLink obj = new AttachmentLink();
			obj.href = source.readString();
			obj.src = source.readString();
			return obj;
		}
	};
	
}