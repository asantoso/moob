package com.neusou.moobook;

import com.neusou.Logger;

public class FBConnectionException extends Exception{

	public FBConnectionException(String message){
		super(message);	
		Logger.l(Logger.DEBUG, "FBConnectionException", "FBConnectionException: "+message);			
	}
	
}
