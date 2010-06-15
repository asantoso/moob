package com.neusou.moobook.model;

import android.os.Parcel;
import android.os.Parcelable;

public class RemoteCallResult implements Parcelable
{
	public boolean status;
	public static final String XTRA_PARCELABLE_OBJECT = RemoteCallResult.class.getName();
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeByte(status?(byte)1:0);
	}
	
	public static Creator<RemoteCallResult> CREATOR = new Creator<RemoteCallResult>() {
		
		@Override
		public RemoteCallResult[] newArray(int size) {
			return new RemoteCallResult[size];
		}
		
		@Override
		public RemoteCallResult createFromParcel(Parcel source) {
			RemoteCallResult rcr = new RemoteCallResult();
			rcr.status = source.readByte()==1;
			return rcr;
		}
	};
	
}