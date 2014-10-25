package com.nate.whocancall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootBroadcastReceiver extends BroadcastReceiver {

	final String ACTION = "android.intent.action.BOOT_COMPLETED";

	@Override
	public void onReceive(Context context, Intent intent) {
		System.out.println("[BootBroadcastReceiver][onReceive] enter");	
		if (intent.getAction().equals(ACTION)){
			System.out.println("[BootBroadcastReceiver][onReceive] android.intent.action.BOOT_COMPLETED");
			
			/*
			Intent iMainActivity = new Intent(context , MainActivity.class);			
			iMainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);		
			iMainActivity.putExtra("parameter" ,ACTION);			
			context.startActivity(iMainActivity);
			*/
			context.startService(new Intent("com.nate.whoiscall.WhoCanCallService"));
		}
	}
}

