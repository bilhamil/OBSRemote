package com.bilhamil.obsremote.messages.updates;

import com.bilhamil.obsremote.WebSocketService;
import com.google.gson.annotations.SerializedName;

public class StreamStatus extends Update
{
    @SerializedName("total-stream-time")
    public int totalStreamTime;
    
    public int fps;
    
    @SerializedName("preview-only")
    public boolean previewOnly;
    
    public boolean streaming;
    
    public float strain;
    
    @SerializedName("num-total-frames")
    public int numTotalFrames;
    
    @SerializedName("num-dropped-frames")
    public int numDroppedFrames;

    @SerializedName("bytes-per-sec")
    public int bytesPerSecond;
    
    @Override
    public void dispatchUpdate(WebSocketService serv)
    {
        serv.notifyStreamStatusUpdate(totalStreamTime, fps, strain, numDroppedFrames, numTotalFrames, bytesPerSecond);
    }
    
}
