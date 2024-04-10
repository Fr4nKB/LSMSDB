import {loadData} from "./pagination.js";

function loadSearchListTiles(jsonList) {
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

function loadSearch() {
    const url = new URL("/search/" + window.search_type + "/" + window.search_query, window.location.origin);
    url.searchParams.append('offset', window.offset);
    let data = loadData(url)
        .then(data => {
            window.offset += data.length;
            loadSearchListTiles(data);
        });
}

window.offset = 0;
window.onload = function() {loadSearch();}
const button = document.getElementById('loadmore');
button.addEventListener('click', function() {
    loadSearch();
});