package com.neusou.moobook.data;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.Logger;
import com.neusou.util.Poolable;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

public class Attachment implements Parcelable, Poolable<Attachment>{
	final String LOG_TAG = "Attachment";

	public String toString(){
		StringBuffer sb = new StringBuffer();
		String mediaType = AttachmentMedia.getTypeName(mMediaType);
		String space = " ";		
		sb.append(mediaType).append(mIcon).append(space).append(mCaption).append(space)
		.append(mHref).append(space).append(mNumMedias);
		return sb.toString();
	}
	
	/*
	public static final byte ATTACHMENT_TYPE_MEDIALINK = 0;
	public static final byte ATTACHMENT_TYPE_MEDIAIMAGE = 1;
	public static final byte ATTACHMENT_TYPE_MEDIAVIDEO = 2;
	public static final byte ATTACHMENT_TYPE_MEDIAMP3 = 3;
	public static final byte ATTACHMENT_TYPE_NOTE = 4;
	*/
	/*
	public static final String MEDIATYPE_LINK = "link";
	public static final String MEDIATYPE_IMAGE = "photo";
	public static final String MEDIATYPE_FLASH = "video";
	public static final String MEDIATYPE_MP3 = "mp3";
	*/
	/*
	 public static final byte col_atid = 1;  
	 public static final byte col_post_id = 2;
	 public static final byte col_data = 3;
	 
	 public static final String cn_atid = "atid";  
	 public static final String cn_post_id = "post_id";
	 public static final String cn_data = "data"; 
	 */
	 public String atid;
	 public String post_id;
	 public String data; //raw json data

	 public void clear(){
		 atid =  null;
		 post_id = null;
		 data = null;
	 }
	 
	 /*
	 public static Attachment parseCursor(Cursor c, Attachment s){
		
		boolean valid = c.moveToNext();
		if(!valid){
			return null;
		}
		
		if(s == null){
			s = new Attachment();
		}
		
		s.atid = c.getString(col_atid);
		s.post_id = c.getString(col_post_id);
		s.data = c.getString(col_data);
		
		c.close();
		return s;
	}
	*/
	 
	 /*
	public ContentValues toContentValues(ContentValues cv){
		
		if(cv == null){
			cv = new ContentValues();
		}
		
		cv.put(cn_atid, atid);		
		cv.put(cn_post_id, post_id);
		cv.put(cn_data, data);		
		
		return cv;
	}
		*/
	public static final String fields_icon = "icon";
	public static final String fields_fb_object_type = "fb_object_type";
	public static final String fields_description = "description";
	public static final String fields_fb_object_id = "fb_object_id";
	public static final String fields_name = "name";
	public static final String fields_caption = "caption";
	public static final String fields_properties = "properties";
	public static final String fields_href = "href";
	public static final String fields_media = "media";
	
	public void parseJson(String jsonData){
		try{
			JSONObject data = new JSONObject(jsonData);
			try{
			mIcon = data.getString(fields_icon);
			}catch(JSONException e){}
			try{
			mFbObjectType = data.getString(fields_fb_object_type);
			}catch(JSONException e){}
			try{
			mFbObjectId = data.getString(fields_fb_object_id);
			}catch(JSONException e){}
			try{
			mDescription = data.getString(fields_description);
			}catch(JSONException e){}
			try{
			mName = data.getString(fields_name);
			}catch(JSONException e){}
			try{
			mCaption = data.getString(fields_caption);
			}catch(JSONException e){}
			try{
			mProperties = data.getString(fields_properties);
			}catch(JSONException e){}
			try{
			mHref = data.getString(fields_href);
			}catch(JSONException e){}
			
			JSONArray mediaArray = data.getJSONArray(fields_media);
			
			if(mediaArray != null){
				mNumMedias =  mediaArray.length();
				if(mNumMedias > 0 ){
					try{
						mMediaType = AttachmentMedia.identifyType(mediaArray.getJSONObject(0));
						//Logger.l(Logger.DEBUG, LOG_TAG, "media type: "+mMediaType);						
						mAttachmentMediaList = AttachmentMedia.createMediaList(mMediaType);						
					}catch(AttachmentMediaTypeUnrecognizedException e){
						Logger.l(Logger.ERROR, LOG_TAG, e.getMessage());
						mMediaType = AttachmentMedia.UNKNOWN;
						return;
					}
					
					for(int i=0;i<mNumMedias;i++){
						JSONObject jsonObj = mediaArray.getJSONObject(i);
						try{
							AttachmentMedia media = AttachmentMedia.parse(mMediaType, jsonObj);
							((ArrayList<AttachmentMedia>)mAttachmentMediaList).add(media);
						}catch(AttachmentMediaUnparsableException e){	
							e.printStackTrace();
						}catch(AttachmentMediaTypeUnrecognizedException e){
							e.printStackTrace();
						}
					}
				}
			}
			
		}
		catch(JSONException e){
			
		}	
		
	}
	
