package jp.co.osstech.jeidreader;

import android.nfc.Tag;
import android.util.Log;
import java.security.MessageDigest;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import jp.co.osstech.libjeid.InvalidPinException;
import jp.co.osstech.libjeid.JPKIAP;
import jp.co.osstech.libjeid.JPKICertificate;
import jp.co.osstech.libjeid.JeidReader;
import jp.co.osstech.libjeid.util.Hex;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.util.ASN1Dump;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.json.JSONObject;

public class JPKICertReaderTask
    implements Runnable
{
    private static final String TAG = MainActivity.TAG;
    private final WeakReference<JPKICertReaderActivity> activityRef;
    private Tag nfcTag;
    private String type;
    private String password;

    public JPKICertReaderTask(JPKICertReaderActivity activity, Tag nfcTag,
                              String type, String password) {
        this.activityRef = new WeakReference<>(activity);
        this.nfcTag = nfcTag;
        this.type = type;
        this.password = password;
    }

    private void publishProgress(String msg) {
        JPKICertReaderActivity activity = activityRef.get();
        if (activity != null) {
            activity.print(msg);
        }
    }

    @Override
    public void run() {
        Log.d(TAG, getClass().getSimpleName() + "#run()");
        JPKICertReaderActivity activity = activityRef.get();
        if (activity == null) {
            return;
        }
        activity.clear();
        publishProgress("# 読み取り開始、カードを離さないでください");

        // 読み取り中ダイアログを表示
        ProgressDialogFragment progress = new ProgressDialogFragment();
        activity.runOnUiThread(() -> {
            progress.show(activity.getSupportFragmentManager(), "progress");
        });

        try {
            JeidReader reader = new JeidReader(nfcTag);
            JPKIAP jpkiAP = reader.selectJPKIAP();
            JPKICertificate cert = null;
            switch (this.type) {
            case "AUTH":
                cert = jpkiAP.getAuthCert();
                break;
            case "AUTH_CA":
                cert = jpkiAP.getAuthCACert();
                break;
            case "SIGN":
                if (password.isEmpty()) {
                    publishProgress("パスワードを入力してください。");
                    return;
                }
                try {
                    jpkiAP.verifySignPin(password);
                    cert = jpkiAP.getSignCert();
                } catch (InvalidPinException e) {
                    activity.showInvalidPasswordDialog(e);
                    return;
                }
                break;
            case "SIGN_CA":
                cert = jpkiAP.getSignCACert();
                break;
            }

            if (cert == null) {
                return;
            }

            JSONObject obj = new JSONObject();
            obj.put("showcert-subject", cert.getSubjectX500Principal().toString());
            obj.put("showcert-issuer", cert.getIssuerX500Principal().toString());
            obj.put("showcert-not-before", cert.getNotBefore().toString());
            obj.put("showcert-not-after", cert.getNotAfter().toString());
            obj.put("showcert-version", Integer.toString(cert.getVersion()));
            obj.put("showcert-serial-number", cert.getSerialNumber().toString() + " (0x"
                    + cert.getSerialNumber().toString(16) + ")");

            Certificate x509Cert = Certificate.getInstance(cert.getEncoded());
            SubjectPublicKeyInfo spiObject = x509Cert.getSubjectPublicKeyInfo();
            obj.put("showcert-public-key-alg", cert.getPublicKey().getAlgorithm());
            obj.put("showcert-public-key-alg-params", spiObject.getAlgorithm().getParameters().toString());
            obj.put("showcert-public-key", Hex.encode(cert.getPublicKey().getEncoded(), ":"));

            if ("RSA".equalsIgnoreCase(cert.getPublicKey().getAlgorithm())) {
                RSAPublicKey rsaPublicKey = RSAPublicKey.getInstance(spiObject.parsePublicKey());
                obj.put("showcert-public-key-rsa-size", rsaPublicKey.getModulus().bitLength() + " bit");
                obj.put("showcert-public-key-rsa-modulus", Hex.encode(rsaPublicKey.getModulus().toByteArray(), ":"));
                obj.put("showcert-public-key-rsa-exponent", rsaPublicKey.getPublicExponent().toString() + " (0x"
                        + rsaPublicKey.getPublicExponent().toString(16) + ")");
            }

            obj.put("showcert-sig-alg", cert.getSigAlgName());
            obj.put("showcert-sig-alg-params", x509Cert.getSignatureAlgorithm().getParameters().toString());
            obj.put("showcert-signature", Hex.encode(cert.getSignature(), ":"));

            obj.put("showcert-fingerprint-sha1", Hex.encode(MessageDigest.getInstance("SHA-1").digest(cert.getEncoded()), ":"));
            obj.put("showcert-fingerprint-sha256", Hex.encode(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()), ":"));

            Set<String> criticalExtensionOIDs = cert.getCriticalExtensionOIDs();
            Set<String> extensionOIDs = new HashSet<>();
            extensionOIDs.addAll(criticalExtensionOIDs);
            extensionOIDs.addAll(cert.getNonCriticalExtensionOIDs());
            int unsupportedOidIndex = 0;
            for (String oid : extensionOIDs) {
                switch (oid) {
                case "2.5.29.14":
                    byte[] subjectKeyIdentifier = cert.getExtensionValue(oid);
                    if (subjectKeyIdentifier != null) {
                        obj.put("showcert-subject-key-identifier",
                                ASN1Dump.dumpAsString(
                                        SubjectKeyIdentifier.getInstance(
                                                JcaX509ExtensionUtils.parseExtensionValue(subjectKeyIdentifier)))
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.15":
                    boolean[] keyUsage = cert.getKeyUsage();
                    if (keyUsage != null && keyUsage.length != 0) {
                        obj.put("showcert-key-usage", keyUsageToString(keyUsage)
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.17":
                    byte[] subjectAlternativeName = cert.getExtensionValue(oid);
                    if (subjectAlternativeName != null) {
                        obj.put("showcert-subject-alt-name",
                                ASN1Dump.dumpAsString(
                                        GeneralNames.getInstance(
                                                JcaX509ExtensionUtils.parseExtensionValue(subjectAlternativeName)))
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.18":
                    byte[] issuerAlternativeName = cert.getExtensionValue(oid);
                    if (issuerAlternativeName != null) {
                        obj.put("showcert-issuer-alt-name",
                                ASN1Dump.dumpAsString(
                                        GeneralNames.getInstance(
                                                JcaX509ExtensionUtils.parseExtensionValue(issuerAlternativeName)))
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.19":
                    int basicConstraints = cert.getBasicConstraints();
                    if (basicConstraints != -1) {
                            obj.put("showcert-basic-constraints", Integer.toString(basicConstraints)
                                    + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.31":
                    byte[] crlDistributionPoint = cert.getExtensionValue(oid);
                    if (crlDistributionPoint != null) {
                        obj.put("showcert-crl-distribution-point",
                                ASN1Dump.dumpAsString(
                                        CRLDistPoint.getInstance(
                                                JcaX509ExtensionUtils.parseExtensionValue(crlDistributionPoint)))
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.32":
                    byte[] certificatePolicies = cert.getExtensionValue(oid);
                    if (certificatePolicies != null) {
                        obj.put("showcert-certificate-policies",
                                ASN1Dump.dumpAsString(
                                        CertificatePolicies.getInstance(
                                                JcaX509ExtensionUtils.parseExtensionValue(certificatePolicies)))
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.35":
                    byte[] authorityKeyIdentifier = cert.getExtensionValue(oid);
                    if (authorityKeyIdentifier != null) {
                        obj.put("showcert-authority-key-identifier",
                                ASN1Dump.dumpAsString(
                                        AuthorityKeyIdentifier.getInstance(
                                                JcaX509ExtensionUtils.parseExtensionValue(authorityKeyIdentifier)))
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "2.5.29.37":
                    List<String> extendedKeyUsage = cert.getExtendedKeyUsage();
                    if (extendedKeyUsage != null) {
                        obj.put("showcert-extended-key-usage",
                                extendedKeyUsageToString(extendedKeyUsage)
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                case "1.3.6.1.5.5.7.1.1":
                    byte[] authorityInformationAccess = cert.getExtensionValue(oid);
                    if (authorityInformationAccess != null) {
                        obj.put("showcert-authority-information-access",
                                ASN1Dump.dumpAsString(
                                        AuthorityInformationAccess.getInstance(
                                                JcaX509ExtensionUtils.parseExtensionValue(authorityInformationAccess)))
                                + " (" + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    }
                    break;
                default:
                    obj.put("showcert-unsupported-oid" + Integer.toString(unsupportedOidIndex), oid);
                    obj.put("showcert-unsupported-oid" + Integer.toString(unsupportedOidIndex) + "-value",
                            Hex.encode(cert.getExtensionValue(oid), ":") + " ("
                            + getCriticalLabel(criticalExtensionOIDs.contains(oid)) + ")");
                    unsupportedOidIndex++;
                    break;
                }
            }

            // ビューアは別Activityではなく読み取り画面内のFragmentとして表示する
            final String json = obj.toString();
            activity.runOnUiThread(() -> activity.showViewer(json));
        } catch (Exception e) {
            Log.e(TAG, "error at " + getClass().getSimpleName() + "#doInBackground()", e);
            publishProgress("エラー: カードを読み取れませんでした" + e.getMessage());
            return;
        } finally {
            activity.runOnUiThread(() -> {
                progress.dismissAllowingStateLoss();
            });
        }
    }

    private String keyUsageToString(final boolean[] keyUsage) {
        if (keyUsage == null || keyUsage.length != 9) {
            return "";
        }
        List<String> list = new ArrayList<>();
        if (keyUsage[0]) {
            list.add("電子署名");
        }
        if (keyUsage[1]) {
            list.add("否認防止");
        }
        if (keyUsage[2]) {
            list.add("鍵暗号");
        }
        if (keyUsage[3]) {
            list.add("データ暗号");
        }
        if (keyUsage[4]) {
            list.add("鍵交換");
        }
        if (keyUsage[5]) {
            list.add("電子証明書の検証");
        }
        if (keyUsage[6]) {
            list.add("CRLの署名検証");
        }
        if (keyUsage[7]) {
            list.add("鍵交換時のデータ暗号用");
        }
        if (keyUsage[8]) {
            list.add("鍵交換時のデータ複号用");
        }
        return listToString(list);
    }

    private String extendedKeyUsageToString(final List<String> extendedKeyUsage) {
        if (extendedKeyUsage == null) {
            return "";
        }
        List<String> list = new ArrayList<>();
        for (String oid : extendedKeyUsage) {
            if ("1.3.6.1.5.5.7.3.2".equals(oid)) {
                list.add("クライアント認証");
            } else {
                list.add(oid);
            }
        }
        return listToString(list);
    }

    private String getCriticalLabel(boolean isCritical) {
        if (isCritical) {
            return "Critical";
        } else {
            return "Non-critical";
        }
    }

    private String listToString(final List<String> list) {
        if (list == null || list.size() == 0) {
            return "null";
        }
        int i = 0;
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append('[');
        for (String str : list) {
            strBuilder.append(str);
            if (i < list.size() - 1) {
                strBuilder.append(", ");
            }
            i++;
        }
        strBuilder.append(']');
        return strBuilder.toString();
    }
}
