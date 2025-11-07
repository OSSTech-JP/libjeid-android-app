
function createRow(name, value) {
    const tr = document.createElement('tr');
    const td1 = document.createElement('td');
    td1.textContent = name;
    const td2 = document.createElement('td');
    if (typeof value === 'string') {
      td2.textContent = value;
    } else {
      td2.appendChild(value);
    }
    tr.appendChild(td1);
    tr.appendChild(td2);
    return tr;
}

function dlstr2elm(str) {
    const div = document.createElement('div');
    if (typeof str == "string") {
        return str;
    }
    for (var i=0; i<str.length; i++) {
        var c = str[i];
        if (c['type'] == 'text/plain') {
            let text = document.createTextNode(c['value']);
            div.appendChild(text)
        } else if (c['type'] == 'image/png') {
            let img = document.createElement('img');
            img.style = 'height: 1em; position: relative; top: 0.14em;';
            img.src = 'data:image/png;base64,' + c['value'];
            div.appendChild(img);
        } else if (c['type'] == 'image/x-missing') {
            // 欠字
            let text = document.createTextNode('？');
            div.appendChild(text)
        }
    }
    return div;
}

function full2half(str) {
    return str.replace(/[ａ-ｚ０-９（）]/g, function(s) {
        return String.fromCharCode(s.charCodeAt(0) - 0xFEE0);
    });
}

