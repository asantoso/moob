package com.neusou.moobook.adapters;

import org.apache.commons.lang.NullArgumentException;

import android.database.DataSetObserver;
import android.widget.BaseAdapter;

/**
 * Wraps BaseAdapter as a DataSetObserver
 * @author asantoso
 *
 */
public class AdapterDataSetObserver extends DataSetObserver{
	BaseAdapter adapter;
	public AdapterDataSetObserver(BaseAdapter adapter) throws NullArgumentException{
		if(adapter == null){
			throw new NullArgumentException("adapter");
		}
		this.adapter = adapter;
	}
	
	@Override
	public void onChanged() {	
		super.onChanged();
		this.adapter.notifyDataSetChanged();
	}
	
	@Override
	public void onInvalidated() {	
		super.onInvalidated();
		this.adapter.notifyDataSetInvalidated();
	}
	
}