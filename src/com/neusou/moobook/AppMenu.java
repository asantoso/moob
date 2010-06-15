package com.neusou.moobook;

import com.neusou.moobook.activity.StreamActivity;
import com.neusou.moobook.activity.ViewPhotosActivity;
import com.neusou.moobook.thread.BaseManagerThread;
import com.neusou.moobook.thread.ManagerThread;
import com.neusou.web.ImageUrlLoader2;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderInput;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderProgress;
import com.neusou.web.ImageUrlLoader2.AsyncLoaderResult;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.MenuItem.OnMenuItemClickListener;

public class AppMenu {
	
	public static final int CONTEXT_MENUITEM_VIEW_WALL = 0;
	public static final int CONTEXT_MENUITEM_VIEW_PROFILE = 1;
	public static final int CONTEXT_MENUITEM_VIEW_PHOTOS = 2;
	public static final int CONTEXT_MENUITEM_VIEW_TAGGED_PHOTOS = 3;
	
	public static void createActorMenu(
		Menu menu,
		OnMenuItemClickListener menuItemClickListener,
		String name, String imageUri, int outHandlerKey, long actor_id, Context ctx){
		Resources res = ctx.getResources();
		
		final SubMenu actorNameSM = menu.addSubMenu(name);		
    	Bitmap headerIconBmp = App.mImageUrlLoader2.loadImage(imageUri, true);
    	Drawable actorProfileDrawable = null;
    	if(headerIconBmp != null){
    		actorProfileDrawable = new BitmapDrawable(headerIconBmp);
    		actorNameSM.setHeaderIcon(actorProfileDrawable);
    		//actorNameSM.setIcon(actorProfileDrawable);
    	}else{
    		//asynchronously load profile image
    		
    		AsyncLoaderInput input = new AsyncLoaderInput();
    		input.imageUri = imageUri;
    		App.mImageUrlLoader2.loadImageAsync(    				
    				App.INSTANCE.mExecScopeImageLoaderTask,
    				input,
    				new ImageUrlLoader2.AsyncListener(){
    					
				@Override
				public void onCancelled() {
				}

				@Override
				public void onPostExecute(AsyncLoaderResult result) {
				}

				@Override
				public void onPreExecute() {
				}

				@Override
				public void onPublishProgress(final AsyncLoaderProgress progress) {
					
					
					try{
						BitmapDrawable bd = new BitmapDrawable(progress.bitmap);
						actorNameSM.setHeaderIcon(bd);
					}catch(Throwable e){
						
					}
				}
    			
    		});
    	}
    	
		actorNameSM.setHeaderTitle(name);
		MenuItem viewWallMenuItem = actorNameSM.add(0,CONTEXT_MENUITEM_VIEW_WALL,0, res.getString(R.string.view_wall));
		MenuItem viewProfileMenuItem = actorNameSM.add(0,CONTEXT_MENUITEM_VIEW_PROFILE,1, res.getString(R.string.view_profile));
		MenuItem viewPhotosMenuItem = actorNameSM.add(0,CONTEXT_MENUITEM_VIEW_PHOTOS,2, res.getString(R.string.view_photos));
		MenuItem viewTaggedPhotosMenuItem = actorNameSM.add(0, CONTEXT_MENUITEM_VIEW_TAGGED_PHOTOS, 3, res.getString(R.string.view_tagged_photos));
				
		viewProfileMenuItem.setOnMenuItemClickListener(menuItemClickListener);
		viewWallMenuItem.setOnMenuItemClickListener(menuItemClickListener);
		viewPhotosMenuItem.setOnMenuItemClickListener(menuItemClickListener);
		viewTaggedPhotosMenuItem.setOnMenuItemClickListener(menuItemClickListener);
				//createGetTaggedMenuItemOnClick(pd,outHandlerKey, actor_id, ctx));

		/*
		if (longClickedItemData.targetName != null
				&& longClickedItemData.targetName.length() > 0) {
			SubMenu targetNameSM = menu
					.addSubMenu(longClickedItemData.targetName);
			targetNameSM.setHeaderTitle(longClickedItemData.targetName);
			targetNameSM.setHeaderIcon(android.R.drawable.presence_online);
			// actorNameSM.setHeaderIcon(util.loadImage(longClickedItemData.profilePictureUrl,true));

			targetNameSM.add("View Wall");
			targetNameSM.add("View Profile");
			targetNameSM.add("View Photos");
		}
		 */
    	
    }
	
	private static ProgressDialog createProgressDialog(Context ctx) {
		ProgressDialog pd;
		pd = new ProgressDialog(ctx);
		return pd;
	}
	
	public static OnMenuItemClickListener createGetTaggedMenuItemOnClick(final ProgressDialog pd, final int outHandlerKey, final long actor_id, final Context ctx){
		OnMenuItemClickListener mGetTaggedPhotosMenuItemOnClick = 
			new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					
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
					
					pd.show();
					
					return true;
				}
			};
		return mGetTaggedPhotosMenuItemOnClick;
	}
	
	
	
}