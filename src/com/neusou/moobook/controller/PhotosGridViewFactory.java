package com.neusou.moobook.controller;

import java.util.Date;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;
import com.neusou.moobook.data.Stream;

public class PhotosGridViewFactory extends BaseListViewFactory<Cursor>{
		
	Drawable mLoadingDrawable;
	Util util = new Util();

	ColorMatrix mColorMatrix = new ColorMatrix(
			new float[] { 
			0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
			0.1f, 0.4f, 0.1f, 0.0f, 0.0f, 
			0.2f, 0.2f, 0.1f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f, 1.0f, 0.0f });

	ColorMatrixColorFilter mColorMatrixColorFilter = new ColorMatrixColorFilter(mColorMatrix);
	
	public class Holder {		
		public ImageView thumbnailImage;
		public String thumbnailImageUrl;
		public AsyncTask loader;
		public int position; //cancel asynctask if the recycled row is used as a new row
	}
	
	public PhotosGridViewFactory(Activity ctx){
		super(ctx);
		mLoadingDrawable = ctx.getResources().getDrawable(R.drawable.mobook2_exclaimation_64);
	}
	
	public View createView(
			Cursor ds,			
			int position, 
			View convertView, 
			final ViewGroup parent
	){
		
		if (convertView != null) {
			Holder h = (Holder) convertView.getTag();
			if (position != h.position) { // cancel asyncTask of the row if the row is a previous row.
				if (h.loader != null) {
					h.loader.cancel(true);
					h.loader = null;
				}
			}			
		} 	
		
		if (convertView == null) {			
			convertView = mLayoutInflater.inflate(R.layout.t_photo,parent, false);
		}

		Holder tag = (Holder) convertView.getTag();

		//bind child views and store in tag
		if (tag == null) {
			tag = new Holder();		
			tag.thumbnailImage = (ImageView) convertView.findViewById(R.id.profile_pic);						
		} 
		
		String pic_square = null;
		String pic_small = null;
		String pic_big = null;
		String pic = null;
		String name = null;
		String message = null;
		ds.moveToPosition(position);		
		
		//target_name = ds.getString(Stream.col_target_id);
		//Log.d("ViewFactory", "name: "+name+" ,pic: "+pic_square);
		
		tag.thumbnailImageUrl = pic_square;
		tag.position = position;		
				
		convertView.setTag(tag);		
		
		// when no image for profile is available, use default profile image
		if (tag.thumbnailImageUrl == null || tag.thumbnailImageUrl.length() == 0) {
			Drawable img = mResources.getDrawable(R.drawable.mobook2_64);
			tag.thumbnailImage.setImageDrawable(img);
		} 
		
		else {
			// otherwise...
			// try to immediately load the image from cache.
		
			Drawable img = App.mImageUrlLoader.loadImage(tag.thumbnailImageUrl, true);
		
			if (img != null) {
				
			} else {
				// if we cant get the image fast enough, then..
				
				img = mLoadingDrawable;		
				
				tag.thumbnailImage.setImageDrawable(img);

				AsyncTask<Holder, Void, Drawable> getImageInBG = new AsyncTask<Holder, Void, Drawable>() {
					Holder mHolder;

					@Override
					protected Drawable doInBackground(Holder... params) {
						mHolder = params[0];
						return App.mImageUrlLoader.loadImage(params[0].thumbnailImageUrl,
								false);						
					}

					@Override
					protected void onPostExecute(Drawable result) {
						super.onPostExecute(result);					
						mHolder.thumbnailImage.setImageDrawable(result);
						mHolder = null;
					}

					@Override
					protected void onPreExecute() {
						super.onPreExecute();
					}
				};

				tag.loader = getImageInBG;
				getImageInBG.execute(tag);

				return convertView;
			}
			tag.thumbnailImage.setImageDrawable(img);

		}

		return convertView;

	}
	
		
	}
	

