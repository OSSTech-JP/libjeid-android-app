// 第2世代在留カード等・特定在留カード等のビューア。
// このファイルは libjeid-ios-app app/WebAssets/rc2/ にも同じものを置いているので、
// 変更したら両方を更新すること。
//
// libjeid が変換した券面表示。名前は読み取り側
// (RC2ReaderTask / RCSReaderTask / iOS RC2ViewerData) が "<key>-name" で渡す。
// 出入国在留管理庁のコード表(国籍・在留資格・許可の種類)も仕様書本文のコード定義
// (性別・就労制限の有無・資格外活動許可欄など)も、ライブラリ側の *Name() が
// 引き当てて未知のコードでは null を返すので、扱いは同じ。
// コードそのものは読み取りログに出るのでビューアでは名前だけを出し、
// 名前が来なかったコードだけ「不明 (コード)」で明示する。
function decodeName(name, code) {
    if (code === undefined || code === null || code === '') {
        return '';
    }
    if (!name) {
        return '不明 (' + code + ')';
    }
    return name;
}

// YYYYMMDD を YYYY年M月D日 に整形する
function formatDate(value) {
    if (!value || !/^[0-9]{8}$/.test(value)) {
        return value || '';
    }
    return parseInt(value.substr(0, 4), 10) + '年'
        + parseInt(value.substr(4, 2), 10) + '月'
        + parseInt(value.substr(6, 2), 10) + '日';
}

function setText(id, text) {
    var elem = document.getElementById(id);
    if (elem) {
        elem.textContent = text === undefined || text === null ? '' : text;
    }
}

function setImage(id, src) {
    var elem = document.getElementById(id);
    if (elem && src) {
        elem.src = src;
        elem.classList.add('img-exists');
    }
}

function render(json) {
    var data = JSON.parse(json);

    // カード種別 (第二世代/特定在留カード等仕様書 v1.1 3.3.4.2)
    //   "05" = 第2世代在留カード       "06" = 第2世代特別永住者証明書
    //   "07" = 特定在留カード          "08" = 特定特別永住者証明書
    // 特定在留カード等は個人番号カード上の在留APだが、記録内容は第2世代と共通のため
    // この画面を共用する。英語表記は仕様に定義が無いので基となる券種のものを使う。
    var cardType = data['rc2-card-type'];
    var isSprc = (cardType === '06' || cardType === '08');
    var isSpecified = (cardType === '07' || cardType === '08');
    if (isSprc || isSpecified) {
        var nameJp = document.getElementById('rc2-card-name-jp');
        var nameEn = document.getElementById('rc2-card-name-en');
        nameJp.textContent = (isSpecified ? '特定' : '')
            + (isSprc ? '特別永住者証明書' : '在留カード');
        nameEn.textContent = isSprc
            ? 'SPECIAL PERMANENT RESIDENT CERTIFICATE' : 'RESIDENCE CARD';
    }
    if (isSprc) {
        document.getElementById('rc2-header').classList.add('type-sprc');
        document.getElementById('rc2-card-name-jp').classList.add('type-sprc');
        document.getElementById('rc2-card-name-en').classList.add('type-sprc');
        // 在留資格・在留期間・許可・資格外活動許可欄は在留カードのみの項目。
        // 行だけでなく許可欄の見出し・ブロックごと隠す
        var rows = document.getElementsByClassName('rc2-only');
        for (var i = 0; i < rows.length; i++) {
            rows[i].style.display = 'none';
        }
        // 券面下端の一文は「このカードは」ではなく「この証明書は」
        setText('rc2-validity-noun', '証明書');
    }

    setText('rc2-card-number', data['rc2-card-number']);
    setText('rc2-birth-date', formatDate(data['rc2-birth-date']));
    setText('rc2-sex', decodeName(data['rc2-sex-name'], data['rc2-sex']));
    setText('rc2-nationality',
            decodeName(data['rc2-nationality-name'], data['rc2-nationality']));
    setText('rc2-status',
            decodeName(data['rc2-status-name'], data['rc2-status']));
    setText('rc2-work-restriction',
            decodeName(data['rc2-work-restriction-name'], data['rc2-work-restriction']));
    setText('rc2-stay-period',
            decodeName(data['rc2-stay-period-name'], data['rc2-stay-period']));
    setText('rc2-stay-period-expire-date', formatDate(data['rc2-stay-period-expire-date']));
    setText('rc2-permission-type',
            decodeName(data['rc2-permission-type-name'], data['rc2-permission-type']));
    setText('rc2-permission-date', formatDate(data['rc2-permission-date']));
    setText('rc2-card-expire-date', formatDate(data['rc2-card-expire-date']));

    // 1歳未満の中長期在留者・特別永住者では顔画像が格納されない
    setImage('rc2-face-image', data['rc2-face-image']);
    setImage('rc2-name-image', data['rc2-name-image']);
    setImage('rc2-address-image', data['rc2-address-image']);

    setText('rc2-comprehensive',
            decodeName(data['rc2-comprehensive-name'], data['rc2-comprehensive']));
    setText('rc2-comprehensive-limit', formatDate(data['rc2-comprehensive-limit']));
    setText('rc2-individual',
            decodeName(data['rc2-individual-name'], data['rc2-individual']));
    setText('rc2-update-status',
            decodeName(data['rc2-update-status-name'], data['rc2-update-status']));
    setText('rc2-commissioner-entry',
            decodeName(data['rc2-commissioner-entry-name'], data['rc2-commissioner-entry']));
    setText('rc2-reserved', data['rc2-reserved']);

    if ('rc2-validation-result' in data) {
        // 真正性検証結果は VALID / INVALID_SIGNATURE / INVALID_CERTIFICATE の3パターン。
        var status = data['rc2-validation-result'];
        var icon = (status === 'VALID') ? 'verify-success.png' : 'verify-failed.png';
        document.getElementById('rc2-validation-result-icon').src = icon;
        document.getElementById('rc2-validation-result-text').textContent = status;
    }
}
