import {loadData} from "./util.js";

let panel = document.getElementById("notifPanel");
let loadmore = null;
let queryDiv = document.getElementById("queryBtns");

function loadNotif(jsonList) {
    let friendReqTable = document.getElementById("notifPanel")
        .getElementsByTagName("tbody")[0];

    jsonList.forEach(function(jsonString) {
        let obj = JSON.parse(jsonString);
        let row = friendReqTable.insertRow();
        let c = row.insertCell(0);

        let text = document.createTextNode(obj.uname + " sent you a friend request");
        text.onclick = function() {window.location.href = window.location.origin + "/user/" + obj.uid;}

        let btnDiv = document.createElement("div");

        let accBtn = document.createElement("button");
        accBtn.innerHTML = "ACCEPT";
        accBtn.onclick = function () {
            let url= new URL("/acceptRequest/" + obj.uid, window.location.origin);
            console.log(url);
            loadData(url).then(res => {
                if(res === true) {
                    let row = accBtn.parentNode.parentNode.parentNode;
                    row.parentNode.removeChild(row);
                    if(window.offset > 1) window.offset -= 1;
                    window.location.reload();
                }
            });
        };

        let declBtn = document.createElement("button");
        declBtn.innerHTML = "DECLINE";
        declBtn.onclick = function () {
            let url =  new URL("/declineRequest/" + obj.uid, window.location.origin);
            loadData(url).then(res => {
                if(res === true) {
                    let row = accBtn.parentNode.parentNode.parentNode;
                    row.parentNode.removeChild(row);
                    if(window.offset > 1) window.offset -= 1;
                    window.location.reload();
                }
            });
        };

        c.appendChild(text);
        btnDiv.appendChild(accBtn)
        btnDiv.appendChild(declBtn);
        c.appendChild(btnDiv);
    });

    let row = friendReqTable.insertRow();
    row.id = "loadMoreRow"
    let c = row.insertCell(0);
    loadmore = document.createElement("button");
    loadmore.innerHTML = "LOAD MORE";
    loadmore.onclick = function () {
        fetchNotif()
        let moreRow = document.getElementById("loadMoreRow")
        moreRow.parentNode.removeChild(moreRow);
    }
    c.appendChild(loadmore);
}

function fetchNotif() {
    const url = new URL("/getFriendRequest/", window.location.origin);
    url.searchParams.append('offset', window.offset);
    loadData(url).then(jsonList => {
        window.offset += jsonList.length;
        loadNotif(jsonList)
    })
}

async function topbar() {
    let ret = await loadData(window.location.origin + '/logout');
    if(ret === true) {
        location.replace(window.location.origin + "/")
    }
}

const logoutBtn = document.getElementById('logout');
logoutBtn.addEventListener('click', function() {
    topbar().then(r => null);
});

if(window.logged_id != null) {  // user is not an admin
    let userPage = document.getElementById('userPage');
    userPage.onclick = function() {
        window.location.href = window.location.origin + "/user/" + window.logged_id;
    };

    let notifBtn = document.getElementById('notif');
    notifBtn.onclick = function() {
        if(panel.style.display !== "table") {
            fetchNotif();
            panel.style.display = "table";
        }
    };

    document.addEventListener('click', function(event) {
        let isClickInside = panel.contains(event.target);
        if (!isClickInside && event.target !== notifBtn && event.target !== loadmore) {
            let friendReqTable = document.getElementById("notifPanel")
                .getElementsByTagName("tbody")[0];
            friendReqTable.innerHTML = "";
            panel.style.display = "none";
            window.offset = 0;
        }
    });

    let scoreBoardBtn = document.createElement("button");
    scoreBoardBtn.innerHTML = "TOP 10 FRIENDS"
    scoreBoardBtn.onclick = function() {
        window.location.href = window.location.origin + "/recom/0";
    };

    let ownRecomBtn = document.createElement("button");
    ownRecomBtn.innerHTML = "BASED ON YOUR TASTES"
    ownRecomBtn.onclick = function() {
        window.location.href = window.location.origin + "/recom/1";
    };

    let friendsTagRecomBtn = document.createElement("button");
    friendsTagRecomBtn.innerHTML = "BASED ON FRIENDS' TAGS"
    friendsTagRecomBtn.onclick = function() {
        window.location.href = window.location.origin + "/recom/2";
    };

    let friendsScoreRecomBtn = document.createElement("button");
    friendsScoreRecomBtn.innerHTML = "BASED ON FRIENDS' MVG"
    friendsScoreRecomBtn.onclick = function() {
        window.location.href = window.location.origin + "/recom/3";
    };

    queryDiv.appendChild(scoreBoardBtn)
    queryDiv.appendChild(ownRecomBtn);
    queryDiv.appendChild(friendsTagRecomBtn);
    queryDiv.appendChild(friendsScoreRecomBtn);
}
else {      // user is admin

    let hatersBtn = document.createElement("button");
    hatersBtn.innerHTML = "TOP 10 HATERS"
    hatersBtn.onclick = function() {
        window.location.href = window.location.origin + "/haters/";
    };

    let mvrBtn = document.createElement("button");
    mvrBtn.innerHTML = "BEST REVIEWERS"
    mvrBtn.onclick = function() {
        window.location.href = window.location.origin + "/bestReviewers/";
    };

    queryDiv.appendChild(hatersBtn)
    queryDiv.appendChild(mvrBtn);

}

let hottestGamesBtn = document.createElement("button");
hottestGamesBtn.innerHTML = "TOP 10 GAMES OF THE WEEK"
hottestGamesBtn.onclick = function() {
    window.location.href = window.location.origin + "/hottestGames/";
};
queryDiv.appendChild(hottestGamesBtn);
