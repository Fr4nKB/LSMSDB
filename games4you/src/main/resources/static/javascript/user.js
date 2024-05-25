import {doRequest, loadData} from "./util.js";
import {loadPreviewReviewTiles} from "./loadReviewPages.js";

let overlay = document.getElementById("overlay");
let panel = document.getElementById("panel");
let offset = 0;

async function mngUser() {
    let jsonData = await loadData(window.location.origin + '/checkFriendship/' + window.page_id);

    let par = panel.children[0];
    let btn = panel.children[1];

    if(Object.keys(jsonData).length === 0 || jsonData.length === 0) {    // not friend, not pending
        par.innerHTML = "You are not friends";
        btn.innerHTML = "SEND FRIEND REQUEST";
        btn.onclick = async function(){
            await doRequest("sendRequest");
            window.location.reload();
        };
    }
    else if('origin' in jsonData) {    // pending request
        if(jsonData.origin === window.page_id) {
            par.innerHTML = "He/she wants to be your friend";
            btn.innerHTML = "ACCEPT REQUEST";
            btn.onclick = async function(){
                await doRequest("acceptRequest");
                window.location.reload();
            };

            let b2 = document.createElement("button");
            b2.innerHTML = "DECLINE REQUEST";
            b2.onclick = async function(){
                await doRequest("declineRequest");
                window.location.reload();
            };
            panel.appendChild(b2);
        }
        else {
            par.innerHTML = "You sent a friend request";
            btn.innerHTML = "REVOKE REQUEST";
            btn.onclick = async function(){
                await doRequest("revokeRequest");
                window.location.reload();
            };
        }
    }
    else if('since' in jsonData) {    // friends
        let date = new Date(jsonData.since * 1000);
        par.innerHTML = "You are friends since " + date.toUTCString();
        btn.innerHTML = "REMOVE FRIEND";
        btn.onclick = async function(){
            await doRequest("removeFriend");
            window.location.reload();
        };
    }

    overlay.style.display = "block";
    panel.style.display = "block";

}

async function ban() {
    if(window.page_id === null) return;
    let ret = await loadData(window.location.origin + '/ban/' + window.page_id);
    if(ret === true) {
        alert("User was successfully banned");
        history.back();
    }
}

function friendList() {
    window.open(window.location.origin + "/search/friends/" + window.page_id,"_self");
}

function gameList() {
    window.open(window.location.origin + "/search/games/" + window.page_id,"_self");
}

function loadUserReviews() {
    if(window.page_id === null) return;
    const url = new URL("/user/reviews/", window.location.origin);
    url.searchParams.append('uid', window.page_id);
    url.searchParams.append('offset', offset);

    let data = loadData(url)
        .then(data => {
            offset += data.length;
            let jsonList = data.map(item => JSON.parse(item))
            loadPreviewReviewTiles(jsonList);
        });
}

function loadUserPage() {
    window.page_id = null;
    let obj = JSON.parse(window.jsonData);
    if(obj === null) return;
    window.page_id = obj.uid;

    if('latestReviews' in obj) {
        offset = obj.latestReviews.length;
        loadPreviewReviewTiles(obj.latestReviews);
    }
    else offset = 0;

    document.getElementById('uname').innerText = obj.uname;
    let date = new Date(obj.datecreation * 1000);
    document.getElementById('since').innerText = "Member since " + date.toUTCString();
    document.getElementById('name').innerText = obj.firstname + " " + obj.lastname;
    document.getElementById('datebirth').innerText = "Date of birth: " + obj.datebirth;
    if(obj.hasOwnProperty("lastGamePlayed")) {
        let lastGame = document.getElementById('lastGame');
        lastGame.innerText = "Last game played: " + obj.lastGamePlayed.name;
        lastGame.className = "clickable";

        lastGame.onclick = function() {window.location.href = window.location.origin + "/game/" + obj.lastGamePlayed.gid;}
    }

    let btn_div =
        document.getElementById("listButtons");

    if(window.logged_id !== null) {
        if(window.page_id !== window.logged_id) {
            let b1 = document.createElement('button');
            b1.innerHTML = 'MANAGE FRIENDSHIP';
            b1.onclick = function(){mngUser()};
            btn_div.appendChild(b1);

            document.addEventListener('click', function(event) {
                let isClickInside = panel.contains(event.target);
                if (!isClickInside && event.target !== b1) {
                    overlay.style.display = "none";
                    panel.style.display = "none";

                    while(panel.children.length > 2) {
                        panel.removeChild(panel.lastChild);
                    }
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
