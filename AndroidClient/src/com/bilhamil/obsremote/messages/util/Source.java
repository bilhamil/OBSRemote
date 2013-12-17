package com.bilhamil.obsremote.messages.util;

public class Source
{
    public String name;
    public boolean render;
    public float x, cx, y, cy;
    
    @Override
    public String toString()
    {
        return name;
    }

    public void conform(Source s)
    {
        /* deep copy */
        name = s.name;
        render = s.render;
        x = s.x;
        y = s.y;
        cx = s.cx;
        cy = s.cy;
    }
}
