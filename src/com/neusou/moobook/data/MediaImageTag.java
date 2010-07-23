package com.neusou.moobook.data;

import android.os.Parcel;
import android.os.Parcelable;

public class MediaImageTag implements Parcelable{
	public String imageSrc;
	public long aid;
	public long pid;
	public long width;
	public long height;	
	public long index;
	public long owner;
	
	@Override
	public String toString() {
		return "src:"+imageSrc;
	}
	
	public static final Parcelable.Creator<MediaImageTag> CREATOR = new Creator<MediaImageTag>() {
		
		@Override
		public MediaImageTag[] newArray(int size) {
			
			return null;
		}
		
		@Override
		public MediaImageTag createFromParcel(Parcel source) {
			MediaImageTag m = new MediaImageTag();			
			m.imageSrc = source.readString();
			m.aid = source.readLong();
			m.pid = source.readLong();
			m.width = source.readLong();
			m.height = source.readLong();
			m.index = source.readLong();
			m.owner = source.readLong();
			return m;
		}
	};
	
	@Override
	public int describeContents() {		
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(imageSrc);
		dest.writeLong(aid);
		dest.writeLong(pid);
		dest.writeLong(width);
		dest.writeLong(height);
		dest.writeLong(index);
		dest.writeLong(owner);		
	}
}