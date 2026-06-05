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
}

