var service;
var username;
var showstream;
var onlywhenstreaming;
var showchat;


$(function() {
	/* initialize dialog */
	$( "#dialog-form" ).dialog({
      autoOpen: false,
      height: 370,
      width: 500,
      modal: true,
      buttons: {
        "Ok": StreamConfDiagOk,
        Cancel: function() {
          $( this ).dialog( "close" );
        }
      },
      close: function() {
        }
    });
    
    $( "#configuregear" ).on("click", function() {
        $( "#dialog-form" ).dialog( "open" );
      });
    
    
    if(localStorage["service"])
    {
    	service = localStorage["service"];
    	username = localStorage["username"];
    	showstream = localStorage["showstream"] == "true";
    	showchat = localStorage["showchat"] == "true";
    	if(localStorage["onlywhenstreaming"] == undefined)
    	{
    		localStorage["onlywhenstreaming"] = "true";
    	}
    	onlywhenstreaming = localStorage["onlywhenstreaming"] == "true";
    	
    	
    	
    	$("#username").val(username);
    	$("#showstream").attr("checked", showstream);
    	$("#showchat").attr("checked", showchat);
    	$("#onlywhenstreaming").attr("checked", onlywhenstreaming);
    	
    	loadStreamAndOrChat(service, username, showstream, showchat, onlywhenstreaming);
    }
});

function StreamConfDiagOk()
{
	service = "Twitch.tv";
	username = $("#username").val();
	showstream = $("#showstream").is(':checked');
	showchat = $("#showchat").is(':checked');
	onlywhenstreaming = $("#onlywhenstreaming").is(":checked");
	
	console.log("service: " + service + " username: " + username + " showstream: " + showstream + " show chat: " + showchat);
	
	$( this ).dialog( "close" );
	
	loadStreamAndOrChat(service, username, showstream, showchat, onlywhenstreaming);
}

var currentlyShowingStream = false;
var currentlyShowingChat = false;
var currentlyShowingUsername = "";

function loadStreamAndOrChat(service, username, showstream, showchat, onlywhenstreaming)
{
	localStorage["service"] = service;
	localStorage["username"] = username;
	localStorage["showstream"] = (showstream)?"true":"false";
	localStorage["showchat"] = (showchat)?"true":"false";
	localStorage["onlywhenstreaming"] = onlywhenstreaming;
	
	if(service === "Twitch.tv")
	{
		if(showstream && (!currentlyShowingStream || currentlyShowingUsername != username) && (!onlywhenstreaming || currentlyStreaming))
		{
			$("#streambox").empty();
			$(makeTwitchStream(username)).appendTo("#streambox");
			currentlyShowingStream = true;
		}
		else if(!showstream || (onlywhenstreaming && !currentlyStreaming))
		{
			$("#streambox").empty();
			currentlyShowingStream = false;
		}
		
		if(showchat && (!currentlyShowingChat || currentlyShowingUsername != username))
		{
			removeChatDiv();
			showChatDiv();
			if(!getChatCollapsed())
			{
				$(makeTwitchChat(username)).appendTo("#actualChat");
			}
			currentlyShowingChat = true;	
		}
		else if(!showchat)
		{
			removeChatDiv();
			currentlyShowingChat = false;
		}
		
		currentlyShowingUsername = username;
	}
}

var showStreamTimeoutVariable = null;
function streamConfigStartStreaming()
{
	showStreamTimeoutVariable = setTimeout(function() {
		if(onlywhenstreaming && showstream)
		{
			$("#streambox").empty();
			$(makeTwitchStream(username)).appendTo("#streambox");
			currentlyShowingStream = true;
		}
		showStreamTimeoutVariable = null;
	}, 6000);
	
	_gaq.push(['_trackEvent', 'Streaming', 'Start Streaming', username]);
}

