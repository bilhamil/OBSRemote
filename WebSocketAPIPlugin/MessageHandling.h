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

#pragma once

#include "WebSocketMain.h"
#include "jansson.h"
#include <deque>
#include <hash_map>
#include <string>

#define REQ_GET_CURRENT_SCENE "GetCurrentScene"
#define REQ_GET_SCENE_LIST "GetSceneList"
#define REQ_SET_CURRENT_SCENE "SetCurrentScene"

#define REQ_SET_SOURCES_ORDER "SetSourceOrder"
#define REQ_SET_SOURCE_RENDER "SetSourceRender"
#define REQ_SET_SCENEITEM_POSITION_AND_SIZE "SetSceneItemPositionAndSize"

#define REQ_GET_STREAMING_STATUS "GetStreamingStatus"
#define REQ_STARTSTOP_STREAMING    "StartStopStreaming"
#define REQ_GET_VOLUMES    "GetVolumes"
#define REQ_SET_VOLUMES "SetVolumes"

struct OBSAPIMessageHandler;

typedef json_t*(*MessageFunction)(OBSAPIMessageHandler*, json_t*);

struct eqstr
{
  bool operator()(const char* s1, const char* s2) const
  {
    return strcmp(s1, s2) == 0;
  }
};

struct OBSAPIMessageHandler
{
    /*Message ID to Function Map*/
    stdext::hash_map<std::string, MessageFunction> messageMap; 
    bool mapInitialized;
    void initializeMessageMap();

    /* Message Handlers */
    static json_t* HandleGetCurrentScene(OBSAPIMessageHandler* handler, json_t* message);
    static json_t* HandleGetSceneList(OBSAPIMessageHandler* handler, json_t* message);
    static json_t* HandleSetCurrentScene(OBSAPIMessageHandler* handler, json_t* message);
    static json_t* HandleSetSourcesOrder(OBSAPIMessageHandler* handler, json_t* message);
    static json_t* HandleSetSourceRender(OBSAPIMessageHandler* handler, json_t* message);
    static json_t* HandleSetSceneItemPositionAndSize(OBSAPIMessageHandler* handler, json_t* message);
    static json_t* HandleGetStreamingStatus(OBSAPIMessageHandler* handler, json_t* message);
    static json_t* HandleStartStopStreaming(OBSAPIMessageHandler* handler, json_t* message);
    static json_t* HandleGetVolumes(OBSAPIMessageHandler* handler, json_t* message);
    static json_t* HandleSetVolumes(OBSAPIMessageHandler* handler, json_t* message);

    struct libwebsocket *wsi;
    
    std::deque<json_t *> messagesToSend;

    OBSAPIMessageHandler(struct libwebsocket *ws);
    
    bool HandleReceivedMessage(void *in, size_t len);
};

class WebSocketOBSTriggerHandler : public OBSTriggerHandler
{
    HANDLE updateQueueMutex;
    List<json_t*> updates;
public:
    WebSocketOBSTriggerHandler();
    ~WebSocketOBSTriggerHandler();
    virtual void StreamStarting(bool previewOnly);
    virtual void StreamStopping(bool previewOnly);

    virtual void StreamStatus(bool streaming, bool previewOnly = false, 
                              UINT bytesPerSec = 0, double strain = 0, 
                              UINT totalStreamtime = 0, UINT numTotalFrames = 0,
                              UINT numDroppedFrames = 0, UINT fps = 0);
    
    virtual void ScenesSwitching(CTSTR scene);
    virtual void ScenesChanged();
    virtual void SourceOrderChanged();
    virtual void SourcesAddedOrRemoved();
    virtual void SourceChanged(CTSTR sourceName, XElement* source);

    json_t* popUpdate();
};