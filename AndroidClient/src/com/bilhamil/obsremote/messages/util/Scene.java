package com.bilhamil.obsremote.messages.util;

import java.util.ArrayList;

public class Scene
{
    public String name;
    
    public ArrayList<Source> sources;
    
    @Override
    public String toString()
    {
        return name;
    }
}
