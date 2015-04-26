/********************************************************************************
 Copyright (C) 2013 Hugh Bailey <obs.jim@gmail.com>
 Copyright (C) 2013 William Hamilton <bill@ecologylab.net>

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
********************************************************************************/

#include "MessageHandling.h"

int failedAuthAttempts = 0;

void OBSAPIMessageHandler::initializeMessageMap()
{
    messageMap[REQ_GET_VERSION] =                       OBSAPIMessageHandler::HandleGetVersion;
    messageMap[REQ_GET_AUTH_REQUIRED] =                 OBSAPIMessageHandler::HandleGetAuthRequired;
    messageMap[REQ_AUTHENTICATE] =                      OBSAPIMessageHandler::HandleAuthenticate;
    messageMap[REQ_GET_CURRENT_SCENE] =                 OBSAPIMessageHandler::HandleGetCurrentScene;
    messageMap[REQ_GET_SCENE_LIST] =                    OBSAPIMessageHandler::HandleGetSceneList;
    messageMap[REQ_SET_CURRENT_SCENE] =                 OBSAPIMessageHandler::HandleSetCurrentScene;
    messageMap[REQ_SET_SOURCES_ORDER] =                 OBSAPIMessageHandler::HandleSetSourcesOrder;
    messageMap[REQ_SET_SOURCE_RENDER] =                 OBSAPIMessageHandler::HandleSetSourceRender;
    messageMap[REQ_SET_SCENEITEM_POSITION_AND_SIZE] =   OBSAPIMessageHandler::HandleSetSceneItemPositionAndSize;
    messageMap[REQ_GET_STREAMING_STATUS] =              OBSAPIMessageHandler::HandleGetStreamingStatus;
    messageMap[REQ_STARTSTOP_STREAMING] =               OBSAPIMessageHandler::HandleStartStopStreaming;
	messageMap[REQ_STARTSTOP_RECORDING] =               OBSAPIMessageHandler::HandleStartStopRecording;
    messageMap[REQ_TOGGLE_MUTE] =                       OBSAPIMessageHandler::HandleToggleMute;
    messageMap[REQ_GET_VOLUMES] =                       OBSAPIMessageHandler::HandleGetVolumes;
    messageMap[REQ_SET_VOLUME] =                        OBSAPIMessageHandler::HandleSetVolume;

    messagesNotRequiringAuth.insert(REQ_GET_VERSION);
    messagesNotRequiringAuth.insert(REQ_GET_AUTH_REQUIRED);
    messagesNotRequiringAuth.insert(REQ_AUTHENTICATE);

    mapInitialized = true;
}

OBSAPIMessageHandler::OBSAPIMessageHandler(struct libwebsocket *ws):wsi(ws), mapInitialized(FALSE)
{
    if(!mapInitialized)
    {
        initializeMessageMap();
    }
    authenticated = false;

    challenge = getRemoteConfig()->getChallenge();
}

json_t* GetOkResponse(json_t* id = NULL)
{
    json_t* ret = json_object();
    json_object_set_new(ret, "status", json_string("ok"));
    if(id != NULL && json_is_string(id))
    {
        json_object_set(ret, "message-id", id);
    }
    
    return ret;
}

json_t* GetErrorResponse(const char * error, json_t *id = NULL)
{
    json_t* ret = json_object();
    json_object_set_new(ret, "status", json_string("error"));
    json_object_set_new(ret, "error", json_string(error));
    
    if(id != NULL && json_is_string(id))
    {
        json_object_set(ret, "message-id", id);
    }

    return ret;
}

