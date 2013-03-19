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
#include <stdio.h>

#include "WebSocketMain.h"
#include "MessageHandling.h"
#include "libwebsockets.h"

extern "C" __declspec(dllexport) bool LoadPlugin();
extern "C" __declspec(dllexport) void UnloadPlugin();
extern "C" __declspec(dllexport) CTSTR GetPluginName();
extern "C" __declspec(dllexport) CTSTR GetPluginDescription();

HANDLE WebSocketThread;

const int port = 4444;
bool running;
WebSocketOBSTriggerHandler* triggerHandler = NULL;

enum demo_protocols {
    /* always first */
    PROTOCOL_HTTP = 0,
    PROTOCOL_OBS_API
};

#define LOCAL_RESOURCE_PATH INSTALL_DATADIR"/plugins/WebSocketAPIPlugin"

/* http protocol handling */
int callback_http(struct libwebsocket_context *context,
        struct libwebsocket *wsi,
        enum libwebsocket_callback_reasons reason, void *user,
                               void *in, size_t len)
{
    wchar_t client_ip[128];

    switch (reason) 
    {
    
        case LWS_CALLBACK_HTTP:
        {
            fprintf(stderr, "serving HTTP URI %s\n", (char *)in);

            char *requested_uri = (char *) in;
            printf("requested URI: %s\n", requested_uri);
           
            if (strcmp(requested_uri, "/") == 0) 
            {
               requested_uri = "/index.html"; 
            }
            
            char *resource_path;
                           
            // allocate enough memory for the resource path
            size_t pathSize = strlen(LOCAL_RESOURCE_PATH) + strlen(requested_uri) + 1;
            resource_path = (char *)malloc(pathSize);
           
            // join current working direcotry to the resource path
            _snprintf(resource_path,pathSize,"%s%s", LOCAL_RESOURCE_PATH, requested_uri);
            
            char *extension = strrchr(resource_path, '.');
            char *mime;
           
            // choose mime type based on the file extension
            if (extension == NULL) {
                mime = "text/plain";
            } else if (strcmp(extension, ".png") == 0) {
                mime = "image/png";
            } else if (strcmp(extension, ".html") == 0) {
                mime = "text/html";
            } else if (strcmp(extension, ".js") == 0) {
                mime = "text/javascript";
            } else if (strcmp(extension, ".css") == 0) {
                mime = "text/css";
            } else if (strcmp(extension, ".ico") == 0) {
                mime = "image/x-icon";
            } else if (strcmp(extension, ".ttf") == 0 || strcmp(extension, ".eot") == 0) {
                mime = "font/opentype";
            } else if (strcmp(extension, ".jpg") == 0) {
                mime = "image/jpg";
            } else if (strcmp(extension, ".gif") == 0) {
                mime = "image/gif";
            } else {
                mime = "text/plain";
            }

            libwebsockets_serve_http_file(wsi, resource_path, mime);
            free(resource_path);
            break;
        }
                

        /*
         * filtering based on client ip-address
         */

        case LWS_CALLBACK_FILTER_NETWORK_CONNECTION:
                        
            break;

        default:
            break;
    }

    return 0;
}

int
callback_obsapi(struct libwebsocket_context *context,
            struct libwebsocket *wsi,
            enum libwebsocket_callback_reasons reason,
                           void *user, void *in, size_t len)
{
    int n;
    OBSAPIMessageHandler **userp = (OBSAPIMessageHandler**)user;
    OBSAPIMessageHandler *messageHandler = *(userp);

    switch (reason) {

    case LWS_CALLBACK_ESTABLISHED:
        fprintf(stderr, "callback_obsapi: "
                         "LWS_CALLBACK_ESTABLISHED\n");

        /* initiate handler */
        *userp = new OBSAPIMessageHandler(wsi);

        break;

    case LWS_CALLBACK_SERVER_WRITEABLE:
        if(!messageHandler->messagesToSend.empty())
        {
            json_t *message = messageHandler->messagesToSend.front();
            messageHandler->messagesToSend.pop_front();

            char *messageText = json_dumps(message, 0);
            
            if(messageText != NULL)
            {
                int sendLength = strlen(messageText);
                
                /* copy json text into memory buffer properly framed for libwebsockets */
                char* messageBuf = (char*) malloc(LWS_SEND_BUFFER_PRE_PADDING + sendLength + LWS_SEND_BUFFER_POST_PADDING);
                memcpy(messageBuf + LWS_SEND_BUFFER_PRE_PADDING, messageText, sendLength);
                

                n = libwebsocket_write(wsi, (unsigned char *)
                   messageBuf + LWS_SEND_BUFFER_PRE_PADDING,
                   sendLength, LWS_WRITE_TEXT);
                if (n < 0) {
                    fprintf(stderr, "ERROR writing to socket");
                }
                free(messageBuf);
            }
            free((void *)messageText);
            json_decref(message);

            libwebsocket_callback_on_writable(context, wsi);
        }
        break;

    case LWS_CALLBACK_BROADCAST:
        /* should get called with update messages */
		if(!getRemoteConfig()->useAuth || messageHandler->authenticated)
        {
            n = libwebsocket_write(wsi,(unsigned char *) in, len, LWS_WRITE_TEXT);
            if (n < 0)
                fprintf(stderr, "update write failed\n");
        }
        break;

    case LWS_CALLBACK_RECEIVE:
        if(messageHandler->HandleReceivedMessage(in, len))
        {
            libwebsocket_callback_on_writable(context, wsi);
        }
        break;
    
    case LWS_CALLBACK_FILTER_PROTOCOL_CONNECTION:
        /* you could return non-zero here and kill the connection */
        break;

    case LWS_CALLBACK_CLOSED:
        /* free user data */
        delete(*userp);
        break;
    default:
        break;
    }

    return 0;
}

