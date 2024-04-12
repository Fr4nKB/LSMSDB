import {doRequest, loadData} from "./util.js";
import {loadPreviewReviewTiles} from "./loadReviewPages.js";

let overlay = document.getElementById("overlay");
let panel = document.getElementById("panel");


async function mngGame() {
    let jsonData = await loadData(window.location.origin + '/checkGame/' + window.page_id);

    let par = panel.children[0];
    let btn = panel.children[1];

    if (window.adm === true) {
        btn.innerHTML = 'DELETE';
        btn.onclick = async function () {
            await doRequest("deleteGame");
            history.back();
        };
    }
    else {
        if (jsonData.hours === null) {
            btn.innerHTML = 'ADD GAME';
            btn.onclick = async function () {
                await doRequest("addGame");
                window.location.reload();
            };
        }
        else {
            par.innerHTML = "You have played for a total of " + jsonData.hours + " hours";
            btn.innerHTML = 'REMOVE GAME';
            btn.onclick = async function () {
                await doRequest("removeGame");
                window.location.reload();
            };
        }
        if (jsonData.rev.in !== null) {
            let rev = document.createElement("a");
            let date = new Date(jsonData.rev.in * 1000);

            rev.text = "You have reviewed this game in " + date.toUTCString();
            rev.href = "/review/" + jsonData.rev.id;

            panel.appendChild(rev);
        }
        else {
            let rev = document.createElement("p");
            let revBtn = document.createElement("button");

            rev.innerHTML = "You have not reviewed this game yet"
            revBtn.innerHTML = "REVIEW GAME";
            revBtn.onclick = async function () {
                window.open(window.location.origin + "/newReview/" + window.page_id,"_self");
            };

            panel.appendChild(rev);
            panel.appendChild(revBtn);
        }
    }

    overlay.style.display = "block";
    panel.style.display = "block";
}

function loadGameReviews() {
    if(window.page_id === null) return;
    const url = new URL("/game/reviews/", window.location.origin);
    url.searchParams.append('gid', window.page_id);
    url.searchParams.append('offset', window.offset);

    let data = loadData(url)
        .then(data => {
            window.offset += data.length;
            loadPreviewReviewTiles(data);
        });
}

function loadGamePage() {
    window.page_id = null;
    let obj = JSON.parse(window.jsonData);
    console.log(obj);
    if(obj === null) return;
    window.page_id = obj.gid;

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

    let btn = document.getElementById("mngBtn");
    btn.innerHTML = 'MANAGE';
    btn.onclick = function(){mngGame()};

    document.addEventListener('click', function(event) {
        let isClickInside = panel.contains(event.target);
        if (!isClickInside && event.target !== btn) {
            overlay.style.display = "none";
            panel.style.display = "none";

            while(panel.children.length > 2) {
                panel.removeChild(panel.lastChild);
            }
        }
    });
}

window.onload = function() {loadGamePage()};
const button = document.getElementById('loadmore');
button.addEventListener('click', function() {
    loadGameReviews();
});
