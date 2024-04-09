async function loadData(endpoint, offset) {
    const url = new URL(endpoint, window.location.origin);
    url.searchParams.append('offset', offset);

    const response = await fetch(url, {
        method: 'GET',
        credentials: 'include', // Include cookies in the request
    });

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
}

function loadHomePageTiles(jsonList) {
    let table = document.getElementById('tableContent').getElementsByTagName('tbody')[0];

    jsonList.forEach(function(jsonString) {
        let obj = JSON.parse(jsonString);

        let row = table.insertRow();
        let cell = row.insertCell(0);

        let friendLink = document.createElement("a");
        friendLink.href = "/user/" + obj.friend.id;
        friendLink.text = obj.friend.name;

        if(obj.type === "F") {

            let fofLink = document.createElement("a");
            fofLink.href = "/user/" + obj.object.id;
            fofLink.text = obj.object.name;

            cell.appendChild(friendLink);
            cell.appendChild(document.createTextNode(" IS NOW FRIEND WITH "));
            cell.appendChild(fofLink);
        }
        else if(obj.type === "R") {

            let revlink = document.createElement("a");
            revlink.href = "/review/" + obj.object.rid;
            revlink.text = " HAS REVIEWED ";

            let gamelink = document.createElement("a");
            gamelink.href = "/game/" + obj.object.gid;
            gamelink.text = obj.object.name;

            cell.appendChild(friendLink);
            cell.appendChild(revlink);
            cell.appendChild(gamelink);
        }

        let date = new Date(obj.time * 1000);
        let cell2 = row.insertCell(1);
        cell2.textContent = date.toUTCString();
    });
}

function populateSearchListTiles(jsonList) {
    var table = document.getElementById('tableContent').getElementsByTagName('tbody')[0];

    jsonList.forEach(function(jsonString) {
        var obj = JSON.parse(jsonString);

        var row = table.insertRow();
        var cell = row.insertCell(0);

        var elem = document.createElement("a");
        if(obj.type === "U") elem.href = "/user/" + obj.id;
        else if(obj.type === "G") elem.href = "/game/" + obj.id;
        elem.text = obj.name;

        cell.appendChild(elem);
    });
}

function loadHome(str) {
    let data = loadData("/home/more", window.offset)
        .then(data => {
            window.offset += data.length;
            loadHomePageTiles(data);
        });
}

function loadSearch() {
    let data = loadData("/search/" + window.search_type + "/" + window.search_query, window.offset)
        .then(data => {
            offset += data.length;
            populateSearchListTiles(data);
        });
}