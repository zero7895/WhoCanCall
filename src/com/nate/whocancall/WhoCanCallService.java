package com.nate.whocancall;

import java.util.logging.Logger;

import org.json.JSONArray;

import com.nate.whocancall.UIHandler;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.Log;

public class WhoCanCallService extends Service {
	public static final String Phone_Call_Receive_Action = "android.intent.action.PHONE_STATE";
	private static final String TAG = WhoCanCallService.class.getName();
	private static UIHandler uiHandler;
	PhoneCallReceiver mPhoneCallReceiver;

	@Override
	public void onCreate() {
		Log.i(TAG, "[onCreate] enter");
		mPhoneCallReceiver = new PhoneCallReceiver();
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "[onStartCommand] enter");	
		
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Phone_Call_Receive_Action);
		registerReceiver(mPhoneCallReceiver, intentFilter);

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		this.unregisterReceiver(mPhoneCallReceiver);
		super.onDestroy();
	}

	public static void registerHandler(Handler handler) {
		uiHandler = (UIHandler) handler;
	}

	public void setSharedData(String type, String value) {
		Log.i(TAG, "[setSharedData] type:" + type + ", value:" + value);

		SharedPreferences settings = this.getSharedPreferences("Nate_Info_File", 0);
		SharedPreferences.Editor PE = settings.edit();
		PE.putString(type, value);
		PE.commit();
	}

	public String getSharedData(String type) {
		Log.i(TAG, "[setSharedData] type:" + type );	
		
		SharedPreferences settings = this.getSharedPreferences("Nate_Info_File", 0);
		String str = settings.getString(type, "null");
		return str;
	}

	public String getPhoneNumberList(boolean bIsShow) {
		Log.i(TAG, "[getPhoneNumberList] enter");

		SharedPreferences settings = this.getSharedPreferences("Nate_Number_File", 0);
		String str = settings.getString("NumberList", "NoString");
		Log.i(TAG, "[getPhoneNumberList] str:" + str);
		
		JSONArray jsonNumberArray = null;
		try {
			jsonNumberArray = new JSONArray(settings.getString("NumberList", "[]"));
			Log.i(TAG, "[getPhoneNumberList] jsonArray2:" + jsonNumberArray.toString());	
		    for (int i = 0; i < jsonNumberArray.length(); i++) {
		    	Log.i(TAG, "your number list " + i + ":"+ jsonNumberArray.getString(i));	
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}	

		return jsonNumberArray.toString();
	}
	
	//////////////class PhoneCallReceiver start
	public class PhoneCallReceiver extends BroadcastReceiver {	
		private final long[] vibrateArray = { 100, 100, 100, 200 };

		public PhoneCallReceiver() {
			System.out.println("[PhoneCallReceiver][PhoneCallReceiver] PhoneCallReceiver init");
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			System.out.println("[PhoneCallReceiver] [onReceive]" + action);
			if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
				doReceivePhone(context, intent);
			}
		}

		public void doReceivePhone(Context context, Intent intent) {
			boolean status = getStatus();
			Log.i(TAG, "[PhoneCallReceiver] [doReceivePhone] status:" + status);	
			
			if(status == false)
				return;
			
			TelephonyManager telMgr;
			telMgr = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
			String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);		

			switch (telMgr.getCallState()) {
			case TelephonyManager.CALL_STATE_RINGING:
				Log.i(TAG, "[PhoneCallReceiver] [doReceivePhone] State: CALL_STATE_RINGING");
				//get number list to decide vibrate or not
				String NumListStr = getPhoneNumberList(false);				
				if (NumListStr.indexOf(number) > -1) {
					Log.i(TAG, "[doReceivePhone] number is in the list: true");
					startVibrate(context);
				}				
				
				if(uiHandler == null) //activity may be killed , so that can't callback to activity from service
					System.out.println("[PhoneCallReceiver] [doReceivePhone] uiHandler == null");				
				else
					sendMessageToUI(UIHandler.Call_Info, number);
				
				break;
				
			case TelephonyManager.CALL_STATE_OFFHOOK:
				Log.i(TAG, "[PhoneCallReceiver] [doReceivePhone] State: CALL_STATE_OFFHOOK");					
				stoptVibrate(context);
				break;

			case TelephonyManager.CALL_STATE_IDLE:
				Log.i(TAG, "[PhoneCallReceiver] [doReceivePhone] State: CALL_STATE_IDLE");	
				stoptVibrate(context);
				break;
			}
			
			Log.i(TAG, "[PhoneCallReceiver] [doReceivePhone] Number:" + number);	
		}

		// startVibrate
		public void startVibrate(Context context) {
			Log.i(TAG, "[PhoneCallReceiver][startVibrate] enter");	
			Vibrator myVibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
			myVibrator.vibrate(vibrateArray, 0);
		}

		// stopVibrate
		public void stoptVibrate(Context context) {
			Log.i(TAG, "[PhoneCallReceiver][stoptVibrate] enter");
			Vibrator myVibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
			myVibrator.cancel();
		}

		//
		public boolean getStatus() {
			Log.i(TAG, "[PhoneCallReceiver][getStatus] enter");
			String strStatus = getSharedData(UIHandler.Status_Info);
			if(strStatus.equalsIgnoreCase("YES"))
				return true;
			else 
				return false;
		}
		
		// send message to UI, type is Call_Info/Enable, value is corresponding value
		public void sendMessageToUI(String type, String value) {
			Log.i(TAG, "[PhoneCallReceiver][sendMessageToUI] enter, type:" + type + " , value:" + value);
			Message msg = uiHandler.obtainMessage();
			msg.getData().putString(type, value);
			uiHandler.sendMessage(msg);
		}
	}
	//////////////class PhoneCallReceiver end

}
