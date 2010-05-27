package com.neusou.android.core.view;

import android.view.View;
import android.view.ViewGroup;
import com.neusou.android.core.model.IAdvancedListViewData;

public interface IAdvancedListViewItemFactory<D extends IAdvancedListViewData<?>>{
	public View createView(D data, int index, View convertView, ViewGroup parent);
	public void notifyDataSetChanged();
}