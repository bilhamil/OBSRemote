package com.bilhamil.obsremote;

public interface RemoteUpdateListener
{
    public void onConnectionOpen();
    
    public void onConnectionClosed(int code, String reason);
}
