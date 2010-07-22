package com.neusou.moobook.view;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextSwitcher;

import com.neusou.moobook.App;
import com.neusou.moobook.R;
import com.neusou.moobook.data.ContextMenuDescriptor;
import com.neusou.moobook.data.ContextProfileData;

public class TitleBar{
	
	public static final String NAMESPACE = "neusou"; 
	Activity mContext;
	int mDefStyle;
	long mUserId;
	String mUserName;
	int mScreenId;
	Resources mResources;
	public TextSwitcher mHeaderText;
	public View mLoadingIndicator;
	public ImageView mProfileImage;
	
	public TitleBar(Activity act) {
		bind(act);
		initViews();
	}
		
	public void bind(Activity act){
		mContext = act;
		mHeaderText = (TextSwitcher) act.findViewById(R.id.topheader);
		mLoadingIndicator = act.findViewById(R.id.loadingindicator);
		mProfileImage = (ImageView) act.findViewById(R.id.th_image);
	}
	
	public void initViews(){
		mContext.registerForContextMenu(mProfileImage);
		ContextMenuDescriptor cmd = new ContextMenuDescriptor();
		cmd.type = App.CONTEXT_MENU_TOPHEADER_PROFILE;
		cmd.activity = mContext;
		mProfileImage.setTag(R.id.tag_contextmenu_data, cmd);
		mProfileImage.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				ContextMenuDescriptor cmd = (ContextMenuDescriptor) v.getTag(R.id.tag_contextmenu_data);
				cmd.activity.openContextMenu(v);				
			}
		});
	}
	
	public void setImage(Bitmap bmp){
		if(mProfileImage != null){
			mProfileImage.setImageBitmap(bmp);
		}
	}
	
	public void update(){
		
	}
	
}