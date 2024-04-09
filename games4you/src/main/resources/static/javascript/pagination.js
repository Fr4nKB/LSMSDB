let offset =  0;

function loadHomePageTiles(jsonList) {
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
}

async function loadData(endpoint) {
    const url = new URL(endpoint, window.location.origin);
    url.searchParams.append('offset', offset);

    const response = await fetch(url, {
        method: 'GET',
        credentials: 'include', // Include cookies in the request
    });

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    offset += data.length;
    loadHomePageTiles(data);
}