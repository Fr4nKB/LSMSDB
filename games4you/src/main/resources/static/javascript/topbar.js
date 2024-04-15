import {doRequest, loadData} from "./util.js";

let panel = document.getElementById("notifPanel");
let loadmore = null;

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

const logoutBtn = document.getElementById('logout');
logoutBtn.addEventListener('click', function() {
    topbar().then(r => null);
});

