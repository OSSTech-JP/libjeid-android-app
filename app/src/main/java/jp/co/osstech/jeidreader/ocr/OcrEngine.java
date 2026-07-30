package jp.co.osstech.jeidreader.ocr;

import android.graphics.Bitmap;
import java.util.List;

/**
 * OCRエンジンの差し替え境界。
 *
 * <p>実装を入れ替えることで、ML Kit以外のエンジン(Tesseract4Android、
 * PaddleOCR、商用SDK等)へカメラ層を変更せずに載せ替えられる。
 *
 * <p>1フレームの認識失敗は正常系として扱う(次のフレームで再試行するため)。
 * 実装は例外を投げずに空のリストを返すこと。
 */
public interface OcrEngine {
    /**
     * 画像から認識できた文字列を行単位で返します。
     *
     * @param bitmap 認識対象の画像
     * @return 認識した行のリスト。認識できなかった場合は空のリスト
     */
    List<String> recognizeLines(Bitmap bitmap);

    /**
     * エンジンが確保しているリソースを解放します。
     */
    void close();
}
