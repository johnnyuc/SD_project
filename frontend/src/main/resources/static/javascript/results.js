var stompClient = null;

function connect() {
    var socket = new SockJS('/results-socket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/contextualized-analysis', function (message) {
            putContextualizedAnalysis(JSON.parse(message.body))
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    console.log('Disconnected');
}

function putContextualizedAnalysis(message) {
    // Replace the button with the text from the function parameter
    $("#generate-contextualized-analysis").replaceWith('<p>' + message.content + '</p>');
}

function sendIndexTopStoriesRequest() {
    var urlParams = new URLSearchParams(window.location.search);
    var query = urlParams.get('query');
    console.log('Sending index top stories request with query: ' + query);
    stompClient.send('/app/index-top-stories', {}, JSON.stringify({ 'content': query }));

    // Send notification to client
    var notification = 'Request sent successfully';
    alert(notification);
}


$(function () {
    $('#index-top-stories').click(function () {
        $(this).prop('disabled', true);
        sendIndexTopStoriesRequest();
    });

    $('#generate-contextualized-analysis').click(function () {
        $(this).prop('disabled', true);
        var urlParams = new URLSearchParams(window.location.search);
        var query = urlParams.get('query');
        console.log('Sending generate contextualized analysis request with query: ' + query);
        stompClient.send('/app/generate-contextualized-analysis', {}, JSON.stringify({ 'content': query }));
    });
});

window.onload = function () {
    connect();
};

window.onbeforeunload = function () {
    disconnect();
};