bool OBSAPIMessageHandler::HandleReceivedMessage(void *in, size_t len)
{
    json_error_t err;
    json_t* message = json_loads((char *) in, JSON_DISABLE_EOF_CHECK, &err);
    if(message == NULL)
    {
        /* failed to parse the message */
        return false;
    }
    json_t* type = json_object_get(message, "request-type");
    json_t* id = json_object_get(message, "message-id");
    if(!json_is_string(type))
    {
        this->messagesToSend.push_back(GetErrorResponse("message type not specified", id));
        json_decref(message);
        return true;
    }

    const char* requestType = json_string_value(type);
    MessageFunction messageFunc = messageMap[requestType];
    json_t* ret = NULL;
    
    if(messageFunc != NULL)
    {
        if(!getRemoteConfig()->useAuth || 
            this->authenticated || 
            messagesNotRequiringAuth.find(requestType) != messagesNotRequiringAuth.end())
        {
            ret = messageFunc(this, message);
        }
        else
        {
            ret = GetErrorResponse("Not Authenticated", id);
        }
    }
    else
    {
        this->messagesToSend.push_back(GetErrorResponse("message type not recognized", id));
        json_decref(message);
        return true;
    }

    if(ret != NULL)
    {
        if(json_is_string(id))
        {
            json_object_set(ret, "message-id", id);
        }

        json_decref(message);
        this->messagesToSend.push_back(ret);
        return true;
    }
    else
    {
        this->messagesToSend.push_back(GetErrorResponse("no response given", id));
        json_decref(message);
        return true;
    }

    return false;
}

json_t* json_string_wchar(CTSTR str)
{
    if(!str)
    {
        return NULL;
    }

    size_t wcharLen = slen(str);
    size_t curLength = (UINT)wchar_to_utf8_len(str, wcharLen, 0);

    if(curLength)
    {
        char *out = (char *) malloc((curLength+1));
        wchar_to_utf8(str,wcharLen, out, curLength + 1, 0);
        out[curLength] = 0;
        json_t* ret = json_string(out);
        free(out);
        return ret;
    }
    else
    {
        return NULL;
    }
}

json_t* getSourceJson(XElement* source)
{
    json_t* ret = json_object();
    CTSTR name = source->GetName();
    float x = source->GetFloat(TEXT("x"));
    float y = source->GetFloat(TEXT("y"));
    float cx = source->GetFloat(TEXT("cx"));
    float cy = source->GetFloat(TEXT("cy"));
    bool render = source->GetInt(TEXT("render")) > 0;
    
    json_object_set_new(ret, "name", json_string_wchar(name));
    json_object_set_new(ret, "x", json_real(x));
    json_object_set_new(ret, "y", json_real(y));
    json_object_set_new(ret, "cx", json_real(cx));
    json_object_set_new(ret, "cy", json_real(cy));
    json_object_set_new(ret, "render", json_boolean(render));

    return ret;
}

json_t* getSceneJson(XElement* scene)
{
    XElement* sources = scene->GetElement(TEXT("sources"));
    json_t* ret = json_object();
    json_t* scene_items = json_array();
    json_object_set_new(ret, "name", json_string_wchar(scene->GetName()));

    if(sources != NULL)
    {
        for(UINT i = 0; i < sources->NumElements(); i++)
        {
            XElement* source = sources->GetElementByID(i);
            
            json_array_append(scene_items, getSourceJson(source));
        }
    }

    json_object_set_new(ret, "sources", scene_items);

    return ret;
}

/* Message Handlers */
json_t* OBSAPIMessageHandler::HandleGetVersion(OBSAPIMessageHandler* handler, json_t* message)
{
    json_t* ret = GetOkResponse();

    json_object_set_new(ret, "version", json_real(OBS_REMOTE_VERSION));
    
    return ret;
}

json_t* OBSAPIMessageHandler::HandleGetAuthRequired(OBSAPIMessageHandler* handler, json_t* message)
{
    json_t* ret = GetOkResponse();

    json_object_set_new(ret, "authRequired", json_boolean(getRemoteConfig()->useAuth));
    
    if(getRemoteConfig()->useAuth)
    {
        json_object_set_new(ret, "challenge", json_string(handler->challenge.c_str()));
        json_object_set_new(ret, "salt", json_string(getRemoteConfig()->authSalt.c_str()));
    }

    return ret;
}

json_t* OBSAPIMessageHandler::HandleAuthenticate(OBSAPIMessageHandler* handler, json_t* message)
{
    json_t* auth = json_object_get(message, "auth");
    if(auth == NULL || !json_is_string(auth))
    {
        return GetErrorResponse("auth not specified!");
    }
    
    bool authCheck = getRemoteConfig()->checkChallengeAuth(json_string_value(auth), handler->challenge.c_str());

    if(authCheck && failedAuthAttempts < OBS_REMOTE_MAX_FAILED_AUTH_ATTEMPTS)
    {
        json_t* ret = GetOkResponse();
        handler->authenticated = true;

		failedAuthAttempts = 0;

        return ret;
    }
    else
    {
		failedAuthAttempts++;
		json_t* ret = NULL;

		if(failedAuthAttempts >= OBS_REMOTE_MAX_FAILED_AUTH_ATTEMPTS)
		{
			ret = GetErrorResponse("OBS Remote Locked: Maximum failed authentication attempts reached. Please Restart OBS to Unlock.");
		}
		else
		{
			char buf[64];
			_snprintf(buf, 64, "Authentication Failed. Attempts Remaining: %d", OBS_REMOTE_MAX_FAILED_AUTH_ATTEMPTS - failedAuthAttempts);
			ret = GetErrorResponse(buf);
		}
		
        return ret;
    }
}

