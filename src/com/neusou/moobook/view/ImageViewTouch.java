package com.neusou.moobook.view;

import com.neusou.Logger;
import com.neusou.moobook.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Cap;
import android.graphics.Path.Direction;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class ImageViewTouch extends ImageViewTouchBase {
 
    private boolean mEnableTrackballScroll;
    private PhotoTag[] mPhotoTags;
    
    private int[] mTagsDrawOrder;
    private int numTags;
    
    String LOG_TAG = "ImageViewTouch";
    
    private Paint mPaint;
    private Paint mPaintFocus;
    int mPhase;
    public int activeTag = -1;
    Path mTagPath;
    Path mTagFocusPath; 
    DashPathEffect mDashPathEffect;
    DashPathEffect mDashPathEffect2;
    
    final int tagStrokeColor = 0xFFffffff;
    final int tagStrokeWidth = 3;
    final int tagShadowColor=0x44000000;
    final int tagRadius = 18;    
    final int tagFocusStrokeColor = 0xFFCCFF33;    
    final int tagFocusRadius = 35;        
    final int tagFocusStrokeWidth = 8;    
    final int tagFocusShadowColor=0xAA000000;
    final float[] strokePattern = new float[] {5, 12, 5, 12};
    final float[] strokePattern2 = new float[] {5, 8, 5, 8};
    final float[] intervals = new float[]{4,4,4,4};
    boolean isHideTags = true;
    
    public void setPhotoTags(PhotoTag[] tags){
    	mPhotoTags = tags;
    	if(tags != null){
    		numTags = tags.length;
    	}else{
    		numTags = 0;
    	}
    }
    
    public void clearTags(){
    	mPhotoTags = null;
    	postInvalidateDelayed(10);
    }
    
  
    
    private void drawTags(Canvas canvas){    	
    	
    	
    	//Logger.l(Logger.DEBUG, LOG_TAG, "[drawTags()]");
    	
    	if(numTags == 0){
    		return;
    	}
    	
    	if(mBitmapDisplayed == null){
    		return;
    	}
    	
    	Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.mobook2_64);		
		Matrix mat = mSuppMatrix;
		float matrixValues[] = new float[9];
		mat.getValues(matrixValues);
		
		int w = mBitmapDisplayed.getWidth();
		int h = mBitmapDisplayed.getHeight();
		int bw = 0;
		int bh = 0;
		if(mBitmapDisplayed != null){
			bw = mBitmapDisplayed.getWidth();
			bh = mBitmapDisplayed.getHeight();
			//Logger.l(Logger.DEBUG,LOG_TAG,"bw:"+bw+", bh:"+bh);	
		}else{
			//Logger.l(Logger.DEBUG,LOG_TAG,"mBitmapDisplayed is null");
		}
		//Logger.l(Logger.DEBUG,LOG_TAG,"w:"+w+", h:"+h+", numTags:"+numTags);
		Matrix mat2 = new Matrix();
		int scalar = Math.max(bw,bh);
				
		for(int i=0;i<numTags;i++){			
			try{
			PhotoTag pt = mPhotoTags[i];			
			mat2.reset();
			mat2.postTranslate(pt.xcoord*(float)bw/100,  pt.ycoord*(float)scalar/100);
			mat2.postConcat(mat);
			canvas.setMatrix(mat2);
			if(activeTag == i){
				mPaintFocus.setPathEffect(mDashPathEffect);
				mPaintFocus.setShadowLayer(2, 2, 2,tagShadowColor);
				mPaintFocus.setStrokeCap(Cap.ROUND);
				canvas.drawPath(mTagFocusPath, mPaintFocus);	
			}else{				
				mPaint.setPathEffect(mDashPathEffect2);
				mPaint.setShadowLayer(3, 2, 2, tagFocusShadowColor);
				canvas.drawPath(mTagPath, mPaint);
			}	
			}catch(Exception e){
				
			}
		}
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    	//Logger.l(Logger.DEBUG, LOG_TAG, "[onDraw()]");
    	super.onDraw(canvas);
    	mPhase++;
    	createEffect(mPhase);
    	drawTags(canvas);
    	postInvalidateDelayed(50);    	
    }


    
    public ImageViewTouch(Context context) {
        super(context);
        init();
    	
    }
    
    public int getActiveTag(){
    	return activeTag;
    }
    
    public void setActiveTag(int index){
    	if(numTags == 0){
    		index = -1;
    	}
    	if(index > numTags){
    		index = 0; 
    	}
    	activeTag = index;
    }
    
    public void viewNextTag(){
    	if(numTags <= 0){
    		return;
    	}
		activeTag++;
		if(activeTag >= numTags){
			activeTag = 0;
		}
	}

	public void viewPreviousTag(){
		if(numTags <= 0){
    		return;
    	}
		
		activeTag--;
		if(activeTag < 0){
			activeTag = numTags - 1;
		}
	}
	
	
	
    private void init(){
        
        mTagPath = new Path();
        mTagPath.addCircle(0,0,tagRadius,Direction.CW);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(tagStrokeWidth);
        mPaint.setColor(tagStrokeColor);
        mPaint.setStrokeCap(Cap.ROUND);
        
        mTagFocusPath = new Path();
        mTagFocusPath.addCircle(0,0,tagFocusRadius,Direction.CCW);        
        mPaintFocus = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintFocus.setStyle(Paint.Style.STROKE);    
        mPaintFocus.setStrokeWidth(tagFocusStrokeWidth);
        mPaintFocus.setColor(tagFocusStrokeColor);           
        mPaintFocus.setStrokeCap(Cap.ROUND);
               
    }

    private void createEffect(int phase){
    	 mDashPathEffect = new DashPathEffect(strokePattern, phase);
    	 mDashPathEffect2 = new DashPathEffect(strokePattern2, phase+1);
    }
    
    public ImageViewTouch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setEnableTrackballScroll(boolean enable) {
        mEnableTrackballScroll = enable;
    }

    public void postTranslateCenter(float dx, float dy) {
        super.postTranslate(dx, dy);
        //center(true, true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
      

        return super.onKeyDown(keyCode, event);
    }
}
