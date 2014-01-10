package com.bilhamil.obsremote.messages.updates;

import com.bilhamil.obsremote.WebSocketService;
import com.google.gson.annotations.SerializedName;

public class SwitchScenes extends Update
{
    @SerializedName("scene-name")
    public String sceneName;

    @Override
    public void dispatchUpdate(WebSocketService serv)
    {
        serv.notifyOnSceneSwitch(sceneName);
    }
}
