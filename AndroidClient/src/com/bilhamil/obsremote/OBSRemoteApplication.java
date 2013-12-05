package com.bilhamil.obsremote;

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
import android.util.Log;
import android.widget.Toast;

public class OBSRemoteApplication extends Application 
{
    public static final String TAG = "com.bilhamil.obsremote";
    private static final String[] wsSubProtocols = {"obsapi"};
    
    /* Preference names */
    private static final String HOST = "hostname";
    private static final String SALT = "salt";
        
    private Gson gson;
    private String connectingHostname;
    private String authChallenge;
    private String authSalt;
        
    private final WebSocketConnection remoteConnection = new WebSocketConnection();

    private ArrayList<RemoteUpdateListener> listeners = new ArrayList<RemoteUpdateListener>();
    
    private HashMap<String, ResponseHandler> responseHandlers = new HashMap<String, ResponseHandler>(); 
	
    
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
	
	public void connect(String hostname) 
	{
		String wsuri = "ws://" + "192.168.11.59" + ":4444/";
		connectingHostname = hostname;
		
		try {
			remoteConnection.connect(wsuri, wsSubProtocols, 
			                         new WSHandler(), new WebSocketOptions(), 
			                         null);
			
		} catch (WebSocketException e) {

			Log.d(TAG, e.toString());
		}
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
	
	private class WSHandler implements WebSocket.ConnectionHandler
	{
	    @Override
        public void onTextMessage(String message)
        {
            Log.d(TAG, "Incoming Message: " + message);
            handleIncomingMessage(message);
        }
        
        @Override
        public void onOpen()
        {
            Log.d(TAG, "Status: Connected");
            
            notifyOnOpen();
        }
        
        @Override
        public void onClose(int code, String reason)
        {
            Log.d(TAG, "Connection lost.");
            notifyOnClose(code, reason);
        }
        
        @Override
        public void onBinaryMessage(byte[] arg0)
        {
            //nothing
        }
        
        @Override
        public void onRawTextMessage(byte[] arg0)
        {
            //nothing
        }
	}
	
	public void sendRequest(Request request)
	{
	    sendRequest(request, null);
	}
	
    public void sendRequest(Request request, ResponseHandler messageHandler)
    {
        String messageJson = gson.toJson(request);
        
        if(messageHandler != null)
        {
            responseHandlers.put(request.messageId, messageHandler);
        }
        
        remoteConnection.sendTextMessage(messageJson);
    }
    
    public void handleIncomingMessage(String message)
    {
        IncomingMessage inc = gson.fromJson(message, IncomingMessage.class);
        if(inc.isUpdate())
        {
            Update update = (Update)inc;
            // TODO write update code
            
        }
        else
        {
            //it's a response
            Response resp = (Response) inc;
            String messageId = resp.getID();
            ResponseHandler handler = responseHandlers.get(messageId); 
            if(handler != null)
            {
                handler.handleResponse(message);
            } 
        }
    }

    public void addUpdateListener(RemoteUpdateListener listener)
    {
        this.listeners .add(listener);
    }
    
    public void removeUpdateListener(RemoteUpdateListener listener)
    {
        this.listeners.remove(listener);
    }
    
    /* methods for updating listeners */
    private void notifyOnOpen()
    {
        this.setDefaultHostname(connectingHostname);
        
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onConnectionOpen();
        }
    }
    
    private void notifyOnClose(int code, String reason)
    {
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onConnectionClosed(code, reason);
        }
    }
}
