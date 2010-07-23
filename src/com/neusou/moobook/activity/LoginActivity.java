package com.neusou.moobook.activity;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Enumeration;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.FBApp;
import com.neusou.moobook.FBPermissions;
import com.neusou.moobook.FBSession;
import com.neusou.moobook.FBWSResponse;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.R;
import com.neusou.moobook.data.ContextProfileData;
import com.neusou.moobook.model.database.ApplicationDBHelper;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;
import com.neusou.moobook.view.JSONArrayListAdapter;
import com.neusou.moobook.view.TitleBar;

public class LoginActivity extends BaseActivity implements CommonActivityReceiver.IBaseReceiver {

	static final String LOG_TAG = "LoginActivity";
	boolean fb_login_success = false;

	long FBSessionLastCheck = 0;
	String fb_session_key = "";
	String fb_session_secret = "";
	String fb_session_sig = "";
	long fb_session_uid = 0;
	int fb_session_expires = 0;
	String fb_session_json;// holds the session string (in the form of JSON
	// object)
	// that is outputted in the URL after the login is
	// successful

	// holds the current session state
	
	WebView mWebView;
	WebChromeClient mWCC;
	WebViewClient mWVCLogin;
	WebViewClient mWVCPermissions;
	ProgressBar mLoadingIndicator;
	// TextView mTopHeaderText;
	TextSwitcher mTopHeaderText;
	RelativeLayout mWebViewContainer;

	boolean isLoadingPermissionPage = false;
	boolean isLoadingLoginPage = true;
	boolean isSigningIn = false;
	boolean flag_permissionfinish_onnextpageload = false;
	int remainingPermissions = 12;

	String mLblFacebookLoginLoading = "Loading Facebook Login Page";
	String mLblFacebookLoginPage = "Please login to Facebook";
	String mLblFacebookSigningIn = "Connecting to Facebook...";
	String mLblFacebookPerms = "Moob\n -You're now logged in to Facebook-";

	//public static final int CALLBACK_CHECKSESSION = 0;
	public static final int CALLBACK_ERROR = 6;

	// activity states flag
	public static final String XTRAKEY_FORWARD_TO_FACEBOOK_SIGN_IN = "state.fbsession.isvalid";

	// only used when session has been in invalid state, so that
	// requesting for a session check will just ignore the request
	// and redirect user to sig in.
	boolean flag_forwardToFacebookSignin = false;

	static final int DIALOG_QUITTING = 0;
	static final long HANDLER_SEND_MESSAGE_DELAY = 50l;
	static final int CALLER_CHECKUSERSESSION = 808001;
	public final int TOAST_ERROR_DURATION = 3000; // in milliseconds

	Handler mThreadUIHandler = new WebLoginHandler();
	Handler mCallbackHandler = new CallbackHandler();

	JSONArrayListAdapter mAdapter;
	Facebook mFacebook;

	/*
	public static final String template = "method=${method}" + "&v=${v}"
	+ "&call_id=${call_id}" + "&api_key=${api_key}"
	+ "&session_key=${session_key}" + "&sig=${sig}"
	+ "&format=${format}";
	*/
	
	private boolean[] perms;

	Button mNext;
	TextView mPermissions;
	
	class CallbackHandler extends Handler {
		final private String name = "CallbackHandler";

		public void handleMessage(Message msg) {
			Log.d(LOG_TAG, name + " handleMessage msg.what:" + msg.what);

			Bundle data = msg.getData();
			String response = data.getString(Facebook.XTRA_RESPONSE);
			int bytelength = data.getInt(Facebook.XTRA_RESPONSE_BYTELENGTH);

			Log.d(LOG_TAG, "response: " + response);
			Log.d(LOG_TAG, "bytelength: " + bytelength);

			FBWSResponse fbresponse = FBWSResponse.parse(response);

			switch (msg.what) {

			case CALLBACK_ERROR: {
				// first check if connection to server was successful
				int servercallstatus = data.getInt(Facebook.XTRA_SERVERCALL_STATUS_CODE);
				if (servercallstatus == Facebook.SERVERCALL_ERROR) {					
					int errorCode = data.getInt(Facebook.XTRA_SERVERCALL_ERROR_CODE);
					final String reason = data.getString(Facebook.XTRA_SERVERCALL_ERROR_MSG);
					Toast.makeText(LoginActivity.this, errorCode+" : "+reason, 1000).show();
					return;
				}
				break;
			}

		

			}
		}
	}

