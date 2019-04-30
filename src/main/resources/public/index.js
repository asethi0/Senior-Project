let id = id => document.getElementById(id);

if(document.loggedIn === "true") {
    id("login-button").hide();
}
