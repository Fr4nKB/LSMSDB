import {loadData} from "./util.js";

function loadHomePageTiles(jsonList) {
    let table = document.getElementById('tableContent').getElementsByTagName('tbody')[0];

    jsonList.forEach(function(jsonString) {
        let obj = JSON.parse(jsonString);
        console.log(obj);

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

            row.onclick = function() {window.location.href = window.location.origin + "/review/" + obj.object.rid;}

            let gamelink = document.createElement("a");
            gamelink.href = "/game/" + obj.object.gid;
            gamelink.text = obj.object.name;

            cell.appendChild(friendLink);
            cell.appendChild(document.createTextNode(" HAS REVIEWED "));
            cell.appendChild(gamelink);
        }

        let cell2 = row.insertCell(1);
        let date = new Date(obj.time * 1000);
        cell2.textContent = date.toUTCString();
    });
}

function loadHome() {
    const url = new URL("/home/more", window.location.origin);
    url.searchParams.append('offset', window.offset);
    let data = loadData(url)
        .then(data => {
            window.offset += data.length;
            loadHomePageTiles(data);
        });
}

window.offset = 0;
window.onload = function() {
    loadHome();
}
const button = document.getElementById('loadmore');
button.addEventListener('click', function() {
    loadHome();
});

