package com.bilhamil.obsremote;

import com.bilhamil.obsremote.messages.updates.StreamStatus;

public interface RemoteUpdateListener
{
    public void onConnectionOpen();
    
    public void onConnectionClosed(int code, String reason);

    public void onStreamStarting();
    
    public void onStreamStopping();
    
    public void onStreamStatus(StreamStatus status);
}
