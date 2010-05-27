package com.neusou.moobook.activity;

import android.app.AlarmManager;
import android.app.Service;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

import com.neusou.Logger;
import com.neusou.moobook.App;
import com.neusou.moobook.R;
// GallerySettings
//
// This is the setting screen for Gallery.
// It reads the available settings from an XML resource.
public class AppSettings extends PreferenceActivity implements 
OnSharedPreferenceChangeListener,
OnPreferenceChangeListener,
OnPreferenceClickListener

{
	
	public static final String LOG_TAG = "AppSettings";
    
    CheckBoxPreference mEnableNotifsCheckBox;
    RingtonePreference mRingtoneList;
    Preference mClearLocalCache; 
    Preference mClearSdCardCache;
    CheckBoxPreference mSdCardCacheCheckBox;
    ListPreference mPollingIntervalList;
    
    Resources mResources;
    
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.preferences);
        mResources = getResources();
        bindViews();
        initViews();
    }
    
    public void onResume(){
        super.onResume();
    	//String fileName = getPreferenceManager().getSharedPreferencesName();
    	//Logger.l(Logger.DEBUG, LOG_TAG, "shared preferences name: "+fileName);
        
        updateNotifRingtoneUi();
        updateNotifIntervalUi();
        
        try{
        	int length = mPollingIntervalList.getEntries().length;
        	Logger.l(Logger.DEBUG, LOG_TAG, "# entries:"+length);
        }catch(Exception e){
        	
        }
        
        try{
        	int length =  mPollingIntervalList.getEntryValues().length;
        	Logger.l(Logger.DEBUG, LOG_TAG, "# entry values:"+length);
        }catch(Exception e){
        	
        }
        
    }
    
    private void bindViews(){    	
    	PreferenceScreen ps = getPreferenceScreen();
    	
    	mEnableNotifsCheckBox = (CheckBoxPreference)(ps.findPreference(mResources.getString(R.string.preference_notification_enabled)));
    	mRingtoneList = (RingtonePreference)(ps.findPreference(mResources.getString(R.string.preference_notification_sound_resource)));
    	//mRingtoneCheckBox = (CheckBoxPreference)(ps.findPreference(mResources.getString(R.string.preference_notification_sound_enabled)));
    	mClearLocalCache = (Preference)(ps.findPreference(mResources.getString(R.string.preference_cache_local_clear)));
    	mClearSdCardCache = (Preference)(ps.findPreference(mResources.getString(R.string.preference_cache_sdcard_clear)));
    	mSdCardCacheCheckBox = (CheckBoxPreference) (ps.findPreference(mResources.getString(R.string.preference_cache_sdcard_enabled)));
    	mPollingIntervalList =  (ListPreference) (ps.findPreference(mResources.getString(R.string.preference_notification_checkpollinginterval)));
    }
    
    private void initViews(){
    	  mEnableNotifsCheckBox.setOnPreferenceChangeListener(this);
    	  mRingtoneList.setOnPreferenceChangeListener(this);
    	  mClearLocalCache.setOnPreferenceClickListener(this);
    	  mClearSdCardCache.setOnPreferenceClickListener(this);
    	  mSdCardCacheCheckBox.setOnPreferenceChangeListener(this);
    	  mPollingIntervalList.setOnPreferenceChangeListener(this);
    }
        
    private void updateNotifRingtoneUi(){								
		Uri ringtoneContentUri = App.INSTANCE.getNotificationRingtoneUri();
		if(ringtoneContentUri != null){
			String ringtoneName = RingtoneManager.getRingtone(App.INSTANCE,ringtoneContentUri).getTitle(App.INSTANCE);
			mRingtoneList.setSummary(ringtoneName);
		}else{		
			mRingtoneList.setSummary("Silent");
		}	
	
    }
    
    private void updateNotifIntervalUi(){    	
		int pollingInterval = App.INSTANCE.getNotificationPollingInterval();		
		mPollingIntervalList.setSummary(
				pollingInterval+" minutes"
		);
    }
    
    private void updateNotifIntervalUi(int pollingInterval){    	
		mPollingIntervalList.setSummary(
				pollingInterval+" minutes"
		);
    }
  
    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	
    	Logger.l(Logger.DEBUG, LOG_TAG, "[onSharedPreferenceChanged()] key: "+key);
    	/*
        // Let's do something when my counter preference value changes
        if (key.equals(KEY_MY_PREFERENCE)) {
            Toast.makeText(this, "Thanks! You increased my count to "
                    + sharedPreferences.getInt(key, 0), Toast.LENGTH_SHORT).show();
        }
        */
    }

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		Logger.l(Logger.DEBUG, LOG_TAG, "[onPreferenceChange()] pref: "+preference.getKey()+", value: "+newValue);
		String key = preference.getKey();
		if(key.equals(mResources.getString(R.string.preference_notification_sound_resource))){
			String ringtoneContentUri = (String) newValue;
			Uri newRingtone;
			try{
				newRingtone = Uri.parse(ringtoneContentUri);
				App.INSTANCE.loadNotificationRingtone(newRingtone);
				App.INSTANCE.mNotifRingtoneUri = newRingtone;
			}catch(NullPointerException e){
				App.INSTANCE.mNotifRingtoneUri = null;	
			}			
			updateNotifRingtoneUi();
		}
		else if(key.equals(mResources.getString(R.string.preference_notification_checkpollinginterval))){			
			
			try{
				int pollingIntervalSeconds = Integer.parseInt((String)newValue);
				updateNotifIntervalUi(pollingIntervalSeconds);				
				int pollingIntervalMilliseconds = pollingIntervalSeconds * 1000;
				App.INSTANCE.updateNotificationAlarm(true, pollingIntervalMilliseconds);		
				
			}catch(NumberFormatException e){
			}catch(NullPointerException e){				
			}
		}
		else if(key.equals(mResources.getString(R.string.preference_notification_enabled))){
			boolean isNotifEnabled = (Boolean) newValue;
			App.INSTANCE.updateNotificationAlarm(isNotifEnabled);
		}
		
		return true;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		Logger.l(Logger.DEBUG, LOG_TAG, "[onPreferenceClick()] pref: "+preference.getKey());
		String key = preference.getKey();
		
		if(key.equals(mResources.getString(R.string.preference_notification_sound_resource))){
			
		}
		else if(key.equals(mResources.getString(R.string.preference_cache_sdcard_enabled))){
			
		}		
		else if(key.equals(mResources.getString(R.string.preference_cache_sdcard_clear))){
			
		}		
		else if(key.equals(mResources.getString(R.string.preference_cache_local_clear))){
			
		}		
		
		return true;
	}
	
	
}
