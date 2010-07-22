package com.neusou.moobook.activity;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.neusou.DecoupledHandlerThread;
import com.neusou.ProactiveThread;
import com.neusou.Logger;
import com.neusou.Utility;
import com.neusou.moobook.App;
import com.neusou.moobook.FBPhotoUploadTask;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;

import com.neusou.moobook.controller.StandardUiHandler;
import com.neusou.moobook.model.RemoteCallResult;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;

public class PostActivity extends BaseActivity{
	public static final String LOG_TAG = Logger.registerLog(PostActivity.class);
	
	public static final String XTRA_COMMENTBAR_EDITTEXT_HINT = "xtr.pa.cbar.et.hint";
	public static final String XTRA_COMMENTBAR_HEADER_TEXT = "xtr.pa.cbar.he.txt";	
	public static final String XTRA_SHOW_COMMENTBAR = "pa.xtr.shw.cmmtbar";
	public static final String XTRA_SHOW_PHOTOBAR = "pa.xtr.shw.phbar";
	public static final String XTRA_FOCUS = "pa.xtr.shw.focus";
	public static final String XTRA_OBJECTID = "nsu.pa.xtr.objid";
	
	public static final boolean DEFAULT_SHOW_PHOTOBAR = true;
	public static final boolean DEFAULT_SHOW_COMMENTBAR = true;	

	public static final byte DIALOG_POSTINGCOMMENT = 2;
	public static final int REQUESTCODE_POSTCOMMENT = 1;	
	public static final byte COMPONENT_COMMENTBAR = 1;
	
	//to hold extra values
	String mCommentBarEditTextHint;
	String mCommentBarHeaderText;
	
	//
	boolean mShowCommentBar = DEFAULT_SHOW_COMMENTBAR;
	boolean mShowPhotoBar = DEFAULT_SHOW_PHOTOBAR;
	byte mWhichFocus;	
	long uid;
	Uri mPhotoUploadUri;
	
	ImageView mPhotoPreviewImage;
	TextView mPhotoInfoText;
	
	// threads and locks
	CountDownLatch mWaitThreadLatch = new CountDownLatch(1);
	long mWrapperAnimDuration = 300l;
	DecoupledHandlerThread mDHT;
	CountDownLatch mWaitAnimationLatch;
	
	// ui views
	
	View mMainWrapper;
	View mCommentBarWrapper;
	View mPhotoBarWrapper;
	View mUploadBarWrapper;
	TextView mCommentBarHeader;
	Button mPostComment;
	EditText mEditComment;
	Button mPostPhotoFromGallery;
	Button mPostPhotoFromCamera;
	Button mUpload;
	Button mGetAlbums;
	String mObjectId;
	Facebook mFacebook;
	ManagerThread mWorkerThread;
	Handler mUIHandler;
	ProgressDialog mProgressDialog;
	ViewAnimationListener mAnimationListener;
	ViewAnimationListener mUploadBarAnimationListener;
	
	public static final int REQUEST_PICKPHOTO = 4;
	
	View.OnClickListener mPostPhotoFromGalleryOnClick;
	View.OnClickListener mPostPhotoFromCameraOnClick;
	View.OnClickListener mUploadOnClick;
	
	FBPhotoUploadTask mAsyncPhotoUploader;
	
	private void resetWaitLatch(){
		mWaitAnimationLatch = new CountDownLatch(1);
	}
	
	class ViewAnimationListener implements AnimationListener {
		public byte mType;
		View mWrapper;
		public static final byte SHOW = 0;
		public static final byte HIDE = 1;

		public ViewAnimationListener(byte type, View wrapper) {
			mType = type;
			mWrapper = wrapper;
		}

		@Override
		public void onAnimationStart(Animation animation) {
			mWrapper.setVisibility(View.VISIBLE);
			
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			if (mType == SHOW) {
				mWrapper.setVisibility(View.VISIBLE);
			} else if (mType == HIDE) {
				mWrapper.setVisibility(View.GONE);
			}
			mWaitAnimationLatch.countDown();
		}
	};

	
	BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			String action = intent.getAction();
				
