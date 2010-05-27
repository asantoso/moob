package com.neusou.moobook.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.OnGestureListener;
import android.widget.ZoomButtonsController;

public class FacebookGallery extends ViewGroup implements GestureDetector.OnGestureListener {
	
	private ZoomButtonsController mZoomButtonsController;
	GestureDetector mGestureDetector;
	GestureDetector.OnGestureListener mGestureListener;
	
    private View mCommentsBtn;
    private View mTagsBtn;
    static final int MODE_NORMAL = 1;
    static final int MODE_SLIDESHOW = 2;
    private int mMode = MODE_NORMAL;
    
    FBGalleryListener mListener;
    View.OnClickListener mCommentsOnClick;
    View.OnClickListener mTagsOnClick;
    
    
    interface FBGalleryListener{
    	public void onNextPage();
    	public void onPrevPage();
    }
    
	public FacebookGallery(Context context) {
		super(context);

	}
	
	public FacebookGallery(Context ctx, AttributeSet attr) {
		super(ctx,attr);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		
	}
		
	private void init(){

		
	}

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	
	
}