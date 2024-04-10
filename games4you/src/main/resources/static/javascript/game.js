import {loadData} from "./pagination.js";
import {loadPreviewReviewTiles} from "./loadReviewPages.js";

async function addGame() {
    if(window.page_uid === null) return;
    let ret = await loadData(window.location.origin + '/addGame/' + window.page_gid);
    if(ret === 1) alert("The game has been added to your library");
    else if(ret === 0) alert("The game is already in your library");
    else alert("Couldn't add the game to your library");
}

async function removeGame() {
    if(window.page_uid === null) return;
    let ret = await loadData(window.location.origin + '/removeGame/' + window.page_gid);
    if(ret === 1) alert("The game has been removed from your library");
    else if(ret === 0) alert("The game already isn't your library");
    else alert("Couldn't remove the game from your library");
}

async function deleteGame() {
    if(window.page_uid === null) return;
    let ret = await loadData(window.location.origin + '/deleteGame/' + window.page_gid);
    if(ret === true) {
        alert("Game was successfully removed");
        history.back();
    }
}

function loadGameReviews() {
    if(window.page_uid === null) return;
    const url = new URL("/game/reviews/", window.location.origin);
    url.searchParams.append('gid', window.page_gid);
    url.searchParams.append('offset', window.offset);

    let data = loadData(url)
        .then(data => {
            window.offset += data.length;
            loadPreviewReviewTiles(data);
        });
}

function loadGamePage() {
    window.page_gid = null;
    let obj = JSON.parse(window.jsonData);
    if(obj === null) return;
    console.log(obj);
    window.page_gid = obj.gid;

    if('latestReviews' in obj) {
        window.offset = obj.latestReviews.length;
        loadPreviewReviewTiles(obj.latestReviews);
    }
    else window.offset = 0;

    document.getElementById('name').innerText = obj.name;
    let date = new Date(obj.release_date * 1000);
    document.getElementById('release').innerText = date.toUTCString();
    document.getElementById('tags').innerText = obj.tags.join(", ");

    if('description' in obj) document.getElementById('description').innerText = obj.description;
    if('header_image' in obj) {
        let img_div = document.getElementById('header_image');
        let img = document.createElement("img");
        img.src = obj.header_image;
        img_div.appendChild(img);
    }

    let btn_div =
        document.getElementById("buttons");
    if(window.adm === true) {
        let b1 = document.createElement('button');
        b1.innerHTML = 'BAN';
        b1.onclick = function(){deleteGame()};
        btn_div.appendChild(b1);
    }
    else {
        let b1 = document.createElement('button');
        b1.innerHTML = 'ADD GAME';
        b1.onclick = function(){addGame()};
        btn_div.appendChild(b1);

        let b2 = document.createElement('button');
        b2.innerHTML = 'REMOVE GAME';
        b2.onclick = function(){removeGame()};
        btn_div.appendChild(b2);
    }
}

window.onload = function() {loadGamePage()};
const button = document.getElementById('loadmore');
button.addEventListener('click', function() {
    loadGameReviews();
});
