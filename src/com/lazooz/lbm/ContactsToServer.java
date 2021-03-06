package com.lazooz.lbm;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.lazooz.lbm.businessClasses.Contact;
import com.lazooz.lbm.communications.ServerCom;
import com.lazooz.lbm.preference.MySharedPreferences;
import com.lazooz.lbm.utils.Utils;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.util.Log;

public class ContactsToServer extends AsyncTask<String, Void, String> {
	
	
	
	private Context mContext;


	public ContactsToServer(Context context) {
		super();
		mContext = context;
	}

	@Override
	protected String doInBackground(String... params) {
		
      	ServerCom bServerCom = new ServerCom(mContext);
    	
          
    	JSONObject jsonReturnObj=null;
		try {
			MySharedPreferences msp = MySharedPreferences.getInstance();
			
			JSONArray dataList = readContacts();
			dataList.length();
			String s = dataList.toString();
			Log.e("CONI", "array length: "+ dataList.length()  + "");
			Log.e("CONI", "string length: "+ s.length()  + "");
			byte[] dataCompressed = Utils.compress(s);
			Log.e("CONI", "compressed length: "+ dataCompressed.length  + "");
			bServerCom.setContactsZip(msp.getUserId(mContext), msp.getUserSecret(mContext), dataCompressed);
			
			jsonReturnObj = bServerCom.getReturnObject();
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
    	
    	String serverMessage = "";

		try {
			if (jsonReturnObj == null)
				serverMessage = "ConnectionError";
			else {
				serverMessage = jsonReturnObj.getString("message");
				if (serverMessage.equals("success")){
					if (jsonReturnObj.has("contacts_that_are_users")){
						JSONArray contacts = jsonReturnObj.getJSONArray("contacts_that_are_users");
						Log.e("TAG", contacts.toString());
						MySharedPreferences.getInstance().saveContactsWithInstalledApp(mContext, contacts);
					}
					if (jsonReturnObj.has("recommended_contacts_that_arent_users")){
						JSONArray contacts = jsonReturnObj.getJSONArray("recommended_contacts_that_arent_users");
						Log.e("TAG", contacts.toString());
						MySharedPreferences.getInstance().saveContactsRecommended(mContext, contacts);
					}
				}
			}
		} 
		catch (JSONException e) {
			e.printStackTrace();
			serverMessage = "GeneralError";
		}
		
		
		return serverMessage;
	}
	
	@Override
	protected void onPostExecute(String result) {
		
		if (result.equals("success_distance_achieved")){

		}
	}
		
	
	@Override
	protected void onPreExecute() {
		
	}

	
	private JSONArray readContacts(){
		JSONArray jsArray = new JSONArray();
		
		String currentLocale = Utils.getCurrentLocale(mContext);
		Cursor phones = mContext.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null,null, null);
		while (phones.moveToNext()) {

			String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

			String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

			Contact objContact = new Contact(currentLocale);
			//objContact.setName(name);
			objContact.setName("");
			objContact.setPhoneNo(phoneNumber);
			
			jsArray.put(objContact.toJSON());
			

		}
		phones.close();
		return jsArray;
		
		
	}
	
	
}