	public byte mMediaType = AttachmentMedia.UNKNOWN;;
	public int mNumMedias;
	public String mIcon;
	public String mFbObjectType;
	public String mDescription;
	public String mFbObjectId;
	public String mName;
	public String mCaption;
	public String mProperties;
	public String mHref;
	public String mTargetId;
	
	public ArrayList<? extends AttachmentMedia> mAttachmentMediaList;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeByte(mMediaType);
		dest.writeInt(mNumMedias);
		dest.writeString(mIcon);
		dest.writeString(mFbObjectType);
		dest.writeString(mDescription);
		dest.writeString(mFbObjectId);
		dest.writeString(mName);
		dest.writeString(mCaption);
		dest.writeString(mProperties);
		dest.writeString(mHref);
		dest.writeString(mTargetId);
		
		Iterator<? extends AttachmentMedia> it = mAttachmentMediaList.iterator();
		while(it.hasNext()){
			AttachmentMedia data = it.next();
			data.writeToParcel(dest, flags);			
		}
	}
	 
	
	public static Parcelable.Creator<Attachment> CREATOR = new Creator<Attachment>() {
		
		@Override
		public Attachment[] newArray(int size) {
			
			return new Attachment[size];
		}
		
		@Override
		public Attachment createFromParcel(Parcel source) {
			Attachment obj = new Attachment();
			obj.mMediaType = source.readByte();
			obj.mNumMedias = source.readInt();
			obj.mIcon = source.readString();
			obj.mFbObjectType = source.readString();
			obj.mDescription = source.readString();
			obj.mFbObjectId = source.readString();
			obj.mName = source.readString();
			obj.mCaption = source.readString();
			obj.mProperties = source.readString();
			obj.mHref = source.readString();
			obj.mTargetId = source.readString();			
			
			ArrayList<? extends AttachmentMedia> list = new ArrayList<AttachmentMedia>(obj.mNumMedias);
			
			for(int i=0;i<obj.mNumMedias;i++){
				switch(obj.mMediaType){
					case AttachmentMedia.LINK:{
						((ArrayList<AttachmentLink>)list).add(AttachmentLink.CREATOR.createFromParcel(source));
						break;
					}
					case AttachmentMedia.MUSIC:{
						((ArrayList<AttachmentMusic>)list).add(AttachmentMusic.CREATOR.createFromParcel(source));
						break;
					}
					case AttachmentMedia.PHOTO:{
						((ArrayList<AttachmentPhoto>)list).add(AttachmentPhoto.CREATOR.createFromParcel(source));
						break;
					}
					case AttachmentMedia.VIDEO:{
						((ArrayList<AttachmentVideo>)list).add(AttachmentVideo.CREATOR.createFromParcel(source));
						break;
					}
				}
			}
			
			obj.mAttachmentMediaList = list;
			
			return obj;
		}
	};

	@Override
	public Attachment getNextPoolable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setNextPoolable(Attachment element) {
		// TODO Auto-generated method stub
		
	} 
}