struct libwebsocket_protocols protocols[] = {
    /* first protocol must always be HTTP handler */

    {
        "http-only",        /* name */
        callback_http,        /* callback */
        0            /* per_session_data_size */
    },
    {
        "obsapi",
        callback_obsapi,
        sizeof(OBSAPIMessageHandler*)
    },
    {
        NULL, NULL, 0        /* End of list */
    }
};



DWORD STDCALL MainWebSocketThread(LPVOID lpUnused)
{
    struct libwebsocket_context *context;
    const char *interface = NULL;
    unsigned char buf[LWS_SEND_BUFFER_PRE_PADDING + 1024 +
                          LWS_SEND_BUFFER_POST_PADDING];

    context = libwebsocket_create_context(port, interface, protocols,
                libwebsocket_internal_extensions,
                NULL, NULL, NULL, -1, -1, 0, NULL);
    
    unsigned int oldus = 0;
    int n = 0;

    buf[LWS_SEND_BUFFER_PRE_PADDING] = 'x';

    //json_set_alloc_funcs(malloc, free);
    while (n >= 0 && running) 
    {
        struct timeval tv;

        gettimeofday(&tv, NULL);

        /*
         * Send out all recieved update broadcasts.
         */

        json_t* message = NULL;
        while((message = triggerHandler->popUpdate()) != NULL)
        {
            char *messageText = json_dumps(message, 0);
            
            if(messageText != NULL)
            {
                int sendLength = strlen(messageText);
                
                /* copy json text into memory buffer properly framed for libwebsockets */
                char* messageBuf = (char*) malloc(LWS_SEND_BUFFER_PRE_PADDING + sendLength + LWS_SEND_BUFFER_POST_PADDING);
                memcpy(messageBuf + LWS_SEND_BUFFER_PRE_PADDING, messageText, sendLength);
                

                n = libwebsockets_broadcast(&protocols[PROTOCOL_OBS_API],
                    (unsigned char *) messageBuf + LWS_SEND_BUFFER_PRE_PADDING, sendLength);
                if (n < 0) {
                    fprintf(stderr, "ERROR writing to socket");
                }
                free(messageBuf);
                free((void *)messageText);
            }
            json_decref(message);
        }

        /*
         * This example server does not fork or create a thread for
         * websocket service, it all runs in this single loop.  So,
         * we have to give the websockets an opportunity to service
         * "manually".
         *
         * If no socket is needing service, the call below returns
         * immediately and quickly.  Negative return means we are
         * in process of closing
         */
        n = libwebsocket_service(context, 50);
    }
    
    libwebsocket_context_destroy(context);

    return 0;
}

bool LoadPlugin()
{
    running = true;
    DWORD dummy; 

    /* initialize and register trigger handler */
    triggerHandler = new WebSocketOBSTriggerHandler();
    
    WebSocketThread = CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE)MainWebSocketThread, NULL, 0, &dummy);
    return true;
}

void UnloadPlugin()
{
    running = false;
    DWORD exitStatus;
    OSWaitForThread(WebSocketThread, &exitStatus);

    delete triggerHandler;
    triggerHandler = NULL;
}



CTSTR GetPluginName()
{
    return TEXT(OBS_REMOTE_FULLNAME);
}

CTSTR GetPluginDescription()
{
    return TEXT("An http/websocket interface enabling remote control through web clients. Accessible on port# 4444");
}