var monthMap = {
    '01': 'JAN', '02': 'FEB', '03': 'MAR', '04': 'APR',
    '05': 'MAY', '06': 'JUN', '07': 'JUL', '08': 'AUG',
    '09': 'SEP', '10': 'OCT', '11': 'NOV', '12': 'DEC'
}

function render(json) {
    data = JSON.parse(json);
    if ('ep-type' in data) {
        var type = data['ep-type'].replace(/</g, '');
        document.getElementById("ep-type").textContent = type;
    }
    if ('ep-issuing-country' in data) {
        var issuerCountry = data['ep-issuing-country'];
        document.getElementById("ep-issuing-country").textContent = issuerCountry;
    }
    if ('ep-passport-number' in data) {
        var passportNumber = data['ep-passport-number'];
        document.getElementById("ep-passport-number").textContent = passportNumber;
    }
    if ('ep-surname' in data) {
        document.getElementById("ep-surname").textContent = data['ep-surname'];
    }
    if ('ep-given-name' in data) {
        document.getElementById("ep-given-name").textContent = data['ep-given-name'];
    }
    if ('ep-nationality' in data) {
        if (data['ep-nationality'] == 'JPN') {
            document.getElementById("ep-nationality").textContent = 'JAPAN';
        } else {
            document.getElementById("ep-nationality").textContent = data['ep-nationality'];
        }
    }
    if ('ep-date-of-birth' in data) {
        var birthYear = data['ep-date-of-birth'].substr(0, 2);
        var birthMonth = data['ep-date-of-birth'].substr(2, 2);
        var birthDay = data['ep-date-of-birth'].substr(4, 2);
        document.getElementById("ep-date-of-birth").textContent
                = birthDay + " " + monthMap[birthMonth] + " XX" + birthYear;
    }
    if ('ep-sex' in data) {
        document.getElementById("ep-sex").textContent = data['ep-sex'];
    }
    if ('ep-date-of-expiry' in data) {
        var expiryYear = "20" + data['ep-date-of-expiry'].substr(0, 2);
        var expiryMonth = data['ep-date-of-expiry'].substr(2, 2);
        var expiryDay = data['ep-date-of-expiry'].substr(4, 2);
        document.getElementById("ep-date-of-expiry").textContent
                = expiryDay + " " + monthMap[expiryMonth] + " " + expiryYear;
        if (passportNumber && issuerCountry == 'JPN') {
            var initial = passportNumber.substr(0, 1);
            if (initial == "M" || initial == "N") {
                document.getElementById("ep-date-of-issue").textContent
                        = expiryDay + " " + monthMap[expiryMonth] + " " + (expiryYear - 5);
            } else if (initial == "T") {
                document.getElementById("ep-date-of-issue").textContent
                        = expiryDay + " " + monthMap[expiryMonth] + " " + (expiryYear - 10);
            }
        }
    }
    if ('ep-mrz' in data) {
        var mrz = data['ep-mrz'];
        var half = mrz.length / 2 | 0;
        var container = document.getElementById("ep-mrz");
        container.textContent = '';
        [mrz.substr(0, half), mrz.substr(half)].forEach(function(lineStr) {
            var line = document.createElement('div');
            line.className = 'ep-mrz-line';
            for (var i = 0; i < lineStr.length; i++) {
                var ch = document.createElement('span');
                ch.textContent = lineStr.charAt(i);
                line.appendChild(ch);
            }
            container.appendChild(line);
        });
    }
    if ('ep-photo' in data) {
        document.getElementById("ep-photo").src = data['ep-photo'];
    }

    var items = ['ep-ac-result', 'ep-aa-result', 'ep-pa-result'];
    for (var i = 0; i < items.length; i++) {
        if (items[i] in data) {
            if (data[items[i]]) {
                document.getElementById(items[i]).src = 'verify-success.png';
            } else {
                document.getElementById(items[i]).src = 'verify-failed.png';
            }
        }
    }
    if ('ep-ac-method' in data) {
        document.getElementById('ep-ac-method').textContent = data['ep-ac-method'];
    }
}

