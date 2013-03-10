var currentlyStreaming = false;
var currentlyPreviewing = false;
var totalSecondsStreaming = 0;

/*auth variables*/
var authSalt = "";
var authChallenge = "";

$(function() {
	/* select nothing but inputs */
	$('body *').not(':has(input)').not('input').disableSelection();
	
	$("#button1").on("click", startStreaming);
	$("#button2").on("click", startPreview);
	
	/* initialize auth dialog */
	$( "#auth-dialog-form" ).dialog({
      autoOpen: false,
      height: 220,
      width: 450,
      modal: true,
      closeOnEscape: false,
      buttons: {
        "Ok": Authenticate,
      },
      beforeclose: function(){ return false; },
      close: function() {
        }
    });
    
    /* initialize auth dialog */
	$( "#oldversion-dialog-form" ).dialog({
      autoOpen: false,
      height: 220,
      width: 530,
      modal: true,
      closeOnEscape: false,
      buttons: {
        "Use Old Client": NavigateToOldClient,
        "Get Latest Plugin": NavigateToLatestPlugin
      },
      beforeclose: function(){ return false; },
      close: function() {
        }
    });
    
    $("#authfield").keydown(function(event) {
    	if(event.keyCode == 13)
    	{
    		event.preventDefault();
    		Authenticate();
    		return false;
    	}
    });
});

function pad(num, size) {
    var s = num+"";
    while (s.length < size) s = "0" + s;
    return s;
}

function onWebSocketConnected()
{
	console.log("websocket connected");
	$("#Lights").attr("class", "");
	
	connectionPopupConnectionStatusChange(true);
	
	$("#Lights").attr("title", "Manage Connection:\nConnected to OBS");
	
	checkVersion();
}

function checkVersion()
{
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "GetVersion";
	
	sendMessage(myJSONRequest, versionResponse);
}

var pluginVersion = 0;

function versionResponse(resp)
{
	pluginVersion = resp["version"];
	
	console.log("plugin version: v" + pluginVersion );
	
	connectionPopupPluginVersionUpdate(pluginVersion);
	
	if(pluginVersion >= requiredPluginVersion)
	{
		checkAuthentication();
	}
	else
	{
		oldVersionFound(pluginVersion);
	}
}

function oldVersionFound()
{
	console.log("found old plugin version" );
	
	$("#version-notif").text("You are using an old version of the plugin (version " + pluginVersion.toFixed(2) + ")");

	$("#oldversion-dialog-form").dialog("open");
}

function NavigateToOldClient()
{
	window.location.href = oldPluginUrls[pluginVersion.toFixed(2)];
}

function NavigateToLatestPlugin()
{
	window.location.href = "http://www.obsremote.com/download.html";
}

function checkAuthentication()
{
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "GetAuthRequired";
	
	sendMessage(myJSONRequest, authenticationRequiredResponse);
}

function authenticationRequiredResponse(resp)
{
	var authRequired = resp["authRequired"];
	
	if(authRequired)
	{
		authSalt = resp["salt"];
		authChallenge = resp["challenge"];
		startAuth();
	}
	else
	{
		onConnectInitilization();
	}
}

function startAuth()
{
	$( "#auth-dialog-form" ).dialog( "open" );
	$("#authdiagerror").text("");
}

function utf8_encode (string) {
    string = string.replace(/\r\n/g,"\n");
    var utftext = "";

    for (var n = 0; n < string.length; n++) {

        var c = string.charCodeAt(n);

        if (c < 128) {
            utftext += String.fromCharCode(c);
        }
        else if((c > 127) && (c < 2048)) {
            utftext += String.fromCharCode((c >> 6) | 192);
            utftext += String.fromCharCode((c & 63) | 128);
        }
        else {
            utftext += String.fromCharCode((c >> 12) | 224);
            utftext += String.fromCharCode(((c >> 6) & 63) | 128);
            utftext += String.fromCharCode((c & 63) | 128);
        }

    }

    return utftext;
}

function Authenticate()
{
	var pass = $("#authfield").val();
	
	var authHash = CryptoJS.SHA256(utf8_encode(pass) + utf8_encode(authSalt)).toString(CryptoJS.enc.Base64);
		
	var authResp = CryptoJS.SHA256(utf8_encode(authHash) + utf8_encode(authChallenge)).toString(CryptoJS.enc.Base64);
	
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "Authenticate";
	
	myJSONRequest["auth"] = authResp;
	
	sendMessage(myJSONRequest, authenticationResponse);
}

