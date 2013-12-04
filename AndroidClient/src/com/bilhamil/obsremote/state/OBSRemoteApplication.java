package com.bilhamil.obsremote.state;

import com.bilhamil.obsremote.messages.IncomingMessage;
import com.bilhamil.obsremote.messages.IncomingMessageAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.tavendo.autobahn.*;
import android.app.Application;
import android.util.Log;
import android.widget.Toast;

public class OBSRemoteApplication extends Application 
{
    private Gson gson;
    
    private final WebSocketConnection remoteConnection = new WebSocketConnection();
	public static final String TAG = "com.bilhamil.obsremote";
	
	private static final String[] wsSubProtocols = {"obsapi"};
	
	public OBSRemoteApplication()
	{
	    GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(IncomingMessage.class, new IncomingMessageAdapter());
        
        gson = builder.create();
	}
	
	public void connect(String hostname) 
	{
		String wsuri = "ws://" + "192.168.11.59" + ":4444/";

		try {
			remoteConnection.connect(wsuri, wsSubProtocols, 
			                         new WSHandler(), new WebSocketOptions(), 
			                         null);
			
		} catch (WebSocketException e) {

			Log.d(TAG, e.toString());
		}
	}
	
	private class WSHandler implements WebSocket.ConnectionHandler
	{
	    @Override
        public void onTextMessage(String message)
        {
            // TODO Auto-generated method stub
            Log.d(TAG, "Incoming Message: " + message);
        }
        
        @Override
        public void onOpen()
        {
            Log.d(TAG, "Status: Connected");
        }
        
        @Override
        public void onClose(int arg0, String arg1)
        {
            Log.d(TAG, "Connection lost.");
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
}
