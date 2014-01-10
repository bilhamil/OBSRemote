package com.bilhamil.obsremote.messages.updates;

import com.bilhamil.obsremote.WebSocketService;
import com.google.gson.annotations.SerializedName;

public class StreamStarting extends Update
{
    @SerializedName("preview-only")
    public boolean previewOnly;

    @Override
    public void dispatchUpdate(WebSocketService serv)
    {
        serv.notifyOnStreamStarting(previewOnly);
    }
}
