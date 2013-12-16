package com.bilhamil.obsremote;

import com.bilhamil.obsremote.messages.updates.StreamStatus;

public interface RemoteUpdateListener
{
    public void onConnectionAuthenticated();
    
    public void onConnectionClosed(int code, String reason);

    public void onStreamStarting(boolean previewOnly);
    
    public void onStreamStopping();
    
    public void onStreamStatus(StreamStatus status);

    public void onFailedAuthentication(String message);

    public void onNeedsAuthentication();

    public void notifyStreamStatusUpdate(int totalStreamTime, int fps,
            float strain, int numDroppedFrames, int numTotalFrames, int bps);

    public void notifySceneSwitch(String sceneName);

    public void notifyScenesChanged();
}
