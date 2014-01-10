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
    
    public boolean equals(Object o)
    {
        if(!(o instanceof Source))
            return false;
        Source os = (Source) o;
        return name.equals(os.name) && render == os.render && 
               x == os.x && cx == os.cx &&
               y == os.y && cy == os.cy;
    }
}
