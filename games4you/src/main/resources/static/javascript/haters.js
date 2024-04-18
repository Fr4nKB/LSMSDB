window.onload = function() {
    let table = document.getElementById('tableContent')
        .getElementsByTagName('tbody')[0];

    window.jsonData.forEach(function(jsonString) {
        let obj = JSON.parse(jsonString);

        let row = table.insertRow();
        let cell = row.insertCell(0);

        let text1 = document.createTextNode("Reviews of user ")
        let creator = document.createElement("a")
        creator.innerText = obj.creator_name;
        creator.href = "/user/" + obj.creator_id;
        let text2 = document.createTextNode(" have been reported a total of " + obj.count + " time(s) by this ");
        let reporter = document.createElement("a");
        reporter.innerText = "user";
        reporter.href = "/user/" + obj.reporter_id;

        cell.appendChild(text1);
        cell.appendChild(creator);
        cell.appendChild(text2);
        cell.appendChild(reporter);
        row.appendChild(cell);

    });
}