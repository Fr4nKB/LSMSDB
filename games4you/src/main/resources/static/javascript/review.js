import {doRequest} from "./util.js";

function loadReviewPage() {
    let jsonData = JSON.parse(window.jsonString);
    if(jsonData === null) return;

    if(window.page_uid != null) {
        let btn = document.createElement('button');
        if(jsonData.uid !== window.page_uid) {
            let b1 = document.createElement('button');
            b1.innerHTML = 'UPVOTE REVIEW';
            b1.onclick = async function(){
                await doRequest("upvoteReview");
                window.location.reload();
            };
            btn.innerHTML = 'REPORT REVIEW';
            btn.onclick = async function(){
                await doRequest("reportReview");
                window.location.reload();
            };
        }
        else {
            btn.innerHTML = 'DELETE REVIEW';
            btn.onclick = async function(){
                await doRequest("removeReview");
                history.back();
            };
        }
        document.body.appendChild(btn);
    }

    document.getElementById("uname").innerText = jsonData.uname;
    document.getElementById("game").innerText = jsonData.game;
    let date = new Date(jsonData.creation_date * 1000);
    document.getElementById("date").innerText = date.toUTCString();
    document.getElementById("content").innerText = jsonData.content;
    if(jsonData.rating === true) document.getElementById("rating").innerText = "Positive";
    else document.getElementById("rating").innerText = "Negative";

}

window.onload = function() {loadReviewPage();}