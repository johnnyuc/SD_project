var stompClient = null;

function connect() {
    var socket = new SockJS('/results-socket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    console.log('Disconnected');
}

function sendIndexTopStoriesRequest() {
    var urlParams = new URLSearchParams(window.location.search);
    var query = urlParams.get('query');
    console.log('Sending index top stories request with query: ' + query);
    stompClient.send('/app/index-top-stories', {}, JSON.stringify({ 'query': query }));
}

$(function () {
    $('form').on('submit', function (e) {
        e.preventDefault();
    });
    $('#index-top-stories').click(function () { sendIndexTopStoriesRequest(); });
});

window.onload = function () {
    connect();
};

window.onbeforeunload = function () {
    disconnect();
};