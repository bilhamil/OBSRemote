var socket_obsapi;
var websocketConnected = false;
var reconnectIntervalId = null;
var currentMessageCounter = 1;
var requestCallbacks = {};

$(function() {
	connectWebSocket();
});

function connectWebSocket()
{
	console.log("trying to connect");
	if (typeof MozWebSocket != "undefined") {
	socket_obsapi = new MozWebSocket(get_appropriate_ws_url(),
			   "obsapi");
	} else {
		socket_obsapi = new WebSocket(get_appropriate_ws_url(),
				   "obsapi");
	}
	
	try {
		socket_obsapi.onopen = _onWebSocketConnected;
		socket_obsapi.onmessage = _onWebSocketReceiveMessage;
		socket_obsapi.onerror = _onWebSocketError;
		socket_obsapi.onclose = _onWebSocketClose;
	} catch(exception) {
		alert('<p>Error' + exception);  
	}
}

function reconnectWebSocket()
{
	connectWebSocket();
}

function _onWebSocketConnected()
{
	websocketConnected = true;
	if(reconnectIntervalId != null)
	{
		clearInterval(reconnectIntervalId);
		reconnectIntervalId = null;
	}
	
	/* call the generic onWebSocketConnected function */
	onWebSocketConnected();
}

function _onWebSocketReceiveMessage(msg)
{
	var response = JSON.parse(msg.data);
	if(!response)
	{
		return;
	}
	
	var updateType = response["update-type"]; 
	if(updateType)
	{
		/* this is an update */
		switch(updateType)
		{
			case "StreamStatus":
				onStreamStatus(response);
				break;
			case "StreamStarting":
				onStartStreaming(response);
				break;
			case "StreamStopping":
				onStopStreaming(response);
				break;
			case "SwitchScenes":
				onSceneSwitched(response);
				break;
			case "ScenesChanged":
				onScenesChanged(response);
				break;
			case "SourceOrderChanged":
				onSourceOrderChanged(response);
				break;
			case "SourceChanged":
				onSourceChanged(response);
				break;
			case "RepopulateSources":
				onRepopulateSources(response);
				break;
			case "VolumeChanged":
				onVolumeChanged(response);
		}
	}
	else
	{
		/* else this is a response */
		var id = response["message-id"];
		
		if(response["status"] == "error")
		{
			console.log("Error: " + response["error"]);
		}
		
		var callback = requestCallbacks[id];
		if(callback)
		{
			callback(response);
			requestCallbacks[id] = null;
		}
	}
}

function _onWebSocketError()
{
	console.log("websocket error");
	socket_obsapi.close();
}

function _onWebSocketClose()
{
	if(reconnectIntervalId == null)
	{
		reconnectIntervalId = setInterval(reconnectWebSocket, 10000);
		websocketConnected = false;
	}
	
	onWebSocketClose();
}

function getNextID()
{
	currentMessageCounter++;
	return currentMessageCounter + "";
}

function sendMessage(msg, callback)
{
	if(websocketConnected)
	{
		var id =  getNextID();
		if(!callback)
		{
			requestCallbacks[id] = function(){};
		}
		else
		{
			requestCallbacks[id] = callback;
		}
		msg["message-id"] = id;
		
		var serializedMessage = JSON.stringify(msg);
		socket_obsapi.send(serializedMessage);
	}
}