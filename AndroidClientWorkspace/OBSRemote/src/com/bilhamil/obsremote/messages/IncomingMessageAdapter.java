package com.bilhamil.obsremote.messages;

import java.lang.reflect.Type;

import android.util.Log;

import com.bilhamil.obsremote.OBSRemoteApplication;
import com.bilhamil.obsremote.messages.responses.Response;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class IncomingMessageAdapter implements JsonDeserializer<IncomingMessage>
{
    private static final String UPDATE_TYPE = "update-type";
    
    @Override
    public IncomingMessage deserialize(JsonElement json, Type arg1,
            JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject jsonObject = json.getAsJsonObject();
        
        if(jsonObject.has(UPDATE_TYPE))
        {
            /* is an update */
            String updateName = jsonObject.get(UPDATE_TYPE).getAsString();
            Class<?> updateClass = null;
            try 
            {
                updateClass = Class.forName("com.bilhamil.obsremote.messages.updates."+updateName);
            }
            catch (ClassNotFoundException e)
            {
                Log.e(OBSRemoteApplication.TAG, "Couldn't map update: " + updateName, e);
                return null;
            }
            
            return context.deserialize(jsonObject, updateClass);
        }
        else
        {
            /* is a response */
            return context.deserialize(jsonObject, Response.class);
        }
    }

}
