/* pre-cache images in scene list */

$(function() {
	var sourceList = $( "#SourceList" );
	sourceList.sortable({placeholder: "scene-placeholder", 
								distance: 5,
								cursor: "move",
								axis: "y"});
	sourceList.on("sortstop", updateOnSortStop);
});

function updateSourceList(sources)
{
	$("#SourceList").empty();
	for(var i = 0; i < sources.length; i++)
	{
		var source = sources[i];
		var render = source["render"];
		var newHTML = '<li> <div> <div class="arrow"></div><p class="sourceName">' + 
						source["name"] + "</p>" + 
						'<div title="Toggle Source" class="eye'+ ((render)?"":" off")  +'"></div>' + "</div></li>";
		var li = $(newHTML).appendTo('#SourceList');
		jQuery.data(li[0], "source", source);
	}
	$("div.eye").on("click", toggleSourceRender);
}

function onRepopulateSources(update)
{
	var sources = update["sources"];
	updateSourceList(sources);
}

function getCurrentSources() {
	var sources = [];
	$("#SourceList li").each(function() {
		var source = jQuery.data(this, "source");
		sources.push(source);
	});
	
	return sources;
}

function toggleSourceRender()
{
	var li = $(this).parent().parent();
	var source = jQuery.data(li[0], "source");
	
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "SetSourceRender";
	myJSONRequest["source"] = source["name"];
	myJSONRequest["render"] = !source["render"];
	
	sendMessage(myJSONRequest);
}

function updateOnSortStop(event, ui)
{
	var sourceOrder = [];
	
	$("#SourceList li").each(function() {
		var source = jQuery.data(this, "source");
		sourceOrder.push(source["name"]);
	});
		
	var myJSONRequest = {};
	myJSONRequest["request-type"] = "SetSourceOrder";
	myJSONRequest["scene-names"] = sourceOrder;
	
	
	sendMessage(myJSONRequest);
}

function orderNeedsUpdate(sourceOrder)
{
	var needsUpdate = false;
    $("#SourceList li").each(function(index, value) {
		var source = jQuery.data(this, "source");
		if(source["name"] != sourceOrder[index])
		{
			needsUpdate = true;
		}
	});
	
	return needsUpdate;
}

function onSourceOrderChanged(update)
{
	var sourceOrder = update["sources"];

	if(orderNeedsUpdate(sourceOrder))
	{
		for(var i = 0; i < sourceOrder.length; i++)
		{
			var foundSource = null;
			$("#SourceList li").each(function() {
				var source = jQuery.data(this, "source");
				if(source["name"] === sourceOrder[i])
				{
					foundSource = this;
				}
			});
			if(foundSource)
			{
				var detachedSource = $(foundSource).detach();
				detachedSource.appendTo("#SourceList");
			}
		}
		$("#SourceList").sortable( "refreshPositions" );
	}
}

function updateSource(foundSource, newSource)
{
	$(foundSource).find("p.sourceName").text(newSource["name"]);
	
	var render = newSource["render"];
	$(foundSource).find("div.eye").attr("class", "eye" + ((render)?"":" off"));
	
	jQuery.data(foundSource, "source", newSource);
}

function onSourceChanged(update)
{
	var oldSourceName = update["source-name"];
	var newSource = update["source"];
	
	var foundSource = null;
	$("#SourceList li").each(function() {
		var source = jQuery.data(this, "source");
		if(source["name"] === oldSourceName)
		{
			foundSource = this;
		}
	});
	
	if(foundSource)
	{
		updateSource(foundSource, newSource);
	}
}


