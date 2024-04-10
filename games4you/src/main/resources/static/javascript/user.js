import {loadData} from "./pagination.js";
import {loadPreviewReviewTiles} from "./loadReviewPages.js";

function loadUserReviews() {
    const url = new URL("/user/reviews/", window.location.origin);
    url.searchParams.append('uid', window.uid);
    url.searchParams.append('offset', window.offset);

    let data = loadData(url)
        .then(data => {
            console.log(data);
            window.offset += data.length;
            loadPreviewReviewTiles(data);
        });
}

function loadUserPage() {
    let obj = JSON.parse(window.jsonData);
    window.uid = obj.uid;
    if('latestReviews' in obj) {
        window.offset = obj.latestReviews.length;
        loadPreviewReviewTiles(obj.latestReviews);
    }
    else window.offset = 0;

    console.log(obj);
    document.getElementById('uname').innerText = obj.uname;
    document.getElementById('name').innerText = obj.firstname + " " + obj.lastname;
    document.getElementById('datebirth').innerText = obj.datebirth;

}

window.onload = function() {loadUserPage()};
const button = document.getElementById('loadmore');
button.addEventListener('click', function() {
    loadUserReviews();
});