function streamConfigStopStreaming(secondsStreamed)
{
	if(onlywhenstreaming)
	{
		$("#streambox").empty();
		currentlyShowingStream = false;
	}
	if(showStreamTimeoutVariable)
	{
		window.clearTimeout(showStreamTimeoutVariable);
		showStreamTimeoutVariable = null;
	}
	
	_gaq.push(['_trackEvent', 'Streaming', 'Stop Streaming', username, secondsStreamed]);
}

/* getters and setters for local storage chat box prefs*/
function setChatCollapsed(collapsed)
{
	localStorage["chatcollapsed"] = collapsed?"true":"false";
}

function getChatCollapsed()
{
	return localStorage["chatcollapsed"] == "true";
}

function setChatWidth(chatwidth)
{
	localStorage["chatwidth"] = "" + chatwidth; 
}

function getChatWidth()
{
	if(localStorage["chatwidth"])
	{
		return parseInt(localStorage["chatwidth"])
	}
	else
	{
		return 440;
	} 
}

/* chat box resize */
function collapseChatBox() {
	var chatbox = $('#chatView');
	var targetWidth;
	
	var showing = false;
	
	if(chatbox.width() > 4)
	{
		targetWidth = 4;
		setChatWidth(chatbox.width());
	}
	else
	{
		targetWidth = getChatWidth();
		showing = true;
	}
	
	setChatCollapsed(!showing);
	
	chatbox.animate(
		{width: targetWidth},
		{
			duration: 200, 
			step: function(currentWidth) {
				$('#streamView').css("right", currentWidth);
			},
			complete: function()
			{
				if(!showing)
				{
					$("#actualChat").empty();
					$('#chatResizeBar').off("mousedown.resizing");
					$("#chathideshow").attr("class", "open");
				}
				else
				{
					$(makeTwitchChat(currentlyShowingUsername)).appendTo("#actualChat");
					$('#chatResizeBar').on("mousedown.resizing", startResizeChat);
					$("#chathideshow").attr("class", "");
				}
			}
		}
	);
}

function showChatDiv()
{
	var html = '<div id="chatView">' + 
					'<div id="chathideshow" title="Show/Hide Chat"> <div></div>' +  
					'</div>' + 
					'<div id="chatResizeBar" class="resizeBarVert">' + 
					'</div>' + 
					'<div id="actualChat">' + 
					'</div>	' + 
				'</div>;';
	$(html).appendTo('#bigOne');
	
	$('#chathideshow').on("click.resizing", collapseChatBox);
	
	var targetWidth = 4;
	if(!getChatCollapsed())
	{
		targetWidth = getChatWidth();
		$('#chatResizeBar').on("mousedown.resizing", startResizeChat);	
	}
	else
	{
		$("#chathideshow").attr("class", "open");	
	}
	
	$('#chatView').width(targetWidth+"px");
	$('#streamView').css("right", targetWidth + "px");
}

function removeChatDiv()
{
	$("#chatView").remove();
	$('#streamView').css("right", "0px");
}

function twitchStreamSuccess(msg)
{
	console.log("message: " + msg);
}

function makeTwitchStream(username)
{
	var player = '<object type="application/x-shockwave-flash" height="100%" width="100%" id="live_embed_player_flash" data="http://www.twitch.tv/widgets/live_embed_player.swf?channel=' + username + '" bgcolor="#777777">'+ 
	'<param name="allowFullScreen" value="true" />' + 
	'<param name="allowScriptAccess" value="always" />' + 
	'<param name="allowNetworking" value="all" />'+ 
	'<param name="wmode" value="transparent" />' +
	'<param name="movie" value="http://www.twitch.tv/widgets/live_embed_player.swf" />' +
	'<param name="flashvars" value="hostname=www.twitch.tv&channel=' + username + '&auto_play=true&start_volume=0&watermark_position=bottom_left" />'+
	'</object>';
	
	return player;
}

function makeTwitchChat(username)
{
	var chat = '<iframe frameborder="0" scrolling="no" id="chat_embed" src="http://twitch.tv/chat/embed?channel=' + username + '&amp;popout_chat=false" ></iframe>';
	return chat;
}
