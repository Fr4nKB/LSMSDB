window.onload = function() {
    var jsonList = window.jsonList;
    var table = document.getElementById('tableContent').getElementsByTagName('tbody')[0];

    jsonList.forEach(function(jsonString) {
        var obj = JSON.parse(jsonString);

        var row = table.insertRow();
        var cell = row.insertCell(0);

        var elem = document.createElement("a");
        if(obj.type === "U") elem.href = "/user/" + obj.id;
        else if(obj.type === "G") elem.href = "/game/" + obj.id;
        elem.text = obj.name;

        cell.appendChild(elem);
    });
};