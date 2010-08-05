package com.neusou.moobook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Debug.MemoryInfo;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.neusou.Logger;
import com.neusou.SoftHashMap;
import com.neusou.async.UserTaskExecutionScope;
import com.neusou.web.ImageUrlLoader2;
import com.neusou.web.ImageUrlLoader2.AsyncListener;

public class Util {

	public static String packageName;
	public static Context ctx;
	public static String sdcache_imagesfolder;
	public static Drawable defaultProfileStreamDrawable = null;

	public static final long SECONDS_WEEKS = 60 * 60 * 24 * 7;
	public static final long SECONDS_DAYS = 60 * 60 * 24;
	public static final long SECONDS_HOURS = 60 * 60;
	public static final long SECONDS_MINS = 60;

	public static boolean showLog = true;
	public static int LC_ONSTART = 0;
	public static int LC_ONRESUME = 1;
	public static int LC_ONDESTROY = 2;
	public static int LC_ONPAUSE = 3;
	public static int LC_ONCREATE = 4;
	public static int LC_ONLOWMEMORY = 5;
	public static int LC_ONRESTART = 6;
	public static int LC_ONSTOP = 7;

	private static StringBuffer sb = new StringBuffer();

	public static boolean seeLifecycle = true;
	Object readImageLock = new Object();
	public static SoftHashMap<String, Drawable> drawablesMap;
	static Object drawablesMapLock = new Object();

	public static void init(Context ctx) {
		packageName = ctx.getPackageName();
		sdcache_imagesfolder = "/sdcard/" + packageName;
		drawablesMap = new SoftHashMap<String, Drawable>(100, 0.75f);
		defaultProfileStreamDrawable = ctx.getResources().getDrawable(
				R.drawable.mobook2_64);
	}
/*
	public static void lc(Activity act, int code) {
		try {
			sb.delete(0, sb.length());
		} catch (StringIndexOutOfBoundsException e) {
		}

		sb.append(act.getClass().getCanonicalName());
		sb.append(", ");
		if (code == LC_ONSTART) {
			sb.append("ON_START");
		} else if (code == LC_ONRESUME) {
			sb.append("ON_RESUME");
		} else if (code == LC_ONDESTROY) {
			sb.append("ON_DESTROY");
		} else if (code == LC_ONPAUSE) {
			sb.append("ON_PAUSE");
		} else if (code == LC_ONCREATE) {
			sb.append("ON_CREATE");
		} else if (code == LC_ONLOWMEMORY) {
			sb.append("ON_LOWMEMORY");
		} else if (code == LC_ONRESTART) {
			sb.append("ON_RESTART");
		} else if (code == LC_ONSTOP) {
			sb.append("ON_STOP");
		}

		if (seeLifecycle)
			Logger.l(Logger.DEBUG,"lifecycle", sb.toString());

	}
*/
	public static double getSimpleElementValueAsDouble(Document doc,
			String elementName) throws NumberFormatException,
			NoSuchFieldException, IllegalArgumentException {
		if (doc == null) {
			throw new IllegalArgumentException();
		}
		try {
			String value;
			value = getSimpleElementValue(doc, elementName);
			return Double.parseDouble(value);
		} catch (DOMException e) {
			e.printStackTrace();
			throw new NoSuchFieldException();
		}

	}

	public static String getSimpleElementValue(Document doc, String elementName)
			throws NoSuchFieldException, DOMException, NullPointerException,
			IllegalArgumentException {
		if (doc == null) {
			throw new IllegalArgumentException();
		}
		NodeList nodeList = doc.getElementsByTagName(elementName);
		if (nodeList == null) {
			throw new NoSuchFieldException();
		}
		int num = nodeList.getLength();
		if (num == 0) {
			throw new NoSuchFieldException();
		}
		Node node = nodeList.item(0);
		return node.getFirstChild().getNodeValue();
	}

