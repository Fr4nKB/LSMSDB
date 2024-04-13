import {loadData} from "./util.js";

async function logout() {
    let ret = await loadData(window.location.origin + '/logout');
    if(ret === true) {
        location.replace(window.location.origin + "/")
    }
}

const button = document.getElementById('logout');
button.addEventListener('click', function() {
    logout().then(r => null);
});

