package com.neusou.android.core.model;

/**
 * Interface to be implemented by datasource of a list view.
 * @author asantoso
 *
 * @param <D> data model type
 */
public interface IAdvancedListViewData<D>{
	public int getCount();
	public Object get(int index,Object key);
	public int getItemId(int index);
	public D getItem(int index);
}