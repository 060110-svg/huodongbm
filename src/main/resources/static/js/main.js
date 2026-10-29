function goBack(btn) {
    var home = btn.getAttribute('data-home') || '/home';
    if (window.history.length > 1 && document.referrer.indexOf(window.location.host) !== -1) {
        window.history.back();
    } else {
        window.location.href = home;
    }
}