	public static Address getAddressFromGeoLocation(Context context,
			double latitude, double longitude) throws IOException,
			IllegalArgumentException {
		Geocoder geocoder = new Geocoder(context);
		List<Address> addresses = geocoder.getFromLocation(latitude, longitude,
				1);
		if (addresses == null || addresses.size() == 0) {
			return null;
		}
		return addresses.get(0);
	}

	public static String getNodeAttributeValue(Node node, String attName) {
		try {
			NamedNodeMap nnm = node.getAttributes();
			return nnm.getNamedItem(attName).getNodeValue();
		} catch (Exception e) {
			return null;
		}
	}

	/*
	 * public Drawable loadImage(final String url, boolean immediately) {
	 * 
	 * Log.d("agus", "UTIL: trying to load image: " + url);
	 * 
	 * if (url == null || url.compareTo("null") == 0) { //Log.d("agus",
	 * "UTIL:  url is null"); return null; }
	 * 
	 * Drawable dcached; //synchronized(drawablesMapLock){ int size =
	 * drawablesMap.size();
	 * Logger.l(Logger.DEBUG,"Util","drawablesMap size: "+size); dcached =
	 * drawablesMap.get(url); //}
	 * 
	 * if (dcached != null || immediately) { Log.d("agus",
	 * "UTIL:  cached is not null"); //Log.d("agus",
	 * "UTIL:  cached is not null ? " + (dcached != // null)); if(dcached ==
	 * null){ drawablesMap.remove(url); } return dcached; }
	 * 
	 * Log.i("agus", "trying to load from sdcard "+sdcache_imagesfolder);
	 * 
	 * File imageFile = null; File imageDir = new File(sdcache_imagesfolder);
	 * 
	 * try {
	 * 
	 * if (!imageDir.canRead()) {// Check for SDCARD availability Log.i("agus",
	 * "can not read sdcard cached image directory"); // Log.i("agus",
	 * "trying to create image direcory " // + imageDir.getAbsolutePath());
	 * boolean createDirSuccess = imageDir.mkdirs(); Log.i("agus",
	 * "is create success? " + createDirSuccess);
	 * 
	 * // try to load from the web try { URL imageUrl = new URL(url);
	 * InputStream imageStream = (InputStream) imageUrl .getContent(); Drawable
	 * d = Drawable.createFromStream(imageStream, url); if (d != null) {
	 * drawablesMap.put(url, d); return d; } else { return
	 * defaultProfileStreamDrawable; } } catch (Exception noConnectionEx) {
	 * return defaultProfileStreamDrawable; } }
	 * 
	 * String s = url; MessageDigest m = MessageDigest.getInstance("MD5");
	 * m.update(s.getBytes(), 0, s.length()); BigInteger bi = new BigInteger(1,
	 * m.digest()); String bigInt = bi.toString(16); //Log.i("agus",
	 * "img url: "+url+", MD5: " + bigInt);
	 * 
	 * imageFile = new File(sdcache_imagesfolder, bigInt);
	 * 
	 * //if image not on SDCard or the if it is in SDCard but the size is 0 then
	 * //try retrieving the image from the web //if filesize is 0 then the file
	 * will be overwritten if (!imageFile.canRead() || imageFile.length() == 0)
	 * { // Log.i("agus", "can not read cached image file on sdcard"); URL _url
	 * = new URL(url); imageFile.createNewFile(); InputStream is; try{ is =
	 * (InputStream) _url.getContent(); }catch(Exception e){ return null; }
	 * String canonPath = imageFile.getCanonicalPath(); OutputStream os = new
	 * FileOutputStream(canonPath); BufferedOutputStream bos = new
	 * BufferedOutputStream(os, 256); BufferedInputStream bis = new
	 * BufferedInputStream(is, 256); byte[] buffer = new byte[256];
	 * 
	 * // Log.i("agus", // "STARTED reading image data and writing to sdcard");
	 * 
	 * while (true) { int numBytesRead = bis.read(buffer); if (numBytesRead ==
	 * -1) { break; } bos.write(buffer, 0, numBytesRead); }
	 * 
	 * // Log.i("agus", //
	 * " FINISHED reading image data and writing to sdcard");
	 * 
	 * bos.close(); bis.close(); os.close(); is.close(); }
	 * 
	 * if (imageFile.canRead()) {
	 * 
	 * synchronized (readImageLock) { FileInputStream fis = new
	 * FileInputStream(imageFile);
	 * 
	 * //Log.i("agus", // "can read cached image on sdcard cachefile:" // +
	 * imageFile.getAbsolutePath() + " url:" + url // + ", fis null?:" + (fis ==
	 * null)); int available = fis.available(); // Log.i("agus", "available: " +
	 * available); if (available > 0) { try{ Drawable d =
	 * Drawable.createFromStream(fis, url); fis.close(); // Log.i("agus",
	 * "is loaded null? "+(d==null)); if (d == null) { drawablesMap.remove(url);
	 * return null; } else { // maps.put(nid, d); drawablesMap.put(url, d);
	 * return d; } }catch(java.lang.OutOfMemoryError e){ e.printStackTrace(); }
	 * } }
	 * 
	 * }
	 * 
	 * return null;
	 * 
	 * } catch (MalformedURLException e) { // Log.e("Util",
	 * "MalformedURLException " + e.getMessage()); return null; } catch
	 * (IOException e) { // Log.e("Util", "IOException " + e.getMessage());
	 * 
	 * try { if (imageFile != null) { imageFile.delete(); } } catch (Exception
	 * e2) { }
	 * 
	 * return null; } catch (NoSuchAlgorithmException e) { // Log.e("Util",
	 * "NoSuchAlgorithmException " + e.getMessage()); return null; } }
	 */
/*
	public static void logi(String tag, String m) {
		if (Util.showLog) {
			Log.i(tag, m);
		}
	}

	public static void loge(String tag, String m) {
		if (Util.showLog) {
			Log.e(tag, m);
		}
	}

	public static void logd(String tag, String m) {
		if (Util.showLog) {
			Log.d(tag, m);
		}
	}
*/
	
