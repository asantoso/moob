package com.neusou.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.neusou.Logger;
import com.neusou.SoftHashMap;
import com.neusou.async.UserTask;


import android.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.ImageView;

public class ImageUrlLoader2 {
	
	SoftHashMap<String, Bitmap> drawablesMap;
	String sdcachepath;
	Bitmap DEFAULT_BITMAP;
	Object readImageLock = new Object();
	Context mContext;
	
	static final String DEFAULT_IMAGECACHEDIRECTORY = "cache";
	
	static final float cacheExpandFactor = 0.75f;
	static final int cacheInitialCapacity = 1;
	
	static final String LOG_TAG = "ImageUrlLoader2";
	
	//static HashMap<Integer, > mRegistry;
	
	public ImageUrlLoader2(Context ctx){		
		initObjects(ctx);
		getDefaultCachePath();
		createCacheDirectory();
	}
	
	public ImageUrlLoader2(Context ctx, String imageFolder){
		initObjects(ctx);
		sdcachepath = imageFolder; 
		File cacheDirectory = new File(sdcachepath);
		boolean isDirectory = cacheDirectory.isDirectory();
		if(!isDirectory){
			try{
				getDefaultCachePath();
			}catch(Exception e){
			}
		}else{
			if(!cacheDirectory.canRead()){
				createCacheDirectory();
			}			
		}
	}
	
	private void getDefaultCachePath(){
		String ess = Environment.getExternalStorageDirectory().toString();
		sdcachepath = ess + "/" + mContext.getPackageName()+"/"+DEFAULT_IMAGECACHEDIRECTORY;		
	}
	
	private void initObjects(Context ctx){
		mContext = ctx;
		drawablesMap = new SoftHashMap<String, Bitmap>(cacheInitialCapacity,cacheExpandFactor);		
		DEFAULT_BITMAP = null;
	}
	
	private boolean createCacheDirectory(){
		File path = new File(sdcachepath);
		return path.mkdirs();
	}
	
	public static interface AsyncListener{
		public void onPreExecute();
		public void onPostExecute(AsyncLoaderResult result);
		public void onCancelled();		
		public void onPublishProgress(AsyncLoaderProgress progress);
	}
	
	public static class AsyncLoaderInput{
		public String imageUri = null;
		public ImageView imageView;
		public long code = 0;
		public long groupCode = 0;
	}
	
	public static class AsyncLoaderProgress{		
		public String imageUri;
		public boolean success;
		public Bitmap bitmap;
		public ImageView imageView;
		public long code;
		public long groupCode;
		
		public void cleanUp(){
			imageUri = null;
			imageView = null;
			code = -1;
			groupCode = -1;
		}
	}
	
