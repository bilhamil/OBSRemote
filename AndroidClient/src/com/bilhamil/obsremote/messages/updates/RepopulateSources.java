package com.bilhamil.obsremote.messages.updates;

import java.util.ArrayList;

import com.bilhamil.obsremote.WebSocketService;
import com.bilhamil.obsremote.messages.util.Source;

public class RepopulateSources extends Update
{
    public ArrayList<Source> sources;
    
    @Override
    public void dispatchUpdate(WebSocketService serv)
    {
        serv.notifyRepopulateSources(sources);
    }

}
