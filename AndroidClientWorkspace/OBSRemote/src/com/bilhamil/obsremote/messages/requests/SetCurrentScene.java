package com.bilhamil.obsremote.messages.requests;

import com.google.gson.annotations.SerializedName;

public class SetCurrentScene extends Request
{
    @SerializedName("scene-name")
    public String sceneName;
    
    public SetCurrentScene(String sceneName)
    {
        super("SetCurrentScene");
        
        this.sceneName = sceneName;
    }
}
