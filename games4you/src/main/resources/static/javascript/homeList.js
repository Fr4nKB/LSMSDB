window.onload = function() {
    var jsonList = window.jsonList;
    var table = document.getElementById('tableContent').getElementsByTagName('tbody')[0];

    jsonList.forEach(function(jsonString) {
        var obj = JSON.parse(jsonString);

        var row = table.insertRow();
        var string = "";

        if(obj.type === "F") string = obj.friend + " IS NOW FRIEND WITH " + obj.object;
        else if(obj.type === "R") string = obj.friend + " HAS REVIEWED " + obj.object;

        var cell1 = row.insertCell(0);
        cell1.textContent = string;

        var date = new Date(obj.time * 1000);
        var cell2 = row.insertCell(1);
        cell2.textContent = date.toUTCString();
    });
};