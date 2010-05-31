package com.neusou.moobook.pool;

import com.neusou.util.Poolable;
import com.neusou.util.PoolableManager;

public class AttachmentPool<Attachment> implements Poolable<Attachment> {

	@Override
	public Attachment getNextPoolable() {
		
		return null;
	}

	@Override
	public void setNextPoolable(Attachment element) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
}