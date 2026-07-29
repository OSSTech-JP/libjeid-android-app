
var testData = {
    'showcert-subject': 'CN=XXXXXXXXXXXXXXXXXXXXX, C=JP',
    'showcert-issuer': 'OU=Japan Agency for Local Authority Information Systems, OU=JPKI for user authentication, O=JPKI, C=JP',
    'showcert-not-before': 'Thu Jul 02 00:00:00 GMT+9:00 2016',
    'showcert-not-after': 'Mon Jun 01 23:59:59 GMT+9:00 2020',
    'showcert-serial-number': 'XXXXXXX (0xXXXXXX)',
    'showcert-version': '3',
    'showcert-public-key-alg': 'RSA',
    'showcert-public-key-alg-params': 'NULL',
    'showcert-public-key-rsa-size': '2048 bit',
    'showcert-public-key-rsa-modulus': '(RSA モジュラスの内容)',
    'showcert-public-key-rsa-exponent': '65537 (0x10001)', 
    'showcert-public-key': '(公開鍵の内容)',
    'showcert-subject-key-identifier': '(サブジェクトキー識別子の内容)',
    'showcert-key-usage': '(キー使用法の内容)',
    'showcert-subject-alt-name': '(サブジェクト代替名の内容)',
    'showcert-issuer-alt-name': '(発行者の別名の内容)',
    'showcert-basic-constraints': '(基本制限の内容)',
    'showcert-crl-distribution-point': '(CRL配布ポイントの内容)',
    'showcert-certificate-policies': '(証明書ポリシーの内容)',
    'showcert-authority-key-identifier': '(機関キー識別子の内容)',
    'showcert-extended-key-usage': '(拡張キー使用法の内容)',
    'showcert-authority-information-access': '(機関情報アクセスの内容)',
    'showcert-sig-alg': 'SHA256WITHRSA',
    'showcert-sig-alg-params': 'NULL',
    'showcert-fingerprint-sha1': '(SHA1フィンガープリントの内容)',
    'showcert-signature': 'XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX(以下略)',
    'showcert-fingerprint-sha1': '(SHA1フィンガープリントの内容)',
    'showcert-fingerprint-sha256': '(SHA256フィンガープリントの内容)',
    'showcert-unsupported-oid0': '2.5.29.XX',
    'showcert-unsupported-oid0-value': '(2.5.29.XXの内容)'
};

window.onload = function() {
    addMessage("onload");
    var json = JSON.stringify(testData);
    render(json);
}
