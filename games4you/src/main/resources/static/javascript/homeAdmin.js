import {loadData} from "./pagination.js";
import {loadReviewTiles} from "./loadReviewPages.js";

let offset = 0;
function loadHomeAdmin() {
    let data = loadData("/home/more", offset)
        .then(data => {
            offset += data.length;
            loadReviewTiles(data);
        });
}

window.offset = 0;
window.onload = function() {loadHomeAdmin();}
const button = document.getElementById('loadmore');
button.addEventListener('click', function() {
    loadHomeAdmin();
});
