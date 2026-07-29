
var testData = {
    'color-class': '優良',
    'expire': '平成24年07月01日',
    'conditions': ['眼鏡等','中型車は中型車（８ｔ）に限る'],
    'license-number': '123456789000',
    'categories': [
        {"tag":0x22,"name":"二・小・原","date":"平成20年04月01日","licensed":true},
        {"tag":0x23,"name":"他","date":"平成20年04月01日","licensed":true},
        {"tag":0x24,"name":"二種","date":"平成00年00月00日","licensed":false},
        {"tag":0x25,"name":"大型","date":"平成00年00月00日","licensed":false},
        {"tag":0x26,"name":"普通","date":"平成00年00月00日","licensed":false},
        {"tag":0x27,"name":"大特","date":"平成00年00月00日","licensed":false},
        {"tag":0x28,"name":"大自二","date":"平成00年00月00日","licensed":false},
        {"tag":0x29,"name":"普自二","date":"平成20年04月01日","licensed":true},
        {"tag":0x2A,"name":"小特","date":"平成00年00月00日","licensed":false},
        {"tag":0x2B,"name":"原付","date":"平成00年00月00日","licensed":false},
        {"tag":0x2C,"name":"け引","date":"平成00年00月00日","licensed":false},
        {"tag":0x2D,"name":"大二","date":"平成00年00月00日","licensed":false},
        {"tag":0x2E,"name":"普二","date":"平成00年00月00日","licensed":false},
        {"tag":0x2F,"name":"大特二","date":"平成00年00月00日","licensed":false},
        {"tag":0x30,"name":"け引二","date":"平成00年00月00日","licensed":false},
        {"tag":0x31,"name":"中型","date":"平成20年04月01日","licensed":true},
        {"tag":0x32,"name":"中二","date":"平成00年00月00日","licensed":false},
        {"tag":0x33,"name":"準中型","date":"平成00年00月00日","licensed":false}
    ],
    'photo': "/9j/4AAQSkZJRgABAQIAHAAcAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wAALCADwAMABAREA/8QAFwABAQEBAAAAAAAAAAAAAAAAAAECA//EAB4QAQEBAAICAwEAAAAAAAAAAAABEQIxEkEhUWFx/9oACAEBAAA/AKAKAAAAAACAoCJ5RPI8vxfKLsUAEUARREvL6Z2sigEtjXk0KACKCW4xbqCCgILLjcutAAAluOdu0BBQEFGuN9VtFAEYt2sqCCgIKDcuqoAxyvpgVqTVvH6YRRZNSzEFEWXK6CgOd7ZVqTW1ZvHXPMBvj7as1ysxFEV0nSgI5osdZMmKCWS9sXjnTLfHptLNc7MQRW+PTSKJeq5o6cJ7bGbTyWXVSyUkyYmpta7ZvH6YQdOPTQgXpzR2kyKJkFAQFZs3+uaOnHpoBL05rxny6KCKAAgzyntzdOPSioObfH20Beq5y2OkuxQc+XcXjyu5WgS9VydOPSqAmQkyKAx41uTJigxym/Jx4+60AnjPpQAABQABAAEUAAFBFBAAAAAFATMUEAvwz5T9WXYoAOd7bl2KKAACDHK+mW500IqKxyntJcroKDHV/F8ok+brYDNuOStzpVQURzsxZyz49OighkFBLccrdRW+PSqAIlmsLLY3OUv40ACJbjn2IrXHttFRQEs1m8b/AFJG4ppoDNZwy/R41fFrJAUABEGjDDA6ZFFBAUASoNStAnTNQUUBFAEQBuXVGLdQFUAAASoALtQAFUAABKgAAAKCgiooyAAAAsUBFBFZAAAAWKiooAiAAAALFQV//9k=",
    'signature-valid': true,
    'signature-issuer': "cn=issuer",
    'signature-subject': "cn=subject",
    'signature-ski': "SKI",
};

window.onload = function() {
    //log("onload");
    var json = JSON.stringify(testData);
    render(json);
}
