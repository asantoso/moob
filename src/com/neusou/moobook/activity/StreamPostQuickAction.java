package com.neusou.moobook.activity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.neusou.DecoupledHandlerThread;
import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.FBPhotoUploadTask;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.controller.StandardUiHandler;
import com.neusou.moobook.model.RemoteCallResult;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;

public class StreamPostQuickAction extends BaseActivity {
	public static final String LOG_TAG = Logger.registerLog(StreamPostQuickAction.class);
	
	static final int WAIT_ANIM_MAX_MILLISECONDS = 5000;
	AtomicBoolean mIsExit = new AtomicBoolean(false);

	static class StreamPostQuickActionInvocationData implements Parcelable {
		public int top = 0;
		public int right = 0;
		public int bottom = 0;
		public int left = 0;
		public int width = 0;
		public int height = 0;
	
		public static final String XTRA_PARCELABLE_OBJECT = StreamPostQuickActionInvocationData.class
				.getCanonicalName();

		static class Creator implements
				Parcelable.Creator<StreamPostQuickActionInvocationData> {

			@Override
			public StreamPostQuickActionInvocationData createFromParcel(Parcel source) {
				StreamPostQuickActionInvocationData d = new StreamPostQuickActionInvocationData();
				d.left = source.readInt();
				d.top = source.readInt();
				d.bottom = source.readInt();
				d.right = source.readInt();
				d.width = source.readInt();
				d.height = source.readInt();
				return d;
			}

			@Override
			public StreamPostQuickActionInvocationData[] newArray(int size) {
				return null;
			}

		}

