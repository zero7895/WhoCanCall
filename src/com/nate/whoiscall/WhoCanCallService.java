package com.nate.whoiscall;

import org.json.JSONArray;

import com.nate.whoiscall.UIHandler;

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

public class WhoCanCallService extends Service {
	public static final String Phone_Call_Receive_Action = "android.intent.action.PHONE_STATE";
	
	private static UIHandler uiHandler;
	PhoneCallReceiver mPhoneCallReceiver;

	@Override
	public void onCreate() {
		System.out.println("[MyService][onCreate] enter");
		mPhoneCallReceiver = new PhoneCallReceiver();
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		System.out.println("[MyService][onStartCommand] enter");		
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
		System.out.println("[MyService][setSharedData] type:" + type + ", value:" + value);

		SharedPreferences settings = this.getSharedPreferences("Nate_Info_File", 0);
		SharedPreferences.Editor PE = settings.edit();
		PE.putString(type, value);
		PE.commit();
	}

	public String getSharedData(String type) {
		System.out.println("[MyService][getSharedData] type:" + type);		
		
		SharedPreferences settings = this.getSharedPreferences("Nate_Info_File", 0);
		String str = settings.getString(type, "null");
		return str;
	}

	public String getPhoneNumberList(boolean bIsShow) {
		System.out.println("[MyService][getPhoneNumberList] enter");

		SharedPreferences settings = this.getSharedPreferences("Nate_Number_File", 0);
		String str = settings.getString("NumberList", "NoString");
		System.out.println("[MyService][getPhoneNumberList] str:" + str);
		
		JSONArray jsonNumberArray = null;
		try {
			jsonNumberArray = new JSONArray(settings.getString("NumberList", "[]"));
		    System.out.println("[MyService][getPhoneNumberList] jsonArray2:" + jsonNumberArray.toString());
		    for (int i = 0; i < jsonNumberArray.length(); i++) {
		    	System.out.println("your number list " + i + ":"+ jsonNumberArray.getString(i));
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
			System.out.println("[PhoneCallReceiver] [doReceivePhone] status:" + status);
			
			if(status == false)
				return;
			
			TelephonyManager telMgr;
			telMgr = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
			String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);		

			switch (telMgr.getCallState()) {
			case TelephonyManager.CALL_STATE_RINGING:
				System.out.println("[PhoneCallReceiver] [doReceivePhone] State: CALL_STATE_RINGING");
				//get number list to decide vibrate or not
				String NumListStr = getPhoneNumberList(false);				
				if (NumListStr.indexOf(number) > -1) {
					System.out.println("[doReceivePhone] number is in the list: true");
					startVibrate(context);
				}				
				
				if(uiHandler == null) //activity may be killed , so that can't callback to activity from service
					System.out.println("[PhoneCallReceiver] [doReceivePhone] uiHandler == null");				
				else
					sendMessageToUI(UIHandler.Call_Info, number);
				
				break;
				
			case TelephonyManager.CALL_STATE_OFFHOOK:
				System.out.println("[PhoneCallReceiver] [doReceivePhone] State: CALL_STATE_OFFHOOK");
				stoptVibrate(context);
				break;

			case TelephonyManager.CALL_STATE_IDLE:
				System.out.println("[PhoneCallReceiver] [doReceivePhone] State: CALL_STATE_IDLE");
				stoptVibrate(context);
				break;
			}
			
			System.out.println("[PhoneCallReceiver] [doReceivePhone] Number:" + number);
		}

		// startVibrate
		public void startVibrate(Context context) {
			System.out.println("[PhoneCallReceiver][startVibrate] enter");
			Vibrator myVibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
			myVibrator.vibrate(vibrateArray, 0);
		}

		// stopVibrate
		public void stoptVibrate(Context context) {
			System.out.println("[PhoneCallReceiver][stoptVibrate] enter");
			Vibrator myVibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
			myVibrator.cancel();
		}

		//
		public boolean getStatus() {
			String strStatus = getSharedData(UIHandler.Status_Info);
			if(strStatus.equalsIgnoreCase("YES"))
				return true;
			else 
				return false;
		}
		
		// send message to UI, type is Call_Info/Enable, value is corresponding value
		public void sendMessageToUI(String type, String value) {
			System.out.println("[PhoneCallReceiver][sendMessageToUI] enter, type:" + type + " , value:" + value);
			Message msg = uiHandler.obtainMessage();
			msg.getData().putString(type, value);
			uiHandler.sendMessage(msg);
		}
	}
	//////////////class PhoneCallReceiver end

}
