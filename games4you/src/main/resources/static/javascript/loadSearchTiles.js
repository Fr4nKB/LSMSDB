
export function loadSearchListTiles(jsonList) {
    let table = document.getElementById('tableContent').getElementsByTagName('tbody')[0];

    jsonList.forEach(function(jsonString) {
        let obj = JSON.parse(jsonString);

        let row = table.insertRow();
        row.classList.add('clickable');
        let cell = row.insertCell(0);

        let name = document.createTextNode(obj.name);
        cell.appendChild(name);

        let extra_field = null;
        if(obj.type === "U") {
            row.onclick = function() {window.location.href = window.location.origin + "/user/" + obj.id;}
            if('since' in obj) {
                let date = new Date(obj.since * 1000);
                extra_field = document.createTextNode(", friends since " + date.toUTCString());
            }
            else if('score' in obj) {
                extra_field = document.createTextNode(", with a score of " + obj.score);
            }
        }
        else if(obj.type === "G") {
            row.onclick = function() {window.location.href = window.location.origin + "/game/" + obj.id;}
            if('hours' in obj) {
                extra_field = document.createTextNode(", played " + obj.hours + " hours");
            }
            else if('tags' in obj) {
                extra_field = document.createTextNode(", tags: " + obj.tags.join(", "));
            }
        }

        if(extra_field !== null) cell.appendChild(extra_field);
    });
}