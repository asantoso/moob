package com.neusou.moobook.model.database;

import java.util.HashMap;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.text.StrSubstitutor;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
//import android.util.Log;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.FBPermissions;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.data.Event;
import com.neusou.moobook.data.FBApplication;
import com.neusou.moobook.data.FBNotification;
import com.neusou.moobook.data.FriendList;
import com.neusou.moobook.data.Stream;
import com.neusou.moobook.data.User;

/**
 * <p>
 * Database helper
 * </p>
 * @author asantoso
 *
 */
public class ApplicationDBHelper extends DBHelper{
	
	public ApplicationDBHelper(Context context) {
		super(context, DB_NAME, DB_VERSION);
		Resources res = context.getResources();
		SQL_INSERTSTREAM = res.getString(R.string.sql_stream_insert);
		SQL_INSERTEVENT =  res.getString(R.string.sql_event_insert);
		SQL_GETSTREAMS_COMPLETE = res.getString(R.string.sql_getstreams_complete);	
		SQL_GETNOTIFICATIONS_COMPLETE = res.getString(R.string.sql_notifications_complete);
	}	
	
	
	protected static final String LOG_TAG = "ApplicationDBHelper";
	protected static final String DB_NAME = "moobook";
	protected static final int DB_VERSION = 1;
	
	private static final String TABLE_PERMISSIONS = "user_permissions";
	private static final String PROFILES_TABLE = "profiles";
	private static final String STREAMS_TABLE = "streams";
	private static final String PAGES_TABLE = "pages";
	private static final String COMMENTS_TABLE = "comments";
	private static final String FRIENDLISTS_TABLE = "friendlists";
	private static final String EVENTS_TABLE = "events";
	private static final String USERS_TABLE = "users";
	private static final String NOTIFICATIONS_TABLE = "notifications";
	private static final String APPLICATIONS_TABLE = "applications";
	private static final String ATTACHMENTS_TABLE = "attachments"; 
	
	private static final String SQL_GET_ALL_PERMISSIONS =  "select * from "+TABLE_PERMISSIONS;
	
	public static final int START_STREAM_INDEX_GETALL_STREAMPOSTS_AND_USERBASIC = 12;
	static String SQL_INSERTSTREAM;	
	static String SQL_INSERTEVENT; 
	static String SQL_GETSTREAMS_COMPLETE;
	static String SQL_GETNOTIFICATIONS_COMPLETE;
	
	static final String SQL_GETALL_POSTS_COMMENTS =  "select c.* , u.* from (select * from "+COMMENTS_TABLE+" where post_id = ?)as c left outer join users as u on u.uid = c.from_id order by time desc" ;
	static final String SQL_GET_NOTIFICATION =  "select n.* from "+NOTIFICATIONS_TABLE+" as n where notification_id = ?";
	static final String SQL_GET_APPLICATION =  "select app.* from "+APPLICATIONS_TABLE+" as app where app_id = ?";
	static final String SQL_GET_EVENT =  "select e.* from "+EVENTS_TABLE+" as e where eid = ?";
	static final String SQL_GETALL_FRIENDLISTS =  "select * from "+FRIENDLISTS_TABLE+" where uid = ?" ;
	static final String SQL_GET_STREAM_LASTUPDATEDTIME = "select updated_time from "+STREAMS_TABLE+" order by updated_time desc limit 1 offset 0 ";
	
	
	synchronized public long insertStream(Stream stream, SQLiteDatabase db){
		long rowId = -1;
    	try{    	
    		rowId = db.insertOrThrow(STREAMS_TABLE, null, stream.toContentValues(null));
    	//	Log.d("agus","##############  success inserting stream "+rowId+" name:"+stream.post_id+" , ccount: "+stream.comments_count);
    	}
    	catch(SQLException e){
    		//
    		if(rowId == -1){
    		//	Log.d("agus","############# error inserting stream.. trying to update ######################");
    			db.update(STREAMS_TABLE, stream.toContentValues(null), "post_id=?",new String[]{stream.post_id});
    			//Log.d("agus","success updating stream num rows affected: "+numRows);
    			return 0; 
    		}	
    		return rowId;
    	}
    	return -1;
	}
	
