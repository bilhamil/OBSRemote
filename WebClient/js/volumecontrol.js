$(function() {
	$("#MicMuteButton").on("click", ToggleMicMute);
	$("#DesktopMuteButton").on("click", ToggleDesktopMute);
	$("#MicVolume").on("mousedown.volume", StartAdjustingMicVolume);
	$("#DesktopVolume").on("mousedown.volume", StartAdjustingDesktopVolume);
});

function ToggleMicMute()
{
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "ToggleMute";
	myJSONRequest["channel"] = "microphone";
		
	sendMessage(myJSONRequest);
}

function ToggleDesktopMute()
{
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "ToggleMute";
	myJSONRequest["channel"] = "desktop";
		
	sendMessage(myJSONRequest);
}

function requestVolumes()
{
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "GetVolumes";
		
	sendMessage(myJSONRequest, receiveVolumes);
}

function receiveVolumes(resp)
{
	var status = resp["status"];
	
	if(status === "ok")
	{
		SetMicrophoneMeterVolume(resp["mic-volume"]);
		SetDesktopMeterVolume(resp["desktop-volume"]);
		
		
		$("#MicMuteButton").attr("class", (resp["mic-muted"])?"muted":"");
		$("#DesktopMuteButton").attr("class", (resp["desktop-muted"])?"muted":"");
		
	}
}

function onVolumeChanged(update)
{
	if(update["channel"] === "desktop")
	{
		SetDesktopMeterVolume(update["volume"]);
		$("#DesktopMuteButton").attr("class", (update["muted"])?"muted":"");
	}
	else if(update["channel"] === "microphone")
	{
		SetMicrophoneMeterVolume(update["volume"]);
		$("#MicMuteButton").attr("class", (update["muted"])?"muted":"");
	}
}

function SetMicrophoneMeterVolume(level)
{
	var width = $("#MicVolume").width();
	$("#MicVolume div.VolumeRed").width(Math.floor(width * level));
}

function SetDesktopMeterVolume(level)
{
	var width = $("#DesktopVolume").width();
	$("#DesktopVolume div.VolumeRed").width(Math.floor(width * level));
}

function sendVolumeAdjustMessage(volume, channel, fin)
{
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "SetVolume";
	myJSONRequest["channel"] = channel;
	myJSONRequest["volume"] = volume;
	myJSONRequest["final"] = fin;
	
	sendMessage(myJSONRequest);
}

/* interactive code */
function getControlLevel(selector, event)
{
	var control = $(selector);
	var left = control.offset().left;
	var posX = event.pageX - left;
	
	if(posX < 0.0)
		return 0.0;
	
	posX = posX / control.width();
	
	if(posX > 1.0)
		return 1.0;
	
	return posX;
}

var lastVolLevel = -1;

/* Adjusting mic volume code */
function StartAdjustingMicVolume(event)
{
	$(window).on("mousemove.volume", AdjustMicVolume);
	$(window).on("mouseup.volume", StopAdjustMicVolume);
	
	var level = getControlLevel("#MicVolume", event);
	lastVolLevel = level;
	
	sendVolumeAdjustMessage(level, "microphone", false); 	
}

function AdjustMicVolume(event)
{
	var level = getControlLevel("#MicVolume", event);
	if(lastVolLevel != level)
	{
		lastVolLevel = level;
		sendVolumeAdjustMessage(level, "microphone", false);	
	}
}

function StopAdjustMicVolume(event)
{
	$(window).off("mousemove.volume");
	$(window).off("mouseup.volume");
	
	var level = getControlLevel("#MicVolume", event);
	
	lastVolLevel = -1;
	sendVolumeAdjustMessage(level, "microphone", true);	
}

/* Adjusting desktop volume code */
function StartAdjustingDesktopVolume(event)
{
	$(window).on("mousemove.volume", AdjustDesktopVolume);
	$(window).on("mouseup.volume", StopAdjustDesktopVolume);
	
	var level = getControlLevel("#DesktopVolume", event);
	lastVolLevel = level;
	
	sendVolumeAdjustMessage(level, "desktop", false); 	
}

function AdjustDesktopVolume(event)
{
	var level = getControlLevel("#DesktopVolume", event);
	if(lastVolLevel != level)
	{
		lastVolLevel = level;
		sendVolumeAdjustMessage(level, "desktop", false);	
	}
}

function StopAdjustDesktopVolume(event)
{
	$(window).off("mousemove.volume");
	$(window).off("mouseup.volume");
	
	var level = getControlLevel("#DesktopVolume", event);
	
	lastVolLevel = -1;
	sendVolumeAdjustMessage(level, "desktop", true);	
}
