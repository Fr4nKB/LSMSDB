export async function loadData(url) {
    const response = await fetch(url, {
        method: 'GET',
        credentials: 'include', // Include cookies in the request
    });

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
}


export async function doRequest(str, choice) {
    if(window.page_uid === null) return;
    let ret = await loadData(window.location.origin + '/' + str + '/' + window.page_id);

    if(ret === true) window.location.reload();
}