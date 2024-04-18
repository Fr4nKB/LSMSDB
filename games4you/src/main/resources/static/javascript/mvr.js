window.onload = function() {
    let table = document.getElementById('tableContent')
        .getElementsByTagName('tbody')[0];

    window.jsonData.forEach(function(jsonString) {
        let obj = JSON.parse(jsonString);

        let row = table.insertRow();
        let cell = row.insertCell(0);

        let game = document.createElement("a")
        game.innerText = obj.name + ":";
        game.href = "/game/" + obj.gid;

        let user = document.createElement("a")
        user.innerText = obj.uname;
        user.href = "/user/" + obj.uid;

        let text = document.createTextNode(" is the top reviewer for this game with "
            + obj.reviewCount + " reviews, " + obj.averageUpvotes + " average upvotes and " + obj.sumReports + " total reports");

        cell.appendChild(game);
        cell.appendChild(user);
        cell.appendChild(text);
        row.appendChild(cell);

    });
}