export function loadFullReviewTiles(jsonList) {
    let table = document.getElementById('tableContent').getElementsByTagName('tbody')[0];

    jsonList.forEach(function(jsonString) {
        let obj = JSON.parse(jsonString);

        let row = table.insertRow();
        let c1 = row.insertCell(0);
        let c2 = row.insertCell(1);

        let user = document.createElement("a");
        user.href = "/user/" + obj.id;
        user.text = obj.uname;

        let game = document.createElement("a");
        game.href = "/game/" + obj.id;
        game.text = obj.game;

        let creation_date = new Date(obj.creation_date * 1000);
        let last_report_date = new Date(obj.reports.lastRep * 1000);

        c1.appendChild(user);
        c1.appendChild(document.createTextNode(" HAS REVIEWED "));
        c1.appendChild(game);
        c1.appendChild(document.createElement("br"));
        c1.appendChild(document.createTextNode(creation_date.toUTCString()));
        c2.appendChild(document.createElement("br"));
        c2.appendChild(document.createTextNode(obj.content));
        c2.appendChild(document.createElement("br"));
        c2.appendChild(document.createElement("br"));
        c2.appendChild(document.createTextNode(obj.reports.numRep + " user(s) reported this review"));
        c2.appendChild(document.createElement("br"));
        c2.appendChild(document.createTextNode("Last report was " + last_report_date.toUTCString()));
    });
}

export function loadPreviewReviewTiles(jsonList) {
    let table = document.getElementById('tableContent').getElementsByTagName('tbody')[0];

    jsonList.forEach(function(obj) {

        let row = table.insertRow();
        row.onclick = function() {window.location.href = window.location.origin + "/review/" + obj.rid;}
        let c1 = row.insertCell(0);

        let user = document.createElement("a");
        user.href = "/user/" + window.uid;
        user.text = obj.uname;

        let game = document.createElement("a");
        game.href = "/game/" + obj.gid;
        game.text = obj.game;

        let rating_str;
        if(obj.rating === true) rating_str = " POSITEVELY"
        else rating_str = " NEGATIVELY"


        c1.appendChild(user);
        c1.appendChild(document.createTextNode(" HAS REVIEWED "));
        c1.appendChild(game);
        c1.appendChild(document.createTextNode(rating_str));
    });
}