json_t* OBSAPIMessageHandler::HandleGetCurrentScene(OBSAPIMessageHandler* handler, json_t* message)
{
    OBSEnterSceneMutex();
    
    json_t* ret = GetOkResponse();

    XElement* scene = OBSGetSceneElement();
    json_object_set_new(ret, "name", json_string_wchar(scene->GetName()));

    XElement* sources = scene->GetElement(TEXT("sources"));
    json_t* scene_items = json_array();
    if(sources != NULL)
    {
        for(UINT i = 0; i < sources->NumElements(); i++)
        {
            XElement* source = sources->GetElementByID(i);
            json_array_append_new(scene_items, getSourceJson(source));
        }
    }

    json_object_set_new(ret, "sources", scene_items);
    OBSLeaveSceneMutex();
    return ret;
}

json_t* OBSAPIMessageHandler::HandleGetSceneList(OBSAPIMessageHandler* handler, json_t* message)
{
    OBSEnterSceneMutex();
    json_t* ret = GetOkResponse();
    XElement* xscenes = OBSGetSceneListElement();

    XElement* currentScene = OBSGetSceneElement();
    json_object_set_new(ret, "current-scene", json_string_wchar(currentScene->GetName()));

    json_t* scenes = json_array();
    if(scenes != NULL)
    {
        int numScenes = xscenes->NumElements();
        for(int i = 0; i < numScenes; i++)
        {
            XElement* scene = xscenes->GetElementByID(i);
            json_array_append_new(scenes, getSceneJson(scene));
        }
    }
    
    json_object_set_new(ret,"scenes", scenes);
    OBSLeaveSceneMutex();

    return ret;
}

json_t* OBSAPIMessageHandler::HandleSetCurrentScene(OBSAPIMessageHandler* handler, json_t* message)
{
    json_t* newScene = json_object_get(message, "scene-name");

    if(newScene != NULL && json_typeof(newScene) == JSON_STRING)
    {
        String name = json_string_value(newScene);

        OBSSetScene(name.Array(), true);
    }
    return GetOkResponse();
}

json_t* OBSAPIMessageHandler::HandleSetSourcesOrder(OBSAPIMessageHandler* handler, json_t* message)
{
    StringList sceneNames;
    json_t* arry = json_object_get(message, "scene-names");
    if(arry != NULL && json_typeof(arry) == JSON_ARRAY)
    {
        for(size_t i = 0; i < json_array_size(arry); i++)
        {
            json_t* sceneName = json_array_get(arry, i);
            String name = json_string_value(sceneName);

            sceneNames.Add(name);
        }
        OBSSetSourceOrder(sceneNames);
    }
    return GetOkResponse();
}

json_t* OBSAPIMessageHandler::HandleSetSourceRender(OBSAPIMessageHandler *handler, json_t* message)
{
    StringList sceneNames;
    json_t* source = json_object_get(message, "source");
    if(source == NULL || json_typeof(source) != JSON_STRING)
    {
        return GetErrorResponse("No source specified");
    }

    json_t* sourceRender = json_object_get(message, "render");
    if(source == NULL || !json_is_boolean(sourceRender))
    {
        return GetErrorResponse("No render specified");
    }

    json_t* ret = GetOkResponse();

    String sourceName = json_string_value(source);
    OBSSetSourceRender(sourceName.Array(), json_typeof(sourceRender) == JSON_TRUE);

    return ret;
}

json_t* OBSAPIMessageHandler::HandleSetSceneItemPositionAndSize(OBSAPIMessageHandler* handler, json_t* message)
{
    return GetOkResponse();
}

