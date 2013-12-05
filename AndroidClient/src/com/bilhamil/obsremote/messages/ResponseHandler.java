package com.bilhamil.obsremote.messages;

import com.google.gson.*;

public interface ResponseHandler
{
    public void handleResponse(String jsonMessage);
}
