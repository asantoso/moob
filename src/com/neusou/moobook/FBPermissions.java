package com.neusou.moobook;

import android.content.ContentValues;
import android.database.Cursor;

public class FBPermissions {
	
	
	public static final short publish_stream = 0;
	public static final short read_stream = 1;
	public static final short create_event = 2;
	public static final short offline_access = 3;
	public static final short rsvp_event = 4;
	public static final short email = 5;	
	public static final short read_mailbox = 6;
	
	
	public static final short all[] =  new short[]
	                                             {publish_stream, read_stream, create_event, offline_access, rsvp_event , email , read_mailbox};
	
	public static final String names[] =
		new String[]{
	
		"publish_stream",
		"read_stream",
		"create_event",
		"offline_access",
		"rsvp_event",	
		"email",				
		"read_mailbox"};
	
	public static final String fullNames[] =
		new String[]{
	
		"Publish Stream",
		"Read Stream",
		"Create Event",
		"Offline Access",
		"RSVP Event",	
		"Email",				
		"Read Mailbox"};
	
	
	public static final int TOTAL_PERMISSIONS = names.length;
	
	//convert permission names to permission booleans
	public static boolean[] generateFlags(String[] permNames){
		int im = permNames.length;
		int jm = names.length;
		
		boolean perms[] = new boolean[TOTAL_PERMISSIONS];
		
		for(int i=0;i<im;i++){
			for(int j=0;j<jm;j++){
				if( permNames[i].equals(names[j])){
					perms[j] = true;
				}
			}
		}
		
		return perms;
	}	
	
	public static String[] getFullNames(boolean[] flags){
		int N = flags.length;
		String[] names = new String[flags.length];
		for(int i=0;i<N;i++){
			if(flags[i]){
				names[i] = fullNames[i];	
			}			  
		}		
		return names;
	}
	
	//concatenate some permissions (cformat: sv)
	public static String generateSome(short[] perms){
		StringBuilder sb = new StringBuilder();
		int j = perms.length-1;
		for (int i : perms) {
			sb.append(names[i]);
			if(i<j)sb.append(",");
		}
		return sb.toString();
	}
	
	//concatenate all possible permissions (format: csv)
	public static String generateAll(){		
		return generateSome(all);	
	}

	synchronized public static String[] fromBooleansToStrings(boolean[] perms, boolean all, boolean mask){
	//	Log.d("agus","all?:"+all+", mask: "+mask);
		//mask = true;
		//all = true;
		StringBuilder b = new StringBuilder(30);
		
		
		int maxi = perms.length; 
		boolean first = true;
		
		for(int i=0;i<maxi;i++){			
			if(all){
				if(!first){				
					b = b.append(",");
				}else{
					first = false;
				}
				
				//Log.d("agus",names[i]);
				b = b.append(names[i]);					
			
			}	
			else if(perms[i] == mask){
				if(!first){
					b = b.append(",");
				}else{
					first = false;
				}
				//Log.d("agus",names[i]);
				b = b.append(names[i]);
			} 	
		}
		
	//	Log.d("agus","result: "+b.toString());
		return b.toString().split(",",TOTAL_PERMISSIONS);
		
	}
	
	public static ContentValues toContentValues(boolean[] perms){
		int maxi = perms.length;
		ContentValues cv = new ContentValues(TOTAL_PERMISSIONS);
		for(int i=0;i<maxi;i++){
			cv.put(names[i], perms[i]);
		}
		return cv;
	}
	
	synchronized public static boolean[] parseCursor(Cursor c){
		if(c == null || c.getCount() == 0){
			return null;
		}
		
		c.moveToFirst();
		boolean ret[] = new boolean[TOTAL_PERMISSIONS];
		for(int i=0,maxi = TOTAL_PERMISSIONS;i<maxi;i++){
			ret[i] = c.getInt(i+2)==1?true:false;
		}
		return ret;		
	}
}