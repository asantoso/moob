package com.neusou.moobook.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class WakefulIntentService extends IntentService {
	public static final String LOCK_NAME_STATIC = "com.neusou.moobook.service.WakefulIntentService";
	private static PowerManager.WakeLock lockStatic = null;

	public static void acquireStaticLock(Context context) {
		getLock(context).acquire();
	}

	synchronized private static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic == null) {
			PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME_STATIC);			
			lockStatic.setReferenceCounted(true);
		}
		return (lockStatic);
	}

	public WakefulIntentService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		getLock(this).release();
	}
}