	synchronized public long updateStreamCommentsInfo(Stream stream, SQLiteDatabase db) throws NullArgumentException{
		Logger.l(Logger.DEBUG,LOG_TAG, "comments count: "+stream.comments_count);
		//Log.d(LOG_TAG, "comments count: "+stream.comments_can_post);
		
		if(stream == null || stream.post_id == null){
			throw new NullArgumentException("stream is null or post_id is null");
		}
		
    	try{    	
    		db.update(STREAMS_TABLE, stream.commentsToContentValues(null), "post_id=?",new String[]{stream.post_id});    			
    	}
    	catch (SQLException e) {
        }
    	return -1;
	}
	
	public long deleteAllStreams(SQLiteDatabase db){
		try{    	
    		db.delete(STREAMS_TABLE,null,null );    		    			
    	}
    	catch (SQLException e) {
        }
    	return -1;
	}
	
	public long deleteStreams(SQLiteDatabase db, long updatedTime){
		try{    	
    		db.delete(STREAMS_TABLE,"where updated_time > ?",new String[]{Long.toString(updatedTime)});		
    	}
    	catch (SQLException e) {
        }
    	return -1;
	}
	
	/*
	public long insertComment(Comment comment, SQLiteDatabase db){
		Log.d(LOG_TAG, "insertComment "+comment.comment);
    	try{    		
    		long rowId = db.insertOrThrow(COMMENTS_TABLE, null, comment.toContentValues(null));   		
    		
    		if(rowId == -1){
    			Log.d(LOG_TAG, "trying to update comment...");
      			int success = db.update(COMMENTS_TABLE,comment.toContentValues(null),"post_id=?",new String[]{comment.post_id});
      			Log.d(LOG_TAG, "trying to update success? "+success);
    		}
    		return rowId;
    	}
    	catch (SQLException e) {    		
    		e.printStackTrace();
    		Log.d(LOG_TAG,"SQLException: " + e.getMessage());
        }
    	return -1;
	}
	*/

	public long insertEvent(Event data, SQLiteDatabase db){
		//Logger.l(Logger.DEBUG,LOG_TAG,"########################### inserting event: "+data.name+", rsvp:"+data.rsvp_status);
    	try{    		
    		long rowId = db.insertOrThrow(EVENTS_TABLE, null, data.toContentValues(null));
    		return rowId;
    	}
    	catch (SQLException e) {
    		db.update(EVENTS_TABLE,data.toContentValues(null), "eid=?",new String[]{Long.toString(data.eid)} );
    		//e.printStackTrace();
        }
    	return -1;
	}
	
	
	public String[] generateUserFilter(String nameConstraint, int ordering){
		String ret[] = new String[2];
		String orderClause = "";
		String whereClause = "";
		if(ordering ==  1){
			orderClause = " order by name ASC ";
		}		
		if(nameConstraint != null){
			whereClause = " where "+nameConstraint;
		}
		
		ret[0] = whereClause;
		ret[1] = orderClause;
		return ret;
	}
	
	public static final short[] mDisplayUsers = new short[]{};
	
	synchronized public Cursor getAllUsers(SQLiteDatabase db,  short[] selection, int ordering, String nameConstraint, int processFlag){
		String filter[] = generateUserFilter(nameConstraint, ordering);	
		String columnNames = User.createColumnNames(selection);
		
		String []rColumns = columnNames.split(",");
		String rSelection = "_process_flag = ? ";
		String rSelectionArgs[] = new String[]{String.valueOf(0)};
		String rGroupBy = null;
		String rHaving = null;
		String rOrderBy = "name ASC";
		
		String whereClause = " where _process_flag = "+processFlag;
		String orderClause = " order by name ASC";
    	
		try{
    		String query = "select "+columnNames+" from "+USERS_TABLE+whereClause+orderClause;
    		Logger.l(Logger.DEBUG, LOG_TAG, "[getAllUsers()] sql: "+query);
    		 
    		//Cursor c = db.query(USERS_TABLE, null, rSelection, rSelectionArgs, rGroupBy, rHaving, rOrderBy);
      		Cursor c = db.rawQuery(query,null);
    		return c;
    	}
    	catch (SQLException e) {
        }
    	return null;		
	}
	
	
	
