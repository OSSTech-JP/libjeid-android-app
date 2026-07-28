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

var COMMISSIONER_MARK = {
    '0': '無し',
    '1': '出入国在留管理庁長官が記録'
};

function decode(table, code) {
    if (code === undefined || code === null || code === '') {
        return '';
    }
    if (code in table) {
        return table[code] + ' (' + code + ')';
    }
    return code;
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
function formatPeriod(value) {
    if (!value) {
        return '';
    }
    if (value === '0000') {
        return '無期限 (0000)';
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

    // "05" = 第2世代在留カード, "06" = 第2世代特別永住者証明書
    var isSprc = (data['rc2-card-type'] === '06');
    if (isSprc) {
        document.getElementById('rc2-header').classList.add('type-sprc');
        var nameJp = document.getElementById('rc2-card-name-jp');
        var nameEn = document.getElementById('rc2-card-name-en');
        nameJp.textContent = '特別永住者証明書';
        nameJp.classList.add('type-sprc');
        nameEn.textContent = 'SPECIAL PERMANENT RESIDENT CERTIFICATE';
        nameEn.classList.add('type-sprc');
        // 在留資格・在留期間・許可・資格外活動許可欄は在留カードのみの項目
        var rows = document.getElementsByClassName('rc2-only');
        for (var i = 0; i < rows.length; i++) {
            rows[i].style.display = 'none';
        }
    }

    setText('rc2-card-number', data['rc2-card-number']);
    setText('rc2-birth-date', formatDate(data['rc2-birth-date']));
    setText('rc2-sex', decode(SEX, data['rc2-sex']));
    setText('rc2-nationality', data['rc2-nationality']);
    setText('rc2-status', data['rc2-status']);
    setText('rc2-work-restriction', decode(WORK_RESTRICTION, data['rc2-work-restriction']));
    setText('rc2-period', formatPeriod(data['rc2-period']));
    setText('rc2-period-until', formatDate(data['rc2-period-until']));
    setText('rc2-permit-category', data['rc2-permit-category']);
    setText('rc2-permit-date', formatDate(data['rc2-permit-date']));
    setText('rc2-valid-until', formatDate(data['rc2-valid-until']));

    // 1歳未満の中長期在留者・特別永住者では顔画像が格納されない
    setImage('rc2-photo', data['rc2-photo']);
    setImage('rc2-name-image', data['rc2-name-image']);
    setImage('rc2-address-image', data['rc2-address-image']);

    setText('rc2-comprehensive', decode(COMPREHENSIVE, data['rc2-comprehensive']));
    setText('rc2-comprehensive-limit', formatDate(data['rc2-comprehensive-limit']));
    setText('rc2-individual', decode(INDIVIDUAL, data['rc2-individual']));
    setText('rc2-update-status', decode(UPDATE_STATUS, data['rc2-update-status']));
    setText('rc2-commissioner-mark', decode(COMMISSIONER_MARK, data['rc2-commissioner-mark']));
    setText('rc2-reserved', data['rc2-reserved']);

    if ('rc2-validation-result' in data) {
        // 真正性検証結果は VALID / INVALID_SIGNATURE / INVALID_CERTIFICATE の3パターン。
        var status = data['rc2-validation-result'];
        var icon = (status === 'VALID') ? 'verify-success.png' : 'verify-failed.png';
        document.getElementById('rc2-validation-result-icon').src = icon;
        document.getElementById('rc2-validation-result-text').textContent = status;
    }
}
