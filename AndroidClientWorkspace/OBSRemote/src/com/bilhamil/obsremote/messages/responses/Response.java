package com.bilhamil.obsremote.messages.responses;

import com.bilhamil.obsremote.messages.IncomingMessage;
import com.google.gson.annotations.SerializedName;

public class Response implements IncomingMessage
{
    public static final String OK = "ok";
    public static final String ERROR = "error";
    
    @SerializedName("message-id")
    protected String messageID;
    
    protected String status;
    
    protected String error;
        
    @Override
    public boolean isUpdate()
    {
        //this is a response so return false
        return false;
    }
    
    public String getID()
    {
        return messageID;
    }
    
    public boolean isError()
    {
        return status.equals(ERROR);
    }
    
    public boolean isOk()
    {
        return status.equals(OK);
    }
    
    public String getError()
    {
        return error;
    }
    
}