	public synchronized long insertUser(User user, short[] selection, SQLiteDatabase db){
		if(user == null){
			return -1;
		}		
		//Logger.l(Logger.DEBUG,LOG_TAG,"[insertUser()] uid:"+user.uid+" name: "+user.name+" pic_square:"+user.pic_square+", flag:"+user._process_flag);
		
    	try{
      		long rowId = db.insertOrThrow(USERS_TABLE, null, user.toContentValues(null, selection));
      		//Logger.l(Logger.DEBUG, LOG_TAG,"inserting user success "+ rowId);
    	}catch (SQLException e) {    		
      		//Logger.l(Logger.DEBUG, LOG_TAG,"[inserUser()] updating user in db");
      		long numRowsAffected = db.update(USERS_TABLE, user.toContentValues(null, selection),"uid=?",new String[]{Long.toString(user.uid)});
      		return numRowsAffected;
        } 
      		
    	return -1;		
	}
	
	
	/*
	synchronized public long insertPage(Page page, short[] selection, SQLiteDatabase db){
		if(page == null){
			return -1;
		}
		
		//Log.d(LOG_TAG,"inserting user: name: "+user.name +"  "+user.first_name +" "+user.last_name);
		
    	try{
      		long rowId = db.insertOrThrow(PAGES_TABLE, null, page.toContentValues(null, selection));
      		Log.d(LOG_TAG,"inserting user success "+ rowId);
      		if(rowId == -1){
      			Log.d(LOG_TAG,"trying to update user ..");
      			long success = db.update(PAGES_TABLE, page.toContentValues(null, selection),"page_id=?",new String[]{Long.toString(page.page_id)});
      			Log.d(LOG_TAG,"updating user success ?"+ success);
      		}
    		return rowId;
    	}
    	catch (SQLException e) {
    		//e.printStackTrace();
        }
    	return -1;		
	}
	*/
	
	
	public long inserFriendList(FriendList data, SQLiteDatabase db){		
    	try{    	
    		long rowId = db.insertOrThrow(STREAMS_TABLE, null, data.toContentValues(null));
    		
    		if(rowId == -1){    		
    			int numRows = db.update(STREAMS_TABLE, data.toContentValues(null), "flid=?",new String[]{data.flid});    		
    			return 0;
    		}	
    	}
    	catch (SQLException e) {
        }
    	return -1;
	}
	
	public long insertNotification(FBNotification data, SQLiteDatabase db){
		Logger.l(Logger.DEBUG, LOG_TAG,"[insertNotification()] "+data.title_text);
		try{    	
    		long rowId = db.insertOrThrow(NOTIFICATIONS_TABLE, null, data.toContentValues(null));
    		//Logger.l(Logger.DEBUG,  LOG_TAG, "insertNotification row affected:"+rowId);
    		if(rowId == -1){
    			//Logger.l(Logger.DEBUG,LOG_TAG,"updating notification..");
    			int numRows = db.update(NOTIFICATIONS_TABLE, data.toContentValues(null), "notification_id=?",new String[]{Long.toString(data.notification_id)});    		
    			return numRows;
    		}	
    		return rowId;
    	}
    	catch (SQLException e) {
        }
    	return -1;
	}
	

