package com.bilhamil.obsremote.messages.responses;

import java.util.ArrayList;

import com.bilhamil.obsremote.messages.util.Scene;
import com.google.gson.annotations.SerializedName;

public class GetSceneListResponse extends Response
{
    @SerializedName("current-scene")
    public String currentScene;
    
    public ArrayList<Scene> scenes;
}
