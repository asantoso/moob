
package com.neusou.moobook.view;

import com.neusou.Logger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.ImageView;

public abstract class ImageViewTouchBase extends ImageView {

    @SuppressWarnings("unused")
    private static final String TAG = "ImageViewTouchBase";
   
    protected Matrix mBaseMatrix = new Matrix();    
    protected Matrix mSuppMatrix = new Matrix();

    // Temporary buffer used for getting the values out of a matrix.
    private final float[] mMatrixValues = new float[9];

    // The current bitmap being displayed.
    protected Bitmap mBitmapDisplayed;

    int mThisWidth = -1, mThisHeight = -1;

    float mMaxZoom;

   
    @Override
    protected void onLayout(boolean changed, int left, int top,
                            int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mThisWidth = right - left;
        mThisHeight = bottom - top;
           
        if (mBitmapDisplayed != null) {
            setImageMatrix(getImageViewMatrix());
        }        
    }
   
	@Override
    public void setImageBitmap(Bitmap bitmap) {
        setImageBitmap(bitmap, 0);
    }

    private void setImageBitmap(Bitmap bitmap, int rotation) {
        super.setImageBitmap(bitmap);
        mBitmapDisplayed = bitmap;       
    }

    public void clear() {
       
    }
    
    public ImageViewTouchBase(Context context) {
        super(context);
        init();
    }

    public ImageViewTouchBase(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

 
	private void init() {
        setScaleType(ImageView.ScaleType.MATRIX);
    }
   
	protected float getValue(Matrix matrix, int whichValue) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    // Get the scale factor out of the matrix.
    protected float getScale(Matrix matrix) {
        return getValue(matrix, Matrix.MSCALE_X);
    }

    public float getScale() {    	
        return getScale(getImageMatrix());
    }

    // Combine the base matrix and the supp matrix to make the final matrix.
    protected Matrix getImageViewMatrix() {
        return getImageMatrix();        
    }

    // Sets the maximum zoom, which is a scale relative to the base matrix. It
    // is calculated to show the image at 400% zoom regardless of screen or
    // image orientation. If in the future we decode the full 3 megapixel image,
    // rather than the current 1024x768, this should be changed down to 200%.
    protected float maxZoom() {
        if (mBitmapDisplayed == null) {
            return 1F;
        }

        float fw = (float) mBitmapDisplayed.getWidth()  / (float) mThisWidth;
        float fh = (float) mBitmapDisplayed.getHeight() / (float) mThisHeight;
        float max = Math.max(fw, fh) * 4;
        return max;
    }

    protected void postTranslate(float dx, float dy) {
        mSuppMatrix.postTranslate(dx, dy);
    }

    public void panBy(float dx, float dy) {
        postTranslate(dx, dy);
        setImageMatrix(mSuppMatrix);
    }
}
