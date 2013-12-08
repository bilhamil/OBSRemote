package com.bilhamil.obsremote;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import com.bilhamil.obsremote.messages.IncomingMessage;
import com.bilhamil.obsremote.messages.IncomingMessageAdapter;
import com.bilhamil.obsremote.messages.ResponseHandler;
import com.bilhamil.obsremote.messages.requests.GetAuthRequired;
import com.bilhamil.obsremote.messages.requests.Request;
import com.bilhamil.obsremote.messages.responses.Response;
import com.bilhamil.obsremote.messages.updates.Update;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.tavendo.autobahn.*;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

public class OBSRemoteApplication extends Application 
{
    public static final String TAG = "com.bilhamil.obsremote";
    
    /* Preference names */
    private static final String HOST = "hostname";
    private static final String SALT = "salt";
        
    private Gson gson;
    public String connectingHostname;
    private String authChallenge;
    private String authSalt;	
    
	public OBSRemoteApplication()
	{
	    GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(IncomingMessage.class, new IncomingMessageAdapter());
        
        gson = builder.create();
	}
	
	public Gson getGson()
	{
	    return gson;
	}
	
	public String getDefaultHostname()
	{
	    SharedPreferences prefMgr = PreferenceManager.getDefaultSharedPreferences(this);
	    return prefMgr.getString(HOST, HOST);
	}
	
	public void setDefaultHostname(String host)
	{
	    SharedPreferences prefMgr = PreferenceManager.getDefaultSharedPreferences(this);
	    Editor prefEdit = prefMgr.edit();

	    prefEdit.putString(HOST, host);

	    prefEdit.commit();
	}
	
	public static String sign(String password, String salt)
    {
	    try
	    {
	        MessageDigest md = MessageDigest.getInstance("SHA-256");
	        md.update((password + salt).getBytes("UTF8"));
	        return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
	    }
	    catch(Exception e)
	    {
	        Log.e(TAG, "Sign failed: ", e);
	    }
	    return "";
    }
	
	public void setAuthSalt(String salt)
    {
	    SharedPreferences prefMgr = PreferenceManager.getDefaultSharedPreferences(this);
        Editor prefEdit = prefMgr.edit();

        prefEdit.putString(SALT, salt);

        prefEdit.commit();
    }
    
    public String getAuthSalt()
    {
        SharedPreferences prefMgr = PreferenceManager.getDefaultSharedPreferences(this);
        return prefMgr.getString(SALT, "");
    }
	
	public void setAuthChallenge(String challenge)
	{
	    this.authChallenge = challenge;
	}
	
	public String getAuthChallenge()
	{
	    return authChallenge;
	}
	
	
}
