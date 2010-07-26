package com.neusou.moobook;

import java.io.InputStream;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.widget.RemoteViews;

import com.neusou.Logger;
import com.neusou.async.UserTask;
import com.neusou.moobook.activity.HomeActivity;

public class FBPhotoUploadTask extends UserTask<Void, Void, Void> {
	
	public FBPhotoUploadTask(Context ctx, InputStream is, long length, Bitmap preview) throws IllegalArgumentException{
		if(preview == null){
			throw new IllegalArgumentException();
		}
		
		this.is = is;
		this.length = length;
		packageName = ctx.getPackageName();
		mNotificationManager = (NotificationManager) App.INSTANCE.getSystemService(ctx.NOTIFICATION_SERVICE);
		this.preview = preview;
	}

	static final String LOG_TAG = "FBPhotoUploadTask";
		
	int notiid = (int) SystemClock.elapsedRealtime();
	Bitmap preview;
	String packageName;
	NotificationManager mNotificationManager;
	InputStream is;
	long length;
	int currentProgress;
	ConditionVariable cv = new ConditionVariable();
	
	final String notitag = "com.neusou.moobook.notitag";
	byte status = STATUS_UNDEFINED;
	
	static final byte STATUS_UNDEFINED = 0;
	static final byte STATUS_INPROGRESS = 1;
	static final byte STATUS_SUCCESS = 2;
	static final byte STATUS_ERROR = 3;
	
	static final int NOTIFICON_SUCCESS = android.R.drawable.ic_menu_manage;
	static final int NOTIFICON_ERROR = android.R.drawable.ic_menu_close_clear_cancel;
	static final int NOTIFICON_PROGRESS = android.R.drawable.ic_menu_upload;
		
	
	
	private Notification createNotification(String label, int icon, int max,
			int progress, boolean finished) {
		Notification mUploadPhotoNotification;
		mUploadPhotoNotification = new Notification();				
				
		if(!finished){
			mUploadPhotoNotification.flags = Notification.FLAG_ONGOING_EVENT;
		}else{
			mUploadPhotoNotification.flags = Notification.FLAG_SHOW_LIGHTS	|
			Notification.FLAG_AUTO_CANCEL
			;
			
			mUploadPhotoNotification.ledARGB = 0xFF0000FF;
			mUploadPhotoNotification.ledOffMS = 100;
			mUploadPhotoNotification.ledOnMS = 100;
			
		}
				
		
		mUploadPhotoNotification.icon = icon;
		mUploadPhotoNotification.contentView = new RemoteViews(packageName,	R.layout.remote_photo_upload);
		mUploadPhotoNotification.contentView.setProgressBar(R.id.progress, max,	progress, false);
		mUploadPhotoNotification.contentView.setImageViewBitmap(R.id.thumbnail, this.preview);
		mUploadPhotoNotification.contentView.setTextViewText(R.id.status, label);
		Intent i = new Intent(App.INSTANCE, HomeActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		PendingIntent pi = PendingIntent.getActivity(App.INSTANCE, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		mUploadPhotoNotification.contentIntent = pi;
		return mUploadPhotoNotification;
	}

	Handler h = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			int code = msg.what;
			//Logger.l(Logger.DEBUG, LOG_TAG,	"[UploadPhotoListener] [handlerMessage()] " + code);
			switch (code) {
			case Facebook.MESSAGE_UPDATE_UPLOAD_COMMENCING: {				
				Notification noti = createNotification(
						"Uploading photo to Facebook",
						NOTIFICON_PROGRESS, 100, 0, false);
				
				noti.tickerText = "Uploading photo to Facebook..";
				mNotificationManager.notify(notitag, notiid, noti);
				
				status = STATUS_INPROGRESS;
				break;
			}
			case Facebook.MESSAGE_UPDATE_UPLOAD_PROGRESS: {				
				Notification noti = createNotification(
						"Uploading photo to Facebook " + msg.arg1 + "%",
						NOTIFICON_PROGRESS, 100, msg.arg1,false);
				if(msg.arg1%10 == 0){
					mNotificationManager.notify(notitag, notiid, noti);					
				}
				currentProgress = msg.arg1;				
				break;
			}
			case Facebook.MESSAGE_UPDATE_UPLOAD_FINISHED: {
				/*
				Notification noti = createNotification("Finished uploading",
						android.R.drawable.ic_dialog_info, 100, 100, false);
				
				mNotificationManager.notify(notitag,notiid, noti);
				*/				
				break;
			}
			case Facebook.MESSAGE_UPDATE_UPLOAD_ERROR: {
				
				Notification noti = createNotification(
						"Error uploading to Facebook",
						NOTIFICON_ERROR, 100, currentProgress, true);
				noti.tickerText = "Error when uploading photo to facebook";
				mNotificationManager.notify(notitag, notiid, noti);
				
				status = STATUS_ERROR;
				break;
			}

			case Facebook.MESSAGE_UPDATE_UPLOAD_SUCCESS: {
				
				Notification noti = createNotification(
						"Successful upload.",
						NOTIFICON_SUCCESS, 100, 100, true);
				noti.tickerText = "Photo uploaded successfully to Facebook..";
				mNotificationManager.notify(notitag, notiid, noti);
				
				status = STATUS_SUCCESS;
				break;
			}
			}

		}
	};

	@Override
	protected Void doInBackground(Void... params) {
		Facebook fb = Facebook.getInstance();
		String response = fb.uploadPhoto(is, length, h);
		if (response == null) {
			Facebook.sendUpdate(h, Facebook.MESSAGE_UPDATE_UPLOAD_ERROR, 0, 0);
		} else {
			FBWSResponse fbresponse = FBWSResponse.parse(response);
			if (fbresponse == null || fbresponse.hasErrorCode) {
				Facebook.sendUpdate(h, Facebook.MESSAGE_UPDATE_UPLOAD_ERROR, 0,
						0);
			} else {
				Facebook.sendUpdate(h, Facebook.MESSAGE_UPDATE_UPLOAD_SUCCESS,
						0, 0);
			}
		}				
		return null;
	}
	
	@Override
	protected void onPreExecute() {	
		super.onPreExecute();
		
	}
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		
		int delayPost = 5000;
		int delayPostSuccess = 5000;
		int delayPostError = 30000;
		
		if(status == STATUS_SUCCESS){
			delayPost = delayPostSuccess;
		}else{
			delayPost = delayPostError;
		}
		
		h.postDelayed(new Runnable() {			
			@Override
			public void run() {
				mNotificationManager.cancel(notitag, notiid);
			}
		},delayPost);
		
	
	}

}