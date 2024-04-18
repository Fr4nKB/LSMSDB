import {doRequest} from "./util.js";

function loadReviewPage() {
    let jsonData = JSON.parse(window.jsonString);
    if(jsonData === null) return;

    window.page_id = jsonData.rid;

    let btn = document.createElement('button');
    if(jsonData.uid !== window.logged_id) {
        if(!jsonData.upvotes.includes(window.logged_id)) {
            let b1 = document.createElement('button');
            b1.innerHTML = 'UPVOTE REVIEW';
            b1.onclick = async function(){
                await doRequest("upvoteReview");
                window.location.reload();
            };
            document.body.appendChild(b1);

        }

        if(!("reports" in jsonData) || !(jsonData.reports.reporters.includes(window.logged_id))) {
            btn.innerHTML = 'REPORT REVIEW';
            btn.onclick = async function(){
                await doRequest("reportReview");
                window.location.reload();
            };
            document.body.appendChild(btn);
        }
    }
    else if (window.logged_id === null) {
        btn.innerHTML = 'DELETE REVIEW';
        btn.onclick = async function(){
            await doRequest("removeReview");
            history.back();
        };
        document.body.appendChild(btn);
    }

    let uname = document.getElementById("uname");
    uname.innerText = jsonData.uname;
    uname.onclick = function() {window.location.href = window.location.origin + "/user/" + jsonData.uid}

    let game = document.getElementById("game");
    game.innerText = jsonData.game;
    game.onclick = function() {window.location.href = window.location.origin + "/game/" + jsonData.gid}

    let date = new Date(jsonData.creation_date * 1000);
    document.getElementById("date").innerText = date.toUTCString();
    document.getElementById("content").innerText = jsonData.content;
    if(jsonData.rating === true) document.getElementById("reviewBox").className = "posRev"
    else document.getElementById("reviewBox").className = "negRev"
    document.getElementById("rating").innerText = jsonData.numUpvotes + " gamers liked this review";

}

window.onload = function() {loadReviewPage();}