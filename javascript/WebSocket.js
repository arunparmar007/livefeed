var webSocket = new WebSocket("ws://localhost:4567/websocket/livefeed");
var divMsg = document.getElementById("msg-box");
var p = document.getElementById("processed");
console.log("message sent:" + divMsg)

function sendMsg(counterValue) {
    console.log("message sent:" + counterValue)
    webSocket.send(counterValue);
}

webSocket.onmessage = function(message) {
    var array = message.data.split("<br />");
    array.shift();
//    divMsg.innerHTML = message.data;
    divMsg.innerHTML = array.join("<br />");
    p.innerHTML = message.data.split("<br />")[0];
};

webSocket.onopen = function() {
//    divMsg.innerHTML += "Server> : Connection Started";
    console.log("connection opened");
};

webSocket.onclose = function() {
    console.log("connection closed");
};

webSocket.onerror = function wserror(message) {
    console.log("error: " + message);
}
