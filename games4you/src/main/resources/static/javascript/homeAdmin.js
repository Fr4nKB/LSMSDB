import {loadData} from "./util.js";
import {loadFullReviewTiles} from "./loadReviewPages.js";

let offset = 0;

function loadHomeAdmin() {
    const url = new URL("/home/more", window.location.origin);
    url.searchParams.append('offset', offset);
    let data = loadData(url)
        .then(data => {
            offset += data.length;
            loadFullReviewTiles(data);
        });
}

window.onload = function() {loadHomeAdmin();}
const button = document.getElementById('loadmore');
button.addEventListener('click', function() {
    loadHomeAdmin();
});
