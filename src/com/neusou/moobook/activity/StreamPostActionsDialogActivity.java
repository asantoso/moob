package com.neusou.moobook.activity;

import com.neusou.moobook.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class StreamPostActionsDialogActivity extends BaseActivity{
	
		
	static final String LABEL_VIEW_COMMENTS= "Comments";
	static final String LABEL_HIDE_FEED= "Hide feed";
	static final String LABEL_VIEW_WALL= "View Wall";
	static final String LABEL_VIEW_PROFILE= "View profile";
		
	static final byte ICON_VIEW_COMMENTS = 0;
	static final byte ICON_HIDE_FEED = 1;
	static final byte ICON_VIEW_WALL = 2;
	static final byte ICON_VIEW_PROFILE = 3;
	
	static final String LOG_TAG="StreamPostActionsDialog";
		
	static final byte NUM_ICONS = 4;
	Drawable icons[] = new Drawable[NUM_ICONS];
	String labels[] = new String[]{
			"Comments",
			"Hide Feed",
			"View Wall",
			"View Profile"
	};
	
	Gallery mGallery;
	
	ColorMatrix mBlueishColorMatrix;
	ColorMatrixColorFilter mColorFilterBlueish;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	
		super.onCreate(savedInstanceState);
		getWindow().setBackgroundDrawable(null);
		setContentView(R.layout.streampost_actionsmenu);
				
		bindViews();
		initObjects();
		initViews();
	}
	
	@Override
	protected void onResume() {
	
		super.onResume();
	}
		
	@Override
	protected void bindViews() {	
		super.bindViews();		
		mGallery = (Gallery) findViewById(R.id.menu_gallery);		
		}
	
	@Override
	protected void initViews() {
		super.initViews();
		
		icons[ICON_VIEW_COMMENTS] = mResources.getDrawable(android.R.drawable.ic_dialog_alert);
		icons[ICON_VIEW_PROFILE] = mResources.getDrawable(android.R.drawable.ic_dialog_alert);
		icons[ICON_VIEW_WALL] = mResources.getDrawable(android.R.drawable.ic_dialog_alert);
		icons[ICON_HIDE_FEED] =  mResources.getDrawable(android.R.drawable.ic_dialog_alert);
	
		for(int i=0;i<NUM_ICONS;i++){
			icons[i].setColorFilter(mColorFilterBlueish);
		}
		
		mGallery.setSpacing(5);		
		mGallery.setSoundEffectsEnabled(true);
		mGallery.setAdapter(new ImageAdapter(this));
		
	}
		
	@Override
	protected void initObjects() {	
		super.initObjects();
		mBlueishColorMatrix = new ColorMatrix(new float[] { 
				0.0f, 0.0f, 0.0f, 0.0f, 0.1f,
				0.1f, 0.8f, 0.5f, 0.0f, 0.0f,
				0.6f, 0.4f, 0.2f, 0.0f, 0.0f, 
				0.2f, 0.4f, 0.4f, 0.0f, 0.0f });	
		mColorFilterBlueish = new ColorMatrixColorFilter(mBlueishColorMatrix);
	}
	
	 public class ImageAdapter extends BaseAdapter {
	        int mGalleryItemBackground;
	        private Context mContext;
	        
	        class Holder{
	        	public ImageButton icon;
	        	public TextView label;
	        }
	        
	        public ImageAdapter(Context c) {
	            mContext = c;
	            // See res/values/attrs.xml for the <declare-styleable> that defines
	            // Gallery1.
	         //   TypedArray a = obtainStyledAttributes(R.styleable.Gallery1);
	           // mGalleryItemBackground = a.getResourceId(
	             //       R.styleable.Gallery1_android_galleryItemBackground, 0);
	            
	         //   a.recycle();
	        }

	        public int getCount() {
	            return NUM_ICONS;
	        }

	        public Object getItem(int position) {
	            return position;
	        }

	        public long getItemId(int position) {
	            return position;
	        }

	        public View getView(int position, View convertView, ViewGroup parent) {
	           Log.d(LOG_TAG,"getView "+position);
	            Holder tag;
	            if(convertView == null){
	            	convertView = mLayoutInflater.inflate(R.layout.t_actionicon,parent,false);
	            	tag = new Holder();
	            	tag.icon = (ImageButton) convertView.findViewById(R.id.icon);
	            	tag.label = (TextView) convertView.findViewById(R.id.label);
	            	convertView.setTag(tag);
	            }
	            else{
	            	tag = (Holder) convertView.getTag();	            	
	            }
	            
	            tag.icon.setImageDrawable(icons[position]);
	            tag.label.setText(labels[position]);
	            
	            return convertView;
	            
	        }	        
	        
	    }
	
}