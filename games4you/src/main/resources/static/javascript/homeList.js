window.onload = function() {
    var jsonList = window.jsonList;
    var table = document.getElementById('tableContent').getElementsByTagName('tbody')[0];

    jsonList.forEach(function(jsonString) {
        var obj = JSON.parse(jsonString);

        var row = table.insertRow();
        var cell = row.insertCell(0);

        var friendLink = document.createElement("a");
        friendLink.href = "/user/" + obj.friend.id;
        friendLink.text = obj.friend.name;

        if(obj.type === "F") {

            var fofLink = document.createElement("a");
            fofLink.href = "/user/" + obj.object.id;
            fofLink.text = obj.object.name;

            cell.appendChild(friendLink);
            cell.appendChild(document.createTextNode(" IS NOW FRIEND WITH "));
            cell.appendChild(fofLink);
        }
        else if(obj.type === "R") {

            var revlink = document.createElement("a");
            revlink.href = "/review/" + obj.object.rid;
            revlink.text = " HAS REVIEWED ";

            var gamelink = document.createElement("a");
            gamelink.href = "/game/" + obj.object.gid;
            gamelink.text = obj.object.name;

            cell.appendChild(friendLink);
            cell.appendChild(revlink);
            cell.appendChild(gamelink);
        }

        var date = new Date(obj.time * 1000);
        var cell2 = row.insertCell(1);
        cell2.textContent = date.toUTCString();
    });
};