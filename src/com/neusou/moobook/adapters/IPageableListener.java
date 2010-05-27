package com.neusou.moobook.adapters;

public interface IPageableListener{
	
	public void onHasNext();
	public void onHasPrev();
	public void onGetPrev();
	public void onGetNext();
}