	static Date date = new Date();

	static CharSequence commentDateFormatPattern = "d MMM yyyy '('E 'at' k:mm')'";
	static CharSequence commentDayAndTimeFormatPattern = "'('E 'at' k:mm')'";

	// enumerated 2 to 24 hours in terms of # of seconds
	static long hoursEnums[];
	// 60*60,60*60*2,60*60*3,60*60*4,60*60*5,60*60*6

	// enumerated 2 to 60 minutes in terms of # of seconds
	static long minutesEnums[];

	static {
		int start;
		int end;
		int count;

		start = 0;
		end = 60;
		count = end - start + 1;
		minutesEnums = new long[end - start + 1];
		for (int i = 0; i < count; i++) {
			minutesEnums[i] = (i + start) * SECONDS_MINS;
		}

		start = 0;
		end = 24;
		count = end - start + 1;
		hoursEnums = new long[end - start + 1];
		for (int i = 0; i < count; i++) {
			hoursEnums[i] = (i + start) * SECONDS_HOURS;
		}
	};

	public static String createElapsedString(long elapsedSecs,
			long nowMilliseconds, long originalSecs) {

		sb.delete(0, sb.length());
		boolean addTime = false;

		if (elapsedSecs < SECONDS_MINS) { // within a minute, 0 secs < time < 1
											// minute
			if (elapsedSecs == 1) {
				sb.append(" one second");
			} else {
				sb.append(elapsedSecs);
				sb.append(" seconds");
			}
		}

		else if (elapsedSecs < SECONDS_HOURS) { // within an hour 60 secs = 1
												// minute <= time < 1 hr = 60*60
												// secs
			long minutes = 0; // = elapsedSecs / SECONDS_MINS;

			minutes = binarySearch(minutesEnums, elapsedSecs);

			/*
			 * for(int i=0; i< 59; i++){ if(elapsedSecs < minutesEnums[i]){
			 * minutes = i+1; break; } }
			 */

			long secs = elapsedSecs - (minutes * SECONDS_MINS);
			// long secs = (elapsedSecs % SECONDS_MINS);

			if (minutes == 1) {
				sb.append(" one minute");
			} else {
				sb.append(minutes);
				sb.append(" minutes");
			}
			if (secs > 0) {
				sb.append(" ");
				sb.append(secs);
				sb.append(" secs");
			}
		}

		else if (elapsedSecs < SECONDS_DAYS) { // within a day
			long hours = 0;// = elapsedSecs / SECONDS_HOURS;
			long minutes = 0;

			/*
			 * for(int i=0; i<23; i++){ if(elapsedSecs < hoursEnums[i]){ hours =
			 * i+1; break; } }
			 */

			hours = binarySearch(hoursEnums, elapsedSecs);

			// minute seconds combination
			// 59*60 = 3600 - 60 = 3540
			// 3000 + 540

			// hours minutes combination
			// 23*60 = 24*60 - 60 = 1200 + 240 - 60 = 1440 - 60 = 1380
			// 1200 + 180 = 1380

			// total = 1380 + 3540 = 4920

			long tmp = (elapsedSecs - (hours * SECONDS_HOURS));
			minutes = binarySearch(minutesEnums, tmp);
			/*
			 * for(int i=0; i<59; i++){ if(tmp < minutesEnums[i]){ minutes =
			 * i+1; break; } }
			 */

			// long minutes = (elapsedSecs - (hours * SECONDS_HOURS)) / 60;
			// long minutes = (elapsedSecs % SECONDS_HOURS) / SECONDS_MINS;

			if (hours == 1) {
				sb.append(" one hour");
			} else {
				sb.append(hours);
				sb.append(" hours");
			}
			if (minutes > 0) {
				sb.append(" ");
				sb.append(minutes);
				sb.append(" mins");
			}
		}

		else if (elapsedSecs < SECONDS_WEEKS) { // within one week
			addTime = true;
			long days = elapsedSecs / SECONDS_DAYS;
			if (days == 1) {
				sb.append(" yesterday at ");
				date.setTime(originalSecs * 1000);
				int hours_i = date.getHours();
				int minutes_i = date.getMinutes();
				String hours_dig = (hours_i < 10 ? "0" : "") + hours_i;
				String minutes_dig = (minutes_i < 10 ? "0" : "") + minutes_i;
				String time = hours_dig + ":" + minutes_dig;
				sb.append(time);
				return sb.toString();
			} else {
				sb.append(days);
				sb.append(" days");
			}
		}

		else {
			date.setTime(originalSecs * 1000);
			String formattedDate = DateFormat.format(commentDateFormatPattern,
					date).toString();
			sb.append(formattedDate);
			return sb.toString();
		}

		sb.append(" ago.");

		if (addTime) {
			sb.append(" ");
			date.setTime(originalSecs * 1000);
			String formattedDate = DateFormat.format(
					commentDayAndTimeFormatPattern, date).toString();
			sb.append(formattedDate);
		}

		return sb.toString();
	}

