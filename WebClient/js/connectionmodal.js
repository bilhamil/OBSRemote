$(function() {
	
		
	$( "#Lights" ).on("click", function() {
        showConnectToHostDialog();
      });
      
    $("#closeconnectionform").on("click", function() {
        showConnectToHostDialog();
      });
    
    $("#connectionbutton").on("click", tryWebsocketConnect);
    
    if(getOBSHost())
	{
		connectWebSocket(getOBSHost());
	}
	
	else
	{
		showConnectToHostDialog();
	}
	
	/* initialize stats */
	$("#WebClientVersionStat").text("v" + webclientVersion.toFixed(2)).attr("class", "green");
	
});

function connectionPopupPluginVersionUpdate(pluginVersion)
{
	if(pluginVersion < requiredPluginVersion)
	{
		$("#PluginVersionStat").attr("class", "red");
	}
	else
	{
		$("#PluginVersionStat").attr("class", "green");
	}
	
	$("#PluginVersionStat").text("v" + pluginVersion.toFixed(2));
}

function connectionPopupConnectionStatusChange(connected)
{
	if(connected)
	{
		$("#ConnectionStatusStat").text("Connected").attr("class", "green");
	}
	else
	{
		$("#ConnectionStatusStat").text("Disconnected").attr("class", "red");
		$("#PluginVersionStat").text("N/A").attr("class", "red");
	}
}

function showConnectToHostDialog()
{
	if($("#connection-form").css("display") == "none")
	{
		if(getOBSHost())
		{
			$("#hostname").val(getOBSHost());
		}
		else
		{
			$("#hostname").val("localhost");
		}
	}
	
	
	$("#connection-form").toggle("slide",{direction:"down"} , 400);

}

function hideConnectionForm()
{
	hideConnectionForm();
}

function tryToConnectToHost()
{
	
}


function tryWebsocketConnect()
{
	gracefulWebsocketClose();
	connectWebSocket($("#hostname").val());
}