			if(action.equals(App.INTENT_POSTCOMMENT)){
				Bundle b = intent.getExtras();
				RemoteCallResult remoteCallResult = (RemoteCallResult) b.getParcelable(RemoteCallResult.XTRA_PARCELABLE_OBJECT);
				if(remoteCallResult.status){	
					Toast.makeText(App.INSTANCE, "Comment posted.", 2000).show();					
					PostActivity.this.setResult(Activity.RESULT_OK);
				}				
				exit();				
			}
			
		}
		
	};
	

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.post_activity);
		bindViews();
		initObjects();
		initViews();
		getIntentExtras();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		mFacebook.registerOutHandler(R.id.outhandler_activity_post, mWorkerThread.getInHandler());
		mWorkerThread.setOutHandler(mUIHandler);
		registerReceivers();
		showHideViews();
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceivers();
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();		
	}
	
	Handler h = new Handler();
	
	@Override
	protected void onResume() {
		super.onResume();		
		if(mWhichFocus == COMPONENT_COMMENTBAR){
			//mEditComment.requestFocus();
			
		}
				
		showSoftKey();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	
	}
	
	protected void bindViews(){
		mMainWrapper = findViewById(R.id.mainwrapper);
		mPhotoBarWrapper = findViewById(R.id.photobar_wrapper);
		mCommentBarWrapper = findViewById(R.id.commentbar_wrapper);
		mUploadBarWrapper = findViewById(R.id.uploadbar_wrapper);
		mCommentBarHeader = (TextView) findViewById(R.id.commentbar_title);		
		mPostComment = (Button) findViewById(R.id.postcomment);
		mEditComment = (EditText) findViewById(R.id.commentbox);
		mPostPhotoFromGallery = (Button) findViewById(R.id.uploadphotofromgallery);
		mPostPhotoFromCamera = (Button) findViewById(R.id.uploadphotofromcamera);
		mPhotoPreviewImage = (ImageView) findViewById(R.id.photopreview);
		mPhotoInfoText = (TextView) findViewById(R.id.photoinfo);
		mUpload = (Button) findViewById(R.id.upload);
		mGetAlbums = (Button) findViewById(R.id.getalbums);
	}
	
	protected void initObjects(){			
		mDHT = new DecoupledHandlerThread();
		mDHT.start();
				
		//set default values
		mCommentBarEditTextHint = mResources.getString(R.string.commentbar_edittext_hint);
		mCommentBarHeaderText = mResources.getString(R.string.commentbar_header_text);

		mUploadBarAnimationListener = new ViewAnimationListener(ViewAnimationListener.SHOW, mUploadBarWrapper);
		
		mProgressDialog = new ProgressDialog(this);
		mAnimationListener = new ViewAnimationListener(ViewAnimationListener.SHOW, mMainWrapper);
		mUIHandler = new StandardUiHandler(this, mProgressDialog,  findViewById(R.id.adview_wrapper), findViewById(R.id.bottomheader)){
			
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				
				int code = msg.what;
				switch (code) {
					case ManagerThread.MESSAGE_UPDATELIST:{
						Logger.l(Logger.DEBUG,LOG_TAG,"[Handler()] [handleMessage()] update list");						
						break;
					}
					case ManagerThread.MESSAGE_COMMENTS_POSTED:{
						mProgressDialog.dismiss();						
						break;
					}
				}
			}
			
			@Override
			public void onServerCallError() {
				super.onServerCallError();				
				mProgressDialog.dismiss();
			}

			@Override
			public void onTimeoutError() {
				super.onTimeoutError();
				mProgressDialog.dismiss();			
			};
			
			@Override
			public void onWsResponseError() {
				super.onWsResponseError();
				exit();				
			};
		};		
		

		mPostPhotoFromGalleryOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Logger.l(Logger.DEBUG,LOG_TAG,"onPostPhoto");				
				Intent i = new Intent(Intent.ACTION_GET_CONTENT);
	             i.setType("image/*");
	            startActivityForResult(i, REQUEST_PICKPHOTO); 
			}
		};
		
		mPostPhotoFromCameraOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Logger.l(Logger.DEBUG,LOG_TAG,"onPostVideo");				
				Intent i = new Intent(Intent.ACTION_GET_CONTENT);
				i.addCategory(Intent.CATEGORY_OPENABLE);
	            i.setType("video/*");
	            startActivityForResult(i, REQUEST_PICKPHOTO); 
			}
		};
		
		mUploadOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Logger.l(Logger.DEBUG,LOG_TAG,"onUpload");
				if(mPhotoUploadUri != null){
					try{
						hideUploadBar();
						ContentResolver cr = getContentResolver();						
						AssetFileDescriptor thePhoto = cr.openAssetFileDescriptor(mPhotoUploadUri, "r");
						InputStream photoStream = thePhoto.createInputStream();
						uploadPhoto(photoStream);
					}catch(FileNotFoundException e){					
					}catch(IOException e){					
					}
				}				
			}
		};
		
		
		mFacebook = Facebook.getInstance();		
		mWorkerThread = App.INSTANCE.mManagerThread;
	}
	
	protected void initViews(){
		
		 Drawable d = mResources.getDrawable(android.R.drawable.ic_menu_upload);
		 Utility.getScaledDrawable(d, 32, 32);
		 mUpload.setCompoundDrawables(d, null, null, null);
		 
		 mUploadBarWrapper.setVisibility(View.GONE);
		 mPostComment.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				postComment();				
			}
		 });
			
		 mUpload.setOnClickListener(mUploadOnClick);
		 
		 mEditComment.setHint(mCommentBarEditTextHint);
		 mCommentBarHeader.setText(mCommentBarHeaderText);
		 mPostPhotoFromGallery.setOnClickListener(mPostPhotoFromGalleryOnClick);
		 mPostPhotoFromCamera.setOnClickListener(mPostPhotoFromCameraOnClick);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			exit();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){
			case REQUEST_PICKPHOTO:{
				 if (resultCode == RESULT_OK) { 
					 try{
						 ContentResolver cr = getContentResolver();
						 mPhotoUploadUri = data.getData();
						 String path = mPhotoUploadUri.getPath();
						 //cr.openFileDescriptor(data.getData(), "r").
						 AssetFileDescriptor thePhoto = cr.openAssetFileDescriptor(data.getData(), "r");
						 long numKiloBytes = thePhoto.getLength() / 1024;
						 InputStream photoStream = thePhoto.createInputStream();
						 
						 Rect rect = new Rect(0,0,64,64);
						 Options opt = new Options();
						 opt.inSampleSize = 2;
						 Bitmap bmp = BitmapFactory.decodeStream(photoStream, rect, opt);
						 
						
						 mPhotoPreviewImage.setImageBitmap(bmp);
						 mPhotoInfoText.setText(numKiloBytes+" KBytes");						 
						 //uploadPhoto(photoStream);
						 
						 showUploadBar();
					 }catch(FileNotFoundException e){						 
					 }catch(IOException e){						 
					 }
				 }
	             if (resultCode == RESULT_CANCELED) {
	             
	             }
				break;
			}
		}
		
		Logger.l(Logger.DEBUG,LOG_TAG,"onActivityResult() done");
	}
	
	public void uploadPhoto(InputStream photoStream){        
        try {
			//InputStream photoStream = thePhoto.createInputStream();
			Bitmap image = BitmapFactory.decodeStream(photoStream);
			int imageWidth = image.getWidth();
			int imageHeight = image.getHeight();						
			//Toast.makeText(this, imageWidth+", "+imageHeight, 2000).show();
			Bitmap scaled = Bitmap.createScaledBitmap(image,400,300,true);
			File cacheDir = getCacheDir();
			File tmp = new File(cacheDir.getPath()+"/photo");
			tmp.createNewFile();
			FileOutputStream fos = new FileOutputStream(tmp);
			boolean successCompress = scaled.compress(CompressFormat.PNG,100,fos);			
			Logger.l(Logger.DEBUG, LOG_TAG, "success compress? "+successCompress);			
			FileInputStream fis = new FileInputStream(tmp);			
			final InputStream uploadStream = fis;// thePhoto.createInputStream();
			final long length = tmp.length();
			Bitmap preview = Bitmap.createScaledBitmap(image, 32, 32, true);
			mAsyncPhotoUploader = new FBPhotoUploadTask(this, uploadStream, length, preview );
			mAsyncPhotoUploader.execute(null);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e){
			e.printStackTrace();
		}
    }	
		
	public void showWrapper(){
		resetWaitLatch();
		Animation anim = AnimationUtils.loadAnimation(this,R.anim.slide_to_current_from_top);
		anim.setDuration(mWrapperAnimDuration);		
		anim.setAnimationListener(mAnimationListener);
		mMainWrapper.startAnimation(anim);
	}
	
	public void hideWrapper(){
		resetWaitLatch();
		Animation anim = AnimationUtils.loadAnimation(PostActivity.this, R.anim.slide_to_top_from_current);
		anim.setDuration(mWrapperAnimDuration);
		mAnimationListener.mType = ViewAnimationListener.HIDE;
		anim.setAnimationListener(mAnimationListener);
		mMainWrapper.startAnimation(anim);	
	}
	
	public void showSoftKey(){
		mDHT.h.postDelayed(new Runnable() {			
			@Override
			public void run() {
				try{
					mWaitAnimationLatch.await();
					App.INSTANCE.showVirtualKeyboard(mEditComment);
				}catch(InterruptedException e){				
				}				
			}
			}, 500l);		
	}
	
	private void getIntentExtras(){
		Intent i = getIntent();
		Bundle b = i.getExtras();		
		if(b!=null){
			mObjectId = b.getString(XTRA_OBJECTID);
			mShowCommentBar = b.getBoolean(XTRA_SHOW_COMMENTBAR,DEFAULT_SHOW_COMMENTBAR);
			mShowPhotoBar = b.getBoolean(XTRA_SHOW_COMMENTBAR,DEFAULT_SHOW_PHOTOBAR);
			mWhichFocus = b.getByte(XTRA_FOCUS);
			mCommentBarEditTextHint = b.getString(XTRA_COMMENTBAR_EDITTEXT_HINT);
			mCommentBarHeaderText = b.getString(XTRA_COMMENTBAR_HEADER_TEXT);
		}
	}
	
	public void exit(){		
		mProgressDialog.dismiss();
		App.INSTANCE.hideVirtualKeyboard(mEditComment);
		hideWrapper();		
		mDHT.h.postDelayed(new Runnable() {			
			@Override
			public void run() {
				try{
					mWaitAnimationLatch.await(2000l, TimeUnit.MILLISECONDS);
					finish();
				}catch(InterruptedException e){						
				}
			}
		}, 500l);	
	}
	
	private void showUploadBar(){
		Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_current_from_top);
		mUploadBarAnimationListener.mType = ViewAnimationListener.SHOW;
		anim.setDuration(500);
		anim.setAnimationListener(mUploadBarAnimationListener);
		mUploadBarWrapper.startAnimation(anim);
	}
	

	private void hideUploadBar(){
		Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_to_top_from_current);
		mUploadBarAnimationListener.mType = ViewAnimationListener.HIDE;
		anim.setDuration(500);		
		anim.setAnimationListener(mUploadBarAnimationListener);
		mUploadBarWrapper.startAnimation(anim);
	}
	
	private void showHideViews(){
		if(mShowCommentBar){			
			mCommentBarWrapper.setVisibility(View.VISIBLE);
		}else{
			mCommentBarWrapper.setVisibility(View.GONE);
		}
		
		if(mShowPhotoBar){			
			mPhotoBarWrapper.setVisibility(View.VISIBLE);
		}else{
			mPhotoBarWrapper.setVisibility(View.GONE);
		}
		
		showWrapper();
				
	}
	
	public void registerReceivers(){
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(App.INTENT_POSTCOMMENT);
		registerReceiver(mBroadcastReceiver, intentFilter);
	}
	
	public void unregisterReceivers(){
		unregisterReceiver(mBroadcastReceiver);
	}
	
	public static Intent getIntent(Context ctx){
		return new Intent(ctx, PostActivity.class);
	}
	
	private void postComment(){
		showDialog(DIALOG_POSTINGCOMMENT);		
		String comment = mEditComment.getText().toString();		
	
		if( comment == null || comment.trim().length() == 0){
			return;
		}
		
		Bundle callbackDescriptor = new Bundle();
		callbackDescriptor.putString(BaseManagerThread.XTRA_CALLBACK_INTENT_ACTION, App.INTENT_POSTCOMMENT);
		mFacebook.postComment(R.id.outhandler_activity_post, callbackDescriptor, comment, mObjectId, ManagerThread.CALLBACK_POSTCOMMENT, BaseManagerThread.CALLBACK_SERVERCALL_ERROR, BaseManagerThread.CALLBACK_TIMEOUT_ERROR);
		mProgressDialog.setTitle("Posting comment");
		mProgressDialog.setMessage("Sending..");
		mProgressDialog.show();		
	}
	
}