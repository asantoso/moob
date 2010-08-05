package com.neusou.moobook.adapters;
import android.database.Cursor;
public interface CursorRowMapper<D> {		
	public D map(Cursor c);		
}