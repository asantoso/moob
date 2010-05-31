package com.neusou.moobook.pool;

import com.neusou.moobook.data.Attachment;
import com.neusou.util.PoolableManager;

public class AttachmentPoolManager implements PoolableManager<Attachment>{

	private static AttachmentPoolManager mInstance;
	
	public static AttachmentPoolManager getInstance(){
		if(mInstance == null){
			mInstance = new AttachmentPoolManager();
		}
		return mInstance;
	}
	
	@Override
	public void onAcquired(Attachment element) {
		
		
	}

	@Override
	public void onReleased(Attachment element) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Attachment newInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	
	
}