	public long insertApplication(FBApplication data, SQLiteDatabase db){
		//Logger.l(Logger.DEBUG, LOG_TAG,"[insertApplication()] "+data.app_id+", "+data.display_name);
		try{    	
    		long rowId = db.insertOrThrow(APPLICATIONS_TABLE, null, data.toContentValues(null));
    		//Logger.l(Logger.DEBUG, LOG_TAG,"insert application # rows affected:"+rowId);
    		if(rowId == -1){    	
    			//Log.d(LOG_TAG,"updating application..");
    			int numRows = db.update(APPLICATIONS_TABLE, data.toContentValues(null), FBApplication.cn_app_id+"=?",new String[]{Long.toString(data.app_id)});    		
    			return numRows;
    		}	
    		return rowId;
    	}
    	catch (SQLException e) {
        }
    	return -1;
	}
	public Cursor getFriendlists(String uid){
		SQLiteDatabase db;
    	try{    		
    		db = getReadableDatabase();
    		Cursor c = db.rawQuery(
    				SQL_GETALL_FRIENDLISTS,
    				new String[]{uid}
    		);    		
    		return c;
    	}
    	catch (SQLException e) {
        }
    	return null;
	}
	
	/**
	 * Get FBApplication object in the database 
	 * @param db
	 * @param app_id
	 * @param cache
	 * @return
	 */
	public FBApplication getApplication(SQLiteDatabase db, long app_id, FBApplication cache){		
    	try{    		
    		
    		Cursor c = db.rawQuery(
    				SQL_GET_NOTIFICATION, 
    				new String[]{Long.toString(app_id)}
    		);
    		
    		int numCount = c.getCount();
    		if(numCount == 0){
    			return null;
    		}
    		   		
    		if(c.moveToFirst()){
    			cache = FBApplication.parseCursor(c, cache);
    			c.close();	
    			return cache;
    		}
    		else{
    			c.close();
    			return null;
    		}
    		
    	}
    	catch (SQLException e) {
    		e.printStackTrace();
        }
    	return null;		
	}
		
	public Cursor getEvents(SQLiteDatabase db){
		try{
    		Cursor c = db.query(
    				EVENTS_TABLE,
    				null, 
    				null, 
    				null, 
    				null, 
    				null, 
    				"start_time DESC");
    		
    		int numCount = c.getCount();
    		Logger.l(Logger.DEBUG, LOG_TAG, "num events: "+numCount);    		
    		if(numCount == 0){
    			c.close();
    			return null;
    		}
    		return c;    		
    	}
    	catch (SQLException e) {
    		e.printStackTrace();
        }
    	return null;	
	}
	
	public Event getEvent(SQLiteDatabase db, long event_id, Event cache){
		try{
    		Cursor c = db.rawQuery(
    				SQL_GET_EVENT,
    				new String[]{Long.toString(event_id)}
    		);
    		
    		int numCount = c.getCount();
    		if(numCount == 0){
    			return null;
    		}
    		   		
    		if(c.moveToFirst()){
    			cache = Event.parseCursor(c, cache);
    			c.close();	
    			return cache;
    		}
    		else{
    			c.close();
    			return null;
    		}
    		
    	}
    	catch (SQLException e) {
    		e.printStackTrace();
        }
    	return null;
	}
	
	public FBNotification getNotification(SQLiteDatabase db, long notification_id, FBNotification cache){		
    	try{    		
    		
    		Cursor c = db.rawQuery(
    				SQL_GET_NOTIFICATION, 
    				new String[]{Long.toString(notification_id)}
    		);
    		
    		int numCount = c.getCount();
    		if(numCount == 0){
    			c.close();
    			return null;
    		}
    		   		
    		if(c.moveToFirst()){
    			cache = FBNotification.parseCursor(c, cache);
    			c.close();	
    			return cache;
    		}
    		else{
    			c.close();
    			return null;
    		}
    		
    	}
    	catch (SQLException e) {
    		e.printStackTrace();
        }
    	return null;		
	}
	
