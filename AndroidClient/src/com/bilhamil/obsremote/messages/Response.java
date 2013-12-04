package com.bilhamil.obsremote.messages;

import com.google.gson.annotations.SerializedName;

public class Response implements IncomingMessage
{
    public static final String OK = "ok";
    public static final String ERROR = "error";
    
    @SerializedName("message-id")
    protected int messageID;
    
    protected String status;
    
    protected String error;
        
    @Override
    public boolean isUpdate()
    {
        //this is a response so return false
        return false;
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
