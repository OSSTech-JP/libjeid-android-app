package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import jp.co.osstech.libjeid.CardType;
import jp.co.osstech.libjeid.INTextAP;
import jp.co.osstech.libjeid.INVisualAP;
import jp.co.osstech.libjeid.InvalidPinException;
import jp.co.osstech.libjeid.JPKIAP;
import jp.co.osstech.libjeid.JPKICertificate;
import jp.co.osstech.libjeid.JeidReader;
import jp.co.osstech.libjeid.ValidationResult;
import jp.co.osstech.libjeid.in.INTextAttributes;
import jp.co.osstech.libjeid.in.INTextFiles;
import jp.co.osstech.libjeid.in.INTextMyNumber;
import jp.co.osstech.libjeid.in.INVisualEntries;
import jp.co.osstech.libjeid.in.INVisualFiles;
import jp.co.osstech.libjeid.in.INVisualMyNumber;
import jp.co.osstech.libjeid.util.BitmapARGB;
import org.json.JSONObject;

public class INReaderTask
    implements Runnable
{
    private static final String TAG = MainActivity.TAG;
    private final WeakReference<INReaderActivity> activityRef;
    private Tag nfcTag;
    private String pin;

    public INReaderTask(INReaderActivity activity, Tag nfcTag, String pin) {
        this.activityRef = new WeakReference<>(activity);
        this.nfcTag = nfcTag;
        this.pin = pin;
    }

    private void publishProgress(String msg) {
        INReaderActivity activity = activityRef.get();
        if (activity != null) {
            activity.print(msg);
        }
    }

    public void run() {
        Log.d(TAG, getClass().getSimpleName() + "#run()");
        INReaderActivity activity = activityRef.get();
        if (activity == null) {
            return;
        }
        activity.clear();
        publishProgress("# 読み取り開始、カードを離さないでください");

        if (pin.isEmpty() || pin.length() != 4) {
            publishProgress("4桁の暗証番号を入力してください。");
            return;
        }

        ProgressDialogFragment progress = new ProgressDialogFragment();
        activity.runOnUiThread(() -> {
            progress.show(activity.getSupportFragmentManager(), "progress");
        });

        try {
            long startTime = System.currentTimeMillis();
            JeidReader reader = new JeidReader(nfcTag);
            CardType type = reader.detectCardType();
            publishProgress("## カード種別" + type);
            publishProgress("CardType: " + type);
            if (type != CardType.IN) {
                publishProgress("マイナンバーカードではありません");
                return;
            }
            publishProgress("## 券面入力補助APから情報を取得します");
            INTextAP textAp = reader.selectINTextAP();
            try {
                textAp.verifyPin(pin);
            } catch (InvalidPinException e) {
                activity.showInvalidPinDialog(e);
                return;
            }
            long startReadTime = System.currentTimeMillis();
            INTextFiles textFiles = textAp.readFiles();
            long endReadTime = System.currentTimeMillis();
            long readTime = endReadTime - startReadTime;
            publishProgress("完了: " + readTime + "ms");
            JSONObject obj = new JSONObject();
            try {
                INTextMyNumber textMyNumber = textFiles.getMyNumber();
                obj.put("cardinfo-mynumber", textMyNumber.getMyNumber());
            } catch (FileNotFoundException | UnsupportedOperationException ue) {
                // 無償版では個人番号を取得出来ません。
            }
            publishProgress("## 4情報");
            INTextAttributes textAttrs = textFiles.getAttributes();
            obj.put("cardinfo-name", textAttrs.getName());
            obj.put("cardinfo-birth", textAttrs.getBirth());
            obj.put("cardinfo-sex", textAttrs.getSexString());
            obj.put("cardinfo-addr", textAttrs.getAddr());
            publishProgress(textAttrs.toString());

            try {
                publishProgress("## 券面入力補助APの真正性検証");
                ValidationResult validationResult = textFiles.validate();
                publishProgress("検証結果: " + validationResult.toString());
                obj.put("textap-validation-result", validationResult.isValid());
            } catch (UnsupportedOperationException ue) {
                // 無償版では真正性検証をサポートしていません。
            }

            publishProgress("## 券面APから情報を取得します");
            INVisualAP visualAp = reader.selectINVisualAP();
            visualAp.verifyPin(pin);

            startReadTime = System.currentTimeMillis();
            INVisualFiles visualFiles = visualAp.readFiles();
            endReadTime = System.currentTimeMillis();
            readTime = endReadTime - startReadTime;
            publishProgress("完了: " + readTime + "ms");

            INVisualEntries visualEntries = visualFiles.getEntries();
            String expire = visualEntries.getExpire();
            obj.put("cardinfo-expire", expire);
            obj.put("cardinfo-birth2", visualEntries.getBirth());
            obj.put("cardinfo-sex2", visualEntries.getSexString());
            obj.put("cardinfo-name-image", "data:image/png;base64,"
                    + Base64.encodeToString(visualEntries.getName(), Base64.DEFAULT));
            obj.put("cardinfo-address-image", "data:image/png;base64,"
                    + Base64.encodeToString(visualEntries.getAddr(), Base64.DEFAULT));

            publishProgress("## 写真のデコード");
            BitmapARGB argb = visualEntries.getPhotoBitmapARGB();
            Bitmap bitmap = Bitmap.createBitmap(argb.getData(),
                                                argb.getWidth(),
                                                argb.getHeight(),
                                                Bitmap.Config.ARGB_8888);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            byte[] jpeg = os.toByteArray();
            String src = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.DEFAULT);
            obj.put("cardinfo-photo", src);
            publishProgress("完了");

            try {
                publishProgress("## 券面APの真正性検証");
                ValidationResult validationResult = visualFiles.validate();
                publishProgress("検証結果: " + validationResult.toString());
                obj.put("visualap-validation-result", validationResult.isValid());
            } catch (UnsupportedOperationException ue) {
                // 無償版では真正性検証をサポートしていません。

            }
            try {
                INVisualMyNumber visualMyNumber = visualFiles.getMyNumber();
                obj.put("cardinfo-mynumber-image", "data:image/png;base64,"
                        + Base64.encodeToString(visualMyNumber.getMyNumber(), Base64.DEFAULT));
            } catch (FileNotFoundException | UnsupportedOperationException ue) {
                // 無償版では個人番号(画像)を取得できません。
            }
            publishProgress("## ユーザー認証用証明書の有効期限を取得します");
            JPKIAP jpkiAP = reader.selectJPKIAP();
            JPKICertificate cert = jpkiAP.getAuthCert();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
            String certExpireDate = sdf.format(cert.getNotAfter());
            obj.put("cardinfo-cert-expire", certExpireDate);
            publishProgress("完了");

            // Viewerを起動
            Intent intent = new Intent(activity, INViewerActivity.class);
            intent.putExtra("json", obj.toString());
            // バックグラウンドスレッドから直接startActivityを呼んでも動作するが、
            // UIスレッドから呼ぶことが推奨される
            activity.runOnUiThread(() -> activity.startActivity(intent));
        } catch (Exception e) {
            Log.e(TAG, getClass().getSimpleName() + "#run()", e);
            publishProgress("エラー: " + e);
        } finally {
            activity.runOnUiThread(() -> {
                progress.dismissAllowingStateLoss();
            });
        }
    }
}
