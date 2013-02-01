function onScenesChanged()
{
	requestScenes();
}

function requestScenes()
{
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "GetSceneList";
		
	sendMessage(myJSONRequest, receiveScenes);
}

function receiveScenes(resp)
{
	var status = resp["status"];
	
	if(status === "ok")
	{
		var currentscene = resp["current-scene"];
		var scenes = resp["scenes"];
		if(scenes)
		{
			$("#SceneList").empty();
			for(var i = 0; i < scenes.length; i++)
			{
				var scene = scenes[i]; 
				var newHTML = "";
				if(scene["name"] == currentscene)
				{
					newHTML = '<li class="sceneselect">' + scene["name"] +  '</li>';
					updateSourceList(scene["sources"]);
				}
				else
				{
					newHTML = '<li>' + scene["name"] +  '</li>';
				}
				
				var li = $(newHTML).appendTo("#SceneList");
				jQuery.data(li[0], "scene", scene);
				li.on("click", sceneClicked);
			}
		}
	}
}

function saveCurrentSources()
{
	var data = jQuery.data($("#SceneList li.sceneselect")[0], "scene");
	
	var newSources = getCurrentSources();
	
	data["sources"] = newSources;
}

function sceneClicked()
{
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "SetCurrentScene";
	myJSONRequest["scene-name"] = jQuery.data(this, "scene")["name"];
		
	sendMessage(myJSONRequest);
}

function onSceneSwitched(update)
{
	saveCurrentSources();
	
	var newScene = update["scene-name"];
	$("#SceneList li").each(function(){
		var li = $(this);
		var scene = jQuery.data(this, "scene");
		
		if(scene["name"] == newScene)
		{
			li.attr("class", "sceneselect");
			updateSourceList(scene["sources"]);
		}
		else
		{
			li.attr("class", null);
		}
	});
}