	public Cursor getAllNotifications(SQLiteDatabase db, long[] hiddenAppIds){
		
		String orderClause0 = " ";	
		String whereClause0 = " ";		
		String hiddenAppIdsCSV = Util.toCSV(hiddenAppIds,null);
		
		HashMap<String, String> args = new HashMap<String, String>();
		
		if(hiddenAppIds != null){
			whereClause0 = "app.app_id NOT IN ("+hiddenAppIdsCSV+") ";
		}
		
		orderClause0 += "order by "+FBNotification.cn_created_time+" DESC";
		
		args.put("whereClause0", whereClause0);		
		args.put("orderClause0", orderClause0);
				
    	try{    		 
    		
    		String sql = StrSubstitutor.replace(SQL_GETNOTIFICATIONS_COMPLETE, args);    	
    		Logger.l(Logger.DEBUG, LOG_TAG, "[getAllNotifications()] sql: "+sql);
    		Cursor c = db.rawQuery(sql, null);    		
    		
    		int numCount = c.getCount();
    		Logger.l(Logger.DEBUG,LOG_TAG,"count notifications: "+numCount); 
    		
    		return c;
    		
    	}
    	catch (SQLException e) {
    		e.printStackTrace();
        }
    	return null;		
	}
	
	public Cursor getPostComments(String postId){
		SQLiteDatabase db;
    	try{    		
    		db = getReadableDatabase();
    		Cursor c = db.rawQuery(
    				SQL_GETALL_POSTS_COMMENTS, 
    				new String[]{postId}
    		);    		
    		return c;    		
    	}
    	catch (SQLException e) {
        }
    	return null;
	}
	
	public long getPostCommentsCount(String postId, SQLiteDatabase db){
		//Log.d(LOG_TAG,"getPostCommentsCount postId: "+postId);
    	try{ 
    		db = getReadableDatabase();
    		Cursor c = db.rawQuery(
    				SQL_GETALL_POSTS_COMMENTS,    				
    				new String[]{postId}
    		);
    		int count = c.getCount();
    		return count;
    	}
    	catch (SQLException e) {
    		Logger.l(Logger.DEBUG, LOG_TAG, "getPostCommentsCount error: "+e.getMessage());
        }
    	return 0;
	}
	
	public Cursor getWallPosts(SQLiteDatabase db, int processFlag, long source_id, long limit, long offset){
		
		if(db == null || !db.isOpen()){
			return null;
		}
		
				
		
		String sql = createSqlGetStreams(
				Facebook.STREAMMODE_NEWSFEED,
				processFlag,
				source_id, 
				null, limit, offset);
		
		Logger.l(Logger.DEBUG, LOG_TAG, "SQL:  "+sql);
		
    	try{    		
    		Cursor c = db.rawQuery(sql,null);
    		return c;    		
    	}
    	catch (SQLException e) {
    		e.printStackTrace();
        }
    	return null;
		
	}
	
	public Cursor getStreams(SQLiteDatabase db, byte streamtype, int processingFlag, long source_id, long uids_filter[], long limit, long offset){
		
		if(db == null || !db.isOpen()){
			return null;
		}
		
		String sql = createSqlGetStreams(streamtype, processingFlag, source_id, uids_filter, limit, offset);
		
		Logger.l(Logger.DEBUG, LOG_TAG, "SQL:  "+sql);
		
    	try{    		
    		Cursor c = db.rawQuery(sql,null);
    		return c;    		
    	}
    	catch (SQLException e) {
    		e.printStackTrace();
        }
    	return null;
    	
	}
	
	/**
	 * Returns all permissions that were granted by a User 
	 * @return
	 */
	public Cursor getAllPermissions(long uid){
		SQLiteDatabase db;
    	try{
    		db = getReadableDatabase();    		    		
    		Cursor c = db.rawQuery(SQL_GET_ALL_PERMISSIONS, null);    		  
    		return c;
    	}
    	catch(Exception e){
    	}
    	return null;
	}
	
