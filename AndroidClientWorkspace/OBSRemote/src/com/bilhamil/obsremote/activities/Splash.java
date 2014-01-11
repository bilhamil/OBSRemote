package com.bilhamil.obsremote.activities;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bilhamil.obsremote.OBSRemoteApplication;
import com.bilhamil.obsremote.R;
import com.bilhamil.obsremote.RemoteUpdateListener;
import com.bilhamil.obsremote.WebSocketService;
import com.bilhamil.obsremote.WebSocketService.LocalBinder;
import com.bilhamil.obsremote.messages.util.Source;

public class Splash extends FragmentActivity implements RemoteUpdateListener
{
	
    private boolean busy;
    protected boolean authRequired = false;
    public static boolean autoConnect = true;
    public WebSocketService service;

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
    }
	
	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            Splash.this.service = binder.getService();
            Splash.this.service.addUpdateListener(Splash.this);
            
            if(Splash.this.service.isConnected())
            {
                if(Splash.this.service.needsAuth())
                    AuthDialogFragment.startAuthentication(Splash.this,getApp(), Splash.this.service);
            }
            else if(autoConnect)
            {
                setNotBusy();
                defaultConnect();
                autoConnect = false;
            }
            else
            {
                setNotBusy();               
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Splash.this.service.removeUpdateListener(Splash.this);
            Splash.this.service = null;
            
            setNotBusy();
        }
    };
    
	@Override
	protected void onStart()
	{
	    super.onStart();
	    /* Startup the service */
        Intent intent = new Intent(this, WebSocketService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);	    
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
	    if(service != null)
            service.removeUpdateListener(this);
	    
	    unbindService(mConnection);
        service = null;
	}
	
	@Override
	protected void onDestroy()
	{
	    super.onDestroy();
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
		service.connect();
		
		setBusy();
	}
	
	public void defaultConnect()
	{
	    /* Get the service going */
        service.connect();
        
        setBusy();
	}

	public void setBusy()
	{
	    this.busy = true;
	    Button connectButton = (Button)findViewById(R.id.splashconnectbutton);
	    connectButton.setVisibility(View.INVISIBLE);
	    
	    ProgressBar spinner = (ProgressBar)findViewById(R.id.progressBar1);
        spinner.setVisibility(View.VISIBLE);
	}
	
	public void setNotBusy()
	{
	    this.busy = false;
	    Button connectButton = (Button)findViewById(R.id.splashconnectbutton);
        connectButton.setVisibility(View.VISIBLE);
        
        ProgressBar spinner = (ProgressBar)findViewById(R.id.progressBar1);
        spinner.setVisibility(View.INVISIBLE);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) 
	{
	    //No call for super(). Bug on API Level > 11.
	}
	
	@Override
    public void onConnectionAuthenticated()
    {
        /* Startup the remote since we're ready to go */
        Intent intent = new Intent(this, Remote.class);
        startActivity(intent);
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
    public void onStreamStarting(boolean previewOnly)
    {
        // Do nothing
    }

    @Override
    public void onStreamStopping()
    {
        // Do nothing
        
    }

    @Override
    public void onFailedAuthentication(String message)
    {
        AuthDialogFragment.startAuthentication(this, getApp(), service, message);
    }

    @Override
    public void onNeedsAuthentication()
    {
        AuthDialogFragment.startAuthentication(this, getApp(), service);
    }

    @Override
    public void onStreamStatusUpdate(int totalStreamTime, int fps,
            float strain, int numDroppedFrames, int numTotalFrames, int bps)
    {
        // do nothing
    }

    @Override
    public void onSceneSwitch(String sceneName)
    {
        // do nothing
    }

    @Override
    public void onScenesChanged()
    {
        // do nothing
    }

    @Override
    public void onSourceChanged(String sourceName, Source source)
    {
        // do nothing
    }

    @Override
    public void onSourceOrderChanged(ArrayList<String> sources)
    {
        // do nothing        
    }

    @Override
    public void onRepopulateSources(ArrayList<Source> sources)
    {
        // do nothing
    }

    @Override
    public void onVolumeChanged(String channel, boolean finalValue,
            float volume, boolean muted)
    {
        // do nothing
    }

    @Override
    public void onVersionMismatch(float version)
    {
        // Alert user of version mismatch
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage("The App Version (" + WebSocketService.appVersion + ") does not match the plugin version (" + version + ")")
               .setTitle(R.string.versionmismatch);
        
        builder.setPositiveButton(getString(R.string.ok), null);

        AlertDialog dialog = builder.create();
        
        dialog.show();
    }
    
    
}