function authenticationResponse(resp)
{
	if(resp["status"] == "ok")
	{
		$( "#auth-dialog-form" ).dialog( "close" );
		$("#authfield").val("");
		onConnectInitilization();
	}
	else
	{
		/* auth failed */
		$("#authdiagerror").text(resp["error"]);
	}
}

function onConnectInitilization()
{
	requestStreamStatus();
	requestScenes();
	requestVolumes();
}

function onWebSocketClose()
{
	console.log("websocket disconnected");
	$("#Lights").attr("class", "disconnected");
	connectionPopupConnectionStatusChange(false);
	$("#Lights").attr("title", "Manage Connection:\nDisconnected from OBS");
	
	$( "#auth-dialog-form" ).dialog( "close" );
	$("#oldversion-dialog-form").dialog("close");
}

function strainToColor(strain)
{
	var green = 255;
	if (strain > 50.0)
	{
		green = Math.floor((((50.0-(strain-50.0))/50.0)*255.0));
	}
	
	var red = strain / 50.0;
	if(red > 1.0)
	{
		red = 1.0;	
	}
	red = red * 255;
	
	var redHex = pad(red.toString(16), 2);
	var greenHex = pad(green.toString(16), 2);
	var blueHex = "00";
	
	return "#"+redHex+greenHex+blueHex;	
}

function onStreamStatus(update)
{
	var newStreaming = update["streaming"];
	
	if(newStreaming && !(currentlyStreaming || currentlyPreviewing))
	{
		onStartStreaming(update);
	}
	else if(!newStreaming && (currentlyStreaming || currentlyPreviewing))
	{
		onStopStreaming(update);
		totalSecondsStreaming = 0;
	}
	
	if(currentlyStreaming || currentlyPreviewing)
	{
		/* update stats output */
		$("#StatsTable").css("visibility", "visible");
		var sec = Math.floor(update["total-stream-time"] / 1000);
		
		totalSecondsStreaming = sec;
		
		$("#TimeRunning").text(Math.floor(sec / 3600)+ ":" + 
							   pad(Math.floor((sec % 3600) / 60), 2) + ":" + 
							   pad((sec % 60), 2));
		
		var droppedFrames = update["num-dropped-frames"];
		var totalFrames = update["num-total-frames"];
		var percentage = Math.floor(droppedFrames / totalFrames * 10000) / 100;
		$("#DroppedFrames").text(droppedFrames + "(" + percentage + "%)");
		
		var fps = update["fps"];
		$("#FPS").text("" + fps);
		
		var bps = update["bytes-per-sec"];
		var kbps = Math.floor(bps * 8 / 1000);
		var strain = update["strain"];
				
		$("#DataRate").text("" + kbps + " kbps").css("color", strainToColor(strain));
		
	}
}

function onStartStreaming(update)
{
	console.log("start stream");
	var previewOnly = update["preview-only"];
	if(!currentlyStreaming)
	{
		
		$("#OnTheAir").attr("class", "On");
		if(previewOnly)
		{
			$("#OnTheAir p:first").text("PREVIEWING");
			currentlyPreviewing = true;
		}
		else
		{
			currentlyStreaming = true;
			$("#OnTheAir p:first").text("ON THE AIR");
			streamConfigStartStreaming();
		}
		$("#button1").css("visibility", "hidden");
		$("#button2 p:first").html("Stop " + ((previewOnly)?"Preview":"Streaming"));
	}
	
}

function onStopStreaming(update)
{
	console.log("stop stream");
	if(currentlyStreaming || currentlyPreviewing)
	{
		currentlyStreaming = false;
		currentlyPreviewing = false;
		$("#OnTheAir").attr("class", null);
		$("#OnTheAir p:first").text("OFF THE AIR");
		$("#button1").css("visibility", "visible");
		$("#button2 p:first").html("Start Preview");
		$("#StatsTable").css("visibility", "hidden");
	}
	
	streamConfigStopStreaming(totalSecondsStreaming);
}

function requestStreamStatus()
{
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "GetStreamingStatus";
	
	sendMessage(myJSONRequest, streamStatusResponse);
}

function streamStatusResponse(resp)
{
	console.log("stream status response");
	var newStreaming = resp["streaming"];
	if(newStreaming != currentlyStreaming)
	{
		if(newStreaming)
		{
			onStartStreaming(resp);
		}
		else
		{
			onStopStreaming(resp);
		}
	}
}

function startStreaming()
{
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "StartStopStreaming";
	
	sendMessage(myJSONRequest);
}

function startPreview()
{
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "StartStopStreaming";
	myJSONRequest["preview-only"] = true;
	
	sendMessage(myJSONRequest);
}

