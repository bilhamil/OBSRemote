package com.bilhamil.obsremote.messages.responses;

import com.google.gson.annotations.SerializedName;

public class VolumesResponse extends Response
{
    @SerializedName("desktop-volume")
    public double desktopVolume;
    
    @SerializedName("desktop-muted")
    public boolean desktopMuted;
    
    @SerializedName("mic-volume")
    public double micVolume;
    
    @SerializedName("mic-muted")
    public boolean micMuted;
}
