package com.bilhamil.obsremote;

import java.util.ArrayList;
import java.util.HashMap;

import com.bilhamil.obsremote.messages.IncomingMessage;
import com.bilhamil.obsremote.messages.ResponseHandler;
import com.bilhamil.obsremote.messages.requests.Request;
import com.bilhamil.obsremote.messages.responses.Response;
import com.bilhamil.obsremote.messages.updates.Update;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketOptions;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class WebSocketService extends Service
{
    private static final String[] wsSubProtocols = {"obsapi"};

    private final WebSocketConnection remoteConnection = new WebSocketConnection();

    private ArrayList<RemoteUpdateListener> listeners = new ArrayList<RemoteUpdateListener>();
    private HashMap<String, ResponseHandler> responseHandlers = new HashMap<String, ResponseHandler>(); 

    
    
    
    public void connect() 
    {
        String hostname = getApp().getDefaultHostname();
        String wsuri = "ws://" + hostname + ":4444/";
        
        try {
            remoteConnection.connect(wsuri, wsSubProtocols, 
                                     new WSHandler(), new WebSocketOptions(), 
                                     null);
            
        } catch (WebSocketException e) {

            Log.d(OBSRemoteApplication.TAG, e.toString());
        }
    }
    
    public OBSRemoteApplication getApp()
    {
        return (OBSRemoteApplication) getApplicationContext();
    }
    
    public class LocalBinder extends Binder {
        public WebSocketService getService() {
            // Return this instance of WebSocketService so clients can call public methods
            return WebSocketService.this;
        }
    }
   
    @Override
    public IBinder onBind(Intent intent)
    {
        return new LocalBinder();
    }
    
    private class WSHandler implements WebSocket.ConnectionHandler
    {

        @Override
        public void onTextMessage(String message)
        {
            Log.d(OBSRemoteApplication.TAG, "Incoming Message: " + message);
            handleIncomingMessage(message);
        }
        
        @Override
        public void onOpen()
        {
            Log.d(OBSRemoteApplication.TAG, "Status: Connected");
            notifyOnOpen();
        }
        
        @Override
        public void onClose(int code, String reason)
        {
            Log.d(OBSRemoteApplication.TAG, "Connection lost.");
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
        String messageJson = getApp().getGson().toJson(request);
        
        if(messageHandler != null)
        {
            responseHandlers.put(request.messageId, messageHandler);
        }
        
        remoteConnection.sendTextMessage(messageJson);
    }
    
    public void handleIncomingMessage(String message)
    {
        IncomingMessage inc = getApp().getGson().fromJson(message, IncomingMessage.class);
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
    
    public boolean isConnected()
    {
        return this.remoteConnection.isConnected();
    }
    
    /* methods for updating listeners */
    private void notifyOnOpen()
    {
        getApp().setDefaultHostname(getApp().connectingHostname);
        
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