json_t* OBSAPIMessageHandler::HandleGetStreamingStatus(OBSAPIMessageHandler* handler, json_t* message)
{
    json_t* ret = GetOkResponse();
    json_object_set_new(ret, "streaming", json_boolean(OBSGetStreaming()));
    json_object_set_new(ret, "preview-only", json_boolean(OBSGetPreviewOnly()));

    return ret;
}

json_t* OBSAPIMessageHandler::HandleStartStopStreaming(OBSAPIMessageHandler* handler, json_t* message)
{
    json_t* previewOnly = json_object_get(message, "preview-only");

    if(previewOnly != NULL && json_typeof(previewOnly) == JSON_TRUE)
    {
        OBSStartStopPreview();
    }
    else
    {
        OBSStartStopStream();
    }
    return GetOkResponse();
}

json_t* OBSAPIMessageHandler::HandleStartStopRecording(OBSAPIMessageHandler* handler, json_t* message)
{
	OBSStartStopRecording();

    return GetOkResponse();
}

json_t* OBSAPIMessageHandler::HandleToggleMute(OBSAPIMessageHandler* handler, json_t* message)
{
    json_t* channel = json_object_get(message, "channel");

    if(channel != NULL && json_typeof(channel) == JSON_STRING)
    {
        const char* channelVal = json_string_value(channel);
        if(stricmp(channelVal, "desktop") == 0)
        {
            OBSToggleDesktopMute();
        }
        else if(stricmp(channelVal, "microphone") == 0)
        {
            OBSToggleMicMute();
        }
        else
        {
            return GetErrorResponse("Invalid channel specified.");
        }
    }
    else
    {
        return GetErrorResponse("Channel not specified.");
    }
    return GetOkResponse();
}

json_t* OBSAPIMessageHandler::HandleGetVolumes(OBSAPIMessageHandler* handler, json_t* message)
{
    json_t* ret = GetOkResponse();

    json_object_set_new(ret, "mic-volume", json_real(OBSGetMicVolume()));
    json_object_set_new(ret, "mic-muted", json_boolean(OBSGetMicMuted()));

    json_object_set_new(ret, "desktop-volume", json_real(OBSGetDesktopVolume()));
    json_object_set_new(ret, "desktop-muted", json_boolean(OBSGetDesktopMuted()));

    return ret;
}

json_t* OBSAPIMessageHandler::HandleSetVolume(OBSAPIMessageHandler* handler, json_t* message)
{
    json_t* channel = json_object_get(message, "channel");
    json_t* volume = json_object_get(message, "volume");
    json_t* finalValue = json_object_get(message, "final");

    if(volume == NULL)
    {
        return GetErrorResponse("Volume not specified.");
    }

    if(!json_is_number(volume))
    {
        return GetErrorResponse("Volume not number.");
    }
    
    float val = (float) json_number_value(volume);
    val = min(1.0f, max(0.0f, val));

    if(finalValue == NULL)
    {
        return GetErrorResponse("Final not specified.");
    }
    
    if(!json_is_boolean(finalValue))
    {
        return GetErrorResponse("Final is not a boolean.");
    }

    if(channel != NULL && json_typeof(channel) == JSON_STRING)
    {
        const char* channelVal = json_string_value(channel);
        if(stricmp(channelVal, "desktop") == 0)
        {
            OBSSetDesktopVolume(val, json_is_true(finalValue));
        }
        else if(stricmp(channelVal, "microphone") == 0)
        {
            OBSSetMicVolume(val, json_is_true(finalValue));
        }
        else
        {
            return GetErrorResponse("Invalid channel specified.");
        }
    }
    else
    {
        return GetErrorResponse("Channel not specified.");
    }
    return GetOkResponse();
}

/* OBS Trigger Handler */
WebSocketOBSTriggerHandler::WebSocketOBSTriggerHandler()
{
    updateQueueMutex = OSCreateMutex();
}

WebSocketOBSTriggerHandler::~WebSocketOBSTriggerHandler()
{
    OSCloseMutex(updateQueueMutex);
}

