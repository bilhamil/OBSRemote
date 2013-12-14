package com.bilhamil.obsremote.activities;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.bilhamil.obsremote.OBSRemoteApplication;
import com.bilhamil.obsremote.R;
import com.bilhamil.obsremote.RemoteUpdateListener;
import com.bilhamil.obsremote.R.id;
import com.bilhamil.obsremote.R.layout;
import com.bilhamil.obsremote.WebSocketService;
import com.bilhamil.obsremote.WebSocketService.LocalBinder;
import com.bilhamil.obsremote.messages.ResponseHandler;
import com.bilhamil.obsremote.messages.requests.Authenticate;
import com.bilhamil.obsremote.messages.requests.GetAuthRequired;
import com.bilhamil.obsremote.messages.requests.GetVersion;
import com.bilhamil.obsremote.messages.responses.AuthRequiredResp;
import com.bilhamil.obsremote.messages.responses.Response;
import com.bilhamil.obsremote.messages.updates.StreamStatus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

public class Splash extends FragmentActivity implements RemoteUpdateListener
{
	
    private boolean busy;
    private String salted;
    protected boolean authRequired = false;


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
        
        String defaultHostname = getApp().getDefaultHostname();
        hostnameEdit.setText(defaultHostname);
        
        /* Startup the service */
        Intent intent = new Intent(this, WebSocketService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
	
	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            getApp().service = binder.getService();
            getApp().service.addUpdateListener(Splash.this);
            
            if(getApp().service.isConnected())
            {
                checkAuthRequired();
            }
            else
            {
                setNotBusy();
                defaultConnect();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            getApp().service.removeUpdateListener(Splash.this);
            getApp().service = null;
            
            setNotBusy();
        }
    };
    
	@Override
	protected void onStart()
	{
	    super.onStart();
	    WebSocketService service = getApp().service;
	    if(service != null)
	    {
	        service.addUpdateListener(this);
	        if(!service.isConnected())
	        {
	            setNotBusy();
	        }
	    }
	    
	    
	}
	
	@Override
	protected void onResume()
	{
	    super.onResume();
	    if(busy)
	    {
	        this.setBusy();
	    }
	}
	
	@Override
    protected void onPause()
    {
        super.onResume();
        if(busy)
        {
            ImageView icon = (ImageView)findViewById(R.id.splashLogo);
            icon.setAnimation(null);
        }
    }
	
	@Override
	protected void onStop()
	{
	    super.onStop();
	    if(getApp().service != null)
            getApp().service.removeUpdateListener(this);
	}
	
	@Override
	protected void onDestroy()
	{
	    super.onDestroy();
	    
        unbindService(mConnection);
        getApp().service = null;
	}
	
	public OBSRemoteApplication getApp()
	{
	    return (OBSRemoteApplication)getApplicationContext();
	}
	
	public void connect(View view)
	{
		//Get hostname and connect
		String hostname = ((EditText)findViewById(R.id.hostentry)).getText().toString();
		getApp().setDefaultHostname(hostname);
		
		/* Get the service going */
		getApp().service.connect();
		
		setBusy();
	}
	
	public void defaultConnect()
	{
	    /* Get the service going */
        getApp().service.connect();
        
        setBusy();
	}

	public void setBusy()
	{
	    this.busy = true;
	    Button connectButton = (Button)findViewById(R.id.splashconnectbutton);
	    connectButton.setVisibility(View.INVISIBLE);
	    
	    ImageView icon = (ImageView)findViewById(R.id.splashLogo);
	    
	    RotateAnimation anim = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
	    anim.setInterpolator(new LinearInterpolator());
	    anim.setRepeatCount(Animation.INFINITE);
	    anim.setDuration(1500);
	    
	    icon.startAnimation(anim);
	}
	
	public void setNotBusy()
	{
	    this.busy = false;
	    Button connectButton = (Button)findViewById(R.id.splashconnectbutton);
        connectButton.setVisibility(View.VISIBLE);
        
        ImageView icon = (ImageView)findViewById(R.id.splashLogo);
        icon.setAnimation(null);
	}
	
	//Called after authentication is successful
	public void authenticated()
	{
	    if(authRequired && getApp().getRememberPassword())
	    {
	        getApp().setAuthSalted(salted);
	    }
	    
	    Toast toast = Toast.makeText(Splash.this, "Authenticated!", Toast.LENGTH_LONG);
        toast.show();
        
        /* Startup the remote since we're ready to go */
        Intent intent = new Intent(this, Remote.class);
        startActivity(intent);
	}
	
