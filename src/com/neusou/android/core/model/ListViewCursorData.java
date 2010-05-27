package com.neusou.android.core.model;

import android.database.Cursor;
import com.neusou.android.core.model.IAdvancedListViewData;

/**
 * 
 * @since July 20, 2009
 */
public abstract class ListViewCursorData<D> implements IAdvancedListViewData<D>{
	protected Cursor cursor;
	D cacheModel;
	
	protected abstract D createInstance(Cursor c,D cacheModel);
	protected abstract int getPrimaryKeyColumnIndex();
	
	public ListViewCursorData(Cursor c){
		this.cursor = c;
	}
	
	public void setCursor(Cursor c){
		if(c!=null){
			if(this.cursor != null){
				this.cursor.close();
			}
			this.cursor = c;
		}
	}
	
	@Override
	public Object get(int index, Object key) {
		if(cursor == null){
			return null;
		}
		boolean s = cursor.moveToPosition(index);
		if(s){
			return cursor.getString((Integer)key);
		}else{
			return null;
		}
	}

	@Override
	public int getCount() {
		if(cursor==null){
			return 0;
		}
		return cursor.getCount();
	}
	
	@Override
	public D getItem(int index) {
		if(cursor == null){
			return null;
		}		
		int d = cursor.getPosition() - index; 
		boolean s;
		if(d == 1){
			s = cursor.moveToPrevious();			
		}else if(d == -1){
			s = cursor.moveToNext();
		}else{
			s = cursor.moveToPosition(index);
		}
		if(s){
			D sol = createInstance(cursor,cacheModel);
			return sol;
		}
		return null;
	}
	
	@Override
	public int getItemId(int index) {
		if(cursor == null){
			return -1;
		}
		boolean b = cursor.moveToPosition(index);
		if(b){
			return cursor.getInt(getPrimaryKeyColumnIndex());
		}
		return 0;
	}
	
}