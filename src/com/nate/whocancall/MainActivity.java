package com.nate.whocancall;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nate.whocancall.UIHandler;
import com.nate.whoiscall.R;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity  {
	private UIHandler mUIHandler;
	private boolean bIsStatus = false;
	final String BOOT_COMPLETED_ACTION = "android.intent.action.BOOT_COMPLETED";
	
	EditText mEditText;
	TextView mTextView;
	TextView mTextViewStatus;
	ListView listView;
	ArrayList<String> arrListContactAll = new ArrayList<String>();
	
	@Override
    public void onCreate(Bundle savedInstanceState) {    	
        super.onCreate(savedInstanceState);        
        
        System.out.println("[MainActivity][onCreate] enter");
        setContentView(R.layout.activity_main);                   
        mEditText = (EditText)findViewById(R.id.numberEdit);
        mTextView = (TextView)findViewById(R.id.textview);  
        mTextViewStatus = (TextView)findViewById(R.id.textviewStatus);  
        listView = (ListView) findViewById(R.id.listview1);

        init();
        addListenerOnButton();
        
        //call service when boot started (notified by BootBroadcastReveiver)
        Intent intent = this.getIntent();
        String parameter = intent.getStringExtra("parameter");
        if(parameter!= null && parameter.equals(BOOT_COMPLETED_ACTION)){
        	setTextOnScreen(UIHandler.Main_Info, "Start when BOOT_COMPLETED!");
        	moveTaskToBack(true);        	
        }
        
        startService(new Intent("com.nate.whoiscall.WhoCanCallService"));
        bIsStatus = getStatus(); 
    }	
    
	private void init(){
        new Thread(new Runnable()
        {
            public void run()
            {
            	getContact();
            }

        }).start();
	}
	private void addListenerOnButton() {   	
    
    	//On Off btn
    	Button btnUsable = (Button) findViewById(R.id.btnUsable); 
    	btnUsable.setOnClickListener(new OnClickListener() { 
			public void onClick(View arg0) { 	
				if(bIsStatus == false){
					bIsStatus = true;
					setSharedData(UIHandler.Status_Info, "YES");					
				}
				else{
					bIsStatus = false;	
					setSharedData(UIHandler.Status_Info, "NO");	
				}
				getStatus();				
			} 
		});
    	
    	//get number
       	Button btnGetNum = (Button) findViewById(R.id.btnGetNum); 
       	btnGetNum.setOnClickListener(new OnClickListener() { 
			public void onClick(View arg0) {  
				String strWhiteNumbers = getPhoneNumberList(true);				
				System.out.println("[MainActivity] btnGetNum onClick");				
			} 
		});  
       	
    	//clear number
       	Button btnClearNum = (Button) findViewById(R.id.btnClearNum); 
       	btnClearNum.setOnClickListener(new OnClickListener() { 
			public void onClick(View arg0) {  
				SetPhoneNumberList("clear");
				System.out.println("[MainActivity] btnClearNum onClick");				
			} 
		});  
    	
       	//editText
       	mEditText.addTextChangedListener(new TextWatcher() {            
            public void onTextChanged(CharSequence s, int start, int before, int count) {            	 
            	setListView(mEditText.getText().toString());
            }   
               
            public void beforeTextChanged(CharSequence s, int start, int count,   
                    int after) {                   
            }   
               
            public void afterTextChanged(Editable s) {                                 
            }   
        }); 
	}    

	@Override
	public void onResume() {
	    super.onResume();  // Always call the superclass method first	   
	    mUIHandler = new UIHandler(MainActivity.this);
	    WhoCanCallService.registerHandler(mUIHandler);	   	    
	}
	
	public void setTextOnScreen(String type, String value)
	{
		System.out.println("[MainActivity] [sendMessageToUI] enter, type:" + type + " , value:" + value);		
		
		if(type == UIHandler.Call_Info)
			mTextView.setText("tel:" + value);
		else if (type == UIHandler.Main_Info)
			mTextView.setText("Main:" + value);	
		else if (type == UIHandler.Status_Info)
			mTextView.setText("Usable:" + value);	
		else if (type == UIHandler.Number_List)
			mTextView.setText("Number List:" + value);	
	}
    
    public void setSharedData(String type, String value) {
    	System.out.println("[MainActivity][setSharedData] type:" + type + ", value:" + value);
        SharedPreferences settings = getSharedPreferences ("Nate_Info_File", 0);
        SharedPreferences.Editor PE = settings.edit();
        PE.putString(type, value);
        PE.commit();
    }
        
    public String getSharedData(String type) {
    	System.out.println("[MainActivity][getSharedData] type:" + type);
        SharedPreferences settings = getSharedPreferences ("Nate_Info_File", 0);
        return settings.getString(type, "null");       
    }
        
	public boolean getStatus() {
		String strStatus = getSharedData(UIHandler.Status_Info);
		if(strStatus.equalsIgnoreCase("YES")){
			setTextOnScreen(UIHandler.Status_Info, "bIsStatus = true");
			mTextViewStatus.setBackgroundColor(android.graphics.Color.GREEN);
			mTextViewStatus.setText("Status: On");
			return true;
		}
		else{ 
			setTextOnScreen(UIHandler.Status_Info, "bIsStatus = false");
			mTextViewStatus.setBackgroundColor(android.graphics.Color.RED);
			mTextViewStatus.setText("Status: Off");
			return false;
		}
		
	}
	
	
	public void SetPhoneNumberList(String number) {
		System.out.println("[MainActivity][SetPhoneNumberList] add number:" + number);

		SharedPreferences settings = this.getSharedPreferences("Nate_Number_File", 0);
		JSONArray jsonArray = null;
		if(number == "clear")
		{
			jsonArray = new JSONArray(); 
		}
		else
		{
			try {
				//get existed list 
				jsonArray = new JSONArray(getPhoneNumberList(false)); 
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			jsonArray.put(number);
		}		
		
		SharedPreferences.Editor PE = settings.edit();
		PE.putString("NumberList", jsonArray.toString());
		PE.commit();
		System.out.println(jsonArray.toString());
	}

	public String getPhoneNumberList(boolean bIsShow) {
		System.out.println("[MainActivity][getPhoneNumberList] enter");

		SharedPreferences settings = this.getSharedPreferences("Nate_Number_File", 0);
		String str = settings.getString("NumberList", "NoString");
		System.out.println("[MainActivity][getPhoneNumberList] str:" + str);
		
		String whiteNumbers = "";
		JSONArray jsonNumberArray = null;
		try {
			jsonNumberArray = new JSONArray(settings.getString("NumberList", "[]"));		   
		    for (int i = 0; i < jsonNumberArray.length(); i++) {
		    	System.out.println("your number list " + i + ":"+ jsonNumberArray.getString(i));
		    	whiteNumbers += jsonNumberArray.getString(i);
		    	if(i != jsonNumberArray.length()-1)
		    		whiteNumbers +="\n";
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
		
		if(bIsShow){
			setTextOnScreen(UIHandler.Number_List, jsonNumberArray.toString() );
			if(whiteNumbers != "")
				Toast.makeText(this, "�z���ӹq�զW��:\n" + whiteNumbers, Toast.LENGTH_LONG).show();
			else
				Toast.makeText(this, "�S������ӹq�զW��", Toast.LENGTH_LONG).show();
		}
		return jsonNumberArray.toString();
	}
	
	public void getContact()
	{	
		String ContactTag = "ContactTag";
		ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,  null, null, null, null);
        if (cur.getCount() > 0) {
        	while (cur.moveToNext()) {
        		String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
        		String name = cur.getString( cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        		//Log.v(ContactTag, "id:"+id + "  name:" + name);
        		int number = Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
        		if (number > 0) {
        	            Cursor pCur = cr.query(  ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,  ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",   new String[]{id}, null);
        	 	        while (pCur.moveToNext()) {
        	 	        	String phoneNumber = pCur.getString( pCur.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER));  
        	 	        	//Log.v(ContactTag, "id:" + id + " name:" + name +" phone:" + phoneNumber+"!");
        	 	        	if(phoneNumber != null){
        	 	        		Log.v(ContactTag, "name:" + name +" phone:" + phoneNumber + "!!!!!!!!!!!!!!!!!!!!!");
        	 	        		arrListContactAll.add(name + ":" + phoneNumber);
        	 	        	}
        	 		    // Do something with phones
        	 	        } 
        	 	        pCur.close();
        				
        	 	    
        		}
            }
        }
	}
	
	public void setListView(String strStartInput)
	{
		String strStartNum886 = "+886";
		
        final ArrayList<String> targetArrayList = new ArrayList<String>();
        if( strStartInput.matches("[0-9|\\.]*")) //number type
        {
    		if(strStartInput.length() > 0)
    			strStartNum886 += strStartInput.substring(1);
            for (String tmpStr : arrListContactAll) {
            	String tmpStr2 = tmpStr.replaceAll(" ", "");
    			if(tmpStr2.indexOf(strStartInput) > -1  || tmpStr2.indexOf(strStartNum886) > -1 ){
    				targetArrayList.add(tmpStr);
    			}
    		}
        }
        else{ //string type
            for (String tmpStr : arrListContactAll) {            	
    			if(tmpStr.indexOf(strStartInput) > -1 ){
    				targetArrayList.add(tmpStr);
    			}
    		}
        }
        
		//nateaTestListView
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, targetArrayList);
        listView.setAdapter(adapter);
        
        listView.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
        		//mTextView.setText(targetArrayList.get(position));
        		String target = targetArrayList.get(position);
        		SetPhoneNumberList(target);	
        		Toast.makeText(view.getContext(), target + " added", Toast.LENGTH_LONG).show();
			}        	
        });
	}
}

