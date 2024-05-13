var stompClient = null;

function connect() {
    var socket = new SockJS('/stats-socket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/update-stats', function (message) {
            updateStats(JSON.parse(message.body).content)
        });
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    console.log('Disconnected');
}

function updateStats(stats) {
    $("#barrel-stats").append("<tr><td>" + stats + "</td></tr>");
}

function sendMessage() {
    stompClient.send('/app/stats-update', {}, JSON.stringify({ 'content': 'wow :)) !!' }));
}

$(function () {
    $('#send').click(function () { sendMessage(); });
});

window.onload = function () {
    connect();
};

window.onbeforeunload = function () {
    disconnect();
};