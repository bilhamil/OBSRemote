package com.bilhamil.obsremote.messages.updates;

import com.bilhamil.obsremote.WebSocketService;

public class StreamStopping extends Update
{

    @Override
    public void dispatchUpdate(WebSocketService serv)
    {
        serv.notifyOnStreamStopping();
    }

}
