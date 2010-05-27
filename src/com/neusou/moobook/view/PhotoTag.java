package com.neusou.moobook.view;

import com.neusou.moobook.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class PhotoTag extends View implements View.OnFocusChangeListener, View.OnClickListener{
	
	public String description;
	public long uid;
	public float ycoord; //percentage of the height
	public float xcoord; //percentage of the width
	
	public PhotoTag(Context ctx) {
		super(ctx);		
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		
		
	}

	@Override
	public void onClick(View v) {

		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
	
		super.onDraw(canvas);
		Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.mobook2_64);		
		canvas.drawBitmap(bmp, xcoord, ycoord, null);
		canvas.drawBitmap(bmp,0, 0, null);
	}
}