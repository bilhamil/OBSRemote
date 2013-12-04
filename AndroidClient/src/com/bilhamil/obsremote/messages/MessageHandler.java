package com.bilhamil.obsremote.messages;

import com.bilhamil.obsremote.messages.updates.Update;
import com.google.gson.*;

public class MessageHandler
{
    Gson gson;
    
    public MessageHandler()
    {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(IncomingMessage.class, new IncomingMessageAdapter());
        
        gson = builder.create();
    }
}
