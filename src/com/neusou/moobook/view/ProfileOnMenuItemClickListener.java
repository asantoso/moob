package com.neusou.moobook.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

import com.neusou.moobook.App;
import com.neusou.moobook.AppMenu;
import com.neusou.moobook.Facebook;
import com.neusou.moobook.activity.HomeActivity;
import com.neusou.moobook.activity.ViewPhotosActivity;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;

public class ProfileOnMenuItemClickListener implements OnMenuItemClickListener{
		String name;
		String imageUri;
		int outHandlerKey;
		long actor_id;
		Activity ctx;
		ProgressDialog pd;
			
			public ProfileOnMenuItemClickListener(String name, String imageUri, int outHandlerKey, long actor_id, ProgressDialog pd, Activity ctx) {
				this.name = name;
				this.imageUri = imageUri;
				this.outHandlerKey = outHandlerKey;
				this.actor_id = actor_id;
				this.ctx = ctx;
				this.pd = pd;
			}
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				int itemId = item.getItemId();
				switch(itemId){
					case AppMenu.CONTEXT_MENUITEM_VIEW_PHOTOS:{
						break;
					}
					case AppMenu.CONTEXT_MENUITEM_VIEW_PROFILE:{
						break;
					}
					case AppMenu.CONTEXT_MENUITEM_VIEW_TAGGED_PHOTOS:{
						Bundle data = new Bundle();
						data.putString(ManagerThread.XTRA_CALLBACK_INTENT_ACTION, App.INTENT_GET_TAGGED_PHOTOS);
						data.putLong(Facebook.param_uid, actor_id);
						Facebook.INSTANCE.getTaggedPhotos(
								outHandlerKey,
								data,
								actor_id,
								ViewPhotosActivity.NUM_PHOTOS_PER_REQUEST, 0,
								ManagerThread.CALLBACK_GET_TAGGED_PHOTOS,
								BaseManagerThread.CALLBACK_SERVERCALL_ERROR,
								BaseManagerThread.CALLBACK_TIMEOUT_ERROR, 0);
						pd.setTitle("Loading");
						pd.setMessage("Getting tagged photos");
						pd.show();
						return true;						
					}
					case AppMenu.CONTEXT_MENUITEM_VIEW_WALL:{						
						App.showUserWall(ctx, actor_id);						
						break;
					}
				}
				return false;
			}
			
		}