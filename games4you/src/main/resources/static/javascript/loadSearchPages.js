import {loadData} from "./util.js";
import {loadSearchListTiles} from "./loadSearchTiles.js";

let offset = 0;

function loadSearch() {
    const url = new URL("/search/" + window.endpoint, window.location.origin);
    url.searchParams.append('offset', offset);
    url.searchParams.append('limit', -1);
    let data = loadData(url)
        .then(data => {
            offset += data.length;
            loadSearchListTiles(data);
        });
}

window.onload = function() {loadSearch();}
const button = document.getElementById('loadmore');
button.addEventListener('click', function() {
    loadSearch();
});