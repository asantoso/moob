package com.neusou.moobook;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.neusou.Logger;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class FBWSResponse implements Parcelable{
	
	public final static String LOG_TAG = "FBWSResponse";
	public static final String XTRA_PARCELABLE_OBJECT = FBWSResponse.class.getName();
	
	public static final String jsonkey_error_code = "error_code";
	public static final String jsonkey_error_message = "error_msg";

	public static final int JSON_ARRAY = 0;
	public static final int JSON_OBJECT = 1;
	public static final int JSON_LITERAL_DOUBLE = 2;
	public static final int JSON_LITERAL_LONG = 3;
	public static final int JSON_LITERAL = 4;

	public JSONObject jsonObj;
	public JSONArray jsonArray;
	public long valueLong;
	public double valueDouble;
	public String data = null;
	public boolean hasErrorCode = false;
	public int errorCode = 0;
	public String errorDesc = null;
	public int type = 0;
	
	public static Creator CREATOR = new Creator();
	
	@Override
	public int describeContents() {

		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		
		String jsonObjStr = "";
		String jsonArrayStr = "";
		
		try{
			jsonObjStr= jsonObj.toString();
		}catch(Exception e){			
		}
		
		try{
			jsonArrayStr = jsonArray.toString();
		}catch(Exception e){			
		}
		
		try{
			dest.writeString(jsonObjStr);
		}catch(Exception e){}
		try{
			dest.writeString(jsonArrayStr);
		}catch(Exception e){}
		dest.writeInt(errorCode);
		dest.writeInt(type);
		dest.writeLong(valueLong);
		dest.writeDouble(valueDouble);
		dest.writeByte(hasErrorCode?(byte)1:0);
		dest.writeString(errorDesc);
		dest.writeString(data);
		
	}
	
	private static class Creator implements Parcelable.Creator<FBWSResponse> {

		@Override
		public FBWSResponse createFromParcel(Parcel source) {
			
			
			JSONObject jsonObj = null;
			JSONArray jsonArray = null;
			int errorCode = 0;
			int type = 0;			
			long valueLong = 0l;
			double valueDouble = 0;			
			boolean hasErrorCode = false;
			String errorDesc = null;
			String data = null;
			
			// start reading data
			try{
				jsonObj = new JSONObject(source.readString());
			}catch(JSONException e){}
			
			try{
				jsonArray = new JSONArray(source.readString());
			}catch(JSONException e){}
				
			errorCode = source.readInt();
			type = source.readInt();
			valueLong = source.readLong();
			valueDouble = source.readDouble();
			hasErrorCode = (source.readByte()==1)?true:false;
			errorDesc = source.readString();
			data = source.readString();		
			
			// end reading data
			
			FBWSResponse response = new FBWSResponse();
			response.jsonObj = jsonObj;
			response.jsonArray = jsonArray;
			response.errorCode = errorCode;
			response.type = type;
			response.valueLong = valueLong;
			response.valueDouble = valueDouble;
			response.hasErrorCode = hasErrorCode;
			response.errorDesc = errorDesc;
			response.data = data;
			
			return response;
		}

		@Override
		public FBWSResponse[] newArray(int size) {
			return new FBWSResponse[size];
		}
		
	}
	
	public static FBWSResponse parse(String response) {

		if (response == null || response.length() == 0) {
			return null;
		}
		
		FBWSResponse fbWsResponse = new FBWSResponse();
		
		if (response.charAt(0) == '{'&& response.charAt(response.length() - 1) == '}') {
			JSONObject jsonObj;
			try {
				jsonObj = new JSONObject(response);
				fbWsResponse.type = JSON_OBJECT;
				fbWsResponse.jsonObj = jsonObj;
				fbWsResponse.data = response;
				fbWsResponse.hasErrorCode = jsonObj.has(jsonkey_error_code);
				if (fbWsResponse.hasErrorCode) {
					fbWsResponse.errorCode = jsonObj.getInt(jsonkey_error_code);
					fbWsResponse.errorDesc = jsonObj.getString(jsonkey_error_message);
				}
			} catch (JSONException e) {
				Logger.l(Logger.ERROR, LOG_TAG,"[parse()] cant parse as JSON object. "+ e.getMessage());
				return null;
			}
		}

		else if (response.charAt(0) == '['
				&& response.charAt(response.length() - 1) == ']') {
			JSONArray jsonArray;
			try {
				jsonArray = new JSONArray(response);
				fbWsResponse.type = JSON_ARRAY;
				fbWsResponse.data = response;
				fbWsResponse.jsonArray = jsonArray;

			} catch (JSONException e) {
				Log.e(LOG_TAG, "cant parse as JSON Array" + e.getMessage());
				return null;
			}
		}

		else {

			try {
				fbWsResponse.valueLong = Long.parseLong(response);
				fbWsResponse.type = JSON_LITERAL_LONG;
				fbWsResponse.data = response;
			} catch (NumberFormatException e) {
				Log.e(LOG_TAG, "cant parse as long" + e.getMessage());
			}

			try {
				fbWsResponse.valueDouble = Double.parseDouble(response);
				fbWsResponse.type = JSON_LITERAL_DOUBLE;
				fbWsResponse.data = response;
			} catch (NumberFormatException e) {
				Log.e(LOG_TAG, "cant parse as double" + e.getMessage());

			}

			fbWsResponse.type = JSON_LITERAL;
			fbWsResponse.data = response;
		}

		return fbWsResponse;
	}

	public FBWSResponse() {

	}


}