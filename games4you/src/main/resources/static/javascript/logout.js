async function logout() {
    const url = new URL('/logout', window.location.origin);

    const response = await fetch(url, {
        method: 'GET',
        credentials: 'include', // Include cookies in the request
    });

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    let resp = await response.json();
    if(resp === true) {
        location.replace(window.location.origin + "/login")
    }
}
