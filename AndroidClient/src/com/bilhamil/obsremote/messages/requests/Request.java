package com.bilhamil.obsremote.messages.requests;

import com.google.gson.annotations.SerializedName;

public class Request
{
    public static int nextID = 1;
    
    @SerializedName("message-id")
    public String messageId;
    
    public Request()
    {
        messageId = (nextID++) + "";
    }
}
