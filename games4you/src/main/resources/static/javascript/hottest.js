window.onload = function() {
    let table = document.getElementById('tableContent')
        .getElementsByTagName('tbody')[0];

    window.jsonData.forEach(function(jsonString) {
        let obj = JSON.parse(jsonString);

        let row = table.insertRow();
        let cell = row.insertCell(0);

        let game = document.createElement("a")
        game.innerText = obj.name;
        game.href = "/game/" + obj.gid;

        let text = document.createTextNode(", with " + obj.totalHours +
            " total played hours and " + obj.playerCount + " total players, totalling a score of " + obj.score);

        cell.appendChild(game);
        cell.appendChild(text);
        row.appendChild(cell);

    });
}