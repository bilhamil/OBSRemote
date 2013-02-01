var lastChatBoxWidth = 300;


$(function() {
	$('#StatsViewResizeBar').on("mousedown.resizing", startResizeStatsView);
	$('#menusResizeBar').on("mousedown.resizing", startResizeMenus);
});

/* stats view resizing functions */
function startResizeStatsView(event) {
	event.preventDefault();
	$(window).on("mousemove.resizing", resizeStatsView);
	$(window).on("mouseup.resizing", stopAllResizing);				
}

function resizeStatsView(event) {
	event.preventDefault();
	var newHeight = Math.max($('#StatsViewResizeBar').height(), (window.innerHeight - event.pageY)) + 'px';
	$('#statsView').height(newHeight);
	$('#bigOne').css("bottom", newHeight);
}

/*menus resizing functions */
function startResizeMenus(event) {
	event.preventDefault();
	$(window).on("mousemove.resizing", resizeMenuView);
	$(window).on("mouseup.resizing", stopAllResizing);		
}

function resizeMenuView(event) {
	event.preventDefault();
	var newWidth = Math.max($('#menusResizeBar').width(), (event.pageX)) + 'px';
	$('#SourceSceneMenus').width(newWidth);
	$('#sourcesBox').css("left", newWidth);
}

function startResizeChat(event) {
	event.preventDefault();
	$('<div id="chatHider"></div>').appendTo("#chatView");
	
	$(window).on("mousemove.resizing", resizeChatView);
	$(window).on("mouseup.resizing", stopChatResizing);		
}

function resizeChatView(event) {
	event.preventDefault();
	var newWidth = Math.max($('#chatResizeBar').width() + 300, (window.innerWidth - event.pageX));
	
	setChatWidth(newWidth);
	newWidth += "px";
	
	$('#chatView').width(newWidth);
	$('#streamView').css("right", newWidth);
}

function stopChatResizing(event) {
	$("#chatHider").remove();
	stopAllResizing();
}

function stopAllResizing() {
	$(window).off("mousemove.resizing");
	$(window).off("mouseup.resizing");
}