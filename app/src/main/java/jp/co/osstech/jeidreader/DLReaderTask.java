package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import jp.co.osstech.libjeid.*;
import jp.co.osstech.libjeid.dl.*;
import jp.co.osstech.libjeid.util.BitmapARGB;
import jp.co.osstech.libjeid.util.Hex;
import org.json.JSONArray;
import org.json.JSONObject;

public class DLReaderTask
    implements Runnable
{
    private static final String TAG = MainActivity.TAG;
    private static final String DPIN = "****";
    private Tag nfcTag;
    private String pin1;
    private String pin2;
    private DLReaderActivity activity;

    public DLReaderTask(DLReaderActivity activity,
                        Tag nfcTag) {
        this.activity = activity;
        this.nfcTag = nfcTag;
    }

    private void publishProgress(String msg) {
        this.activity.print(msg);
    }

    public void run() {
        Log.d(TAG, getClass().getSimpleName() + "#run()");
        this.activity.clear();
        pin1 = activity.getPin1();
        pin2 = activity.getPin2();
        activity.hideKeyboard();
        publishProgress("# 読み取り開始、カードを離さないでください");
        // 読み取り中ダイアログを表示
        ProgressDialogFragment progress = new ProgressDialogFragment();
        progress.show(activity.getSupportFragmentManager(), "progress");

        try {
            JeidReader reader = new JeidReader(this.nfcTag);
            publishProgress("## 運転免許証の読み取り開始");
            CardType type = reader.detectCardType();
            publishProgress("CardType: " + type);
            if (type != CardType.DL) {
                publishProgress("運転免許証ではありません");
                return;
            }
            DriverLicenseAP ap = reader.selectDriverLicenseAP();

            // PINを入力せず共通データ要素を読み出します
            DriverLicenseCommonData commonData = ap.readCommonData();
            publishProgress("## 共通データ要素");
            publishProgress(commonData.toString());
            DLPinSetting pinSetting = ap.readPinSetting();
            publishProgress("## 暗証番号(PIN)設定");
            publishProgress(pinSetting.toString());

            if (pin1.isEmpty()) {
                publishProgress("暗証番号1を入力してください");
                return;
            }
            if (!pinSetting.isPinSet()) {
                publishProgress("暗証番号(PIN)設定がfalseのため、デフォルトPINの「****」を暗証番号として使用します\n");
                pin1 = DPIN;
            }

            try {
                ap.verifyPin1(pin1);
            } catch (InvalidPinException e) {
                activity.showInvalidPinDialog("暗証番号1", e);
                return;
            }

            if (!pin2.isEmpty()) {
                if (!pinSetting.isPinSet()) {
                    pin2 = DPIN;
                }
                try {
                    ap.verifyPin2(pin2);
                } catch (InvalidPinException e) {
                    activity.showInvalidPinDialog("暗証番号2", e);
                    return;
                }
            }
            // PINを入力した後、DriverLicenseAP#readFiles()を実行すると、
            // 入力されたPINで読み出し可能なファイルをすべて読み出します。
            // PIN1のみを入力した場合、PIN2の入力が必要なファイル(本籍など)は読み出しません。
            DriverLicenseFiles files = ap.readFiles();

            // 券面情報の取得
            DriverLicenseEntries entries = files.getEntries();
            // 外字の取得
            DriverLicenseExternalCharactors extChars = files.getExternalCharactors();
            JSONObject obj = new JSONObject();
            obj.put("dl-name", new JSONArray(entries.getName().toJSON()));
            obj.put("dl-kana", entries.getKana());
            DLDate birthDate = entries.getBirthDate();
            if (birthDate != null) {
                obj.put("dl-birth", birthDate.toString());
            }
            obj.put("dl-addr", new JSONArray(entries.getAddr().toJSON()));
            DLDate issueDate = entries.getIssueDate();
            if (issueDate != null) {
                obj.put("dl-issue", issueDate.toString());
            }
            obj.put("dl-ref", entries.getRefNumber());
            obj.put("dl-color-class", entries.getColorClass());
            DLDate expireDate = entries.getExpireDate();
            if (expireDate != null) {
                obj.put("dl-expire", expireDate.toString());
                Calendar expireCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
                // 有効期限が1日までの場合、2日になった時点で有効期限切れとなる
                expireCal.setTime(expireDate.toDate());
                expireCal.add(Calendar.DAY_OF_MONTH, 1);
                Date now = new Date();
                boolean isExpired = now.compareTo(expireCal.getTime()) >= 0;
                obj.put("dl-is-expired",  isExpired);
            }
            obj.put("dl-number", entries.getLicenseNumber());

            String pscName = entries.getPscName();
            if (pscName != null) {
                obj.put("dl-sc", pscName.replace("公安委員会", ""));
            }
            int i = 1;
            for (String condition : entries.getConditions()) {
                obj.put(String.format(Locale.US, "dl-condition%d", i++), condition);
            }
            JSONArray categories = new JSONArray();
            for (DriverLicenseCategory category : entries.getCategories()) {
                JSONObject categoryObj = new JSONObject();
                categoryObj.put("tag", category.getTag());
                categoryObj.put("name", category.getName());
                categoryObj.put("date", category.getDate().toString());
                categoryObj.put("licensed", category.isLicensed());
                categories.put(categoryObj);
            }
            obj.put("dl-categories", categories);

            publishProgress(entries.toString());

            // 記載事項変更等(本籍除く）を取得
            DriverLicenseChangedEntries changedEntries = files.getChangedEntries();
            publishProgress(changedEntries.toString());

            JSONArray changesObj = new JSONArray();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
            if (changedEntries.isChanged()) {
                for (DriverLicenseChangedEntry entry : changedEntries.getNewAddrList()) {
                    JSONObject entryObj = new JSONObject();
                    entryObj.put("label", "新住所");
                    entryObj.put("date", entry.getDate().toString());
                    entryObj.put("ad", sdf.format(entry.getDate().toDate()));
                    entryObj.put("value", new JSONArray(entry.getValue().toJSON()));
                    entryObj.put("psc", entry.getPsc());
                    changesObj.put(entryObj);
                }
                for (DriverLicenseChangedEntry entry : changedEntries.getNewNameList()) {
                    JSONObject entryObj = new JSONObject();
                    entryObj.put("label", "新氏名");
                    entryObj.put("date", entry.getDate().toString());
                    entryObj.put("ad", sdf.format(entry.getDate().toDate()));
                    entryObj.put("value", new JSONArray(entry.getValue().toJSON()));
                    entryObj.put("psc", entry.getPsc());
                    changesObj.put(entryObj);
                }
                for (DriverLicenseChangedEntry entry : changedEntries.getNewConditionList()) {
                    JSONObject entryObj = new JSONObject();
                    entryObj.put("label", "新条件");
                    entryObj.put("date", entry.getDate().toString());
                    entryObj.put("ad", sdf.format(entry.getDate().toDate()));
                    entryObj.put("value", new JSONArray(entry.getValue().toJSON()));
                    entryObj.put("psc", entry.getPsc());
                    changesObj.put(entryObj);
                }
                for (DriverLicenseChangedEntry entry : changedEntries.getConditionCancellationList()) {
                    JSONObject entryObj = new JSONObject();
                    entryObj.put("label", "条件解除");
                    entryObj.put("date", entry.getDate().toString());
                    entryObj.put("ad", sdf.format(entry.getDate().toDate()));
                    entryObj.put("value", new JSONArray(entry.getValue().toJSON()));
                    entryObj.put("psc", entry.getPsc());
                    changesObj.put(entryObj);
                }
            }

            try {
                // 本籍を取得
                DriverLicenseRegisteredDomicile registeredDomicile = files.getRegisteredDomicile();
                String value = registeredDomicile.getRegisteredDomicile().toJSON();
                if (value != null) {
                    obj.put("dl-registered-domicile", new JSONArray(value));
                }
                publishProgress(registeredDomicile.toString());
                // 写真を取得
                DriverLicensePhoto photo = files.getPhoto();
                publishProgress("写真のデコード中...");
                BitmapARGB argb = photo.getPhotoBitmapARGB();
                Bitmap bitmap = Bitmap.createBitmap(argb.getData(),
                                                    argb.getWidth(),
                                                    argb.getHeight(),
                                                    Bitmap.Config.ARGB_8888);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                byte[] jpeg = os.toByteArray();
                String src = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.DEFAULT);
                obj.put("dl-photo", src);
                // 記載事項変更（本籍）を取得
                changedEntries = files.getChangedRegisteredDomicile();
                if (changedEntries.isChanged()) {
                    for (DriverLicenseChangedEntry entry : changedEntries.getNewRegisteredDomicileList()) {
                        JSONObject entryObj = new JSONObject();
                        entryObj.put("label", "新本籍");
                        entryObj.put("date", entry.getDate().toString());
                        entryObj.put("ad", sdf.format(entry.getDate().toDate()));
                        entryObj.put("value", new JSONArray(entry.getValue().toJSON()));
                        entryObj.put("psc", entry.getPsc());
                        changesObj.put(entryObj);
                    }
                }
                // 電子署名を取得
                DriverLicenseSignature signature = files.getSignature();
                String signatureIssuer = signature.getIssuer();
                publishProgress("署名者: " + signatureIssuer);
                obj.put("signature-issuer", signatureIssuer);
                String signatureSubject = signature.getSubject();
                publishProgress("主体者: " + signatureSubject);
                obj.put("signature-subject", signatureSubject);
                String signatureSKI = Hex.encode(signature.getSubjectKeyIdentifier(), ":");
                publishProgress("主体者鍵識別子: " + signatureSKI);
                obj.put("signature-ski", signatureSKI);

                // 真正性検証
                ValidationResult result = files.validate();
                obj.put("dl-verified", result.isValid());
                obj.put("signature-valid", result.isValid());
                publishProgress("真正性検証結果: " + result);
            } catch(FileNotFoundException e) {
                // PIN2を入力していないfilesオブジェクトは
                // FileNotFoundExceptionをthrowします。
            } catch(UnsupportedOperationException e) {
                // free版の場合、真正性検証処理で
                // UnsupportedOperationException が返ります。
            }

            // 記載事項変更等(本籍除く）と記載事項変更（本籍）合わせた
            // オブジェクトをJSONに追加
            obj.put("dl-changes", changesObj);

            // Viewerを起動
            Intent intent = new Intent(activity, DLViewerActivity.class);
            intent.putExtra("json", obj.toString());
            activity.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
        } finally {
            progress.dismissAllowingStateLoss();
        }
    }
}