	public void insertPermissions(boolean[] perms){
		SQLiteDatabase db;
    	try{
    		db = getReadableDatabase();
    		db.insert(TABLE_PERMISSIONS, null, FBPermissions.toContentValues(perms));
    	}
    	catch (SQLException e) {
        }
	}

	/*
	public int getUsername(SQLiteDatabase db, String uid){
		if(db == null || !db.isOpen()){
			return -1;
		}
		
    	try{    		    		
    		short[] selection = new short[]{User.col_name};
    		db.rawQuery("select ", selectionArgs);
    		int rows_affected = db.query(USERS_TABLE, User.createColumnNames(selection), selection, , null, null, orderBy);   		
    		return rows_affected;
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    	return -1;		
	}
	*/
	
	public int deleteAllNotifications(SQLiteDatabase db){
		if(db == null || !db.isOpen()){
			return -1;
		}
		
    	try{    		    		
    		int rows_affected = db.delete(NOTIFICATIONS_TABLE, "1", null);   		
    		return rows_affected;
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    	return -1;		
	}
	
	public long getStreamLastUpdatedTime(SQLiteDatabase db){
		if(db == null || !db.isOpen()){
			return 0;
		}
		
    	try{
    		    		
    		Cursor c = db.rawQuery(SQL_GET_STREAM_LASTUPDATEDTIME, null);
    		if(c.moveToFirst()){    			
    			long ret = c.getLong(0);
    			c.close();
    			return ret;
    		}
    		
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    	return 0;
	}
	
	
	
	public long updateEventRSVP(SQLiteDatabase db, long eid, Event.RSVPStatus rsvp_status){
		if(db == null || !db.isOpen()){
			return 0;
		}
		
    	try{    		    		
    		ContentValues cv = new ContentValues();
    		cv.put(Event.cn_rsvp_status, rsvp_status.toString());
    		
    		int rows_updated = db.update(EVENTS_TABLE,cv, Event.cn_eid+" = ?", new String[]{Long.toString(eid)});
    		return rows_updated;    		
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    	return -1;
	}
	

	public String createSqlGetStreams(byte streamType, int processFlag, long source_id, long uids[], long limit, long offset){
		HashMap<String, String> args = new HashMap<String, String>();
		String ret = "";
		String whereClause0 = " where 1=1 ";
		String whereClause1 = " where 1=1 ";
		String orderClause0 = "";
		String orderClause1 = "";
		String uidsFilter = null;
		
		if(processFlag == App.PROCESS_FLAG_STREAM_SESSIONUSER){
			whereClause0 += " and p._process_flag = "+processFlag+" ";
		}else{
			//whereClause0 += "and p._process_flag = "+processFlag;PROCESS_FLAG_OTHER IS SAME AS IGNORING THE FLAG.
		}
		
		if(streamType == Facebook.STREAMMODE_LIVEFEED){
			orderClause0 = " order by p.updated_time desc ";
		}
		if(streamType == Facebook.STREAMMODE_NEWSFEED){			
			orderClause0 = " order by p.created_time desc ";
		}
		if(streamType == Facebook.STREAMMODE_WALLFEED){
			orderClause0 = " order by p.updated_time desc ";
		}
				
		if(uids != null){		
			String uids_list = "(";
			for(int i=0;i<uids.length;i++){
				uids_list += uids[i];
				if(i < uids.length - 1){
					uids_list += ",";
				}
				}
			uids_list += ")";
			args.put("uids", uids_list);
			uidsFilter = " u.uid in "+uids_list+" ";
			
		}
		
		if(uidsFilter != null){
			whereClause0 += " and "+uidsFilter;
		}
		
		if(streamType == Facebook.STREAMMODE_WALLFEED){
			whereClause0 += " and p.source_id = "+source_id+" ";
		}else{
			whereClause0 += " and p.source_id != "+source_id+" ";
		}
		
		args.put("whereClause0", whereClause0);
		args.put("whereClause1", whereClause1);
		args.put("orderClause0", orderClause0);
		args.put("orderClause1", orderClause1);
		
		args.put("limit", String.valueOf(limit));
		args.put("offset", String.valueOf(offset));
		
		ret = StrSubstitutor.replace(ApplicationDBHelper.SQL_GETSTREAMS_COMPLETE, args);
		return ret;		
	}
	
	
	/**
	 * 
	 * @param db
	 * @param cutoff the cutoff date in unix-epoch format
	 * @return
	 */
	public int truncateStreams(SQLiteDatabase db, long cutoff){
		StringBuffer sb = new StringBuffer();
		String whereClause;
		sb.append(Stream.cn_updated_time)
		.append(" < ? ");
		whereClause = sb.toString();
		String whereArgs[] = new String[]{Long.toString(cutoff)};
		int numRowsAffected = db.delete(STREAMS_TABLE,cutoff==0?null:whereClause, whereArgs);
		return numRowsAffected;
	}
	
	public void clearAllTables(SQLiteDatabase db){
		db.delete(STREAMS_TABLE, null, null);
		db.delete(USERS_TABLE, null, null);
		db.delete(NOTIFICATIONS_TABLE, null, null);
		db.delete(EVENTS_TABLE, null, null);
	}
	
	public int truncateNotifications(SQLiteDatabase db, long cutoff){
		StringBuffer sb = new StringBuffer();
		String whereClause;
		sb.append(FBNotification.cn_updated_time)
		.append("< ? ");
		whereClause = sb.toString();
		String whereArgs[] = new String[]{Long.toString(cutoff)};
		int numRowsAffected = db.delete(NOTIFICATIONS_TABLE,cutoff==0?null:whereClause, whereArgs);
		return numRowsAffected;
	}
	
	
	public static int getFieldOffset(int sqlCode, int tableCode, int fieldCode){
		int offset = 0;
		switch(sqlCode){
			
			case R.string.sql_notifications_complete:{
				int offset_notifications = 0;
				int offset_applications = FBNotification.TOTAL_COLUMNS;
				int offset_users = offset_applications + FBApplication.TOTAL_COLUMNS;		
				switch(tableCode){
					case R.id.table_applications:{
						offset = offset_applications;
						break;
					}
					case R.id.table_notifications:{
						offset = offset_notifications;
						break;
					}
					case R.id.table_users:{
						offset = offset_users;
						break;
					}
				}
				break;
			}
			

		
			
		}
		return offset;
	}
	
	public static int getCursorIntData(int sqlCode, int tableCode, int fieldCode, Cursor c){
		int fieldOffset = getFieldOffset(sqlCode, tableCode, fieldCode);
		
		return c.getInt(fieldCode + fieldOffset);
	}
	
	public static String getCursorStringData(int sqlCode, int tableCode, int fieldCode, Cursor c){
		int fieldOffset = getFieldOffset(sqlCode, tableCode, fieldCode);
		Logger.l(Logger.DEBUG, LOG_TAG, "[getCursorStringData()] "+fieldOffset);
		return c.getString(fieldCode + fieldOffset);				
	}
	
	public static long getCursorLongData(int sqlCode, int tableCode, int fieldCode, Cursor c){
		int fieldOffset = getFieldOffset(sqlCode, tableCode, fieldCode);
		return c.getLong(fieldCode + fieldOffset);
	}
	
	public static float getCursorFloatData(int sqlCode, int tableCode, int fieldCode, Cursor c){
		int fieldOffset = getFieldOffset(sqlCode, tableCode, fieldCode);
		return c.getFloat(fieldCode + fieldOffset);
	}
	
	public static double getCursorDoubleData(int sqlCode, int tableCode, int fieldCode, Cursor c){
		int fieldOffset = getFieldOffset(sqlCode, tableCode, fieldCode);
		return c.getDouble(fieldCode + fieldOffset);
	}
	
}