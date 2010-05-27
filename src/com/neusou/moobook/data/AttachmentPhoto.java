package com.neusou.moobook.data;

import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.Logger;

import android.os.Parcel;
import android.os.Parcelable;

public class AttachmentPhoto extends AttachmentMedia implements Parcelable{
	
	public static final byte TYPE = PHOTO;
	public static final String NAME = "PHOTO";
	
	public String mOwner;
	public int mIndex;
	public String mPid;
	public int mHeight;
	public int mWidth;
	public String mAid;		

	public static final String fields_photo = "photo";
	public static final String fields_owner = "owner";
	public static final String fields_index = "index";
	public static final String fields_pid = "pid";
	public static final String fields_height = "height";
	public static final String fields_aid = "aid";
	public static final String fields_width = "width";
	
	final static String LOG_TAG = "AttachmentPhoto";
	
	public static AttachmentPhoto parseJson(String jsonData, AttachmentPhoto ap)
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
	
	public static AttachmentPhoto parseJson(JSONObject mediaJson, AttachmentPhoto ap) 
	throws AttachmentMediaUnparsableException{
		//Logger.l(Logger.DEBUG, LOG_TAG, mediaJson.toString());
		try{
			if(ap == null){
				ap = new AttachmentPhoto();
			}
			
			JSONObject photoJson = mediaJson.getJSONObject(fields_photo);
			ap.mOwner = photoJson.getString(fields_owner);
			ap.mIndex = photoJson.getInt(fields_index);
			ap.mPid = photoJson.getString(fields_pid);
			ap.mHeight = photoJson.getInt(fields_height);
			ap.mWidth = photoJson.getInt(fields_width);
			ap.mAid = photoJson.getString(fields_aid);
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
		dest.writeInt(mIndex);
		dest.writeString(mPid);
		dest.writeInt(mHeight);
		dest.writeInt(mWidth);
		dest.writeString(mAid);
	}
	
	public static Parcelable.Creator<AttachmentPhoto> CREATOR = new Creator<AttachmentPhoto>() {
		
		@Override
		public AttachmentPhoto[] newArray(int size) {
			return new AttachmentPhoto[size];
		}
		
		@Override
		public AttachmentPhoto createFromParcel(Parcel source) {
			AttachmentPhoto obj = new AttachmentPhoto();
			obj.mOwner = source.readString();
			obj.mIndex = source.readInt();
			obj.mPid = source.readString();
			obj.mHeight = source.readInt();
			obj.mWidth = source.readInt();
			obj.mAid = source.readString();
			return obj;
		}
	};
	
}