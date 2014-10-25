package com.nate.whocancall;

import android.os.Handler;
import android.os.Message;

public class UIHandler extends Handler {
	 private MainActivity mActivity;
	 public static final String MSG = "msg";
	 public static final String Call_Info = "Call_Info";
	 public static final String Main_Info = "Main_Info";
	 public static final String Status_Info = "Status_Info";
	 public static final String Number_List = "Number_List";
	 
	 public UIHandler(MainActivity activity) {
	  super();
	  	this.mActivity = activity;
	 }

	@Override
	 public void handleMessage(Message msg) {
		 String text = msg.getData().getString(Call_Info);
		 this.mActivity.setTextOnScreen(Call_Info, text);		 
	 }
}
