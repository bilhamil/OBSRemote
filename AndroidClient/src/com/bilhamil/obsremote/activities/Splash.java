package com.bilhamil.obsremote.activities;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.bilhamil.obsremote.OBSRemoteApplication;
import com.bilhamil.obsremote.R;
import com.bilhamil.obsremote.RemoteUpdateListener;
import com.bilhamil.obsremote.R.id;
import com.bilhamil.obsremote.R.layout;
import com.bilhamil.obsremote.messages.ResponseHandler;
import com.bilhamil.obsremote.messages.requests.Authenticate;
import com.bilhamil.obsremote.messages.requests.GetAuthRequired;
import com.bilhamil.obsremote.messages.requests.GetVersion;
import com.bilhamil.obsremote.messages.responses.AuthRequiredResp;
import com.bilhamil.obsremote.messages.responses.Response;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Splash extends Activity implements RemoteUpdateListener
{
	
	@Override
    protected void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.splash);
        
        //Set font for title
        TextView headerTextView=(TextView)findViewById(R.id.splashheader);
        Typeface typeFace=Typeface.createFromAsset(getAssets(),"fonts/neometricmedium.ttf");
        headerTextView.setTypeface(typeFace);
        
        //Set hostname to saved hostname
        EditText hostnameEdit = (EditText)findViewById(R.id.hostentry);
        hostnameEdit.setText(getApp().getDefaultHostname());
    }
	
	@Override
	protected void onStart()
	{
	    super.onStart();
	    
	    getApp().addUpdateListener(this);
	}
	
	@Override
	protected void onStop()
	{
	    super.onStop();
	    getApp().removeUpdateListener(this);
	}
	
	
	public OBSRemoteApplication getApp()
	{
	    return (OBSRemoteApplication)getApplicationContext();
	}
	
	public void connect(View view)
	{
		//Get hostname and connect
		String hostname = ((EditText)findViewById(R.id.hostentry)).getText().toString();
		
		getApp().connect(hostname);
	}

	public void authenticated()
	{
	    
	}
	
	public void startAuthentication()
	{
	    authenticate("password");
	}
	
	public void authenticate(String password)
	{
	    String salted = null, hashed = null;
	    try
        {
	        /* Salt */
	        String salt = getApp().getAuthSalt();
	        String challenge = getApp().getAuthChallenge();
	        
	        MessageDigest md = MessageDigest.getInstance("SHA-256");
	        md.update((password + salt).getBytes("UTF8"));
            salted = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            
            md = MessageDigest.getInstance("SHA-256");
            md.update((salted + challenge).getBytes("UTF8"));
            hashed = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            
        } catch (Exception e)
        {
            //shouldn't ever happen
            Log.e(OBSRemoteApplication.TAG, "Auth Failed: ", e);
        } 
	    
	    getApp().sendRequest(new Authenticate(hashed), new ResponseHandler() {

            @Override
            public void handleResponse(String jsonMessage)
            {
                Response resp = getApp().getGson().fromJson(jsonMessage, Response.class);
                
                if(resp.isOk())
                {
                    Toast toast = Toast.makeText(Splash.this, "Authenticated!", Toast.LENGTH_LONG);
                    toast.show();
                }
                else
                {
                    Toast toast = Toast.makeText(Splash.this, "Auth failed: " + resp.getError(), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        
        });
	    
	}
	
    @Override
    public void onConnectionOpen()
    {
        getApp().sendRequest(new GetAuthRequired(), new ResponseHandler() {

            @Override
            public void handleResponse(String jsonMessage)
            {
                AuthRequiredResp resp = getApp().getGson().fromJson(jsonMessage, AuthRequiredResp.class);
                
                if(resp.authRequired)
                {
                    getApp().setAuthChallenge(resp.challenge);
                    getApp().setAuthSalt(resp.salt);
                    
                    startAuthentication();
                }
                else
                {
                    authenticated();
                }
            }
        
        });
    }


    @Override
    public void onConnectionClosed(int code, String reason)
    {
        
    }
}
