package com.neusou.moobook.data;

import com.neusou.moobook.controller.BaseListViewFactory;

public class BaseRowViewHolder implements BaseListViewFactory.IBaseListViewHolder {

	protected int position = -1;
	
	@Override
	public int getPosition() {
		return position;
	}
	
	@Override
	public void setPosition(int position) {
		this.position = position;
	}

	@Override
	public void onMovedToForeground() {
	}

	@Override
	public void onMovedToScapHeap() {
		position = -1;		
	} 
	
}