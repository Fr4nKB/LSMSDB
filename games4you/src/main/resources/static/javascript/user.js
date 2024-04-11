import {loadData} from "./pagination.js";
import {loadPreviewReviewTiles} from "./loadReviewPages.js";

let overlay = document.getElementById("overlay");
let panel = document.getElementById("panel");

async function doRequest(str) {
    if(window.page_uid === null) return;
    let ret = await loadData(window.location.origin + '/' + str + '/' + window.page_uid);

    if(ret === true) window.location.reload();
}

async function mngUser() {
    let jsonData = await loadData(window.location.origin + '/checkFriendship/' + window.page_uid);
    console.log(jsonData);

    let par = panel.children[0];
    let btn = panel.children[1];

    if(jsonData.length === 0) {    // not friend, not pending
        par.innerHTML = "You are not friends";
        btn.innerHTML = "SEND FRIEND REQUEST";
        btn.onclick = function(){doRequest("sendRequest")};
    }
    else if('origin' in jsonData) {    // pending request
        if(jsonData.origin === window.page_uid) {
            par.innerHTML = "He/she wants to be your friend";
            btn.innerHTML = "ACCEPT REQUEST";
            btn.onclick = function(){doRequest("acceptRequest")};

            let b2 = document.createElement("button");
            b2.innerHTML = "DECLINE REQUEST";
            b2.onclick = function(){doRequest("declineRequest")};
            panel.appendChild(b2);
        }
        else {
            par.innerHTML = "You sent a friend request";
            btn.innerHTML = "REVOKE REQUEST";
            btn.onclick = function(){doRequest("revokeRequest")};
        }
    }
    else if('since' in jsonData) {    // friends
        let date = new Date(jsonData.since * 1000);
        par.innerHTML = "You are friends since " + date.toUTCString();
        btn.innerHTML = "REMOVE FRIEND";
        btn.onclick = function(){doRequest("removeFriend")};
    }

    overlay.style.display = "block";
    panel.style.display = "block";

}

async function ban() {
    if(window.page_uid === null) return;
    let ret = await loadData(window.location.origin + '/ban/' + window.page_uid);
    if(ret === true) {
        alert("User was successfully banned");
        history.back();
    }
}

function friendList() {
    window.open(window.location.origin + "/search/friends/" + window.page_uid,"_self");
}

function gameList() {
    window.open(window.location.origin + "/search/games/" + window.page_uid,"_self");
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
        document.getElementById("mngButtons");

    if(window.uid !== null) {
        if(window.page_uid !== window.uid) {
            let b1 = document.createElement('button');
            b1.innerHTML = 'MANAGE FRIENDSHIP';
            b1.onclick = function(){mngUser()};
            btn_div.appendChild(b1);

            document.addEventListener('click', function(event) {
                let isClickInside = panel.contains(event.target);
                if (!isClickInside && event.target !== b1) {
                    overlay.style.display = "none";
                    panel.style.display = "none";
                }
            });
        }
    }
    else {
        let b1 = document.createElement('button');
        b1.innerHTML = 'BAN';
        b1.onclick = function(){ban()};
        btn_div.appendChild(b1);
    }

    btn_div =
        document.getElementById("listButtons");

    let b1 = document.createElement('button');
    b1.innerHTML = 'FRIEND LIST';
    b1.onclick = function(){friendList()};
    btn_div.appendChild(b1);
    let b2 = document.createElement('button');
    b2.innerHTML = 'GAME LIST';
    b2.onclick = function(){gameList()};
    btn_div.appendChild(b2);


}

window.onload = function() {loadUserPage()};
const button = document.getElementById('loadmore');
button.addEventListener('click', function() {
    loadUserReviews();
});
