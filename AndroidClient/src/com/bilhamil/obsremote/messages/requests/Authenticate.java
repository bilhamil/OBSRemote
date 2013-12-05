package com.bilhamil.obsremote.messages.requests;

public class Authenticate extends Request
{
    public String auth;
    
    public Authenticate(String auth)
    {
        super("Authenticate");
        this.auth = auth;
    }
}
