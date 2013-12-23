package com.bilhamil.obsremote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.bilhamil.obsremote.activities.Remote;
import com.bilhamil.obsremote.messages.IncomingMessage;
import com.bilhamil.obsremote.messages.ResponseHandler;
import com.bilhamil.obsremote.messages.requests.Authenticate;
import com.bilhamil.obsremote.messages.requests.GetAuthRequired;
import com.bilhamil.obsremote.messages.requests.GetVersion;
import com.bilhamil.obsremote.messages.requests.Request;
import com.bilhamil.obsremote.messages.responses.AuthRequiredResp;
import com.bilhamil.obsremote.messages.responses.Response;
import com.bilhamil.obsremote.messages.responses.VersionResponse;
import com.bilhamil.obsremote.messages.updates.Update;
import com.bilhamil.obsremote.messages.util.Source;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketOptions;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.support.v4.app.NotificationCompat.Builder;

public class WebSocketService extends Service
{
    public static final float appVersion = 1.1f;
    private static final String[] wsSubProtocols = {"obsapi"};

    private final WebSocketConnection remoteConnection = new WebSocketConnection();

    private Set<RemoteUpdateListener> listeners = new HashSet<RemoteUpdateListener>();
    private HashMap<String, ResponseHandler> responseHandlers = new HashMap<String, ResponseHandler>();

    /* status members */
    private boolean streaming;
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
        this.setStreaming(false);
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
    
    private Notification notification;
    private NotificationManager notfManager;
    private static final int NOTIFICATION_ID = 2106;
    
    public void setStreaming(boolean newStreaming)
    {
        if(!this.streaming && newStreaming)
        {
            notfManager = (NotificationManager) 
                    getSystemService(NOTIFICATION_SERVICE);
            
            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.remote_notification);
            
            Intent startRemote = new Intent(this, Remote.class);
            startRemote.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            
            // Gets a PendingIntent containing the entire back stack
            PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, startRemote, 0);
            
            Builder builder = new Builder(this);
            // Set Icon
            builder.setSmallIcon(R.drawable.notification_icon);
            // Set Ticker Message
            builder.setTicker("Stream Starting");
            // Don't Dismiss Notification
            builder.setAutoCancel(false);
            // Set PendingIntent into Notification
            builder.setContentIntent(resultPendingIntent);
            // Set RemoteViews into Notification
            builder.setContent(remoteViews);
            
            notification = builder.build();
            
            /* have to set this manually to deal with support library bug *facepalm* */
            notification.contentView = remoteViews;
            
            this.startForeground(NOTIFICATION_ID, notification);
        }
        else if(this.streaming && !newStreaming)        {
            //stop foreground
            this.stopForeground(true);
            notification = null;
        }
        
        this.streaming = newStreaming;
    }
    
    private long lastTimeUpdated = 0;
    
    public void updateNotification(int totalStreamTime, int fps,
            float strain, int numDroppedFrames, int numTotalFrames, int bps)
    {
        if(notification != null)
        {
            
            long currentTime = System.currentTimeMillis();
            
            if(currentTime - lastTimeUpdated < 250)
                return;
            
            lastTimeUpdated = currentTime;
            
            notification.contentView.setTextViewText(R.id.notificationtime, getString(R.string.timerunning) + " " + Remote.getTimeString(totalStreamTime));
            
            notification.contentView.setTextViewText(R.id.notificationfps, getString(R.string.fps) + " " + fps);
            
            notification.contentView.setTextViewText(R.id.notificationbittratevalue, Remote.getBitrateString(bps));
            
            if(android.os.Build.VERSION.SDK_INT > 9)
            {
                notification.contentView.setTextColor(R.id.notificationbittratevalue, Remote.strainToColor(strain));
            }
            
            notification.contentView.setTextViewText(R.id.notificationdropped, getString(R.string.droppedframes) + " " + numDroppedFrames);
            
            notfManager.notify(NOTIFICATION_ID, notification);
            
        }
    }
    
    public boolean getStreaming()
    {
        return streaming;
    }
    
    private class WSHandler implements WebSocket.ConnectionHandler
    {

        @Override
        public void onTextMessage(String message)
        {
            //Log.d(OBSRemoteApplication.TAG, "Incoming Message: " + message);
            handleIncomingMessage(message);
        }
        
        @Override
        public void onOpen()
        {
            Log.d(OBSRemoteApplication.TAG, "Status: Connected");
            checkVersion();
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
        String salt = getApp().getAuthSalt();
        getApp().getAuthChallenge();
            
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
                    getApp().setAuthSalted("");
                    
                    // try authenticating again
                    notifyOnFailedAuthentication(resp.getError());
                }
            }
        
        });
    }
    
    private void checkVersion()
    {
        getApp().service.sendRequest(new GetVersion(), new ResponseHandler()
        {
            
            @Override
            public void handleResponse(Response resp, String jsonMessage)
            {
                VersionResponse vResp = getApp().getGson().fromJson(jsonMessage, VersionResponse.class);
                
                if(vResp.version != appVersion)
                {
                    /* throw a fit */
                    
                    remoteConnection.disconnect();
                    
                    notifyOnVersionMismatch(vResp.version);
                }
                else
                {
                    checkAuthRequired();
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
        this.setStreaming(true);
        this.previewOnly = true;
        
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onStreamStarting(previewOnly);
        }
    }

    public void notifyOnStreamStopping()
    {
        this.setStreaming(false);
        this.previewOnly = false;
        
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onStreamStopping();
        }
    }

    public void notifyStreamStatusUpdate(int totalStreamTime, int fps,
            float strain, int numDroppedFrames, int numTotalFrames, int bps)
    {
        
        updateNotification(totalStreamTime, fps, strain, numDroppedFrames, numTotalFrames, bps);
        
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onStreamStatusUpdate(totalStreamTime, fps, strain, numDroppedFrames, numTotalFrames, bps);
        }
    }

    public void notifyOnSceneSwitch(String sceneName)
    {
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onSceneSwitch(sceneName);
        }
    }

    public void notifyOnScenesChanged()
    {
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onScenesChanged();
        }
    }

    public void notifySourceChange(String sourceName, Source source)
    {
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onSourceChanged(sourceName, source);
        }
    }

    public void notifySourceOrderChanged(ArrayList<String> sources)
    {
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onSourceOrderChanged(sources);
        }
    }

    public void notifyRepopulateSources(ArrayList<Source> sources)
    {
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onRepopulateSources(sources);
        }
    }

    public void notifyVolumeChanged(String channel, boolean finalValue,
            float volume, boolean muted)
    {
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onVolumeChanged(channel, finalValue, volume, muted);
        }
    }

    protected void notifyOnVersionMismatch(float version)
    {
        for(RemoteUpdateListener listener: listeners)
        {
            listener.onVersionMismatch(version);
        }        
    }
}
