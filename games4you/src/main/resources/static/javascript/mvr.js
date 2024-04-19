window.onload = function() {
    let table = document.getElementById('tableContent')
        .getElementsByTagName('tbody')[0];

    window.jsonData.forEach(function(jsonString) {
        let obj = JSON.parse(jsonString);

        let row = table.insertRow();
        row.onclick = function() {window.location.href = window.location.origin + "/review/" + obj.object.rid;}
        let cell = row.insertCell(0);

        let user = document.createElement("a")
        user.innerText = obj.uname;
        user.href = "/user/" + obj.uid;

        let game = document.createElement("a")
        game.innerText = obj.game + ",";
        game.href = "/game/" + obj.gid;

        let text1 = document.createTextNode("is the gamer with the best review for");
        let text2 = document.createTextNode("contributing for " +
            (obj.revPerf/obj.upvotesMinusReports).toFixed(2) +
            "% to make this game one of the most appreciated");

        cell.appendChild(user);
        cell.appendChild(text1);
        cell.appendChild(game);
        cell.appendChild(text2);
        row.appendChild(cell);

    });
}