/* Declare OBS Event Handlers */
extern "C" __declspec(dllexport) void OnStartStream();
extern "C" __declspec(dllexport) void OnStopStream();
extern "C" __declspec(dllexport) void OnStreamStatus(bool, bool, UINT, double, UINT, UINT, UINT, UINT);
extern "C" __declspec(dllexport) void OnSceneSwitch(CTSTR);
extern "C" __declspec(dllexport) void OnScenesChanged();
extern "C" __declspec(dllexport) void OnSourceOrderChanged();
extern "C" __declspec(dllexport) void OnSourceChanged(CTSTR sourceName, XElement* source);
extern "C" __declspec(dllexport) void OnSourcesAddedOrRemoved();
extern "C" __declspec(dllexport) void OnMicVolumeChanged(float level, bool muted, bool finalValue);
extern "C" __declspec(dllexport) void OnDesktopVolumeChanged(float level, bool muted, bool finalValue);

void OnStartStream()
{
    if(triggerHandler)
        triggerHandler->StreamStarting(OBSGetPreviewOnly());
}

void WebSocketOBSTriggerHandler::StreamStarting(bool previewOnly)
{
    json_t* update = json_object();
    json_object_set_new(update, "update-type", json_string("StreamStarting"));
    json_object_set_new(update, "preview-only", json_boolean(previewOnly));

    OSEnterMutex(this->updateQueueMutex);
    this->updates.Add(update);
    OSLeaveMutex(this->updateQueueMutex);
}

void OnStopStream()
{
    if(triggerHandler)
        triggerHandler->StreamStopping(OBSGetPreviewOnly());
}

void WebSocketOBSTriggerHandler::StreamStopping(bool previewOnly)
{
    json_t* update = json_object();
    json_object_set_new(update, "update-type", json_string("StreamStopping"));
    json_object_set_new(update, "preview-only", json_boolean(previewOnly));

    OSEnterMutex(this->updateQueueMutex);
    this->updates.Add(update);
    OSLeaveMutex(this->updateQueueMutex);
}

void OnStreamStatus(bool streaming, bool previewOnly, 
                  UINT bytesPerSec, double strain, 
                  UINT totalStreamtime, UINT numTotalFrames,
                  UINT numDroppedFrames, UINT fps)
{
    if(triggerHandler)
        triggerHandler->StreamStatus(streaming, previewOnly, bytesPerSec, strain,
                                     totalStreamtime, numTotalFrames, numDroppedFrames, fps);
}

void WebSocketOBSTriggerHandler::StreamStatus(bool streaming, bool previewOnly, 
                  UINT bytesPerSec, double strain, 
                  UINT totalStreamtime, UINT numTotalFrames,
                  UINT numDroppedFrames, UINT fps)
{
    json_t* update = json_object();
    json_object_set_new(update, "update-type", json_string("StreamStatus"));
    json_object_set_new(update, "streaming", json_boolean(streaming));
    json_object_set_new(update, "preview-only", json_boolean(previewOnly));
    json_object_set_new(update, "bytes-per-sec", json_integer(bytesPerSec));
    json_object_set_new(update, "strain", json_real(strain));
    json_object_set_new(update, "total-stream-time", json_integer(totalStreamtime));
    json_object_set_new(update, "num-total-frames", json_integer(numTotalFrames));
    json_object_set_new(update, "num-dropped-frames", json_integer(numDroppedFrames));
    json_object_set_new(update, "fps", json_integer(fps));

    OSEnterMutex(this->updateQueueMutex);
    this->updates.Add(update);
    OSLeaveMutex(this->updateQueueMutex);
}

void OnSceneSwitch(CTSTR scene)
{
    if(triggerHandler)
        triggerHandler->ScenesSwitching(scene);
}

void WebSocketOBSTriggerHandler::ScenesSwitching(CTSTR scene)
{
    json_t* update = json_object();
    json_object_set_new(update, "update-type", json_string("SwitchScenes"));
    json_object_set_new(update, "scene-name", json_string_wchar(scene));

    OSEnterMutex(this->updateQueueMutex);
    this->updates.Add(update);
    OSLeaveMutex(this->updateQueueMutex);
}

void OnScenesChanged()
{
    if(triggerHandler)
        triggerHandler->ScenesChanged();
}

void WebSocketOBSTriggerHandler::ScenesChanged()
{
    json_t* update = json_object();
    json_object_set_new(update, "update-type", json_string("ScenesChanged"));
    
    OSEnterMutex(this->updateQueueMutex);
    this->updates.Add(update);
    OSLeaveMutex(this->updateQueueMutex);
}

void OnSourceOrderChanged()
{
    if(triggerHandler)
        triggerHandler->SourceOrderChanged();
}

