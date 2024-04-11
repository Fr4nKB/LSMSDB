async function handleReview(str) {
    const url = new URL(str, window.location.origin);

    const response = await fetch(url, {
        method: 'GET',
        credentials: 'include', // Include cookies in the request
    });

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    let resp = await response.json();
    if(resp === true) {
        history.back();
    }
}

function loadReviewPage() {

    jsonData = JSON.parse(window.jsonString);
    if(jsonData === null) return;

    if(window.uid != null) {
        let btn = document.createElement('button');
        if(jsonData.uid !== window.uid) {
            let b1 = document.createElement('button');
            b1.innerHTML = 'UPVOTE REVIEW';
            b1.onclick = function(){handleReview("/upvote/" + jsonData.rid);};
            btn.innerHTML = 'REPORT REVIEW';
            btn.onclick = function(){handleReview("/report/" + jsonData.rid);};
        }
        else {
            btn.innerHTML = 'DELETE REVIEW';
            btn.onclick = function(){handleReview("/remove/" + jsonData.rid);};
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