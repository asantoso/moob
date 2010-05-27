package com.neusou.moobook.data;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.Logger;

import android.os.Parcelable;

public abstract class AttachmentMedia implements Parcelable {
	public String href;
	public String src;
	
	public static final byte UNKNOWN = 0;
	public static final byte PHOTO = 1;
	public static final byte MUSIC = 2;
	public static final byte LINK = 3;
	public static final byte VIDEO = 4;
	
	public static final String fields_src = "src";
	public static final String fields_type = "type";
	public static final String fields_href = "href";
	public static final String fields_alt = "alt";
	
	public static final String types_photo = "photo";
	public static final String types_video = "video";
	public static final String types_music = "music";
	public static final String types_link = "link";
	
	public static String getTypeName(byte mediaType){
		switch(mediaType){
			case AttachmentLink.TYPE:{
				return AttachmentLink.NAME;
			}
			case AttachmentPhoto.TYPE:{
				return AttachmentPhoto.NAME;
			}
			case AttachmentMusic.TYPE:{
				return AttachmentMusic.NAME;
			}
			case AttachmentVideo.TYPE:{
				return AttachmentVideo.NAME;
			}
		}
		return null;
	}
	
	public static byte identifyType(JSONObject data) throws AttachmentMediaTypeUnrecognizedException{
		try{			
			String type = data.getString(fields_type);
			//Logger.l(Logger.DEBUG, "Attachment","identify type: "+type);
			if(type.equals(types_link)){
				return LINK;
			}else if(type.equals(types_photo)){
				return PHOTO;
			}else if(type.equals(types_video)){
				return VIDEO;
			}else if(type.equals(types_music)){
				return MUSIC;
			}			
		}catch(JSONException e){	
			//Logger.l(Logger.DEBUG, "Attachment","exception: "+e.getMessage());
			//e.printStackTrace();
		}		
		throw new AttachmentMediaTypeUnrecognizedException("media type "+fields_type+" is not recognized");
	}
	
	public static AttachmentMedia createAttachmentMedia(byte type){
		switch(type){
			case LINK: return new AttachmentLink();
			case PHOTO: return new AttachmentPhoto();
			case VIDEO: return new AttachmentVideo();
			case MUSIC: return new AttachmentMusic();
		}
		return null;
	}
	
	public static AttachmentMedia parse(byte mediaType, JSONObject jsonData)
	throws AttachmentMediaUnparsableException, AttachmentMediaTypeUnrecognizedException
	{
		
		AttachmentMedia attachmentMedia = null;
		
		switch(mediaType)
	
		{
			case LINK:{
				attachmentMedia = AttachmentLink.parseJson(jsonData, null);
				break;
			}
			case VIDEO:{				
				attachmentMedia = AttachmentVideo.parseJson(jsonData, null);
				break;
			}
			case PHOTO:{
				attachmentMedia = AttachmentPhoto.parseJson(jsonData, null);
				break;
			}
			case MUSIC:{				
				attachmentMedia = AttachmentMusic.parseJson(jsonData, null);
				break;
			}		
		}
		
		if(attachmentMedia != null){
			try{			
				attachmentMedia.src = jsonData.getString(fields_src);
				attachmentMedia.href = jsonData.getString(fields_href);
				return attachmentMedia;
			}catch(JSONException e){	
				throw new AttachmentMediaUnparsableException(e.getMessage());
			}			
		}
		else{
			throw new AttachmentMediaTypeUnrecognizedException(mediaType+" is not a recognized media type. json data:"+jsonData);
		}
		
	}
	
	public static ArrayList<? extends AttachmentMedia> createMediaList(byte type)
	throws AttachmentMediaTypeUnrecognizedException {
		switch(type){
			case LINK: return new ArrayList<AttachmentLink>();
			case PHOTO: return new ArrayList<AttachmentPhoto>();
			case VIDEO: return new ArrayList<AttachmentVideo>();
			case MUSIC: return new ArrayList<AttachmentMusic>();
		}
		throw new AttachmentMediaTypeUnrecognizedException("media type "+type+" is not recognized");
	}
	
}