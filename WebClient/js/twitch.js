
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
});

function userResponse(error, user)
{
	console.log("Got user: " + user);
}

function channelResponse(error, channel)
{
	if(!error)
	{
		console.log("Got channel: " + channel["name"]);
		initializeOnGetUser(channel["name"]);
		
		$("#twitch-connect").css("display", "none");
		$(".config").css("display", "block");
		
		$("#streamstatus").val(channel.status);
		if(channel.game)
		{
			$("#streamstatus").val(channel.game);	
		}
	}
}

