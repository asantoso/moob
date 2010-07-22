package com.neusou.moobook.task;

import android.util.Log;


public class ProcessTaskFactory {
	
	public static final int PROCESS_USERS = 1;
	public static final int PROCESS_STREAMSMQ = 2;
	public static final int PROCESS_NOTIFICATIONS = 3;
	
	public static int mNumUsers = 0;
	public static int mNumStreamsMQ = 0;
	public static int mNumNotifications = 0;
	
	public static int getCount(int type){
		switch(type){
		case PROCESS_NOTIFICATIONS:{
			return mNumNotifications;		
		}
		case PROCESS_STREAMSMQ:{
			return mNumStreamsMQ;
		}
		case PROCESS_USERS:{
			return mNumUsers;			
		}
		}
		return -1;
	}
	
	public static void reportFinish(int type){
		switch(type){
		case PROCESS_NOTIFICATIONS:{
			mNumNotifications--;		
		}
		case PROCESS_STREAMSMQ:{
			mNumStreamsMQ--;
		}
		case PROCESS_USERS:{
			mNumUsers--;			
		}
		}
	}
	
	public static <T> T create(Class<T> type) throws DeniedTaskCreationException{
		String cname = type.getCanonicalName();
		if(cname.equals(ProcessUsersTask.class.getCanonicalName())){
			if(mNumUsers > 0){
				mNumUsers++;
				try{
					return type.newInstance();
				}catch(InstantiationException e){	
					Log.e("InstantiationException", e.getMessage());
				}catch(IllegalAccessException e){
					Log.e("IllegalAccessException", e.getMessage());
				}
			}
		}
		throw new DeniedTaskCreationException();
	}
	
	public static <T> void finish(Class<T> type){
		String cname = type.getCanonicalName();
		if(cname.equals(ProcessUsersTask.class.getCanonicalName())){
			mNumUsers--;
			if(mNumUsers < 0){
				mNumUsers=0;
			}
		}
	}
	
	
	
	public static Object create(int type) throws DeniedTaskCreationException{
		switch(type){
			case PROCESS_NOTIFICATIONS:{
				if(mNumNotifications > 0){
					throw new DeniedTaskCreationException();
				}
				break;
			}
			case PROCESS_STREAMSMQ:{
				if(mNumStreamsMQ > 0){
					throw new DeniedTaskCreationException();
				}
				break;
			}
			case PROCESS_USERS:{
				if(mNumUsers > 0){
					throw new DeniedTaskCreationException();
				}
				break;
			}
		}
		return null;
		
	}	
	
	
	
}