	class WebLoginHandler extends Handler {

		final private String name = "WebLoginHandler";

		public void handleMessage(Message msg) {
			Log.d(LOG_TAG, name + " handleMessage msg.what:" + msg.what);

			switch (msg.what) {

			case ManagerThread.CALLBACK_SESSION_VALIDATED: {
				Toast.makeText(LoginActivity.this,
						"Facebook session has been validated.", 3000).show();
				break;
			}

			case ManagerThread.CALLBACK_SESSION_EXPIRED: {
				uiOnSessionValidated(false);
				break;
			}

			case ManagerThread.CALLBACK_SESSION_VALID: {
				uiOnSessionValidated(true);
				CookieSyncManager csm = CookieSyncManager.getInstance();
				csm.sync();
				break;
			}

			case ManagerThread.CALLBACK_LOGIN_TO_FACEBOOK: {
				
				Logger.l(Logger.DEBUG, LOG_TAG, name + "  callback login to facebook AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" );
						
				// String ip = getLocalIpAddress();
				// Log.d(LOG_TAG, ip);

				if (mWebView != null) {
					Logger.l(Logger.DEBUG, LOG_TAG, name + "Loading login page..");
					
					String loginUrl = createFBLoginURL(FBApp.api_key);
					Logger.l(Logger.DEBUG, LOG_TAG, "login url: "+loginUrl);
					
					
					// sometimes mWebView is null since a
					// message can arrive considerably late,
					// when orientation changes alot and
					// WebView got killed and recreated
					// constantly.
					mWebViewContainer.setVisibility(View.VISIBLE);
					mWebView.setVisibility(View.VISIBLE);
					mWebView.setWebViewClient(mWVCLogin);
					mWebView.loadUrl(loginUrl);

				} else {
					Logger.l(Logger.DEBUG, LOG_TAG, "[WebLoginHandler][handleMessage()]"+name);
					return;
				}
				break;
			}

			}
			Log.d(LOG_TAG, name + " finished handling message msg.what:"	+ msg.what);
		}

	};

	
	public static Intent getIntent(Context ctx) {		
		return new Intent(ctx, LoginActivity.class);
	}
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	    getWindow().setBackgroundDrawableResource(R.drawable.metal);	        
		setContentView(R.layout.login_activity);
		bindViews();
		initObjects();
		initViews();
	}

	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	protected void onStop() {
		super.onStop();
	//	App.INSTANCE.saveSessionInfo();
	}

	@Override
	protected void onResume() {
		super.onResume();
		//mThreadUIHandler.sendEmptyMessage(ManagerThread.CALLBACK_LOGIN_TO_FACEBOOK);
		//mWebView.loadUrl("http://www.facebook.com");
		mWebView.clearCache(true);
		mWebView.clearFormData();
		mWebView.clearHistory();
		
		mWebView.loadUrl(createFBLoginURL(FBApp.api_key));
	}

	@Override
	protected void onDestroy() {
/*
		mWebViewContainer.removeAllViews();
		mWebViewContainer = null;
		mWebView.destroy();
		mWebView = null;
	*/	
		mCommonReceiver.selfUnregister(this);
		super.onDestroy();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		super.onCreateDialog(id);
		// Dialog b = new Dialog(this);

		// b.setTitle("Go back?");
		// return b;

		switch (id) {
		case DIALOG_QUITTING: {
			AlertDialog ad = new AlertDialog.Builder(this).create();
			ad.setCancelable(true);
			ad.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					Log.d(LOG_TAG, " dialog cancelled");
				}
			});
			ad.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					Log.d(LOG_TAG, " dialog dismissed");
				}
			});
			ad.setTitle("Go back ?");

			ad.setButton(DialogInterface.BUTTON_POSITIVE, "Ok",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Log.d(LOG_TAG, " dialog pressed " + which);
							dismissDialog(DIALOG_QUITTING);
							finish();
						}
					});

			ad.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Log.d(LOG_TAG, " dialog pressed " + which);
						}

					}

			);

			try {
				ad.setIcon(R.drawable.mobook2_exclaimation_64);
			} catch (Exception e) {

			}

			return ad;
		}
		default: {
			return null;
		}
		}

	}

	View.OnClickListener mNextOnClick;
	
	protected void bindViews() {
		mLoadingIndicator = (ProgressBar) findViewById(R.id.loadingindicator);
		mWebViewContainer = (RelativeLayout) findViewById(R.id.stub);
		mTopHeaderText = (TextSwitcher) findViewById(R.id.topheader);
		mNext = (Button) findViewById(R.id.next);
		mPermissions = (TextView) findViewById(R.id.permissions);
	}

	TitleBar mTitleBar;
	
	protected void initViews() {
		getWindow().setBackgroundDrawable(null);
		
		mTitleBar = new TitleBar(this);
		
		mWebView = new WebView(this);
		mWebView.setBackgroundColor(Color.WHITE);
		mWebView.setHorizontalScrollBarEnabled(false);
		mWebView.setClickable(true);
		mWebView.setWebViewClient(mWVCLogin);
		
		final String iPhoneUserAgent = "Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A543a Safari/419.3 ";
		final String firefoxUserAgent = "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2) Gecko/20100222 Ubuntu/10.04 (lucid) Firefox/3.6";
		final String nexusOneUserAgent = "Mozilla/5.0 (Linux; U; Android 2.1; en-us; Nexus One Build/ERD62) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Mobile Safari/530.17";
		final String fcua = "FacebookConnect";
		mWebView.clearCache(true);
		
		WebSettings settings = mWebView.getSettings();		
		settings.setUserAgentString(fcua);
		settings.setSupportZoom(true);	
		settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		settings.setSavePassword(false);
		settings.setSaveFormData(false);
		settings.setJavaScriptEnabled(true);
		settings.setJavaScriptCanOpenWindowsAutomatically(true);
		settings.setLoadsImagesAutomatically(true);

		mWebViewContainer.addView(mWebView);
		mWebViewContainer.setVisibility(View.VISIBLE);
		mWebView.setVisibility(View.VISIBLE);
		
		mTopHeaderText.setFactory(new ViewFactory() {
			@Override
			public View makeView() {
				TextView t = new TextView(LoginActivity.this);
				t = (TextView) mLayoutInflater.inflate(
						R.layout.t_topheadertext, null);
				return t;
			}
		});
		
		mNext.setVisibility(View.INVISIBLE);
		mNext.setOnClickListener(mNextOnClick);
	}

	CommonActivityReceiver mCommonReceiver;
	
	
	
	
		
		
	protected void initObjects() {
		
		mCommonReceiver = new CommonActivityReceiver(this);
		mCommonReceiver.selfRegister(this);
		
		mFacebook = Facebook.getInstance();
		mFacebook.setOutHandler(mCallbackHandler);
		
		isLoadingLoginPage = true;

		mWVCPermissions = new WebViewClient() {
			int count = 3;
			int FLAG_LOADING_PERMISSIONS = 3;
			int FLAG_TALKING_TO_SERVER = 2;
			int FLAG_FINISH_PERMISSIONS = 0;

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				Logger.l(Logger.DEBUG, LOG_TAG, "[Permissions][onPageFinished()] " + url);
				mLoadingIndicator.setVisibility(View.INVISIBLE);

				if (url.indexOf("prompt_permissions.php") == -1
						|| count == FLAG_FINISH_PERMISSIONS) { 
					// still have
					// permissions
					// to ask
					
					
					
					
					/*
					mTopHeaderText.setText("List of permissions granted to Moobook");
					boolean perms[] = extractPermissionsFromURL(url);
					if(perms!=null){
						onSuccessfulPermissionsRequest(perms);
					}
										
					String[] permsfullnames = FBPermissions.getFullNames(perms);
					String out = "";
					for(int i=0,N=permsfullnames.length;i<N;i++){
						out += permsfullnames[i]==null?"":permsfullnames[i]+"\n";
					}
					
					Logger.l(Logger.DEBUG, LOG_TAG, "[Permissions][onPageFinished()] " + out);
					mPermissions.setText(out);
					mPermissions.setVisibility(View.VISIBLE);
						
					*/
					
					mNext.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				Log.d(LOG_TAG, "PageStarted: " + url);
				mLoadingIndicator.setVisibility(View.VISIBLE);
				if (count == FLAG_LOADING_PERMISSIONS) {
					mTopHeaderText.setText(mLblFacebookPerms);
				}
				count--;
			}

			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				Logger.l(Logger.DEBUG,LOG_TAG,"[WebViewClient][onReceivedError()]");
				super.onReceivedError(view, errorCode, description, failingUrl);
				Toast.makeText(LoginActivity.this,description,	TOAST_ERROR_DURATION);
				Message msg = mThreadUIHandler.obtainMessage(ManagerThread.CALLBACK_LOGIN_TO_FACEBOOK);
				msg.sendToTarget();
			}
		};

		mWVCLogin = new WebViewClient() {

			boolean blockaccess_passwordreset = false;
			boolean blockaccess_apppageview = false;
			boolean blockaccess_registerfacebookacc = false;
			boolean blockaccess_canceledlogin = false;

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				
				Logger.l(Logger.DEBUG, LOG_TAG, "[Login] [onPageFinished()] url:" + url);
				mLoadingIndicator.setVisibility(View.INVISIBLE);

				// Do nothing when already blocked.
				if (blockaccess_apppageview || blockaccess_passwordreset
						|| blockaccess_registerfacebookacc
						|| blockaccess_canceledlogin) {

					// reset blockaccess flags
					blockaccess_apppageview = false;
					blockaccess_passwordreset = false;
					blockaccess_registerfacebookacc = false;
					blockaccess_canceledlogin = false;

					return;
				}

				if (isFBLoginSuccess(url)) {
					Logger.l(Logger.DEBUG, LOG_TAG, "[Login] [onPageFinished()] Successful Login");
					
					if (isSigningIn) {
						isSigningIn = false;
						// TODO: refactor Toast string to xml file
						Toast.makeText(LoginActivity.this,	"Successfully logged into Facebook", 2000)		.show();
						Logger.l(Logger.DEBUG, LOG_TAG,"Successfully logged in to Facebook");
						FBSession fbSession = parseFbLoginResponseUrl(url);					
						flag_forwardToFacebookSignin = false; // toggle 
						isLoadingPermissionPage = true;
						onSuccessfulLogin(fbSession);

					} else if (isLoadingPermissionPage
							&& !flag_permissionfinish_onnextpageload) {
						isLoadingPermissionPage = false;
						flag_permissionfinish_onnextpageload = true;
						view.pageDown(true);
						view.scrollBy(0, 100);
					}
				} else {
					
					Logger.l(Logger.DEBUG, LOG_TAG, "Unsuccessful Login attempt !");
					// unsuccessful log in
					// can be due to multiple reasons
					// 1. incorrect password
					// 2. wrong username
					// 3. clicking on specified blocked links

					// set flag to automatically forward user to the sign in
					// page
					// next time a request to check session is initiated.
					flag_forwardToFacebookSignin = true;

					// reset the top header text
					// set header text from "Connecting.." to
					// "Please Login to Facebook"
					mTopHeaderText.setText(mLblFacebookLoginPage);
				}
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				Log.d(LOG_TAG, "[WebViewClient] [PageStarted()] url: " + url);
				super.onPageStarted(view, url, favicon);
				
				// Block webview from going to Facebook reset page
				if (url.indexOf(FBApp.login_cancel_url) == 0) {
					view.stopLoading();
					blockaccess_canceledlogin = true;
					onCancelledLogin();
				}

				// Block webview from going to Facebook reset page
				if (url.indexOf(FBApp.resetpassword_url) == 0) {
					Toast.makeText(LoginActivity.this, "Reset password?", 1000)
							.show();
					blockaccess_passwordreset = true;
					view.stopLoading();
				}

				// Block webview from going to Moobook facebook application info
				if (url.indexOf(FBApp.app_url) > 0) {
					Toast.makeText(LoginActivity.this, "Moobook", 1000).show();
					blockaccess_apppageview = true;
					view.stopLoading();
				}

				// Block webview from going to Facebook registration link
				if (url.indexOf(FBApp.register_fb_acc) > 0) {
					Toast.makeText(LoginActivity.this, "Blocked", 1000).show();
					blockaccess_registerfacebookacc = true;
					view.stopLoading();
				}

				if (blockaccess_apppageview || blockaccess_passwordreset
						|| blockaccess_registerfacebookacc
						|| blockaccess_canceledlogin) {
					isSigningIn = false;
					return;
				} else {// if not block then call super
					super.onPageStarted(view, url, favicon);
				}

				mLoadingIndicator.setVisibility(View.VISIBLE);

				if (flag_permissionfinish_onnextpageload) {
					Log.d(LOG_TAG, "finish permissions asking");
					mTopHeaderText.setText("moobook permissions for Facebook");
					return;
				}

				if (!fb_login_success && url.indexOf("login.php") > 0) {
					isSigningIn = true;
					mTopHeaderText.setText(mLblFacebookSigningIn);
					return;
				}

				if (isLoadingPermissionPage) {
					mTopHeaderText.setText(mLblFacebookPerms);
				}

			}

			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				super.onReceivedError(view, errorCode, description, failingUrl);
				Logger.l(Logger.DEBUG, LOG_TAG, "[LoginClient:WebViewClient] [onReceivedError()] WVCError. " + description + ", " + failingUrl);
				Toast.makeText(LoginActivity.this, description,	TOAST_ERROR_DURATION).show();	
			}

		};
		
		mNextOnClick = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(LoginActivity.this, StreamActivity.class);
				ContextProfileData cpd;
				cpd = App.createSessionUserContextProfileData();
				i.putExtra(StreamActivity.XTRA_STREAMMODE, Facebook.STREAMMODE_LIVEFEED);
				i.putExtra(ContextProfileData.XTRA_PARCELABLE_OBJECT, cpd);
				startActivity(i);
				finish();
			}
			
		};
		
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.d(LOG_TAG, "onSaveInstanceState");
		mWebView.saveState(outState);
		outState.putBoolean(XTRAKEY_FORWARD_TO_FACEBOOK_SIGN_IN,
				flag_forwardToFacebookSignin);
	
	}

	@Override
	protected void onRestoreInstanceState(Bundle outState) {
		super.onRestoreInstanceState(outState);
		Log.d(LOG_TAG, "onRestoreInstanceState");
		mWebView.restoreState(outState);
		flag_forwardToFacebookSignin = outState.getBoolean(
				XTRAKEY_FORWARD_TO_FACEBOOK_SIGN_IN,
				flag_forwardToFacebookSignin);
		
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return super.onRetainNonConfigurationInstance();
	}
	
	@Override
	public Object getLastNonConfigurationInstance() {
		return super.getLastNonConfigurationInstance();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(LOG_TAG, "onConfigurationChanged");
	}

	private boolean isFBLoginSuccess(String url) {
	
		int i = url.indexOf(FBApp.login_success_url);
		fb_login_success = (i == 0) ? true : false;
		if (fb_login_success) {
			return true;
		}
		return false;
	}

	/**
	 * Parses the session information contained in the url
	 * 
	 * @param url
	 * @return FBSession the session instance
	 */
	private FBSession parseFbLoginResponseUrl(String url) {
		FBSession fbsession = new FBSession();
	
		Logger.l(Logger.DEBUG, LOG_TAG, "[parseFBLoginResponse()] "+url);
		String decoded = URLDecoder.decode(url);
		int q = decoded.indexOf("?");
	
		// remove everything before the ?, i.e: get the response query
		// parameters
		String args = decoded.substring(q + 1, decoded.length());
	
		// Log.d(LOG_TAG, args);
		String args_arr[] = args.split("&", 2);
	
		try {
			// remove parameter key name
			fb_session_json = args_arr[0].substring(8, args_arr[0].length());
			Log.d(LOG_TAG, "raw json:" + fb_session_json);
			JSONObject session = new JSONObject(fb_session_json);
			fb_session_key = session.getString("session_key");
			fb_session_uid = session.getLong("uid");
			try {
				fb_session_expires = Integer.parseInt(session
						.getString("expires"));
			} catch (NumberFormatException e) {
				fb_session_expires = 0;
			}
			fb_session_secret = session.getString("secret");
			fb_session_sig = session.getString("sig");
	
			Log.d(LOG_TAG, "fb session key: " + fb_session_key);
			Log.d(LOG_TAG, "fb session uid: " + fb_session_uid);
			Log.d(LOG_TAG, "fb session expires: " + fb_session_expires);
			Log.d(LOG_TAG, "fb session secret: " + fb_session_secret);
			Log.d(LOG_TAG, "fb session sig: " + fb_session_sig);
			Log.d(LOG_TAG, "fb session json: " + fb_session_json);
	
		} catch (JSONException e) {
			Log.d(LOG_TAG, "JSONObject constructor error");
			e.printStackTrace();
			return null;
		}
	
		fbsession.key = fb_session_key;
		fbsession.uid = fb_session_uid;
		fbsession.expires = fb_session_expires;
		fbsession.secret = fb_session_secret;
		fbsession.sig = fb_session_sig;
		fbsession.json = fb_session_json;
		return fbsession;
	}

	private String createFBPermissionRequestURL(String sessionJson, String permsCsv) {
		try {
			return "http://www.facebook.com/connect/prompt_permissions.php?"
					+ "api_key="
					+ FBApp.api_key
					+ "&session="
					+ URLEncoder.encode(sessionJson, FBApp.utf_8)
					+ "&v=1.0&extern=1&next=http://www.facebook.com/connect/login_success.html?xxRESULTTOKENxx&enable_profile_selector=1&ext_perm="
					+ permsCsv;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	// TODO: refactor string to xml
	private void onCancelledLogin() {
		Toast.makeText(LoginActivity.this, "Login cancelled", 1000).show();
		showDialog(DIALOG_QUITTING);
	}

	// TODO: refactor string to xml
	private void onSuccessfulLogin(FBSession session) {
		Logger.l(Logger.DEBUG, LOG_TAG, "[onSuccessfulLogin()]");
		Toast.makeText(LoginActivity.this, "Login successful", 1000).show();
		App.INSTANCE.saveSessionInfo(session);
		mFacebook.setSession(session);
		requestPermissions(session.json);
	}

	private void onSuccessfulPermissionsRequest(boolean[] permissions) {
		
		//ApplicationDBHelper dbh = new ApplicationDBHelper(this);
		//dbh.insertPermissions(permissions);
		//dbh.close();
	}

	private void requestPermissions(String sessionJson) {
		if (mWebView == null) {
			// check for null because repeated orientation change will call
			// onDestroy
			// and thus destroy the WebView, and also because
			// of the delay when calling handler to post a message.
			return;
		}
		if (App.INSTANCE.mFbSession == null) {
			return;
		}

		Toast.makeText(LoginActivity.this, "Requesting Facebook authorization",
				1000).show();
		String perms_csv = FBPermissions.generateAll();

		String url = createFBPermissionRequestURL(sessionJson, perms_csv);
		if (url == null) {
			Log.e(LOG_TAG, "can not create permissions request url");
		}
		mWebView.setWebViewClient(mWVCPermissions);
		mWebView.loadUrl(url);
	}

	private String createFBLoginURL(String apikey) {
		
		
		String url = "http://www.facebook.com/login.php?api_key="
				+ apikey
				// +
				// "&fbconnect=false&v=1.0&return_session=true&display=popup&next=http://www.facebook.com/connect/login_success.html&cancel_url=http://www.facebook.com/connect/login_failure.html&session_key_only=true";

				+ "&v=1.0&return_session=true&display=popup&next=http://www.facebook.com/connect/login_success.html&cancel_url=http://www.facebook.com/connect/login_failure.html&session_key_only=true";

		// req_perms=email,read_mailbox,publish_stream,read_stream,offline_access,create_event,rsvp_event,photo_upload,status_update,video_upload,create_note,share_item";
		
		Logger.l(Logger.DEBUG, LOG_TAG, "[createFBLoginURL()] url: "+url);
		 
		return url;
	}

	// /



	private void uiOnSessionValidated(boolean isValid) {

		if (isValid) {
			Toast.makeText(LoginActivity.this,
					"Facebook session is still valid", 2300).show();
		} else {
			Toast.makeText(LoginActivity.this, "Facebook session has expired",
					2300).show();
		}
	}

	private boolean[] extractPermissionsFromURL(String url) {
		Logger.l(Logger.DEBUG, LOG_TAG, "[extractPermissionsFromUrl()] url: "+url);
		if (url == null || url.length() == 0) {
			return null;
		}
		try {
			url = StringEscapeUtils.unescapeHtml(url);
			url = URLDecoder.decode(url, "UTF-8");
			url = url.substring(url.indexOf("?") + 1, url.length());
			url = url.substring(0, url.indexOf("&"));
			String perms[] = url.split(",", FBPermissions.TOTAL_PERMISSIONS);
			Logger.l(Logger.DEBUG, LOG_TAG, "[extractPermissionsFromUrl()] "+perms.length + " perms granted: " + url);
			return FBPermissions.generateFlags(perms);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (StringIndexOutOfBoundsException e){
			e.printStackTrace();
		}
		
		return null;
	}

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {

		}
		return null;
	}




	private String loadPermissions() {

		ApplicationDBHelper dbh = new ApplicationDBHelper(this);
		// Log.d("agus", "load permissions 1");
		Cursor c = dbh.getAllPermissions(App.INSTANCE.mFbSession.uid);
		// Log.d("agus", "load permissions 2");
		String perms_csv = "";
		// Log.d("agus", "load permissions 3");
		if (c != null && c.getCount() > 0) {
			// Log.d("agus", "load permissions 3a: count:" + c.getCount());
			perms = FBPermissions.parseCursor(c);
			c.close();
			dbh.close();
			// Log.d("agus", "load permissions 3a1: " + perms);
			if (perms != null) {
				String permsCSV[] = FBPermissions.fromBooleansToStrings(perms,
						false, false);
				StringBuilder sb = new StringBuilder();

				for (int i = 0, maxi = permsCSV.length; i < maxi; i++) {
					// Log.d("agus", permsCSV[i]);
					sb.append(permsCSV[i]);
					if (i < maxi - 1) {
						sb.append(",");// TODO refactor comma
					}
				}
				perms_csv = sb.toString();
				// Log.d("agus", "load permissions 3af");
			}
		} else {
			// Log.d("agus", "load permissions 3b");
			// TODO refactor Toast messages
			// Toast.makeText(LoginActivity.this,
			// "Requesting Facebook authorization",10000).show();
			perms_csv = FBPermissions.generateAll();
			// Log.d("agus", "load permissions 3b1");
		}

		// Log.d(LOG_TAG, "permissions csv: " + perms_csv);
		// Log.d("agus", "load permissions done");
		return perms_csv;
	}


	
	@Override
	public void showSessionUserData() {
		
	}

}
