import {loadData} from "./pagination.js";

function loadSearchListTiles(jsonList) {
    let table = document.getElementById('tableContent').getElementsByTagName('tbody')[0];

    jsonList.forEach(function(jsonString) {
        let obj = JSON.parse(jsonString);

        let row = table.insertRow();
        let cell = row.insertCell(0);

        let elem = document.createElement("a");
        let extra_field = null;
        if(obj.type === "U") {
            elem.href = "/user/" + obj.id;
            if('since' in obj) {
                let date = new Date(obj.since * 1000);
                extra_field = document.createTextNode(", friends since " + date.toUTCString());
            }
        }
        else if(obj.type === "G") {
            elem.href = "/game/" + obj.id;
            if('hours' in obj) {
                extra_field = document.createTextNode(", played " + obj.hours + " hours");
            }
        }
        elem.text = obj.name;

        cell.appendChild(elem);
        if(extra_field !== null) cell.appendChild(extra_field);
    });
}

function loadSearch() {
    const url = new URL("/search/" + window.endpoint, window.location.origin);
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