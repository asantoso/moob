package com.neusou.moobook.controller;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.Address;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.neusou.moobook.activity.BaseUiHandler;
import com.neusou.moobook.thread.ManagerThread;

public class StandardUiHandler extends BaseUiHandler {
	ProgressDialog mProgressDialog;
	View mAdView;

	public StandardUiHandler(Context ctx, ProgressDialog pd, View adView) {
		super(ctx);
		mProgressDialog = pd;
		mAdView = adView;
	}

	public void handleMessage(android.os.Message msg) {
		int code = msg.what;		
		switch (code) {
		case ManagerThread.MESSAGE_DISMISS_DIALOG: {
			mProgressDialog.dismiss();
			break;
		}

		case ManagerThread.CALLBACK_ADMOB_ONFAILRECEIVE: {
			mAdView.setVisibility(View.GONE);
			break;
		}

		case ManagerThread.CALLBACK_ADMOB_ONRECEIVE: {
			mAdView.setVisibility(View.VISIBLE);
			break;
		}
		}
	}

	@Override
	public void onServerCallError() {
	
	}

	@Override
	public void onTimeoutError() {
		
	}

}