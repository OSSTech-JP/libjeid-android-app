
function render(json) {
    data = JSON.parse(json);
    if ('cardinfo-name' in data) {
        document.getElementById("cardinfo-name").textContent = data['cardinfo-name'];
        document.getElementById("cardinfo-backside-name").textContent = data['cardinfo-name'];
        document.getElementById("cardinfo-hojo-name").textContent = data['cardinfo-name'];
    }
    if ('cardinfo-birth' in data) {
        var birth = data['cardinfo-birth'];
        var year = birth / 10000 | 0;
        var month = birth % 10000 / 100 | 0;
        var day = birth % 100;
        var eraYear;
        var wareki;
        if (18680908 <= birth && birth < 19120730) {
            eraYear = '明治' + (year - 1867) + '年';
        } else if (19120730 <= birth && birth < 19261225) {
            eraYear = '大正' + (year - 1911) + '年';
        } else if (19261225 <= birth && birth < 19890108) {
            eraYear = '昭和' + (year - 1925) + '年';
        } else if (19890108 <= birth && birth < 20190501) {
            eraYear = '平成' + (year - 1988) + '年';
        } else {
            eraYear = '令和' + (year - 2018) + '年';
        }
        var wareki = eraYear + month + '月' + day + '日';
        wareki = wareki.replace(/(\D)1年/g, '$1元年')
                       .replace(/(\D)(\d)年/g, '$1 $2年')
                       .replace(/(\D)(\d)月/g, '$1 $2月')
                       .replace(/(\D)(\d)日/g, '$1 $2日');
        document.getElementById("cardinfo-birth").textContent = wareki + '生';
        document.getElementById("cardinfo-backside-birth").textContent = wareki + '生';
        document.getElementById("cardinfo-hojo-birth").textContent = birth;
    }
    if ('cardinfo-addr' in data) {
        document.getElementById("cardinfo-addr").textContent = data['cardinfo-addr'];
        document.getElementById("cardinfo-hojo-address").textContent = data['cardinfo-addr'];
    }
    if ('cardinfo-sex' in data) {
        var sex = data['cardinfo-sex'];
        var elm = document.getElementById("cardinfo-sex");
        if (sex == '男' || sex == '男性') {
            elm.textContent = '男';
        } else if (sex == '女' || sex == '女性') {
            elm.textContent = '女';
        } else if (sex == '不明') {
            elm.textContent = '不明';
        } else if (sex == '適用不能') {
            elm.textContent = '不明';
        } else {
            elm.textContent = '不明';
        }
        document.getElementById("cardinfo-hojo-sex").textContent = sex;
    }
    if ('cardinfo-mynumber' in data) {
        var mynumber = data['cardinfo-mynumber'];
        document.getElementById("cardinfo-mynumber-cell-1").textContent = mynumber.substr(0, 4);
        document.getElementById("cardinfo-mynumber-cell-2").textContent = mynumber.substr(4, 4);
        document.getElementById("cardinfo-mynumber-cell-3").textContent = mynumber.substr(8, 4);
        document.getElementById("cardinfo-hojo-mynumber").textContent = mynumber;
    }
    if ('textap-validation-result' in data) {
        document.getElementById("textap-validation-result").textContent = data['textap-validation-result'];
    }
    if ('cardinfo-cert-expire' in data) {
        var certExpire = data['cardinfo-cert-expire'];
        var year = certExpire.substr(0, 4);
        var month = certExpire.substr(4, 2);
        var day = certExpire.substr(6, 2);
        document.getElementById("cardinfo-cert-expire").textContent = year + "年" + month + "月" + day + "日";
    }
    if ('cardinfo-expire' in data) {
        var expire = data['cardinfo-expire'];
        var year = expire.substr(0, 4);
        var month = expire.substr(4, 2);
        var day = expire.substr(6, 2);
        var expireDate = year + "年" + month + "月" + day + "日";
        expireDate = expireDate.replace(/年0(\d)月/g, '年 $1月')
                               .replace(/月0(\d)日/g, '月 $1日');
        document.getElementById("cardinfo-expire").textContent = expireDate + "まで有効";
        document.getElementById("cardinfo-kenmen-expire").textContent = expire;
    }
    if ('cardinfo-birth2' in data) {
        document.getElementById("cardinfo-kenmen-birth").textContent = data['cardinfo-birth2'];
    }
    if ('cardinfo-sex2' in data) {
        document.getElementById("cardinfo-kenmen-sex").textContent = data['cardinfo-sex2'];
    }
    if ('cardinfo-photo' in data) {
        document.getElementById("cardinfo-photo").src = data['cardinfo-photo'];
        document.getElementById("cardinfo-kenmen-photo").src = data['cardinfo-photo'];
    }
    if ('cardinfo-name-image' in data) {
        document.getElementById("cardinfo-kenmen-name").src = data['cardinfo-name-image'];
    }
    if ('cardinfo-address-image' in data) {
        document.getElementById("cardinfo-kenmen-address").src = data['cardinfo-address-image'];
    }
    if ('cardinfo-mynumber-image' in data) {
        document.getElementById("cardinfo-kenmen-mynumber").src = data['cardinfo-mynumber-image'];
    }
    if ('visualap-validation-result' in data) {
        document.getElementById("visualap-validation-result").textContent = data['visualap-validation-result'];
    }
}

