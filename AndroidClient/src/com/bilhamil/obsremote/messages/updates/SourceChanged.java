package com.bilhamil.obsremote.messages.updates;

import com.bilhamil.obsremote.WebSocketService;
import com.bilhamil.obsremote.messages.util.Source;
import com.google.gson.annotations.SerializedName;

public class SourceChanged extends Update
{
    @SerializedName("source-name")
    public String sourceName;
    
    public Source source;
    
    @Override
    public void dispatchUpdate(WebSocketService serv)
    {
        serv.notifySourceChange(sourceName, source);
        
    }

}
