package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.nfc.Tag;
import android.util.Base64;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import jp.co.osstech.libjeid.InvalidPinException;
import jp.co.osstech.libjeid.JPKIAP;
import jp.co.osstech.libjeid.JPKISignature;
import jp.co.osstech.libjeid.JeidReader;
import jp.co.osstech.libjeid.util.Hex;

public class JPKISignTask
    implements Runnable
{
    private static final String TAG = MainActivity.TAG;
    private JPKISignActivity activity;
    private Tag nfcTag;
    private String pin;
    private byte[] input;

    public JPKISignTask(JPKISignActivity activity, Tag nfcTag) {
        this.activity = activity;
        this.nfcTag = nfcTag;
    }

    private void publishProgress(String msg) {
        this.activity.print(msg);
    }

    private void outputFile(JPKISignActivity activity, String filename, byte[] data)
        throws IOException {
        File file = new File(activity.getExternalFilesDir(null), filename);
        FileOutputStream writer = new FileOutputStream(file);
        writer.write(Base64.encode(data, Base64.DEFAULT));
        writer.close();
        MediaScannerConnection.scanFile(activity,
                                        new String[]{file.getPath()},
                                        null, null);
    }

    public void run() {
        Log.d(TAG, getClass().getSimpleName() + "#run()");
        pin = activity.getPin();
        input = activity.getText().getBytes();
        activity.hideKeyboard();
        activity.clear();

        String signAlgo = activity.getSignAlgo();
        int type = activity.getType();

        if (type == JPKIAP.TYPE_AUTH) {
            publishProgress("認証用署名を開始");
        } else {
            publishProgress("署名用署名を開始");
        }

        if (pin.isEmpty()) {
            publishProgress("暗証番号を入力してください。");
            return;
        }

        try {
            publishProgress("input: " + Hex.encode(input));

            publishProgress("write input file: input.txt");
            outputFile(activity, "input.txt", input);

            JeidReader reader = new JeidReader(nfcTag);
            JPKIAP jpki = reader.selectJPKIAP();

            X509Certificate cert;
            if (type == JPKIAP.TYPE_AUTH) {
                cert = jpki.getAuthCert();
            } else {
                jpki.verifySignPin(pin);
                cert = jpki.getSignCert();
            }
            byte[] certDer;
            try {
                certDer = cert.getEncoded();
            } catch (Exception e) {
                publishProgress("DERエンコードエラー");
                return;
            }
            publishProgress("write cert file: cert.txt");
            outputFile(activity, "cert.txt", certDer);

            publishProgress("Signature Algorithm: " + signAlgo);

            JPKISignature signature;
            byte[] signed;
            if (type == JPKIAP.TYPE_AUTH) {
                signature = jpki.getAuthSignature(signAlgo);
                signature.update(input);
                signed = signature.sign(pin);
            } else {
                signature = jpki.getSignSignature(signAlgo);
                signature.update(input);
                signed = signature.sign(pin);
            }
            //publishProgress("signed: " + Hex.encode(signed));
            publishProgress("write signed file: signed.txt");
            outputFile(activity, "signed.txt", signed);

            byte[] digest = signature.getDigest();
            publishProgress("digest: " + Hex.encode(digest));

            publishProgress("write digest file: digest.txt");
            outputFile(activity, "digest.txt", digest);

            publishProgress("署名完了");

            PublicKey pubkey = cert.getPublicKey();
            Signature verifier = Signature.getInstance(signAlgo);
            verifier.initVerify(pubkey);
            verifier.update(input);
            if (verifier.verify(signed)) {
                publishProgress("署名の検証: 成功");
            } else {
                publishProgress("署名の検証: 失敗");
            }

        } catch (NoSuchAlgorithmException e) {
            publishProgress("ダイジェストエラー");
        } catch (InvalidPinException e) {
            publishProgress("エラー: PINが間違っています。のこり: " + e.getCounter());
        } catch (IOException e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
        }
    }
}
