package com.bilhamil.obsremote.activities;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.bilhamil.obsremote.OBSRemoteApplication;
import com.bilhamil.obsremote.R;
import com.bilhamil.obsremote.RemoteUpdateListener;
import com.bilhamil.obsremote.WebSocketService;
import com.bilhamil.obsremote.messages.util.Source;

public class AuthDialogFragment extends DialogFragment implements RemoteUpdateListener 
{
    
    public WebSocketService service;
    
    public String message;
    public OBSRemoteApplication app;
    
    public static void startAuthentication(FragmentActivity fragAct, OBSRemoteApplication app, WebSocketService serv)
    {
        startAuthentication(fragAct, app, serv, null);
    }
    
    public static void startAuthentication(FragmentActivity fragAct, OBSRemoteApplication app,  WebSocketService serv, String errorMessage)
    {
        AuthDialogFragment frag = new AuthDialogFragment();
        frag.message = errorMessage;
        frag.app = app;
        frag.service = serv;
        fragAct.getSupportFragmentManager().beginTransaction().add(frag, OBSRemoteApplication.TAG).commitAllowingStateLoss();
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.password_dialog, null);
        CheckBox rememberCheckbox = (CheckBox) dialogView.findViewById(R.id.rememberPassword);
        rememberCheckbox.setChecked(app.getRememberPassword());
        
        //Set Error message
        if(message != null)
            ((TextView)dialogView.findViewById(R.id.authErrorView)).setText(message);
        
        builder.setView(dialogView);
        
        
        builder.setMessage(R.string.authenticate)
               .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       String password = ((EditText)AuthDialogFragment.this.getDialog().findViewById(R.id.password)).getText().toString();
                       boolean rememberPassword = ((CheckBox)AuthDialogFragment.this.getDialog().findViewById(R.id.rememberPassword)).isChecked();
                       
                       app.setRememberPass(rememberPassword);
                       
                       service.authenticate(password);
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       // User cancelled the dialog, shutdown everything
                       if(service != null)
                       {
                           service.disconnect();
                       }
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	
    	setRetainInstance(true);
        service.addUpdateListener(this);
    }
    
    @Override
    public void onDestroyView() 
    {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy()
    {
    	super.onDestroy();
        service.removeUpdateListener(this);
    }
    
    @Override
	public void onConnectionAuthenticated() 
	{}

	@Override
	public void onConnectionClosed(int code, String reason) 
	{
		this.dismissAllowingStateLoss();
	}

	@Override
	public void onStreamStarting(boolean previewOnly) 
	{}

	@Override
	public void onStreamStopping() 
	{}

	@Override
	public void onFailedAuthentication(String message) 
	{}

	@Override
	public void onNeedsAuthentication() 
	{}

	@Override
	public void onStreamStatusUpdate(int totalStreamTime, int fps,
			float strain, int numDroppedFrames, int numTotalFrames, int bps) 
	{}

	@Override
	public void onSceneSwitch(String sceneName) 
	{}

	@Override
	public void onScenesChanged() 
	{}

	@Override
	public void onSourceChanged(String sourceName, Source source) 
	{}

	@Override
	public void onSourceOrderChanged(ArrayList<String> sources) 
	{}

	@Override
	public void onRepopulateSources(ArrayList<Source> sources) 
	{}

	@Override
	public void onVolumeChanged(String channel, boolean finalValue,
			float volume, boolean muted) 
	{}

	@Override
	public void onVersionMismatch(float version) 
	{}
}