	public void startAuthentication()
	{
	    this.startAuthentication(null);
	}
	
	public void startAuthentication(String errorMessage)
	{
	    AuthDialogFragment frag = new AuthDialogFragment();
	    frag.splash = this;
	    frag.message = errorMessage;
	    frag.show(this.getSupportFragmentManager(), OBSRemoteApplication.TAG);
	}
	
	public void autoAuthenticate()
	{
	    salted = getApp().getAuthSalted();
	    authenticateWithSalted(salted);
	}
	
	public void authenticate(String password)
	{
	    String hashed;

	    String salt = getApp().getAuthSalt();
        String challenge = getApp().getAuthChallenge();
	        
        salted = OBSRemoteApplication.sign(password, salt);      
        authenticateWithSalted(salted);
	}
	
	public void authenticateWithSalted(String salted)
	{
	    String challenge = getApp().getAuthChallenge();
	    String hashed;
	    
	    hashed = OBSRemoteApplication.sign(salted,  challenge);
        
        getApp().service.sendRequest(new Authenticate(hashed), new ResponseHandler() {

            @Override
            public void handleResponse(Response resp, String jsonMessage)
            {
                
                if(resp.isOk())
                {
                    authenticated();
                }
                else
                {
                    Toast toast = Toast.makeText(Splash.this, "Auth failed: " + resp.getError(), Toast.LENGTH_LONG);
                    toast.show();
                    
                    getApp().setAuthSalted("");
                    
                    // try authenticating again
                    startAuthentication(resp.getError());
                }
            }
        
        });
	}
	
	public static class AuthDialogFragment extends DialogFragment {
	    
	    public String message;
        public Splash splash;
	    
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        // Use the Builder class for convenient dialog construction
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        
	        // Get the layout inflater
	        LayoutInflater inflater = getActivity().getLayoutInflater();
	        View dialogView = inflater.inflate(R.layout.password_dialog, null);
	        CheckBox rememberCheckbox = (CheckBox) dialogView.findViewById(R.id.rememberPassword);
	        rememberCheckbox.setChecked(splash.getApp().getRememberPassword());
	        
	        //Set Error message
	        if(message != null)
	            ((TextView)dialogView.findViewById(R.id.authErrorView)).setText(message);
	        
	        builder.setView(dialogView);
	        
	        
	        builder.setMessage(R.string.authenticate)
	               .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       String password = ((EditText)AuthDialogFragment.this.getDialog().findViewById(R.id.password)).getText().toString();
	                       boolean rememberPassword = ((CheckBox)AuthDialogFragment.this.getDialog().findViewById(R.id.rememberPassword)).isChecked();
	                       
	                       splash.getApp().setRememberPass(rememberPassword);
	                       splash.authenticate(password);
	                   }
	               })
	               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       // User cancelled the dialog, shutdown everything
	                       if(splash.getApp().service != null)
	                       {
	                           splash.getApp().service.disconnect();
	                       }
	                   }
	               });
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	}
	
    @Override
    public void onConnectionOpen()
    {
        checkAuthRequired();
    }

    private void checkAuthRequired()
    {
        getApp().service.sendRequest(new GetAuthRequired(), new ResponseHandler() {

            @Override
            public void handleResponse(Response resp, String jsonMessage)
            {
                AuthRequiredResp authResp = getApp().getGson().fromJson(jsonMessage, AuthRequiredResp.class);
                Splash.this.authRequired = authResp.authRequired;
                               
                if(authRequired)
                {
                    getApp().setAuthChallenge(authResp.challenge);
                    
                    if(getApp().getAuthSalt().equals(authResp.salt) && 
                       getApp().getRememberPassword() && 
                       !getApp().getAuthSalted().equals(""))
                    {
                        /* circumstances right to try auto authenticate */
                        autoAuthenticate();
                    }
                    else
                    {
                        /* else just startup dialog authentication */
                        getApp().setAuthSalt(authResp.salt);
                        
                        startAuthentication();
                    }
                    
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
        runOnUiThread(new Runnable() {

            @Override
            public void run()
            {
                setNotBusy();
            }
            
        });
    }

    @Override
    public void onStreamStarting()
    {
        // Do nothing
    }

    @Override
    public void onStreamStopping()
    {
        // Do nothing
        
    }

    @Override
    public void onStreamStatus(StreamStatus status)
    {
        // Do nothing
        
    }
}
