package jp.co.osstech.jeidreader.ocr;

import android.graphics.Bitmap;
import android.util.Log;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ML Kit Text Recognition v2(ラテン文字モデル)による{@link OcrEngine}実装。
 *
 * <p>モデルはAPKに同梱(bundled)されており、ネットワークもGoogle Play開発者サービスも
 * 必要としない。認識はすべて端末内で行われ、券面画像が外部へ送出されることはない。
 */
public class MlKitOcrEngine implements OcrEngine {
    private static final String TAG = "jeidreader";
    /** 1フレームあたりの認識待ち時間。超えたフレームは捨てて次のフレームを処理する。 */
    private static final long TIMEOUT_SECONDS = 3;

    private final TextRecognizer recognizer;

    public MlKitOcrEngine() {
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    @Override
    public List<String> recognizeLines(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        try {
            Text text = Tasks.await(recognizer.process(image), TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<String> lines = new ArrayList<>();
            for (Text.TextBlock block : text.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    lines.add(line.getText());
                }
            }
            return lines;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            // 1フレームの失敗は次のフレームで回復するため、ログのみとする
            Log.w(TAG, getClass().getSimpleName() + ": recognition failed", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void close() {
        recognizer.close();
    }
}
