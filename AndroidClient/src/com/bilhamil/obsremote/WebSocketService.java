package com.bilhamil.obsremote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.bilhamil.obsremote.activities.Splash;
import com.bilhamil.obsremote.messages.IncomingMessage;
import com.bilhamil.obsremote.messages.ResponseHandler;
import com.bilhamil.obsremote.messages.requests.Authenticate;
import com.bilhamil.obsremote.messages.requests.GetAuthRequired;
import com.bilhamil.obsremote.messages.requests.Request;
import com.bilhamil.obsremote.messages.responses.AuthRequiredResp;
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
import android.widget.Toast;

public class WebSocketService extends Service
{
    private static final String[] wsSubProtocols = {"obsapi"};

    private final WebSocketConnection remoteConnection = new WebSocketConnection();

    private Set<RemoteUpdateListener> listeners = new HashSet<RemoteUpdateListener>();
    private HashMap<String, ResponseHandler> responseHandlers = new HashMap<String, ResponseHandler>();

    /* status members */
    public boolean streaming;
    public Object previewOnly; 
        
    private String salted;
    private boolean authRequired;
    private boolean authenticated;

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
    
    public void disconnect()
    {
        remoteConnection.disconnect();
        resetState();
    }
    
    private void resetState()
    {
        responseHandlers.clear();
        streaming = false;
        previewOnly = false;
                
        salted = "";
        authRequired = false;
        authenticated = false;
    }
    
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        
        Log.d(OBSRemoteApplication.TAG, "WebSocketService stopped");
        this.notifyOnClose(0, "Service destroyed");
        
        listeners.clear();
        remoteConnection.disconnect();
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
            checkAuthRequired();
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
        if(inc == null)
            return;
        
        if(inc.isUpdate())
        {
            Update update = (Update)inc;

            /* polymorphic update dispatch */
            update.dispatchUpdate(this);
        }
        else
        {
            //it's a response
            Response resp = null;
            try
            {
                resp = (Response) inc;
            }
            catch(ClassCastException e)
            {
                Log.e(OBSRemoteApplication.TAG, "Failed to cast response.");
                return;
            }
            
            String messageId = resp.getID();
            ResponseHandler handler = responseHandlers.get(messageId); 
            if(handler != null)
            {
                handler.handleResponse(resp, message);
            } 
        }
    }

    /* auth stuff */
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
                    notifyOnAuthenticated();
                }
                else
                {
                    Toast toast = Toast.makeText(getApp(), "Auth failed: " + resp.getError(), Toast.LENGTH_LONG);
                    toast.show();
                    
                    getApp().setAuthSalted("");
                    
                    // try authenticating again
                    notifyOnFailedAuthentication(resp.getError());
                }
            }
        
        });
    }
    
    private void checkAuthRequired()
    {
        getApp().service.sendRequest(new GetAuthRequired(), new ResponseHandler() {

            @Override
            public void handleResponse(Response resp, String jsonMessage)
            {
                AuthRequiredResp authResp = getApp().getGson().fromJson(jsonMessage, AuthRequiredResp.class);
                authRequired = authResp.authRequired;
                               
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
                        /* else notify authentication needed */
                        getApp().setAuthSalt(authResp.salt);
                        
                        notifyNeedsAuthentication();
                    }
                    
                }
                else
                {
                    notifyOnAuthenticated();
                }
            }
        
        });
    }
    
    public void addUpdateListener(RemoteUpdateListener listener)
    {
        this.listeners.add(listener);
    }
    
    public void removeUpdateListener(RemoteUpdateListener listener)
    {
        this.listeners.remove(listener);
    }
    
    public boolean isConnected()
    {
        return this.remoteConnection.isConnected();
    }
    
    /* is everything ready for normal operation */
    public boolean isReady()
    {
        return isConnected() && (!authRequired || authenticated);
    }
    
    public boolean needsAuth()
    {
        return authRequired;
    }
    
    public boolean authenticated()
    {
        return authenticated;
    }
    /* methods for updating listeners */
    private void notifyNeedsAuthentication()
    {
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onNeedsAuthentication();
        }
    }
    
    private void notifyOnAuthenticated()
    {
        this.authenticated = true;
        
        if(authRequired && getApp().getRememberPassword())
        {
            getApp().setAuthSalted(salted);
        }
        
        Toast toast = Toast.makeText(this, "Authenticated!", Toast.LENGTH_LONG);
        toast.show();
        
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onConnectionAuthenticated();
        }
    }
    
    private void notifyOnFailedAuthentication(String message)
    {
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onFailedAuthentication(message);
        }
    }
    
    private void notifyOnClose(int code, String reason)
    {
        this.resetState();
        
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onConnectionClosed(code, reason);
        }
    }
    
    public void notifyOnStreamStarting(boolean previewOnly)
    {
        this.streaming = true;
        this.previewOnly = true;
        
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onStreamStarting(previewOnly);
        }
    }

    public void notifyOnStreamStopping()
    {
        this.streaming = false;
        this.previewOnly = false;
        
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onStreamStopping();
        }
    }

    public void notifyStreamStatusUpdate(int totalStreamTime, int fps,
            float strain, int numDroppedFrames, int numTotalFrames, int bps)
    {
               
        for(RemoteUpdateListener listener: listeners)
        {
            listener.notifyStreamStatusUpdate(totalStreamTime, fps, strain, numDroppedFrames, numTotalFrames, bps);
        }
    }

    public void notifyOnSceneSwitch(String sceneName)
    {
        for(RemoteUpdateListener listener: listeners)
        {
            listener.notifySceneSwitch(sceneName);
        }
    }

    public void notifyOnScenesChanged()
    {
        for(RemoteUpdateListener listener: listeners)
        {
            listener.notifyScenesChanged();
        }
    }

}
