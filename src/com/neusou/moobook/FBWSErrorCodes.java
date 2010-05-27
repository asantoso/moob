package com.neusou.moobook;

public class FBWSErrorCodes {
	public static final int SESSIONEXPIRED = 102;
	
	public static final int UNEXPLAINED = 999;

	public static final int API_EC_SUCCESS = 0;// Success (all)
	public static final int API_EC_UNKNOWN = 1; // An unknown error occurred
												// (all)
	public static final int API_EC_SERVICE = 2;// Service temporarily
												// unavailable (all)
	public static final int API_EC_METHOD = 3; // Unknown method
	public static final int API_EC_TOO_MANY_CALLS = 4;// Application request
														// limit reached (all)
	public static final int API_EC_BAD_IP = 5; // Unauthorized source IP address
												// (all)
	public static final int API_EC_HOST_API = 6; // This method must run on
													// api.facebook.com (all)
	public static final int API_EC_HOST_UP = 7; // This method must run on
												// api-video.facebook.com
	public static final int API_EC_SECURE = 8; // This method requires an HTTPS
												// connection
	public static final int API_EC_RATE = 9; // User is performing too many
												// actions
	public static final int API_EC_PERMISSION_DENIED = 10; // Application does
															// not have
															// permission for
															// this action
	public static final int API_EC_DEPRECATED = 11; // This method is deprecated
	public static final int API_EC_VERSION = 12; //	 This API version is deprecated 
	
	//////////

	public static final int API_EC_PARAM_EC_PARAM = 100 ; //	 Invalid parameter 	 (all)
	public static final int API_EC_PARAM_API_KEY = 101;  //API_KEY 	Invalid API key 	(all)
	public static final int API_EC_PARAM_SESSION_KEY = 102; //SESSION_KEY 	Session key invalid or no longer valid 	(all)
	public static final int API_EC_PARAM_CALL_ID = 103; //CALL_ID 	Call_id must be greater than previous 
	public static final int API_EC_PARAM_SIGNATURE =104;// 	Incorrect signature 	(all)
	public static final int API_EC_PARAM_TOO_MANY=105;// 	The number of parameters exceeded the maximum for this operation
	public static final int API_EC_PARAM_USER_ID =110; //Invalid user id 	photos.addTag
	public static final int API_EC_PARAM_USER_FIELD =111;//	Invalid user info field
	public static final int API_EC_PARAM_SOCIAL_FIELD =112;//Invalid user field
	public static final int API_EC_PARAM_EMAIL 	=113;//Invalid email
	public static final int API_EC_PARAM_ALBUM_ID 	=120;//Invalid album id
	public static final int API_EC_PARAM_PHOTO_ID =121;//	Invalid photo id
	public static final int API_EC_PARAM_FEED_PRIORITY =130;//	Invalid feed publication priority
	public static final int API_EC_PARAM_CATEGORY =140;//	Invalid category
	public static final int API_EC_PARAM_SUBCATEGORY =141;//	Invalid subcategory
	public static final int API_EC_PARAM_TITLE 	=142;//Invalid title
	public static final int API_EC_PARAM_DESCRIPTION =143;//	Invalid description
	public static final int API_EC_PARAM_BAD_JSON =144;//	Malformed JSON string
	public static final int API_EC_PARAM_BAD_EID 	=150;//Invalid eid
	public static final int API_EC_PARAM_UNKNOWN_CITY 	=151;//Unknown city
	public static final int API_EC_PARAM_BAD_PAGE_TYPE 	=152;//Invalid page type 
	
	
	public static final int API_EC_PERMISSION 	= 200; // Permissions error
	public static final int API_EC_PERMISSION_USER 	= 210; // 	User not visible
	public static final int API_EC_PERMISSION_NO_DEVELOPERS = 211; // 	Application has no developers. 	admin.setAppProperties
	public static final int API_EC_PERMISSION_ALBUM 	= 220; // 	Album or albums not visible
	public static final int API_EC_PERMISSION_PHOTO 	= 221; // 	
	public static final int API_EC_PERMISSION_MESSAGE 	= 230; // 	Permissions disallow message to user
	public static final int API_EC_PERMISSION_MARKUP_OTHER_USER 	= 240; // 	Desktop applications cannot set FBML for other users
	public static final int API_EC_PERMISSION_STATUS_UPDATE 	= 250; // 	Updating status requires the extended permission status_update. 	users.setStatus
	public static final int API_EC_PERMISSION_PHOTO_UPLOAD 	= 260; // 	Modifying existing photos requires the extended permission photo_upload 	photos.upload, photos.addTag
	public static final int API_EC_PERMISSION_SMS 		= 270; // Permissions disallow sms to user.
	public static final int API_EC_PERMISSION_CREATE_LISTING 	= 280; // 	Creating and modifying listings requires the extended permission create_listing
	public static final int API_EC_PERMISSION_CREATE_NOTE 	= 281; // 	Managing notes requires the extended permission create_note.
	public static final int API_EC_PERMISSION_SHARE_ITEM 	= 282; // 	Managing shared items requires the extended permission share_item.
	public static final int API_EC_PERMISSION_EVENT 	= 290; // 	Creating and modifying events requires the extended permission create_event
	public static final int API_EC_PERMISSION_LARGE_FBML_TEMPLATE 	= 291; // 	FBML Template isn\'t owned by your application.
	public static final int API_EC_PERMISSION_LIVEMESSAGE 	= 292; // 	An application is only allowed to send LiveMessages to users who have accepted the TOS for that application.
	public static final int API_EC_PERMISSION_RSVP_EVENT 	= 299; // 	RSVPing to events requires the extended permission create_rsvp 
	
	
	public static final int API_EC_SESSION_TIMED_OUT =450; //	 Session key specified has passed its expiration time
	public static final int API_EC_SESSION_METHOD 	 =451; //Session key specified cannot be used to call this method
	public static final int API_EC_SESSION_INVALID 	 =452; //Session key invalid. This could be because the session key has an incorrect format, or because the user has revoked this session
	public static final int API_EC_SESSION_REQUIRED  =453; //	A session key is required for calling this method
	public static final int API_EC_SESSION_REQUIRED_FOR_SECRET  =454; //	A session key must be specified when request is signed with a session secret
	public static final int API_EC_SESSION_CANNOT_USE_SESSION_SECRET =455; //A session secret is not permitted to be used with this type of session key
	
	
}