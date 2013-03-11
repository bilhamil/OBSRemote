
$(function() {
	
	Twitch.init({clientId: 'b8is94zcag2kzmq4kju5kyfb8rq27ow'}, function(error, status) {
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
	    	// user is currently logged in
	    	console.log("Twitch Authenticated!");
	    	
	    	Twitch.api({method: 'user' }, userResponse);
	    	Twitch.api({method: 'channel'}, channelResponse);
	  	}
	});

	
	$('#twitch-connect').click(function() {
	  Twitch.login({scope: [ 'user_read', 'channel_read', 'channel_editor', 'channel_commercial']});
	});
	
	$("#game").click(function() {
   		$(this).select();
	});
	
	$("#streamstatus").click(function() {
   		$(this).select();
	});	
});

function userResponse(error, user)
{
	console.log("Got user: " + user);
}

function channelResponse(error, channel)
{
	if(!error)
	{
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
								var gameThumb = searchresults.games[i].logo.small;
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
		$(".config").css("display", "block");
		
		$("#streamstatus").val(channel.status);
		if(channel.game)
		{
			$("#game").val(channel.game);	
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


