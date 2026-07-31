package jp.co.osstech.jeidreader;

import android.graphics.Bitmap;
import android.nfc.Tag;
import android.util.Base64;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import jp.co.osstech.libjeid.CardType;
import jp.co.osstech.libjeid.InvalidACKeyException;
import jp.co.osstech.libjeid.JeidReader;
import jp.co.osstech.libjeid.RCKey;
import jp.co.osstech.libjeid.ResidenceCard2AP;
import jp.co.osstech.libjeid.ValidationResult;
import jp.co.osstech.libjeid.rc2.*;
import jp.co.osstech.libjeid.util.BitmapARGB;
import org.json.JSONObject;

public class RC2ReaderTask
    implements Runnable
{
    private static final String TAG = MainActivity.TAG;
    private final WeakReference<RC2ReaderActivity> activityRef;
    private Tag nfcTag;
    private String rcNumber;

    public RC2ReaderTask(RC2ReaderActivity activity, Tag nfcTag, String rcNumber) {
        this.activityRef = new WeakReference<>(activity);
        this.nfcTag = nfcTag;
        this.rcNumber = rcNumber;
    }

    private void publishProgress(String msg) {
        RC2ReaderActivity activity = activityRef.get();
        if (activity != null) {
            activity.print(msg);
        }
    }

    public void run() {
        Log.d(TAG, getClass().getSimpleName() + "#run()");
        RC2ReaderActivity activity = activityRef.get();
        if (activity == null) {
            return;
        }
        activity.clear();
        publishProgress("# 読み取り開始、カードを離さないでください");

        if (rcNumber.isEmpty()) {
            publishProgress("在留カード番号または特別永住者証明書番号を設定してください");
            return;
        }

        ProgressDialogFragment progress = new ProgressDialogFragment();
        activity.runOnUiThread(() -> {
            progress.show(activity.getSupportFragmentManager(), "progress");
        });
        try {
            JeidReader reader = new JeidReader(nfcTag);
            publishProgress("## カード種別");
            CardType type = reader.detectCardType();
            publishProgress("CardType: " + type);
            if (type != CardType.RC2) {
                publishProgress("第2世代在留カード/特別永住者証明書ではありません");
                return;
            }
            ResidenceCard2AP ap = reader.selectResidenceCard2AP();
            RC2CommonData commonData = ap.readCommonData();
            publishProgress("commonData: " + commonData);
            RC2CardType cardType = ap.readCardType();
            publishProgress("cardType: " + cardType);
            RCKey rckey = new RCKey(rcNumber);
            publishProgress("## セキュアメッセージング用の鍵交換&認証");
            try {
                ap.startAC(rckey);
            } catch (InvalidACKeyException e) {
                publishProgress("在留カード番号または特別永住者証明書番号が間違っています");
                return;
            }

            publishProgress("## カードから情報を取得します");
            RC2Files files = ap.readFiles();
            JSONObject obj = new JSONObject();
            // "05" = 第2世代在留カード, "06" = 第2世代特別永住者証明書
            String cardTypeCode = cardType.getType();
            obj.put("rc2-card-type", cardTypeCode);

            publishProgress("## 在留カード等の番号");
            RC2CardNumber cardNumber = files.getCardNumber();
            publishProgress(cardNumber.toString());
            obj.put("rc2-card-number", cardNumber.getNumber());

            publishProgress("## 券面記載事項");
            RC2CardEntries cardEntries = files.getCardEntries();
            publishProgress(cardEntries.toString());
            obj.put("rc2-valid-until", cardEntries.getValidUntil());
            obj.put("rc2-birth-date", cardEntries.getBirthDate());
            obj.put("rc2-sex", cardEntries.getSex());
            obj.put("rc2-nationality", cardEntries.getNationality());
            obj.put("rc2-status", cardEntries.getResidenceStatus());
            obj.put("rc2-period", cardEntries.getPeriod());
            obj.put("rc2-permit-category", cardEntries.getPermitCategory());
            obj.put("rc2-permit-date", cardEntries.getPermitDate());
            obj.put("rc2-work-restriction", cardEntries.getWorkRestriction());
            obj.put("rc2-period-until", cardEntries.getPeriodUntil());

            publishProgress("## 氏名イメージ・顔画像のデコード");
            RC2NameImage nameImage = files.getNameImage();
            String src = toPngDataUri(nameImage.getBitmapARGB());
            if (src != null) {
                obj.put("rc2-name-image", src);
            }
            // 1歳未満の中長期在留者・特別永住者では顔画像が格納されない
            RC2FaceImage faceImage = files.getFaceImage();
            src = toJpegDataUri(faceImage.getBitmapARGB());
            if (src != null) {
                obj.put("rc2-face-image", src);
            }
            publishProgress("完了");

            publishProgress("## 住居地イメージのデコード");
            RC2AddressImage addressImage = files.getAddressImage();
            src = toPngDataUri(addressImage.getBitmapARGB());
            if (src != null) {
                obj.put("rc2-address-image", src);
            }
            publishProgress("完了");

            if ("05".equals(cardTypeCode)) {
                // 資格外活動許可欄・在留期間更新等許可申請ステータスは在留カードのみ
                publishProgress("## 資格外活動許可欄");
                RC2Permission permission = files.getPermission();
                publishProgress(String.valueOf(permission));
                if (permission != null) {
                    obj.put("rc2-comprehensive", permission.getComprehensive());
                    obj.put("rc2-comprehensive-limit", permission.getComprehensiveLimit());
                    obj.put("rc2-individual", permission.getIndividual());
                }

                publishProgress("## 在留期間更新等許可申請ステータス");
                RC2UpdateStatus updateStatus = files.getUpdateStatus();
                publishProgress(String.valueOf(updateStatus));
                if (updateStatus != null) {
                    obj.put("rc2-update-status", updateStatus.getStatus());
                }
            }

            publishProgress("## その他");
            RC2Others others = files.getOthers();
            publishProgress(others.toString());
            obj.put("rc2-commissioner-mark", others.getCommissionerMark());
            obj.put("rc2-reserved", others.getReserved());

            publishProgress("## 電子署名");
            RC2Signature signature = files.getSignature();
            publishProgress(signature.toString());

            // 真正性検証
            publishProgress("## 真正性検証");
            try {
                ValidationResult result = files.validate();
                obj.put("rc2-valid", result.isValid());
                obj.put("rc2-validation-result", result.toString());
                publishProgress("真正性検証結果: " + result);
            } catch (UnsupportedOperationException e) {
                // free版の場合、真正性検証処理で
                // UnsupportedOperationException が返ります。
            }
            // ビューアは別Activityではなく読み取り画面内のFragmentとして表示する
            final String json = obj.toString();
            activity.runOnUiThread(() -> activity.showViewer(json));
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            publishProgress("エラー: " + e);
        } finally {
            activity.runOnUiThread(() -> {
                progress.dismissAllowingStateLoss();
            });
        }
    }

    // 氏名・住居地イメージは2値画像のためPNGで埋め込む
    private String toPngDataUri(BitmapARGB argb) {
        Bitmap bitmap = toBitmap(argb);
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        return "data:image/png;base64,"
            + Base64.encodeToString(os.toByteArray(), Base64.DEFAULT);
    }

    private String toJpegDataUri(BitmapARGB argb) {
        Bitmap bitmap = toBitmap(argb);
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
        return "data:image/jpeg;base64,"
            + Base64.encodeToString(os.toByteArray(), Base64.DEFAULT);
    }

    private Bitmap toBitmap(BitmapARGB argb) {
        if (argb == null) {
            return null;
        }
        return Bitmap.createBitmap(argb.getData(),
                                   argb.getWidth(),
                                   argb.getHeight(),
                                   Bitmap.Config.ARGB_8888);
    }
}
