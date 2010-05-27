package com.neusou.moobook.data;

import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.Logger;

import android.os.Parcel;
import android.os.Parcelable;

public class AttachmentMusic extends AttachmentMedia implements Parcelable{
	
	public static final byte TYPE = MUSIC;
	public static final String NAME = "Music";
		
	public String title;
	public String album;
	public String artist;
	public String source_url;
	
	public static final String fields_title = "title";
	public static final String fields_album = "album";
	public static final String fields_artist = "artist";
	public static final String fields_source_url = "source_url";
	
	final static String LOG_TAG = "AttachmentMusic";
	
	public static AttachmentMusic parseJson(String jsonData, AttachmentMusic ap)
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
	
	public static AttachmentMusic parseJson(JSONObject data, AttachmentMusic ap) 
	throws AttachmentMediaUnparsableException{
		Logger.l(Logger.DEBUG, LOG_TAG, data.toString());
		try{
			if(ap == null){
				ap = new AttachmentMusic();
			}
			ap.title = data.getString(fields_title);
			ap.album = data.getString(fields_album);
			ap.artist = data.getString(fields_artist);
			ap.source_url = data.getString(fields_source_url);
			
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
		dest.writeString(title);
		dest.writeString(album);
		dest.writeString(artist);
		dest.writeString(source_url);
	}
	
	public static Parcelable.Creator<AttachmentMusic> CREATOR = new Creator<AttachmentMusic>() {
		
		@Override
		public AttachmentMusic[] newArray(int size) {
			return new AttachmentMusic[size];
		}
		
		@Override
		public AttachmentMusic createFromParcel(Parcel source) {
			AttachmentMusic obj = new AttachmentMusic();
			obj.title = source.readString();
			obj.album = source.readString();
			obj.artist = source.readString();
			obj.source_url = source.readString();
			return obj;
		}
	};
}