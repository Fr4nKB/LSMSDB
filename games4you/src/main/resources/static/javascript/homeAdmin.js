import {loadData} from "./util.js";
import {loadFullReviewTiles} from "./loadReviewPages.js";

let offset = 0;
function loadHomeAdmin() {
    const url = new URL("/home/more", window.location.origin);
    url.searchParams.append('offset', window.offset);
    let data = loadData(url)
        .then(data => {
            window.offset += data.length;
            loadFullReviewTiles(data);
        });
}

window.offset = 0;
window.onload = function() {loadHomeAdmin();}
const button = document.getElementById('loadmore');
button.addEventListener('click', function() {
    loadHomeAdmin();
});
