var socket_obsapi;
var websocketConnected = false;
var reconnectIntervalId = null;
var currentMessageCounter = 1;
var requestCallbacks = {};
var supressWebsocketReconnect = false;

var connectingHost = "";

function getOBSHost()
{
	return localStorage["obs-host"];
}

function setOBSHost(host)
{
	localStorage["obs-host"] = host;
}

function connectWebSocket(host)
{
	
	connectingHost = host;
	
	var url = "ws://" + connectingHost + ":4444";
	
	console.log("trying to connect to: " + url);
	if (typeof MozWebSocket != "undefined") 
	{
		socket_obsapi = new MozWebSocket(url, "obsapi");
	} 
	else 
	{
		socket_obsapi = new WebSocket(url, "obsapi");
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
	reconnectIntervalId = null;
	connectWebSocket(getOBSHost());
}

function _onWebSocketConnected()
{
	websocketConnected = true;
	supressWebsocketReconnect = false;
	
	/* store successfully connected host for future */
	setOBSHost(connectingHost);
	
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

function _onWebSocketError(err)
{
	console.log("websocket error");
	socket_obsapi.close();
}

function gracefulWebsocketClose()
{
	supressWebsocketReconnect = true;
	
	if(socket_obsapi)
	{
		socket_obsapi.onopen = null;
		socket_obsapi.onmessage = null;
		socket_obsapi.onerror = null;
		socket_obsapi.onclose = null;
		
		socket_obsapi.close();
	}
	
	if(reconnectIntervalId != null)
	{
		clearTimeout(reconnectIntervalId);
		reconnectIntervalId = null;
	}
	
	_onWebSocketClose("Closed gracefully.");
}

function _onWebSocketClose(err)
{
	console.log("websocket close");
	if(reconnectIntervalId == null && !supressWebsocketReconnect)
	{
		reconnectIntervalId = setTimeout(reconnectWebSocket, 4000);	
	}
	
	websocketConnected = false;
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