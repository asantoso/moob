package com.neusou.moobook.view;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.Util;

public class JSONArrayListAdapter extends BaseAdapter {

	public JSONArray data;
	public LayoutInflater mLayoutInflater;
	public Activity ctx;
	public Resources mResources;
	private Drawable mLoadingDrawable;
	Util util;

	public JSONArrayListAdapter(Activity ctx) {
		this.mLayoutInflater = ctx.getLayoutInflater();
		this.ctx = ctx;
		this.mResources = ctx.getResources();
		mLoadingDrawable = ctx.getResources().getDrawable(R.drawable.mobook2_exclaimation_64);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		if (data == null) {
			return 0;
		}
		return data.length();
	}

	@Override
	public Object getItem(int position) {
		if (data == null) {
			return null;
		}
		try {
			return data.get(position);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	class Holder {
		public TextView name;
		public ImageView profileImage;
		public String profileImageUrl;
		public AsyncTask loader;
		
		public int position; //used to cancel asynctask if the recycled row is used as a new row
	}

	@Override
	public View getView(int position, View convertView, final ViewGroup parent) {

		String fname = "";
		String lname = "";
		String pic_square = null;
		String pic_small = null;
		String pic_big = null;
		String pic = null;
		
		if (convertView != null) {
			Holder h = (Holder) convertView.getTag();
			// Log.d("MyList","get view at position "+position+" using recycled View at position:"+h.position);

			if (position != h.position) { // cancel asyncTask of the row if the row is a previous row.
				if (h.loader != null) {
					h.loader.cancel(true);
					h.loader = null;
				}
			}
			
		} else {
			 //Log.d("MyList","getView at position "+position);
		}

		//try to get data at position and try to parse it as a JSONObject
		try {
			JSONObject person = data.getJSONObject(position);
			fname = person.getString("first_name");
			lname = person.getString("last_name");
			pic_square = person.getString("pic_square");
			pic_small = person.getString("pic_small");
			pic_big = person.getString("pic_big");
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}

		if (convertView == null) {
			convertView = mLayoutInflater.inflate(R.layout.t_peoplelist,
					parent, false);
		}

		Holder tag = (Holder) convertView.getTag();

		//bind child views and store in tag
		if (tag == null) {
			tag = new Holder();
			tag.name = (TextView) convertView.findViewById(R.id.name);
			tag.profileImage = (ImageView) convertView
					.findViewById(R.id.profile_pic);
		} 
			tag.profileImageUrl = pic_square;
			tag.position = position;
			tag.name.setText(fname + " " + lname);
		
		convertView.setTag(tag);
		
		// select which profile image to use
		if (!(pic_square == null || pic_square.length() == 0)) {
			tag.profileImageUrl = pic_square;
		} else if (!(pic_small == null || pic_small.length() == 0)) {
			tag.profileImageUrl = pic_small;
		} else if (!(pic_big == null || pic_big.length() == 0)) {
			tag.profileImageUrl = pic_big;
		} else if (!(pic == null || pic.length() == 0)) {
			tag.profileImageUrl = pic;
		} else{
			tag.profileImageUrl = "none";
		}

		// when no image for profile is available, use default profile image
		if (tag.profileImageUrl == null || tag.profileImageUrl.length() == 0) {
			Drawable img = mResources.getDrawable(R.drawable.mobook2_64);
			tag.profileImage.setImageDrawable(img);
		} 
		
		else {
			// otherwise...
			// try to immediately load the image from cache.
		
			Drawable img = App.mImageUrlLoader.loadImage(tag.profileImageUrl, true);
		
			if (img != null) {
				
			} else {
				// if we cant get the image fast enough, then..
				
				img = mLoadingDrawable;		
				
				tag.profileImage.setImageDrawable(img);

				AsyncTask<Holder, Void, Drawable> getImageInBG = new AsyncTask<Holder, Void, Drawable>() {
					Holder mHolder;

					@Override
					protected Drawable doInBackground(Holder... params) {
						mHolder = params[0];
						return App.mImageUrlLoader.loadImage(params[0].profileImageUrl,
								false);						
					}

					@Override
					protected void onPostExecute(Drawable result) {
						super.onPostExecute(result);					
						mHolder.profileImage.setImageDrawable(result);
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
			tag.profileImage.setImageDrawable(img);

		}

		return convertView;

	}

}
