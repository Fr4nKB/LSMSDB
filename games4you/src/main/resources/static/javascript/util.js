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


export async function doRequest(endpoint_name, json) {
    let url;
    if(window.page_id == null) url = new URL('/' + endpoint_name, window.location.origin);
    else url = new URL('/' + endpoint_name + '/' + window.page_id, window.location.origin);

    for(let key in json) {
        url.searchParams.append(key, json[key]);
    }

    let ret = await loadData(url);

    if(ret === true) window.location.reload();
}