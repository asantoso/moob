package com.neusou.moobook.controller;

public interface IStatefulListView {
	public boolean getAsyncLoadState(long groupCode);
	public void clearAsyncLoadStates();
	public void removeAsyncLoadState(long groupCode);
	public void addAsyncLoadState(long groupCode);
}