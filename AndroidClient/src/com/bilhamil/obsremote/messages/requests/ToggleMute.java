package com.bilhamil.obsremote.messages.requests;

public class ToggleMute extends Request
{
    public static final String DESKTOP = "desktop";
    public static final String MICROPHONE = "microphone";
    
    public String channel;
    
    public ToggleMute(String chan)
    {
        super("ToggleMute");
        this.channel = chan;
    }
    
    public static ToggleMute getDesktopMute()
    {
        return new ToggleMute(DESKTOP);
    }
    
    public static ToggleMute getMicrophoneMute()
    {
        return new ToggleMute(MICROPHONE);
    }
}