	public static long binarySearch(final long data[], final long search) {
		int start = 0;
		int end = data.length - 1;
		// int mid = (start - end) >> 1 ;

		// [x0][x1][x2][x3]
		// if there exists x0,x1 s.t: x0 < y < x1 then return x0
		// if there exists x0 s.t: x0 == y then return x0

		// ex: 9decimal = 1001binary, shift 1 bit to the left becomes 100binary
		// = 4decimal
		while (true) {
			int mid = (start + end) >> 1;

			if (data[mid] == search) {
				return mid;
			}
			if (start == end) {
				return start;
			}
			// special condition A that's always true : when [start][end] then
			// mid = start
			if (data[mid] > search) {
				end = mid;
			} else if (data[mid] < search) {
				start = mid;
				if (end - start == 1) { // special condition A
					return start;
				}
			}
		}
	}

	public static int boundIndex(int x0, int x, int x1) {
		if (x0 != -1) { // ignore the left side if x0 is -1
			if (x < x0) {
				return x0;
			}
		}
		if (x1 != -1) {// ignore the left side if x1 is -1
			if (x > x1) {
				return x1;
			}
		}
		return x;
	}

	static final String LOG_TAG = "Util";

	public static void writeBitmapToLocalCache(Bitmap bmp, String filename)
		throws FileNotFoundException{
		FileOutputStream fos = new FileOutputStream(filename);
		bmp.compress(CompressFormat.JPEG, 100, fos);
		
	}
	
