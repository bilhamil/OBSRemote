package com.bilhamil.obsremote.messages.requests;

import com.google.gson.annotations.SerializedName;

public class SetVolume extends Request
{
    public String channel;
    
    @SerializedName("final")
    public boolean finalValue;
    
    public float volume;
    
    public SetVolume()
    {
        super("SetVolume");
    }
}
