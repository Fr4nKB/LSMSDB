import {loadData} from "./util.js";
import {loadSearchListTiles} from "./loadSearchTiles.js";

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