	public static Bitmap readBitmapFromLocalCache(String filename) throws FileNotFoundException{
		File imageFile = new File(filename);		
		FileInputStream fis = new FileInputStream(imageFile);
		return BitmapFactory.decodeStream(fis);		
	}
	
	public static void writeStringToLocalCache(Context ctx, String data, String filename) {
		Logger.l(Logger.DEBUG, LOG_TAG,"[saveToLocalCache()] filename: "+ filename.toString());
		
		//File cacheDir = ctx.getCacheDir();
		File cacheDir = ctx.getFilesDir();
		Logger.l(Logger.DEBUG, LOG_TAG, cacheDir.toString());
		
		File cacheFile = new File(cacheDir.getPath() + "/" + filename);
		try {
			cacheFile.createNewFile();
			FileWriter fw = new FileWriter(cacheFile);
			StringReader sr = new StringReader(data);
			char[] buffer = new char[512];
			while (true) {
				int numRead = sr.read(buffer);
				Logger.l(Logger.VERBOSE, LOG_TAG, "numread:"+numRead);
				if(numRead == -1){
					break;
				}
				fw.write(buffer,0,numRead);				
			}
			fw.flush();
			fw.close();
			sr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String readStringFromLocalCache(Context ctx, String filename) throws FileNotFoundException {
		//File cacheDir = ctx.getCacheDir();
		File cacheDir = ctx.getFilesDir();
		
		Logger.l(Logger.DEBUG, LOG_TAG, cacheDir.toString());
		File cacheFile = new File(cacheDir.getPath() + "/" + filename);
		if(!cacheFile.canRead()){
			Logger.l(Logger.ERROR, LOG_TAG,"[readStringFromLocalCache()] can not read file:"+cacheFile.toString());
			throw new FileNotFoundException();
		}
		BufferedReader br = null;
		try {			
			FileReader fr = new FileReader(cacheFile);
			br = new BufferedReader(fr);
			String out = "";
			while(true){
				String line = br.readLine();
				if(line == null){
					break;
				}
				out += line;				
			}
			br.close();
			return out;
		} catch (IOException e) {
			e.printStackTrace();
			try{
				if(br!=null){			
					br.close();
				}
			}catch(IOException e2){				
			}
			return null;
		}
		
	}
	
	public static String getNotNullString(String value){
		if(value == null || value.compareToIgnoreCase("null") == 0){
			return "";
		}
		return value;
	}
	
	public static String toCSV(long[] ids, String quote){
		StringBuffer sb = new StringBuffer();
		if(quote == null){quote = "";}
		if(ids != null){
			String uids_list;
			sb.append("(");			
			for(int i=0;i<ids.length;i++){
				sb.append(quote);
				sb.append(ids[i]);
				sb.append(quote);
				//uids_list += quote+ids[i]+quote;
				if(i < ids.length - 1){
					sb.append(",");
					//uids_list += ",";
				}
			}
			sb.append(")");
			//uids_list += ")";
			return sb.toString();
		}
		return "";
	}
	
	
	
	public static void spitOut(InputStream is){
		try{
		BufferedReader breader = new BufferedReader(new InputStreamReader(is,Charset.forName("UTF-8")));
		int linecount=0;
		String response = "";
		while(true){
			String line = breader.readLine();				
			if(line == null){
				break;					
			}
			linecount++;
			response += line;
			Log.d(LOG_TAG,"[line#:"+linecount+"] "+line);
		}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void clearStringBuffer(StringBuffer sb){
		sb.delete(0, sb.length());
	}
	 
	public static void dumpCursor(Cursor c){
		int N = c.getColumnCount();
		String ret = "";
		for(int i=0;i<N;i++){
			ret += c.getColumnName(i)+":"+c.getString(i)+"; \n";			
		}
		Logger.l(Logger.DEBUG, LOG_TAG, ret);
	}
		
	public static void fetchImage(UserTaskExecutionScope scope, ImageView imageView, Bitmap defaultBitmap, String imageUrl, AsyncListener listener){
		Bitmap d = App.mImageUrlLoader2.loadImage(imageUrl, true);
		if (d == null) {
			ImageUrlLoader2.AsyncLoaderInput input = new ImageUrlLoader2.AsyncLoaderInput();
			App.mImageUrlLoader2.loadImageAsync(scope, input, listener);
			d = defaultBitmap;
		}
		imageView.setImageBitmap(d);	
	}
	
	static MemoryInfo mi = new MemoryInfo();
	
	public static void getMemoryInfo(){
		
		android.os.Debug.getMemoryInfo(mi);		
		int tsd = mi.getTotalSharedDirty();
		int tpd = mi.getTotalPrivateDirty();
		int pss = mi.getTotalPss();

		int gfc = android.os.Debug.getGlobalFreedCount();
		int gac = android.os.Debug.getGlobalAllocCount();
		int gfs = android.os.Debug.getGlobalFreedSize();
		int geac = android.os.Debug.getGlobalExternalAllocCount();
		long nhfs = android.os.Debug.getNativeHeapFreeSize();
		long nhs = android.os.Debug.getNativeHeapSize();
		int tac = android.os.Debug.getThreadAllocCount();
		int tas = android.os.Debug.getThreadAllocSize();
		int teac = android.os.Debug.getThreadExternalAllocCount();
		int teas = android.os.Debug.getThreadExternalAllocSize();
		
		
		Logger.l(Logger.DEBUG, LOG_TAG,"gfs:"+gfs+", gfc:"+gfc+ ", gac:"+gac+", geac:"+geac);
		Logger.l(Logger.DEBUG, LOG_TAG, "nhfs:"+nhfs+", nhs:"+nhs);
		Logger.l(Logger.DEBUG, LOG_TAG, "tac:"+tac+", tas:"+tas+", teac:"+teac+", teas:"+teas);
		
	}
	
	public static boolean filterImageByDimension(Bitmap bmp, int minW, int minH){		
		int w = bmp.getWidth();
		int h = bmp.getHeight();
		if(w < minW || h < minH){
			return false;
		}
		return true;
	}
	
	public static void uiMakeToast(final Activity ctx, final String message, final int length){
		ctx.runOnUiThread(
			new Runnable() {						
				@Override
				public void run() {
					Toast.makeText((Context)ctx, message, length).show();								
				}
			}	
		);	
	}
		
	public static String join(Collection<String> s, String delimiter) {
	    if (s == null || s.isEmpty()) return "";
	    Iterator<String> iter = s.iterator();
	    StringBuilder builder = new StringBuilder(iter.next());
	    while( iter.hasNext() )
	    {
	        builder.append(delimiter).append(iter.next());
	    }
	    return builder.toString();
	}

	public static String getNotNull(String s, String alt){
		if(s==null){
			return alt;
		}else{
			return s;			
		}
	}


}