	public static class AsyncLoaderResult{
		public Bitmap drawable;
		public byte status = 0;
		public static final byte WAITING = 0;
		public static final byte SUCCESS = 1;
		public static final byte FAILURE = 2;
		public void cleanUp(){
			drawable = null;
		}
	}
	/*
	class AsyncImageLoader extends UserTask<AsyncLoaderInput, AsyncLoaderProgress, AsyncLoaderResult>{
		
		AsyncListener mListener;
		AsyncLoaderResult mResult = new AsyncLoaderResult();
		public static final String LOG_TAG = "AsyncImageLoader2";
		protected static BlockingQueue<Runnable> sWorkQueue;
	    protected static BlockingQueue<Runnable> sPendingQueue;
	    
		public AsyncImageLoader(AsyncListener listener){
			mListener = listener;
		}
		
		public void setListener(AsyncListener listener){
			mListener = listener;
		}
		
		private void cleanUp(){
			mListener = null;			
			mResult.cleanUp();
			mResult = null;
		}
		
		@Override
		protected AsyncLoaderResult doInBackground(AsyncLoaderInput... inputs) {
			if(inputs == null || inputs.length == 0){
				return null;
			}
			
			int numImages = inputs.length;
			
			String threadName = Thread.currentThread().getName();
			Logger.l(Logger.DEBUG, LOG_TAG, threadName+" [doInBackground()] loadImageAsync numImages:"+numImages);
			
			try{
				for(int i=0;i<numImages;i++){
					AsyncLoaderInput loadInput = inputs[i];
					String uri = loadInput.imageUri;
					
					Logger.l(Logger.DEBUG, LOG_TAG, threadName+" [doInBackground()] loadImageAsync ["+(i+1)+"/"+numImages+"] uri:"+ uri);
					Bitmap bitmap = loadImage(uri, false);								
					AsyncLoaderProgress progress = new AsyncLoaderProgress();
					progress.imageView = loadInput.imageView;
					progress.code = loadInput.code;
					progress.groupCode = loadInput.groupCode;
				
					Logger.l(Logger.DEBUG, LOG_TAG, threadName+" [doInBackground()] loadImageAsync ["+(i+1)+"/"+numImages+"] publishProgress");
					if(bitmap != null){
						progress.success = true;
						progress.bitmap = bitmap;						
						publishProgress(progress);
					}
					else{
						progress.success = false;
						progress.bitmap = null;
						publishProgress(progress);
					}
				}
				mResult.status = AsyncLoaderResult.SUCCESS;
			}
			catch(NullPointerException e){
				mResult.status = AsyncLoaderResult.FAILURE;		
				Logger.l(Logger.ERROR, LOG_TAG,	e.getMessage());
			}
						
			return mResult;			
		}
		
		@Override
		protected void onPreExecute() {		
			super.onPreExecute();
			if(mListener!=null){
				mListener.onPreExecute();
			}
		}
		
		@Override
		protected void onProgressUpdate(AsyncLoaderProgress... values) {		
			super.onProgressUpdate(values);
			if(mListener!=null){
				mListener.onPublishProgress(values[0]);
			}		
		}
		
		@Override
		protected void onPostExecute(AsyncLoaderResult result) {		
			super.onPostExecute(result);
			if(mListener!=null){
				mListener.onPostExecute(result);
			}			
			cleanUp();
		}
		
		@Override
		protected void onCancelled() {
			super.onCancelled();
			if(mListener!=null){
				mListener.onCancelled();
			}
			cleanUp();
		}
	}
	*/
	private String computeDigest(String uri){
		String s = uri;
		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");
			m.update(s.getBytes(), 0, s.length());
			BigInteger bi = new BigInteger(1, m.digest());			
			String bigInt = bi.toString(16);
			return bigInt;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	ArrayList<String> mFileAccessLocks = new ArrayList<String>(1);
	Object recordLock = new Object();
	
	private boolean isFileUnderProcess(String tag){
		if(!mFileAccessLocks.contains(tag)){
			return false;
		}
		return true;
	}
	private void acquireFileProcessing(String tag){
		mFileAccessLocks.add(tag);		
	}
	private void releaseFileProcessing(String tag){
		if(!mFileAccessLocks.isEmpty()){
			mFileAccessLocks.remove(tag);
		}
	}
	
	byte WRITE_TO_DISK_WAIT = 1;
	byte WRITE_TO_DISK_ERROR = 2;
	byte WRITE_TO_DISK_SUCCESS = 3;
	byte WRITE_TO_DISK_RECALL = 4;
	
	public byte writeToDisk(File imageFile, String savedFilename, String url){
		
		InputStream is;
		try{
			URL _url = new URL(url);			
			imageFile.createNewFile();
			is = (InputStream) _url.getContent();
			String canonPath = imageFile.getCanonicalPath();
			OutputStream os = new FileOutputStream(canonPath);
			int bufferSize = 1024*8;
			BufferedOutputStream bos = new BufferedOutputStream(os, bufferSize);
			BufferedInputStream bis = new BufferedInputStream(is, bufferSize);
			byte[] buffer = new byte[bufferSize];

			// Start reading image data and writing to sdcard.
			Logger.l(Logger.VERBOSE,LOG_TAG,"Start writing image data to disk. file path: "+imageFile.toString());
			int totalBytesRead = 0;
			while (true) {
				int numBytesRead = bis.read(buffer);				
				if (numBytesRead == -1) {
					break;
				}
				totalBytesRead += numBytesRead;
				bos.write(buffer, 0, numBytesRead);
			}
			Logger.l(Logger.VERBOSE,LOG_TAG,"Finished writing image data to disk. file size: "+totalBytesRead);
			// Finished reading image data and writing to sdcard.

			bos.close();
			bis.close();
			os.close();
			is.close();
		}catch(Exception e){
			Logger.l(Logger.ERROR,LOG_TAG, "[writeToDisk()] error: "+e.getMessage());
			if(imageFile.isFile() && imageFile.exists()){
				Logger.l(Logger.ERROR,LOG_TAG, "[writeToDisk()] deleting file..");
				boolean successDelete = imageFile.delete();
				Logger.l(Logger.ERROR,LOG_TAG, "[writeToDisk()] deletion successful? "+successDelete);
			}
			//releaseFileProcessing(digest);
			return WRITE_TO_DISK_ERROR;
		}
		//releaseFileProcessing(digest);
		
		return WRITE_TO_DISK_SUCCESS;
		
	}
	
	public void fetch(long code, long groupCode, String iconUri, ImageView imageView, Bitmap defaultBitmap,
			AsyncListener imageAsyncLoaderListener){
		Bitmap iconBmp = loadImage(iconUri, true);
		if (iconBmp != null) {
			imageView.setImageBitmap(iconBmp);
			iconBmp=null;
		}else{
			// we don't have the bitmap in the cache, so fetch it asynchronously.
			AsyncLoaderInput input = new AsyncLoaderInput();
			input.imageView = imageView;
			input.imageUri = iconUri;
			input.code = code;
			input.groupCode = groupCode;
			imageView.setImageBitmap(defaultBitmap);
			loadImageAsync(input, imageAsyncLoaderListener);
		}		
	}
	
	public void loadImageAsync(final AsyncLoaderInput uri, AsyncListener listener){
		//load image asynchronously..
		try{
			new AsyncImageLoader(listener).execute(uri);
		}catch(RejectedExecutionException e){
			Logger.l(Logger.WARN, LOG_TAG, "rejected executions. "+e.getMessage()); 
		}
	}

	public void loadImageAsync(final AsyncLoaderInput[] imageUris, AsyncListener listener) {
		//load image asynchronously..
		try{
			new AsyncImageLoader(listener).execute(imageUris);
		}catch(RejectedExecutionException e){
			Logger.l(Logger.WARN, LOG_TAG, "rejected executions. "+e.getMessage()); 
		}	
	}
	
	public Bitmap loadImage(final String imageUri, boolean immediately) {
		if(imageUri == null || imageUri.equals("null")){
			return null;
		}
		
		String threadName = Thread.currentThread().getName();
		Logger.l(Logger.DEBUG,LOG_TAG, threadName+" loadImage("+imageUri+","+immediately+")");
			
		final String url;
		
		try{
			url = URLDecoder.decode(imageUri);
		}catch(Exception e){
			return null;
		}
		
		if (url == null || url.compareTo("null") == 0) {			
			Logger.l(Logger.DEBUG,LOG_TAG, threadName+" [loadImage()] requested url is null");
			return null;
		}

			
		Bitmap dcached;
		dcached = drawablesMap.get(url);
				
		if (dcached != null || immediately) {
			if(dcached == null){
				drawablesMap.remove(url);
			}else{
				Logger.l(Logger.DEBUG,LOG_TAG, threadName+" [loadImage()] cached drawable is not null");	
			}
			return dcached;
		}

		File imageFile = null;
		File imageDir = new File(sdcachepath);
		String bigInt = computeDigest(url);
		String savedFilename = bigInt;			
		String ess = Environment.getExternalStorageState();
		boolean canReadImageCacheDir = false;
		boolean isExternalMediaMounted = false;
		
		try {
			
			//IF SDCARD can be accessed but the cache has not been created, then create cache directory
			//otherwise retrieve image from the network and save TO MEMORY CACHE AND RETURN THE IMAGE
			if(ess.compareTo(Environment.MEDIA_MOUNTED) == 0){
				if(!imageDir.canRead()){
					// Check for SDCARD availability				
					Logger.l(Logger.DEBUG,LOG_TAG, threadName+" Can not read SDCARD image directory: "+imageDir.toString());					
					boolean createSuccess = false;
					synchronized (imageDir) {
						createSuccess = imageDir.mkdirs();
					}			
					Logger.l(Logger.DEBUG,LOG_TAG, threadName+" Success create SDCARD image directory?: "+createSuccess);
					canReadImageCacheDir = createSuccess && imageDir.canRead();
				}
				canReadImageCacheDir = imageDir.canRead();
				isExternalMediaMounted = true;
			}
			
			else{
				// try to load image from the web
				try {					
					URL imageUrl = new URL(url);
					   HttpGet httpRequest = null;
		               try {
		            	   httpRequest = new HttpGet(imageUrl.toURI());
		               } catch (URISyntaxException e) {
		                   e.printStackTrace();
		               }

		               HttpClient httpclient = new DefaultHttpClient();
		               HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);

		               HttpEntity entity = response.getEntity();
		               BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
		               InputStream imageStream = bufHttpEntity.getContent();
		               
		               //InputStream imageStream = (InputStream) imageUrl.getContent();	
		               BufferedInputStream bis = new BufferedInputStream(imageStream,1024*10);
		               Bitmap d = BitmapFactory.decodeStream(bis);
		               imageStream.close();
		               
		               if (d != null) {
		            	   drawablesMap.put(url, d);
		            	   dcached = d;
						//return d;
		               } else {
		            	   dcached = DEFAULT_BITMAP;
						//return DEFAULT_PROFILE_DRAWABLE;
		               }
				} catch (Exception e) {
					Logger.l(Logger.ERROR,LOG_TAG, threadName+" [loadImage()] error trying to retrieve image from the web: "+e.getMessage());
					dcached = DEFAULT_BITMAP;
				}
				
				return dcached;
			}

			//IF EXTERNAL SDCARD STORAGE IS MOUNTED
			
			//IF SDCARD CAN BE ACCESSED THEN SAVE IMAGE DATA ON SDCARD
			
			
			Logger.l(Logger.DEBUG,LOG_TAG, threadName+" [loadImage()] Image URL:"+url+", MD5:" + bigInt);	
			imageFile = new File(sdcachepath, bigInt);
			
			byte writeToDiskStatus = 0; 
			
			if(canReadImageCacheDir){
			
							
				while(true){
					synchronized (mFileAccessLocks){					
						String name = Thread.currentThread().getName();
						Logger.l(Logger.DEBUG,"debug", "[loadImage()] current thread name: "+name+" , url:"+url);
						boolean underProcessing = isFileUnderProcess(savedFilename);
					
						if(underProcessing){
							Logger.l(Logger.DEBUG,"debug", "[loadImage()] current thread name: "+name+" has been put to waiting!.");
							//return WRITE_TO_DISK_WAIT;
							
							
							return null;
						
							//try{
							//	final long waitMillis = 10000;
							//	mFileAccessLocks.wait(waitMillis);
							//}catch(InterruptedException e){							
							//}
						
							
						}else{
							acquireFileProcessing(savedFilename);
							break;
						}					
					}
				}
				
				boolean imageFileIsFile = imageFile.isFile();
				boolean canReadImageFile = imageFile.canRead();
				
				long fileSize = imageFile.length();
				Logger.l(Logger.DEBUG, LOG_TAG, threadName+" [loadImage()] uri:"+url+", file length:"+fileSize+", isFile:"+imageFileIsFile);
				//if image not on SDCard or in SDCard but the size is 0 then
				//try retrieving the image from the web and write to disk
				//Note: if filesize is 0 then the file will be written over.
				String digest = computeDigest(url);
				if(!canReadImageFile || (imageFileIsFile && imageFile.length() == 0)){
					//synchronized(url){
						if(imageFile.exists() && imageFile.isFile()){
							imageFile.delete();
						}
						writeToDiskStatus = writeToDisk(imageFile, digest, url);					
						if(writeToDiskStatus == WRITE_TO_DISK_WAIT){
						
						//  	try{
						// 
						//		url.wait(5000);								
						//	}catch(InterruptedException e){		
						//		e.printStackTrace();
						//	}
						
							//do something when past time limit
						}else if(writeToDiskStatus == WRITE_TO_DISK_SUCCESS){
						//	url.notify();
						}else if(writeToDiskStatus == WRITE_TO_DISK_ERROR) {
							//url.notify();
						}
					//}
				}
				
				if (imageFileIsFile && imageFile.canRead()) {
				if(imageFile.length() == 0){
					try{
						Logger.l(Logger.WARN, LOG_TAG, threadName+" [loadImage()] saved file length is 0 bytes. deleting image file: "+bigInt);
						if(imageFile.exists() && imageFile.isFile()){
							imageFile.delete();
						}
					}catch(SecurityException e){
						e.printStackTrace();
					}
				}				
				//synchronized (readImageLock) {
					FileInputStream fis = new FileInputStream(imageFile);
					BufferedInputStream bis = new BufferedInputStream(fis, 1024*10);
					int available = fis.available();
					if (available > 0) {
						try{							
							Bitmap d = BitmapFactory.decodeStream(bis);
							fis.close();							
							bis.close();
							if (d == null) {
								Logger.l(Logger.ERROR, LOG_TAG, "[loadImage()] could not interpret saved image file. ");
								drawablesMap.remove(url);
								if(imageFile.exists() && imageFile.isFile()){
									imageFile.delete();
								}
								dcached = null;								
								
							} else {								
								drawablesMap.put(url, d);
								dcached = d;								
							}							
						}catch(java.lang.OutOfMemoryError e){
							e.printStackTrace();
							dcached = null;
						}
					}
				//}
			}
			}
			else{
				dcached = DEFAULT_BITMAP;
			}
			
			releaseFileProcessing(savedFilename);
			
		} catch (MalformedURLException e) {
			Logger.l(Logger.INFO,LOG_TAG, threadName+" MalformedURLException " + e.getMessage());
			dcached = null;
		} catch (IOException e) {
			Logger.l(Logger.INFO,LOG_TAG, threadName+" IOException " + e.getMessage());

			try {
				if (imageFile != null && imageFile.exists() && imageFile.isFile()) {
					imageFile.delete();
				}
			} catch (Exception e2) {
			}
			dcached = null;
		} finally{
					
		}
		
		
		return dcached;
	}
	
