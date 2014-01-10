package com.bilhamil.obsremote.messages.responses;

import com.google.gson.annotations.SerializedName;

public class StreamStatusResponse extends Response
{
    @SerializedName("preview-only")
    public boolean previewOnly;
    
    public boolean streaming;
}
