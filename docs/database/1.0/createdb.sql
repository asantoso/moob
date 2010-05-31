DROP TABLE IF EXISTS applications;
CREATE TABLE applications ( 
	app_id INTEGER PRIMARY KEY, 
	display_name TEXT, 
	company_name TEXT,
	icon_url TEXT, 
	logo_url TEXT,
	_isHidden INTEGER); 
	
DROP TABLE IF EXISTS session;
CREATE TABLE session (
	session_rowid INTEGER PRIMARY KEY AUTOINCREMENT, 
	uid INTEGER UNIQUE ON CONFLICT ABORT, 
	key TEXT, 
	secret TEXT, 
	expires INTEGER); 
	
DROP TABLE IF EXISTS user_permissions;
CREATE TABLE user_permissions(
	permission_rowid INTEGER PRIMARY KEY AUTOINCREMENT, 
	userid INTEGER,
	status_update INTEGER ,
	photo_upload INTEGER, 
	create_event INTEGER,
	offline_access INTEGER,
	email INTEGER, 
	rsvp_event INTEGER,
	create_note INTEGER, 
	share_item INTEGER,
	video_upload INTEGER, 
	publish_stream INTEGER, 
	read_stream INTEGER,
	read_mailbox INTEGER);

DROP TABLE IF EXISTS app_users;
CREATE TABLE app_users(
	user_rowid INTEGER PRIMARY KEY AUTOINCREMENT,
	userid INTEGER,
	name TEXT, 
	lockpassword TEXT);

DROP TABLE IF EXISTS streams;
CREATE TABLE streams(
	post_rowid INTEGER PRIMARY KEY AUTOINCREMENT,
	post_id TEXT UNIQUE ON CONFLICT REPLACE, 
	actor_id INTEGER,
	target_id TEXT,
	viewer_id TEXT,
	source_id TEXT,
	type INTEGER,
	message TEXT,
	updated_time INTEGER,
	created_time INTEGER,
	likes_count INTEGER,
	likes_friends TEXT,
	likes_canlike INTEGER,
	likes_userlikes INTEGER,
	attribution TEXT,
	attachment TEXT,
	stream_appid INTEGER,
	comments_count INTEGER,
	comments_can_post INTEGER,
	comments_can_remove INTEGER,
);
	
DROP TABLE IF EXISTS comments;
CREATE TABLE comments(
	comment_rowid INTEGER PRIMARY KEY AUTOINCREMENT,
	from_id INTEGER,
	time INTEGER,
	comment TEXT,
	post_id TEXT,
	comment_id TEXT UNIQUE ON CONFLICT ABORT
);

DROP TABLE IF EXISTS events;
CREATE TABLE events(
	event_rowid INTEGER PRIMARY KEY AUTOINCREMENT,
	eid INTEGER UNIQUE ON CONFLICT ABORT, 	
	name TEXT, 	
	tagline TEXT, 	
	nid INTEGER, 	
	pic_small TEXT, 	
	pic_big TEXT, 	
	pic TEXT, 	
	host TEXT, 	
	description TEXT, 	
	event_type TEXT, 	
	event_subtype TEXT, 	
	start_time REAL, 	
	end_time REAL, 	
	creator INTEGER, 	
	update_time REAL, 	
	location TEXT, 	
	venue TEXT, 	
	privacy TEXT, 	
	hide_guest_list INTEGER,
	rsvp_status TEXT
);

DROP TABLE IF EXISTS users;
CREATE TABLE users(
	user_rowid INTEGER PRIMARY KEY AUTOINCREMENT,	
	uid INTEGER UNIQUE ON CONFLICT REPLACE,
	about_me TEXT, 
	activites TEXT,
	affiliations TEXT, 
	birthday STRING, 	
	birthday_date STRING, 	
	books STRING, 	
	current_location TEXT,
	education_history TEXT,
	first_name TEXT,
	
	has_added_app INTEGER,
	hometown_location TEXT,
	hs_info TEXT,
	interests TEXT,
	is_app_user INTEGER,
	is_blocked INTEGER,
	last_name TEXT,
	locale TEXT,
	meeting_for TEXT,
	meeting_sex TEXT, 	
	
	movies STRING, 	
	music STRING,
	name TEXT,
	notes_count INTEGER, 	
	pic TEXT, 	
	pic_with_logo TEXT,
	pic_big TEXT,
	pic_big_with_logo TEXT,
	pic_small TEXT,
	pic_small_with_logo TEXT,	
	
	pic_square TEXT,
	pic_square_with_logo TEXT, 	
	political TEXT,
	profile_blurb TEXT,
	profile_update_time INTEGER,
	proxied_email TEXT,
	quotes TEXT,
	relationship_status TEXT,
	religion TEXT,
	sex TEXT,
	
	significant_other_id INTEGER,
	status TEXT,
	timezone TEXT,
	tv TEXT,
	wall_count INTEGER,
	website TEXT,
	type INTEGER
);

DROP TABLE IF EXISTS friendlists;
CREATE TABLE friendlists(
	friendlist_rowid INTEGER PRIMARY KEY AUTOINCREMENT,
	flid INTEGER UNIQUE ON CONFLICT IGNORE,
	uid INTEGER,
	name TEXT
);

DROP TABLE IF EXISTS notifications;
CREATE TABLE notifications(
	notif_rowid INTEGER PRIMARY KEY AUTOINCREMENT,
	notification_id INTEGER UNIQUE ON CONFLICT ABORT,
	notif_app_id INTEGER,
	sender_id INTEGER,
	recipient_id INTEGER, 	
	created_time INTEGER,  	
	updated_time INTEGER, 	
	title_html TEXT, 	
	title_text TEXT, 	
	body_html TEXT, 	
	body_text TEXT, 	
	href TEXT, 	
	is_unread INTEGER
);