	static class AsyncImageLoader extends UserTask<AsyncLoaderInput, AsyncLoaderProgress, AsyncLoaderResult>{
		
		AsyncListener mListener;
		AsyncLoaderResult mResult = new AsyncLoaderResult();
		public static final String LOG_TAG = "AsyncImageLoader2";
		protected static BlockingQueue<Runnable> sWorkQueue;
	    protected static BlockingQueue<Runnable> sPendingQueue;
	    
		public AsyncImageLoader(AsyncListener listener){
			mListener = listener;
		}
		
		public void setListener(AsyncListener listener){
			mListener = listener;
		}
		
		private void cleanUp(){
			mListener = null;			
			mResult.cleanUp();
			mResult = null;
		}
		
		@Override
		protected AsyncLoaderResult doInBackground(AsyncLoaderInput... inputs) {
			if(inputs == null || inputs.length == 0){
				return null;
			}
			
			int numImages = inputs.length;
			
			String threadName = Thread.currentThread().getName();
			Logger.l(Logger.DEBUG, LOG_TAG, threadName+" [doInBackground()] loadImageAsync numImages:"+numImages);
			
			try{
				for(int i=0;i<numImages;i++){
					AsyncLoaderInput loadInput = inputs[i];
					String uri = loadInput.imageUri;
					
					Logger.l(Logger.DEBUG, LOG_TAG, threadName+" [doInBackground()] loadImageAsync ["+(i+1)+"/"+numImages+"] uri:"+ uri);
					Bitmap bitmap = loadImage(uri, false);								
					AsyncLoaderProgress progress = new AsyncLoaderProgress();
					progress.imageView = loadInput.imageView;
					progress.code = loadInput.code;
					progress.groupCode = loadInput.groupCode;
				
					Logger.l(Logger.DEBUG, LOG_TAG, threadName+" [doInBackground()] loadImageAsync ["+(i+1)+"/"+numImages+"] publishProgress");
					if(bitmap != null){
						progress.success = true;
						progress.bitmap = bitmap;						
						publishProgress(progress);
					}
					else{
						progress.success = false;
						progress.bitmap = null;
						publishProgress(progress);
					}
				}
				mResult.status = AsyncLoaderResult.SUCCESS;
			}
			catch(NullPointerException e){
				mResult.status = AsyncLoaderResult.FAILURE;		
				Logger.l(Logger.ERROR, LOG_TAG,	e.getMessage());
			}
						
			return mResult;			
		}
		
		@Override
		protected void onPreExecute() {		
			super.onPreExecute();
			if(mListener!=null){
				mListener.onPreExecute();
			}
		}
		
		@Override
		protected void onProgressUpdate(AsyncLoaderProgress... values) {		
			super.onProgressUpdate(values);
			if(mListener!=null){
				mListener.onPublishProgress(values[0]);
			}		
		}
		
		@Override
		protected void onPostExecute(AsyncLoaderResult result) {		
			super.onPostExecute(result);
			if(mListener!=null){
				mListener.onPostExecute(result);
			}			
			cleanUp();
		}
		
		@Override
		protected void onCancelled() {
			super.onCancelled();
			if(mListener!=null){
				mListener.onCancelled();
			}
			cleanUp();
		}
	}
	

}