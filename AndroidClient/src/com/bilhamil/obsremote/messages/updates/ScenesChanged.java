package com.bilhamil.obsremote.messages.updates;

import com.bilhamil.obsremote.WebSocketService;

public class ScenesChanged extends Update
{

    @Override
    public void dispatchUpdate(WebSocketService serv)
    {
        serv.notifyOnScenesChanged();
    }

}
