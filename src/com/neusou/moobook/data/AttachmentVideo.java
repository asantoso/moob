package com.neusou.moobook.data;

import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.Logger;

import android.os.Parcel;
import android.os.Parcelable;

public class AttachmentVideo extends AttachmentMedia implements Parcelable{
	
	public static final byte TYPE = VIDEO;
	public static final String NAME = "VIDEO";
	
	public String mOwner;
	public long mCreatedTime;
	public String mSourceType;
	public String mDisplayUrl;
	public String mSourceUrl;
	
	public static final String fields_video = "video";
	public static final String fields_owner = "owner";
	public static final String fields_source_type = "source_type";
	public static final String fields_created_time = "created_time";
	public static final String fields_permalink = "permalink";
	public static final String fields_display_url = "display_url";
	public static final String fields_source_url = "source_url";
	
	final static String LOG_TAG = "AttachmentVideo";
	
	public static AttachmentVideo parseJson(String jsonData, AttachmentVideo ap)
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
	
	public static AttachmentVideo parseJson(JSONObject mediaJson, AttachmentVideo ap) 
	throws AttachmentMediaUnparsableException{
		//Logger.l(Logger.DEBUG, LOG_TAG, mediaJson.toString());
		
		try{
			if(ap == null){
				ap = new AttachmentVideo();
			}			
			JSONObject videoJson = mediaJson.getJSONObject(fields_video);
						
			ap.mOwner = videoJson.getString(fields_owner);
			ap.mCreatedTime = videoJson.getLong(fields_created_time);
			ap.mSourceType = videoJson.getString(fields_source_type);
			ap.mDisplayUrl = videoJson.getString(fields_display_url);
			ap.mSourceUrl = videoJson.getString(fields_source_url);
			ap.mSourceType = videoJson.getString(fields_source_type);
			return ap;
		}catch(JSONException e){	
			throw new AttachmentMediaUnparsableException(e.getMessage());
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mOwner);
		dest.writeLong(mCreatedTime);
		dest.writeString(mSourceType);
		dest.writeString(mDisplayUrl);
		dest.writeString(mSourceUrl);
	}
	
	public static Parcelable.Creator<AttachmentVideo> CREATOR = new Creator<AttachmentVideo>() {
		
		@Override
		public AttachmentVideo[] newArray(int size) {
			return new AttachmentVideo[size];
		}
		
		@Override
		public AttachmentVideo createFromParcel(Parcel source) {
			AttachmentVideo obj = new AttachmentVideo();
			obj.mOwner = source.readString();
			obj.mCreatedTime = source.readLong();
			obj.mSourceType = source.readString();
			obj.mDisplayUrl = source.readString();
			obj.mSourceUrl = source.readString();			
			return obj;
		}
	};
}