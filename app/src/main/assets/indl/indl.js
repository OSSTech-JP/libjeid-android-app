
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

function render(json) {
    data = JSON.parse(json);
    console.log(data);
    const entries = document.getElementById("entries");
    if ('color-class' in data) {
        entries.appendChild(createRow('免許の色区分', data['color-class']));
    }
    if ('expire' in data) {
        entries.appendChild(createRow('有効期限', data['expire']));
    }
    if ('license-number' in data) {
        entries.appendChild(createRow('免許の番号', data['license-number']));
    }
    if ('conditions' in data) {
        const conditions = data['conditions'];
        const ul = document.createElement('ul');
        for (let i=0; i < conditions.length; i++) {
            const li = document.createElement('li');
            li.textContent = conditions[i];
            ul.appendChild(li);
        }
        entries.appendChild(createRow('免許の条件', ul));
    }
    if ('photo' in data) {
        document.getElementById("photo").src = 'data:image/png;base64,' + data['photo'];
    }
    const categories = document.getElementById("categories");
    if ('categories' in data) {
        const cat = data['categories'];
        for (let i=0; i < cat.length; i++) {
            if (cat[i].licensed) {
                if ('date' in cat[i]) {
                    categories.appendChild(createRow(cat[i].name, cat[i].date));
                } else {
                    categories.appendChild(createRow(cat[i].name, '保有'));
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
}

