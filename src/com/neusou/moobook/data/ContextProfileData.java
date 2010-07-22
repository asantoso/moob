package com.neusou.moobook.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.neusou.moobook.FBWSResponse;

public class ContextProfileData implements Parcelable{
	
	public static Creator CREATOR = new ContextProfileData.Creator();
	public static final String XTRA_PARCELABLE_OBJECT = FBWSResponse.class.getName();
	
	public String name;
	public String profileImageUri;
	public int outhandler;
	public long actorId;
		
	public static class Creator implements Parcelable.Creator<ContextProfileData>{

		@Override
		public ContextProfileData createFromParcel(Parcel source) {
			ContextProfileData out = new ContextProfileData();
			out.name = source.readString();
			out.profileImageUri = source.readString();
			out.outhandler = source.readInt();
			out.actorId = source.readLong();
			return out;
		}

		@Override
		public ContextProfileData[] newArray(int size) {
			return null;
		}
		
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(profileImageUri);
		dest.writeInt(outhandler);
		dest.writeLong(actorId);
	}
	
	
	
	
}