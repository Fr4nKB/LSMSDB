import {loadData} from "./pagination.js";
import {loadPreviewReviewTiles} from "./loadReviewPages.js";

async function addFriend() {
    if(window.page_uid === null) return;
    let ret = await loadData(window.location.origin + '/addFriend/' + window.page_uid);
    if(ret === 1) alert("You are now friends");
    else if(ret === 0) alert("You are already friends");
    else alert("User couldn't be added as friend");
}

async function removeFriend() {
    if(window.page_uid === null) return;
    let ret = await loadData(window.location.origin + '/removeFriend/' + window.page_uid);
    if(ret === 1) alert("You are no more friends :(");
}

async function ban() {
    if(window.page_uid === null) return;
    let ret = await loadData(window.location.origin + '/ban/' + window.page_uid);
    if(ret === true) {
        alert("User was successfully banned");
        history.back();
    }
}

function loadUserReviews() {
    if(window.page_uid === null) return;
    const url = new URL("/user/reviews/", window.location.origin);
    url.searchParams.append('uid', window.page_uid);
    url.searchParams.append('offset', window.offset);

    let data = loadData(url)
        .then(data => {
            window.offset += data.length;
            loadPreviewReviewTiles(data);
        });
}

function loadUserPage() {
    window.page_uid = null;
    let obj = JSON.parse(window.jsonData);
    if(obj === null) return;
    window.page_uid = obj.uid;

    if('latestReviews' in obj) {
        window.offset = obj.latestReviews.length;
        loadPreviewReviewTiles(obj.latestReviews);
    }
    else window.offset = 0;

    document.getElementById('uname').innerText = obj.uname;
    document.getElementById('name').innerText = obj.firstname + " " + obj.lastname;
    document.getElementById('datebirth').innerText = obj.datebirth;

    let btn_div =
        document.getElementById("buttons");
    if(window.uid !== null) {
        if(window.page_uid !== window.uid) {
            let b1 = document.createElement('button');
            b1.innerHTML = 'ADD FRIEND';
            b1.onclick = function(){addFriend()};
            btn_div.appendChild(b1);

            let b2 = document.createElement('button');
            b2.innerHTML = 'REMOVE FRIEND';
            b2.onclick = function(){removeFriend()};
            btn_div.appendChild(b2);
        }
    }
    else {
        let b1 = document.createElement('button');
        b1.innerHTML = 'BAN';
        b1.onclick = function(){ban()};
        btn_div.appendChild(b1);
    }

}

window.onload = function() {loadUserPage()};
const button = document.getElementById('loadmore');
button.addEventListener('click', function() {
    loadUserReviews();
});