void WebSocketOBSTriggerHandler::SourceOrderChanged()
{
    json_t* update = json_object();
    json_object_set_new(update, "update-type", json_string("SourceOrderChanged"));

    XElement* xsources = OBSGetSceneElement()->GetElement(TEXT("sources"));
    
    json_t* sources = json_array();
    for(UINT i = 0; i < xsources->NumElements(); i++)
    {
        XElement* source = xsources->GetElementByID(i);

        json_array_append_new(sources, json_string_wchar(source->GetName()));
    }

    json_object_set_new(update, "sources", sources);

    OSEnterMutex(this->updateQueueMutex);
    this->updates.Add(update);
    OSLeaveMutex(this->updateQueueMutex);
}

void OnSourcesAddedOrRemoved()
{
    if(triggerHandler)
        triggerHandler->SourcesAddedOrRemoved();
}

void WebSocketOBSTriggerHandler::SourcesAddedOrRemoved()
{
    json_t* update = json_object();
    json_object_set_new(update, "update-type", json_string("RepopulateSources"));

    XElement* xsources = OBSGetSceneElement()->GetElement(TEXT("sources"));
    
    json_t* sources = json_array();
    for(UINT i = 0; i < xsources->NumElements(); i++)
    {
        XElement* source = xsources->GetElementByID(i);

        json_array_append_new(sources, getSourceJson(source));
    }

    json_object_set_new(update, "sources", sources);

    OSEnterMutex(this->updateQueueMutex);
    this->updates.Add(update);
    OSLeaveMutex(this->updateQueueMutex);
}

void OnSourceChanged(CTSTR sourceName, XElement* source)
{
    if(triggerHandler)
        triggerHandler->SourceChanged(sourceName, source);
}

void WebSocketOBSTriggerHandler::SourceChanged(CTSTR sourceName, XElement* source)
{
    json_t* update = json_object();
    json_object_set_new(update, "update-type", json_string("SourceChanged"));

    XElement* xsources = OBSGetSceneElement()->GetElement(TEXT("sources"));
    
    json_object_set_new(update, "source-name", json_string_wchar(sourceName));
    
    json_object_set_new(update, "source", getSourceJson(source));

    OSEnterMutex(this->updateQueueMutex);
    this->updates.Add(update);
    OSLeaveMutex(this->updateQueueMutex);
}

void OnMicVolumeChanged(float level, bool muted, bool finalValue)
{
    if(triggerHandler)
        triggerHandler->MicVolumeChanged(level, muted, finalValue);
}

void WebSocketOBSTriggerHandler::MicVolumeChanged(float level, bool muted, bool finalValue)
{
    json_t* update = json_object();
    json_object_set_new(update, "update-type", json_string("VolumeChanged"));
    
    json_object_set_new(update, "channel", json_string("microphone"));
    json_object_set_new(update, "volume", json_real(level));
    json_object_set_new(update, "muted", json_boolean(muted));
    json_object_set_new(update, "finalValue", json_boolean(finalValue));

    OSEnterMutex(this->updateQueueMutex);
    this->updates.Add(update);
    OSLeaveMutex(this->updateQueueMutex);
} 

void OnDesktopVolumeChanged(float level, bool muted, bool finalValue)
{
    if(triggerHandler)
        triggerHandler->DesktopVolumeChanged(level, muted, finalValue);
}

void WebSocketOBSTriggerHandler::DesktopVolumeChanged(float level, bool muted, bool finalValue)
{
    json_t* update = json_object();
    json_object_set_new(update, "update-type", json_string("VolumeChanged"));
    
    json_object_set_new(update, "channel", json_string("desktop"));
    json_object_set_new(update, "volume", json_real(level));
    json_object_set_new(update, "muted", json_boolean(muted));
    json_object_set_new(update, "finalValue", json_boolean(finalValue));

    OSEnterMutex(this->updateQueueMutex);
    this->updates.Add(update);
    OSLeaveMutex(this->updateQueueMutex);
} 

json_t* WebSocketOBSTriggerHandler::popUpdate()
{
    OSEnterMutex(this->updateQueueMutex);
    json_t* ret = NULL;
    if(this->updates.Num() > 0)
    {
        ret = this->updates.GetElement(0);
        this->updates.Remove(0);
    }
    OSLeaveMutex(this->updateQueueMutex);

    return ret;
}