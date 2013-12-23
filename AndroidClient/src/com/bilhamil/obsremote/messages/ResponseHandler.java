package com.bilhamil.obsremote.messages;

import com.bilhamil.obsremote.messages.responses.Response;

public interface ResponseHandler
{
    public void handleResponse(Response resp, String jsonMessage);
}
