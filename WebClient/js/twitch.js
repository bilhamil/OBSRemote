var twitchUserName = null;
var currentStatus = null;
var currentGame = null;

var twitchKeys = {
	"http://client.obsremote.com/": "ks67o3uccvz4d0bn9oafusvnud0kyp6",
	"http://test.obsremote.com/": "86ga5ffa6d4xpjtc3kvpbjnkll1sr8o",
	/*put local dev key here*/
	"default": "b8is94zcag2kzmq4kju5kyfb8rq27ow"	
}

function getAPIKey()
{
	return twitchKeys[window.location.href.replace(/#.*$/, '')] || twitchKeys["default"];
}

$(function() {
	
	Twitch.init({clientId: getAPIKey()}, function(error, status) {
		// the sdk is now loaded
		console.log("Twitch API Loaded");
		
		if (error) 
		{
	    	// error encountered while loading
	    	console.log(error);
	  	}
	  	// the sdk is now loaded
	  	if (status.authenticated) 
	  	{
	    	onLogon();
	  	}
	});

	Twitch.events.addListener('auth.login', onLogon);
	Twitch.events.addListener('auth.logout', onLogout);
	
	$('#twitch-connect').click(function() {
	  	Twitch.login({scope: [ 'user_read', 'channel_read', 'channel_editor', 'channel_commercial']});
	});
	
	$("#logouttwitch").click(function() {
		Twitch.logout(function(error){
			if(!error)
			{
				onLogout();
			}
		});
	});
	
	var checkForChange = function(event) {
		if($("#game").val() != currentGame || $("#streamstatus").val() != currentStatus)
		{
			$("#updatetwitch").css("display","block");
		}
		else
		{
			$("#updatetwitch").css("display","none");
		}
	}
	
	$("input.config").on("change.twitchupdate", checkForChange);
	
	$("input.config").on("keyup.twitchupdate", checkForChange);
	
	$("#updatetwitch").click(function() {
		if(twitchUserName)
		{
			var game = $("#game").val().replace(/^\s+|\s+$/g, '');
			var status = $("#streamstatus").val();
			var myparams = {channel:{}};
			myparams.channel.status = status;
			myparams.channel.game = null;
			if(game && game != "")
			{
				myparams.channel.game = game;
			}
			
			Twitch.api({method:"channels/" + twitchUserName, params:myparams, verb:"PUT"}, function(error, channel) {
				if(!error)
				{
					$("#streamstatus").val(channel.status);
					currentStatus = channel.status;
					
					currentGame = null;
					if(channel.game)
					{
						$("#game").val(channel.game);
						currentGame = channel.game;	
					}
					
					$("#updatetwitch").css("display","none");
				}
			});
		}
	});
	
	$("#game").click(function() {
   		$(this).select();
	});
	
	$("#streamstatus").click(function() {
   		$(this).select();
	});	
});

function onLogon()
{
	// user is currently logged in
	console.log("Twitch Authenticated!");
	
	Twitch.api({method: 'channel'}, channelResponse);
}

function onLogout()
{
	$("#twitch-connect").css("display", "block");
				
	$(".config").not("#updatetwitch").css("display", "none");
	
	twitchUserName = null;
}

function userResponse(error, user)
{
	console.log("Got user: " + user);
}

function channelResponse(error, channel)
{
	if(!error)
	{
		twitchUserName = channel.name;
		
		/* set username to channel name */
		updateUserName(channel.name);
		
		$('#game').autocomplete({source: function(request, response) {
			if(request.term)
			{
				Twitch.api({method:"/search/games", params:{query:request.term, type:"suggest"}},
					function (error, searchresults) 
					{
						if(error)
						{
							response([]);
						}
						else
						{
							var results = [];
							for(i = 0; i < searchresults.games.length; i++)
							{
								var gameTitle = searchresults.games[i].name;
								var gameThumb = searchresults.games[i].box.small;
								results.push({label:gameTitle,
											  icon:gameThumb,
											  value:gameTitle});
							}
							response(results);
						}
					});
			}
			else
			{
				response([]);
			}
		}})
		.data( "ui-autocomplete" )._renderItem = function( ul, item ) {
	    	  return $( "<li>" )
	        		.append( "<a>" + "<img src=\""+item.icon+"\"/> <div>" + item.label + "</div></a>" )
	        		.appendTo( ul );
	        };
		
		console.log("Got channel: " + channel["name"]);
		
		$("#twitch-connect").css("display", "none");
		$("#logouttwitch").text("Logout " + channel.display_name);
		
		$(".config").not("#updatetwitch").css("display", "block");
		
		
		$("#streamstatus").val(channel.status);
		currentStatus = channel.status;
		
		currentGame = null;
		if(channel.game)
		{
			$("#game").val(channel.game);
			currentGame = channel.game;	
		}
	}
}

function twitchUpdateViewerCount(username)
{
	Twitch.api({method: 'streams/' + username }, function(error, stream) {
		if(error)
		{
			console.log("Error Getting Viewer Count: " + error);
			return;
		}
		else if(stream.stream.viewers)
		{
			$("#viewercount").text("" + stream.stream.viewers);
		}
	});
}


