window.onload = function() {
    let table = document.getElementById('tableContent')
        .getElementsByTagName('tbody')[0];

    window.jsonData.forEach(function(jsonString) {
        let obj = JSON.parse(jsonString);

        let row = table.insertRow();
        row.onclick = function() {window.location.href = window.location.origin + "/game/" + obj.gid;}
        row.className = "clickable";
        let cell = row.insertCell(0);

        let game = document.createElement("a")
        game.innerText = obj.name;

        let text;
        if(obj.hasOwnProperty("score")) {
             text = document.createTextNode(", with " + obj.totalHours +
                " total played hours and " + obj.playerCount + " total players, totalling a score of " + obj.score);
        }
        else {
            text = document.createTextNode(", with " + obj.totalHours + " total played hours");
        }

        cell.appendChild(game);
        cell.appendChild(text);
        row.appendChild(cell);

    });
}