		public static Creator CREATOR = new StreamPostQuickActionInvocationData.Creator();

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(left);
			dest.writeInt(top);
			dest.writeInt(bottom);
			dest.writeInt(right);
			dest.writeInt(width);
			dest.writeInt(height);
		}
	}

	public static final byte DIALOG_POSTINGCOMMENT = 2;
	
	public static final byte COMPONENT_COMMENTBAR = 1;

	// to hold extra values
	public static final boolean DEFAULT_SHOW_PHOTOBAR = false;
	public static final boolean DEFAULT_SHOW_COMMENTBAR = false;
	boolean mShowCommentBar = DEFAULT_SHOW_COMMENTBAR;
	boolean mShowPhotoBar = DEFAULT_SHOW_PHOTOBAR;
	String mHintEditText;
	String mHeaderText;
	String mObjId;
	int mFocusedView;

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
	Button mPostPhoto;
	Button mPostMovie;
	Button mUpload;
	Button mGetAlbums;
	Facebook mFacebook;
	ManagerThread mWorkerThread;
	Handler mUIHandler;
	ProgressDialog mProgressDialog;
	ViewAnimationListener mAnimationListener;
	ViewAnimationListener mUploadBarAnimationListener;

	public static final int REQUEST_PICKPHOTO = 4;

	View.OnClickListener mPostPhotoOnClick;
	View.OnClickListener mPostMovieOnClick;
	View.OnClickListener mUploadOnClick;

	FBPhotoUploadTask mAsyncPhotoUploader;

	private void resetWaitLatch() {
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
			Bundle b = intent.getExtras();
			
			if (action.equals(App.INTENT_POST_COMMENT)) {
				
				RemoteCallResult remoteCallResult = (RemoteCallResult) b.getParcelable(RemoteCallResult.XTRA_PARCELABLE_OBJECT);
				
				if (remoteCallResult.status) {
					Toast.makeText(App.INSTANCE, "Comment posted.", 2000)
							.show();
					StreamPostQuickAction.this.setResult(Activity.RESULT_OK);
				}else{
					StreamPostQuickAction.this.setResult(Activity.RESULT_CANCELED);
				}
				exit();
			}
			
			if (action.equals(App.INTENT_POST_STREAM)) {
				dismissDialog(DIALOG_PUBLISHING_STREAM);
				RemoteCallResult remoteCallResult = (RemoteCallResult) b.getParcelable(RemoteCallResult.XTRA_PARCELABLE_OBJECT);
				if (remoteCallResult.status) {
					Toast.makeText(App.INSTANCE, "Message published.", 2000)
							.show();
					StreamPostQuickAction.this.setResult(Activity.RESULT_OK);
				}else{
					StreamPostQuickAction.this.setResult(Activity.RESULT_CANCELED);
				}
				exit();
			}

		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
			
		getIntentExtras();
		
		Window mainWindow = getWindow();
		
		mainWindow.setBackgroundDrawable(null);
		
		
		int dimAmt = 70; //In my application this is actually a setting stored in preferences...
	//	mainWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		WindowManager.LayoutParams params = mainWindow.getAttributes();
		
		double containerHeight = mainWindow.getWindowManager().getDefaultDisplay().getHeight() - 48;
		
		mainWindow.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);
		mainWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
		//mainWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		
		params.dimAmount = 0;
		
		params.width = 200;//WindowManager.LayoutParams.WRAP_CONTENT;
		params.height =WindowManager.LayoutParams.WRAP_CONTENT;
		params.horizontalMargin = mInvocationData.left;
		float verticalMargin = (float) ((mInvocationData.top+48+mInvocationData.height) / containerHeight);
		
		
		params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
		
		Log.d(LOG_TAG,"cHeight:"+containerHeight+
				" verticalMargin: "+verticalMargin+
				" top:"+mInvocationData.top+
				" right:"+mInvocationData.right+
				" bottom:"+mInvocationData.bottom+
				" left:"+mInvocationData.left				
		);
		
		params.verticalMargin = verticalMargin; 		
		
		
		
		mainWindow.setAttributes(params);		
		setContentView(R.layout.streampostquickaction);
		bindViews();
		initObjects();
		
		initViews();
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
		mFacebook.registerOutHandler(R.id.outhandler_activity_post,
				mWorkerThread.getInHandler());
		mWorkerThread.setOutHandler(mUIHandler);
		//registerReceivers();
		
		if(! mLifecycleFlags.is(LifecycleFlags.ACTIVITYRESULT)){
		//	showHideViews();
		}
		
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

		
	@Override
	protected void onResume() {
		super.onResume();
		if (mFocusedView == COMPONENT_COMMENTBAR) {
			// mEditComment.requestFocus();

		}
				
		/*
		if(! mLifecycleFlags.is(LifecycleFlags.ACTIVITYRESULT)){
			showSoftKey();	
		}
		*/		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	protected void bindViews() {
		/*
		mMainWrapper = findViewById(R.id.mainwrapper);
		mPhotoBarWrapper = findViewById(R.id.photobar_wrapper);
		mCommentBarWrapper = findViewById(R.id.commentbar_wrapper);
		mUploadBarWrapper = findViewById(R.id.uploadbar_wrapper);
		mCommentBarHeader = (TextView) findViewById(R.id.commentbar_title);
		mPostComment = (Button) findViewById(R.id.postcomment);
		mEditComment = (EditText) findViewById(R.id.commentbox);
		mPostPhoto = (Button) findViewById(R.id.uploadphotofromgallery);
		mPostMovie = (Button) findViewById(R.id.uploadphotofromcamera);
		mPhotoPreviewImage = (ImageView) findViewById(R.id.photopreview);
		mPhotoInfoText = (TextView) findViewById(R.id.photoinfo);
		mUpload = (Button) findViewById(R.id.upload);
		mGetAlbums = (Button) findViewById(R.id.getalbums);
		*/
	}

	protected void initObjects() {
		mDHT = new DecoupledHandlerThread();
		mDHT.start();
	
		mUploadBarAnimationListener = new ViewAnimationListener(
				ViewAnimationListener.SHOW, mUploadBarWrapper);

		mProgressDialog = new ProgressDialog(this);
		mAnimationListener = new ViewAnimationListener(
				ViewAnimationListener.SHOW, mMainWrapper);
		
		mUIHandler = new StandardUiHandler(this, mProgressDialog,
				findViewById(R.id.adview_wrapper),
				findViewById(R.id.bottomheader)) {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
			}

			@Override
			public void onServerCallError() {
				super.onServerCallError();
				dismissDialog(DIALOG_PUBLISHING_STREAM);
				dismissDialog(DIALOG_POSTINGCOMMENT);
			}

			@Override
			public void onTimeoutError() {
				super.onTimeoutError();
				dismissDialog(DIALOG_PUBLISHING_STREAM);
				dismissDialog(DIALOG_POSTINGCOMMENT);
			};

			@Override
			public void onWsResponseError() {
				super.onWsResponseError();
				StreamPostQuickAction.this.setResult(Activity.RESULT_CANCELED);
				exit();
			};
		};

		mFacebook = Facebook.getInstance();
		mWorkerThread = App.INSTANCE.mManagerThread;
	}

	protected void initViews() {
		
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		switch(action){
			case MotionEvent.ACTION_OUTSIDE:{
				setResult(RESULT_CANCELED);
				finish();
				return true;
			}
		}
		
		return super.onTouchEvent(event);
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		/*
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			StreamPostQuickAction.this.setResult(Activity.RESULT_CANCELED);
			exit();
			return true;
		}*/
		return super.onKeyDown(keyCode, event);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		

		Logger.l(Logger.DEBUG, LOG_TAG, "onActivityResult() done");
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	public static int[] getPhotoDimension(Uri photoUri)
			throws FileNotFoundException, IOException {
		ContentResolver cr = App.INSTANCE.getContentResolver();
		AssetFileDescriptor thePhoto = cr
				.openAssetFileDescriptor(photoUri, "r");
		InputStream photoStream = thePhoto.createInputStream();
		BitmapFactory.Options bmpOption = new BitmapFactory.Options();
		bmpOption.inPurgeable = true;
		bmpOption.inInputShareable = true;
		bmpOption.inJustDecodeBounds = true;
		bmpOption.inPreferredConfig = Bitmap.Config.ARGB_8888;
		BitmapFactory.decodeStream(photoStream, null, bmpOption);
		return new int[] { bmpOption.outWidth, bmpOption.outHeight };
	}

	public static AssetFileDescriptor getAssetFileDescriptor(Uri uri)
			throws FileNotFoundException {
		ContentResolver cr = App.INSTANCE.getContentResolver();
		AssetFileDescriptor afd = cr.openAssetFileDescriptor(uri, "r");
		return afd;
	}

	static class UploadPhotoRunnable implements Runnable {

		Uri photoUri;
		WeakReference<Activity> mActivityWeakRef;
		Handler mDialogHandler;

		public UploadPhotoRunnable(Uri photoUri, Activity act, Handler dialogHandler) {
			this.photoUri = photoUri;
			mActivityWeakRef = new WeakReference<Activity>(act);
			mDialogHandler = dialogHandler;
		}

		public void run() {
			
			Message showDialogMessage = mDialogHandler.obtainMessage();
			showDialogMessage.arg1 = SHOW_DIALOG;
			showDialogMessage.what = DIALOG_PROCESSING_IMAGE;			
			showDialogMessage.sendToTarget();	
			
			// get the image dimension of the original image
			
			int[] photoDim = new int[] { -1, -1 };
			try {
				photoDim = getPhotoDimension(photoUri);
			} catch (final FileNotFoundException e) {
				Util.uiMakeToast(mActivityWeakRef.get(), "Photo not found. " + e.getMessage(), 1000);				
				return;
			} catch (final IOException e) {
				Util.uiMakeToast(mActivityWeakRef.get(), "Photo can not be accessed. " + e.getMessage(), 1000);
				return;
			}


			//Toast.makeText(mActivityWeakRef.get(),"w:" + photoDim[0] + ", h:" + photoDim[1], 1000).show();

			// compute the sampling size.

			/*
			 * mtv = 1/s * msv where s is a pos.integer mtv/msv = 1/s, s is a
			 * real number, msv/mtv = s , s2 is an pos integer. Use value of s2
			 * closest to s, i.e: s2 < s
			 */
			int targetMaxWidth = 1024;
			int targetMaxHeight = 768;
			int maxTargetValue = Math.max(targetMaxHeight, targetMaxWidth);
			int maxSourceValue = Math.max(photoDim[0], photoDim[1]);

			double s = maxSourceValue / maxTargetValue;
			int s2 = (int) Math.rint(s);

			if(s2 < 1){
				s2 = 1;
			}
			
			Util.uiMakeToast(mActivityWeakRef.get(), "sampling: "+s2, 1000);
			
			try {

				// read the image from the uri with sampling and then scale it
				// to our desired dimensions

				AssetFileDescriptor fileDescriptor = getAssetFileDescriptor(photoUri);
				BitmapFactory.Options bmpOption = new BitmapFactory.Options();
				bmpOption.inPurgeable = true;
				bmpOption.inInputShareable = true;
				bmpOption.inSampleSize = s2;
				InputStream stream2 = fileDescriptor.createInputStream();
				Bitmap image = BitmapFactory.decodeStream(stream2, null,
						bmpOption);

				
				
				// compute the calculated target dimensions that still maintains
				// the original aspect ratio.
				int targetWidth = Math.min(photoDim[0], targetMaxWidth);
				int targetHeight;
				double scale= ((double) photoDim[0]) / targetWidth;
				if(photoDim[0]<targetWidth){
					targetHeight = (int) (scale * photoDim[1]);	
				}else{
					targetHeight = (int) (photoDim[1]/scale);
				}
				
				Logger.l(Logger.DEBUG,LOG_TAG,"orig.w:"+photoDim[0]+", orig.h:"+photoDim[1]);
				Logger.l(Logger.DEBUG,LOG_TAG,"sampling:"+s2+", scale:"+scale+", tw:"+targetWidth+", th:"+targetHeight);
				Bitmap scaled = Bitmap.createScaledBitmap(image, targetWidth, targetHeight, true);

				// save the scaled image temporarily in the device cache.

				File cacheDir = mActivityWeakRef.get().getCacheDir();
				File tmp = new File(cacheDir.getPath() + "/photo");
				tmp.createNewFile();
				FileOutputStream fos = new FileOutputStream(tmp);
				
				Util.uiMakeToast(mActivityWeakRef.get(), "compressing..", 1000);
				boolean successCompress = scaled.compress(CompressFormat.JPEG,
						80, fos);
				
				Logger.l(Logger.DEBUG, LOG_TAG, "success compress? "
						+ successCompress);

				// get the stream of the saved image file

				FileInputStream uploadStream = new FileInputStream(tmp);
				final long length = tmp.length();
				
				Util.uiMakeToast(mActivityWeakRef.get(), "image size: "+length/1024+"KB", 1000);
				
				Bitmap preview = Bitmap.createScaledBitmap(image, 32, 32, true);
				FBPhotoUploadTask mAsyncPhotoUploader = new FBPhotoUploadTask(
						mActivityWeakRef.get(), uploadStream, length, preview);
				mAsyncPhotoUploader.execute(null);
				
				Util.uiMakeToast(mActivityWeakRef.get(), "now uploading image..", 2000);
			
				Message hideDialog = mDialogHandler.obtainMessage();
				hideDialog.arg1 = HIDE_DIALOG;
				hideDialog.what = DIALOG_PROCESSING_IMAGE;			
				hideDialog.sendToTarget();
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};

	static final int DIALOG_PROCESSING_IMAGE = 1;
	static final int DIALOG_PUBLISHING_STREAM = 2;
	
	protected android.app.Dialog onCreateDialog(int id) {
		ProgressDialog ad = new ProgressDialog(StreamPostQuickAction.this);
		
		switch(id){
			case DIALOG_PROCESSING_IMAGE:{				
				ad.setTitle("Processing Image");
				ad.setMessage("Please wait..");
				ad.setCancelable(false);
				ad.setCanceledOnTouchOutside(false);
				break;
			}
			case DIALOG_PUBLISHING_STREAM:{
				
				ad.setTitle("Publishing Message");
				ad.setMessage("Please wait..");
				ad.setCancelable(false);
				ad.setCanceledOnTouchOutside(false);	
				
				break;
			}
		}
		 
		return ad;
	};
	
	static final int SHOW_DIALOG = 1;
	static final int HIDE_DIALOG = 2;
	
	Handler mDialogHandler = new Handler(){
		public void handleMessage(Message msg) {
			int whichDialog = msg.what;
			Bundle b = msg.getData();
			int operation = msg.arg1;
			switch(operation){
				case SHOW_DIALOG:{
					showDialog(whichDialog);					
					break;
				}
				case HIDE_DIALOG:{
					dismissDialog(whichDialog);
					break;
				}
			}
		};
	};
	
	public void uploadPhoto(Uri photoUri) throws IOException {		
		mDHT.h.post(new UploadPhotoRunnable(photoUri, this, mDialogHandler));
	}

	public void showWrapper() {
		/*
		resetWaitLatch();
		Animation anim = AnimationUtils.loadAnimation(this,
				R.anim.scale_in);
		Interpolator p = AnimationUtils.loadInterpolator(this,
                android.R.anim.overshoot_interpolator);                
		anim.setInterpolator(p);
		anim.setDuration(700l);
		//anim.setDuration(mWrapperAnimDuration);
		anim.setAnimationListener(mAnimationListener);
		mMainWrapper.startAnimation(anim);
		*/
	}

	public void hideWrapper() {
		/*
		resetWaitLatch();		
		Animation anim = AnimationUtils.loadAnimation(StreamPostQuickAction.this, R.anim.scale_out);
		Interpolator p = AnimationUtils.loadInterpolator(this, android.R.anim.anticipate_interpolator);        
		//anim.setDuration(mWrapperAnimDuration);
		anim.setDuration(400l);		
		mAnimationListener.mType = ViewAnimationListener.HIDE;
		anim.setAnimationListener(mAnimationListener);
		mMainWrapper.startAnimation(anim);
		*/		
	}

	public void showSoftKey() {
		mDHT.h.postDelayed(new Runnable() {
			@Override
			public void run() {
				try {					
					if(mWaitAnimationLatch != null){
						mWaitAnimationLatch.await(WAIT_ANIM_MAX_MILLISECONDS,TimeUnit.MILLISECONDS);
						if(mIsExit != null && !mIsExit.get()){	
							//App.INSTANCE.showVirtualKeyboard(mEditComment);
						}
					}
				} catch (InterruptedException e) {
				}
			}
		}, 500l);
	}

	StreamPostQuickActionInvocationData mInvocationData;

	private void getIntentExtras() {
		Intent i = getIntent();
		Bundle b = i.getExtras();
		if (b != null) {
			if (b.containsKey(StreamPostQuickActionInvocationData.XTRA_PARCELABLE_OBJECT)) {
				mInvocationData = b.getParcelable(StreamPostQuickActionInvocationData.XTRA_PARCELABLE_OBJECT);				
			}
		}
	}

	//boolean mIsExiting = false;

	public void exit() {

		if (! mIsExit.get()) {
			mIsExit.set(true);			
		} else {
			return;
		}



		//hideWrapper();
		/*
		mDHT.h.postDelayed(new Runnable() {
			@Override
			public void run() {
				try {
					mWaitAnimationLatch.await(WAIT_ANIM_MAX_MILLISECONDS, TimeUnit.MILLISECONDS);
					finish();
				} catch (InterruptedException e) {
				}
			}
		}, 500l);
		*/
		
	}

	private void showUploadBar() {
		Animation anim = AnimationUtils.loadAnimation(this,
				R.anim.slide_current_from_top);
		mUploadBarAnimationListener.mType = ViewAnimationListener.SHOW;
		anim.setDuration(500);
		anim.setAnimationListener(mUploadBarAnimationListener);
		mUploadBarWrapper.startAnimation(anim);
	}

	private void hideUploadBar() {
		Animation anim = AnimationUtils.loadAnimation(this,
				R.anim.slide_to_top_from_current);
		mUploadBarAnimationListener.mType = ViewAnimationListener.HIDE;
		anim.setDuration(500);
		anim.setAnimationListener(mUploadBarAnimationListener);
		mUploadBarWrapper.startAnimation(anim);
	}

	private void showHideViews() {
		if (mShowCommentBar) {
			mCommentBarWrapper.setVisibility(View.VISIBLE);
		} else {
			mCommentBarWrapper.setVisibility(View.GONE);
		}

		if (mShowPhotoBar) {
			mPhotoBarWrapper.setVisibility(View.VISIBLE);
		} else {
			mPhotoBarWrapper.setVisibility(View.GONE);
		}

		showWrapper();
	}

	public void registerReceivers() {
		
	}

	public void unregisterReceivers() {
		
	}

	public static Intent getIntent(Context ctx) {
		Intent i = new Intent(ctx, StreamPostQuickAction.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		return i;
	}

	private void postMessage(){
		
	}
	
	private void postStream(){
		
	}
	
	private String filter(String txt){
		return txt.replace("&", " ").replace("+", " ");		
	}
	
	private void postComment() {
		Logger.l(Logger.DEBUG, LOG_TAG, "postComment()");

		showDialog(DIALOG_POSTINGCOMMENT);
		String comment = mEditComment.getText().toString();
		
		//filter out bad characters.
		comment = filter(comment);
		
		if (comment == null || comment.trim().length() == 0) {
			return;
		}

		Bundle callbackDescriptor = new Bundle();
		callbackDescriptor.putString(BaseManagerThread.XTRA_CALLBACK_INTENT_ACTION,	App.INTENT_POST_COMMENT);
		mFacebook.postComment(R.id.outhandler_activity_post,
				callbackDescriptor, comment, mObjId,
				ManagerThread.CALLBACK_POST_COMMENT,
				BaseManagerThread.CALLBACK_SERVERCALL_ERROR,
				BaseManagerThread.CALLBACK_TIMEOUT_ERROR);
		
		showDialog(DIALOG_POSTINGCOMMENT);
		//mProgressDialog.setTitle("Posting comment");
		//mProgressDialog.setMessage("Sending..");
		//mProgressDialog.show();
	}

}