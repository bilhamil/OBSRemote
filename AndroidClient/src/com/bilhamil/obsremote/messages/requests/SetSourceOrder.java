package com.bilhamil.obsremote.messages.requests;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

public class SetSourceOrder extends Request
{
    @SerializedName("scene-names")
    public ArrayList<String> sources;
    
    public SetSourceOrder(ArrayList<String> sources)
    {
        super("SetSourceOrder");
        this.sources = sources;
    }

}
