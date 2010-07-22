package com.neusou.moobook.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.FQL;

import android.os.Parcel;
import android.os.Parcelable;

public class Photo implements Parcelable{
	
	public static final String LOG_TAG = Logger.registerLog(Photo.class);
	public static final String XTRA_PARCELABLE_OBJECT = Parcelable.class.getName();
	
	public long mOwner;
	public String mPid;
	public String mAid;
	public long mObjectId;
	public String mLink;
	public String mCaption;
	public long mCreated;
	public long mModified;
	public int mHeight;
	public int mWidth;
	public String mSrc;
			
	public static final String fields_photo = "photo";
	public static final String fields_owner = "owner";
	public static final String fields_index = "index";
	public static final String fields_pid = "pid";
	public static final String fields_height = "height";
	public static final String fields_aid = "aid";
	public static final String fields_width = "width";
	public static final String fields_object_id = "object_id";
	public static final String fields_caption = "caption";
	public static final String fields_created = "created";
	public static final String fields_modified = "modified";
	/*
	
	[{"name":"photo","fql_result_set":[{"pid":"100000425751479_264896","aid":"100000425751479_6556","src_small":"http:\/\/photos-g.ak.fbcdn.net\/hphotos-ak-ash1\/hs421.ash1\/23357_130777473613110_100000425751479_264896_691310_t.jpg","link":"http:\/\/www.facebook.com\/photo.php?pid=264896&id=100000425751479","caption":"","created":1276782899,"modified":1276782900,"object_id":130777473613110}]}]

	*/
	 
	public static Photo parseJson(JSONObject photoObj , Photo cache)
	{
		if(cache == null){
			cache = new Photo();
		}
		
		try{	
			cache.mObjectId = photoObj.getLong(Photo.fields_object_id);
			cache.mPid = photoObj.getString(Photo.fields_pid);
			cache.mAid = photoObj.getString(Photo.fields_aid);
			cache.mCreated= photoObj.getLong(Photo.fields_created);
			cache.mModified = photoObj.getLong(Photo.fields_modified);
			cache.mCaption = photoObj.getString(Photo.fields_caption);
		}catch(JSONException e){
			
		}
		
		return cache;
	}
	
	@Override
	public int describeContents() {		
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(mOwner);
		dest.writeString(mPid);
		dest.writeString(mAid);
		dest.writeLong(mObjectId);
		dest.writeString(mLink);
		dest.writeString(mCaption);
		dest.writeLong(mCreated);
		dest.writeLong(mModified);
		dest.writeInt(mHeight);
		dest.writeInt(mWidth);
		dest.writeString(mSrc);
	}
	
	public static Parcelable.Creator<Photo> CREATOR = new Creator<Photo>() {
		
		@Override
		public Photo[] newArray(int size) {
			return new Photo[size];
		}
		
		@Override
		public Photo createFromParcel(Parcel source) {
			Photo obj = new Photo();
			obj.mOwner = source.readLong();
			obj.mPid = source.readString();
			obj.mAid = source.readString();
			obj.mObjectId = source.readLong();
			obj.mLink = source.readString();
			obj.mCaption = source.readString();
			obj.mCreated = source.readLong();
			obj.mModified = source.readLong();
			obj.mHeight = source.readInt();
			obj.mWidth = source.readInt();
			obj.mSrc = source.readString();
			return obj;
		}
	};
	
}