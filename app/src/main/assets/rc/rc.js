function render(json) {
    data = JSON.parse(json);
    if ('rc-front-image' in data) {
        document.getElementById("rc-card-front").src = data['rc-front-image'];
    }
    if ('rc-photo' in data) {
        document.getElementById("rc-photo").src = data['rc-photo'];
        document.getElementById("rc-photo").classList.add("img-exists");
    }
    if ('rc-card-type' in data) {
        switch(data['rc-card-type']) {
            case '1':
                document.getElementById("rc-labels").style.display = "block";
                document.getElementById("sprc-labels").style.display = "none";
                break;
            case '2':
                document.getElementById("rc-labels").style.display = "none";
                document.getElementById("sprc-labels").style.display = "block";
        }
    }
    if ('rc-validation-result' in data) {
        // 真正性検証結果は VALID / INVALID_SIGNATURE / INVALID_CERTIFICATE の3パターン。
        var status = data['rc-validation-result'];
        var icon = (status === 'VALID') ? "verify-success.png" : "verify-failed.png";
        document.getElementById("rc-validation-result-icon").src = icon;
        document.getElementById("rc-validation-result-text").textContent = status;
    }
}

