var stompClient = null;

function connect() {
    var socket = new SockJS('/stats-socket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/update-stats', function (message) {
            updateStats(JSON.parse(message.body))
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
    $("#barrel-stats").empty(); // Clear barrel-stats
    $("#top-searches").empty(); // Clear top-searches

    stats.barrel_stats.forEach(function (barrelStat) {
        $("#barrel-stats").append("<tr><td>" + barrelStat + "</td></tr>");
    });
    stats.most_searched.forEach(function (search) {
        $("#top-searches").append("<tr><td>" + search + "</td></tr>");
    });
}

window.onload = function () {
    connect();
};

window.onbeforeunload = function () {
    disconnect();
};