function render(json) {
    data = JSON.parse(json);
    if ('dl-name' in data) {
        document.getElementById("dl-name").innerHTML = '';
        document.getElementById("dl-name").appendChild(dlstr2elm(data['dl-name']));
    }
    if ('dl-birth' in data) {
        document.getElementById("dl-birth").textContent = data['dl-birth'] + '生';
    }
    if ('dl-addr' in data) {
        document.getElementById("dl-addr").innerHTML = '';
        document.getElementById("dl-addr").appendChild(dlstr2elm(data['dl-addr']));        
    }
    if ('dl-issue' in data) {
        document.getElementById("dl-issue").textContent = data['dl-issue'];
    }
    if ('dl-ref' in data) {
        document.getElementById("dl-ref").textContent = data['dl-ref'];
    }
    if ('dl-expire' in data) {
        document.getElementById("dl-expire").textContent = data['dl-expire'] + 'まで有効';
    }
    if ('dl-is-expired' in data) {
      if (data['dl-is-expired']) {
        document.getElementById("dl-is-expired").style.display ="block";
      }
    }
    if ('dl-color-class' in data) {
        var color;
        var display;
        if (data['dl-color-class'] == '優良') {
            color = 'dl-color-gold';
            display = 'inline';
        } else if (data['dl-color-class'] == '新規') {
            color = 'dl-color-green';
            display = 'none';
        } else {
            color = 'dl-color-blue';
            display = 'none';
        }
        document.getElementById("dl-expire").classList.add(color);
        document.getElementById("dl-color-class").style.display = display;
    }

    var elm = document.getElementById('dl-condition1');
    if ('dl-condition1' in data) {
        elm.textContent = full2half(data['dl-condition1']);
    } else {
        elm.textContent = '';
    }

    var elm = document.getElementById('dl-condition2');
    if ('dl-condition2' in data) {
        elm.textContent = full2half(data['dl-condition2']);
    } else {
        elm.textContent = '';
    }

    var elm = document.getElementById('dl-condition3');
    if ('dl-condition3' in data) {
        elm.textContent = full2half(data['dl-condition3']);
    } else {
        elm.textContent = '';
    }

    var elm = document.getElementById('dl-condition4');
    if ('dl-condition4' in data) {
        elm.textContent = full2half(data['dl-condition4']);
    } else {
        elm.textContent = '';
    }

    if ('dl-number' in data) {
        document.getElementById("dl-number").textContent = '第　' + data['dl-number'] + '　号';
    }
    if ('dl-sc' in data) {
        document.getElementById("dl-sc").textContent = data['dl-sc'];
    }
    if ('dl-photo' in data) {
        document.getElementById("dl-photo").src = data['dl-photo'];
    }

    if ('signature-valid' in data) {
        document.getElementById("dl-verified").style.display = "inline-block";
        if (data['signature-valid']) {
            document.getElementById("dl-verified").src = "verify-success.png";
        } else {
            document.getElementById("dl-verified").src = "verify-failed.png";
        }
    }else{
        document.getElementById("dl-verified").style.display = "none";
    }

    var elm = document.getElementById("dl-name-etc");
    if ('dl-name' in data) {
        elm.appendChild(createRow('氏名', dlstr2elm(data['dl-name'])));
    }
    if ('dl-kana' in data) {
        elm.appendChild(createRow('呼び名(カナ)', dlstr2elm(data['dl-kana'])));
    }
    if ('dl-addr' in data) {
        elm.appendChild(createRow('住所', dlstr2elm(data['dl-addr'])));
    }
    if ('dl-registered-domicile' in data) {
        elm.appendChild(createRow('本籍', dlstr2elm(data['dl-registered-domicile'])));
    }

    var elm = document.getElementById("dl-categories");
    if ('dl-categories' in data) {
        var categories = data['dl-categories'];
        var traction = false;
        var traction2 = false;
        for(var i=0; i<categories.length; i++) {
            var cat = categories[i];
            if (cat['licensed']) {
                elm.appendChild(createRow(cat['name'], cat['date']));
            }

            switch(cat['tag']) {
            case 0x22:
                document.getElementById("dl-cat-22-date").textContent = cat['date'];
                break;
            case 0x23:
                document.getElementById("dl-cat-23-date").textContent = cat['date'];
                break;
            case 0x24:
                document.getElementById("dl-cat-24-date").textContent = cat['date'];
                break;
            case 0x25:
            case 0x26:
            case 0x27:
            case 0x28:
            case 0x29:
            case 0x2a:
            case 0x2b:
            case 0x2d:
            case 0x2e:
            case 0x2f:
            case 0x31:
            case 0x32:
            case 0x33:
                if(cat['licensed']){
                    var id = 'dl-cat-' + cat['tag'].toString(16) + '-text';
                    var catElm = document.getElementById(id);
                    if (catElm) {
                        catElm.style.display = "block";
                    }
                }
                break;
            case 0x2c:
                traction = cat['licensed'];
                break;
            case 0x30:
                traction2 = cat['licensed'];
                break;
            }
        }
        if (traction || traction2) {
            var catElm = document.getElementById('dl-cat-30-text');
            if (catElm) {
                catElm.style.display = "block";
                if (traction && !traction2) {
                    catElm.className = "dl-cat-cell-2";
                    catElm.textContent = "け引";
                } else if (!traction && traction2) {
                    catElm.className = "dl-cat-cell-3";
                    catElm.textContent = "け引二";
                } else if (traction && traction2) {
                    catElm.className = "dl-cat-cell-3";
                    catElm.innerHTML = "引<div class=\"dl-cat-dot\"></div>引二";
                }
            }
        }
    }

    const signature = document.getElementById("signature");
    if ('signature-valid' in data) {
        signature.appendChild(createRow('電子署名', data['signature-valid']?'有効':'無効'));
    }
    if ('signature-issuer' in data) {
        signature.appendChild(createRow('発行者', data['signature-issuer']));
    }
    if ('signature-subject' in data) {
        signature.appendChild(createRow('主体者', data['signature-subject']));
    }
    if ('signature-ski' in data) {
        signature.appendChild(createRow('主体者鍵識別子', data['signature-ski']));
    }

    var elm = document.getElementById('dl-changes');
    if ('dl-changes' in data) {
        var changes = data['dl-changes'];
        changes.sort(function(a, b) {
            if (a['ad'] > b['ad']) {
                return 1;
            } else if (a['ad'] < b['ad']) {
                return -1;
            } else {
                return 0;
            }
        });
        for(var i=0; i<changes.length; i++) {
            var label = changes[i]['label'];
            var value = changes[i]['value'];
            var psc = changes[i]['psc'];
            var date = changes[i]['date'];
            elm.appendChild(document.createTextNode(date + " " + label + "："));
            if (i == 0) {
                elm.appendChild(document.createElement('br'));
            }
            elm.appendChild(dlstr2elm(value));
            const seal = document.createElement('div');
            seal.className = "dl-changes-seal";
            seal.textContent = psc;
            elm.appendChild(seal);
            elm.appendChild(document.createElement('br'));
        }
    }
}

