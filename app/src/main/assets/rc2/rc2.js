// 第2世代在留カード等・特定在留カード等のビューア。
// このファイルは libjeid-ios-app app/WebAssets/rc2/ にも同じものを置いているので、
// 変更したら両方を更新すること。
//
// 第二世代在留カード等仕様書 v1.1 3.3.4.4 / 3.3.4.7〜3.3.4.9 のコード定義
var SEX = {
    '1': '男',
    '2': '女',
    '3': '不詳'
};

var WORK_RESTRICTION = {
    '1': '就労制限なし',
    '2': '在留資格に基づく就労活動のみ可',
    '4': '指定書により指定された就労活動のみ可',
    '9': '就労不可'
};

var COMPREHENSIVE = {
    '0': '無し',
    '1': '許可(週28時間以内・風俗営業不可)',
    '2': '許可(週28時間以内・教育等の活動)'
};

var INDIVIDUAL = {
    '0': '無し',
    '1': '有り'
};

var UPDATE_STATUS = {
    '0': '無し',
    '1': '申請中'
};


// コードは券面表示(名前)だけを出す。コードそのものは読み取りログに出るので
// ビューアでは冗長になる。参照できないコードだけ「不明 (コード)」で明示する。
function decode(table, code) {
    if (code === undefined || code === null || code === '') {
        return '';
    }
    if (code in table) {
        return table[code];
    }
    // 仕様に定義のないコード。生の値だけを出すと意味が分からないため明示する
    return '不明 (' + code + ')';
}

// 出入国在留管理庁長官記載の有無。読み取り側は boolean で渡す
// (libjeid の RC2Others#hasCommissionerEntry。仕様上このフィールドは常に格納される)
function decodeCommissionerEntry(value) {
    if (value === undefined || value === null) {
        return '';
    }
    return value ? '出入国在留管理庁長官が記録' : '無し';
}

// libjeid の RC2Code (出入国在留管理庁のコード表) で変換した券面表示。
// 名前は読み取り側 (RC2ReaderTask / RCSReaderTask / iOS RC2ViewerData) が
// "<key>-name" で渡す。decode() と同じく名前だけを出し、
// コード表に無いコード (名前が来ない) だけ「不明 (コード)」で明示する。
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

// 在留期間は無期限(永住者など)の場合 "0000"、
// それ以外は YYMM(年月) もしくは DDD(日数)
function formatStayPeriod(value) {
    if (!value) {
        return '';
    }
    if (value === '0000') {
        return '無期限';
    }
    return value;
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
        // 在留資格・在留期間・許可・資格外活動許可欄は在留カードのみの項目
        var rows = document.getElementsByClassName('rc2-only');
        for (var i = 0; i < rows.length; i++) {
            rows[i].style.display = 'none';
        }
    }

    setText('rc2-card-number', data['rc2-card-number']);
    setText('rc2-birth-date', formatDate(data['rc2-birth-date']));
    setText('rc2-sex', decode(SEX, data['rc2-sex']));
    setText('rc2-nationality',
            decodeName(data['rc2-nationality-name'], data['rc2-nationality']));
    setText('rc2-status',
            decodeName(data['rc2-status-name'], data['rc2-status']));
    setText('rc2-work-restriction', decode(WORK_RESTRICTION, data['rc2-work-restriction']));
    setText('rc2-stay-period', formatStayPeriod(data['rc2-stay-period']));
    setText('rc2-stay-period-until', formatDate(data['rc2-stay-period-until']));
    setText('rc2-permission-type',
            decodeName(data['rc2-permission-type-name'], data['rc2-permission-type']));
    setText('rc2-permission-date', formatDate(data['rc2-permission-date']));
    setText('rc2-card-valid-until', formatDate(data['rc2-card-valid-until']));

    // 1歳未満の中長期在留者・特別永住者では顔画像が格納されない
    setImage('rc2-face-image', data['rc2-face-image']);
    setImage('rc2-name-image', data['rc2-name-image']);
    setImage('rc2-address-image', data['rc2-address-image']);

    setText('rc2-comprehensive', decode(COMPREHENSIVE, data['rc2-comprehensive']));
    setText('rc2-comprehensive-limit', formatDate(data['rc2-comprehensive-limit']));
    setText('rc2-individual', decode(INDIVIDUAL, data['rc2-individual']));
    setText('rc2-update-status', decode(UPDATE_STATUS, data['rc2-update-status']));
    setText('rc2-commissioner-entry', decodeCommissionerEntry(data['rc2-commissioner-entry']));
    setText('rc2-reserved', data['rc2-reserved']);

    if ('rc2-validation-result' in data) {
        // 真正性検証結果は VALID / INVALID_SIGNATURE / INVALID_CERTIFICATE の3パターン。
        var status = data['rc2-validation-result'];
        var icon = (status === 'VALID') ? 'verify-success.png' : 'verify-failed.png';
        document.getElementById('rc2-validation-result-icon').src = icon;
        document.getElementById('rc2-validation-result-text').textContent = status;
    }
}
