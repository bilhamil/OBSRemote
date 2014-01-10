package com.bilhamil.obsremote.messages.updates;

import java.util.ArrayList;

import com.bilhamil.obsremote.WebSocketService;

public class SourceOrderChanged extends Update
{
    public ArrayList<String> sources;
    
    @Override
    public void dispatchUpdate(WebSocketService serv)
    {
        serv.notifySourceOrderChanged(sources);
    }

}
