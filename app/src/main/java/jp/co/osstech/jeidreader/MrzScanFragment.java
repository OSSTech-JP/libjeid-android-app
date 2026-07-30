package jp.co.osstech.jeidreader;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import jp.co.osstech.jeidreader.ocr.MlKitOcrEngine;
import jp.co.osstech.jeidreader.ocr.MrzGuideOverlayView;
import jp.co.osstech.jeidreader.ocr.MrzScanResult;
import jp.co.osstech.jeidreader.ocr.MrzTd3Parser;
import jp.co.osstech.jeidreader.ocr.OcrEngine;

/**
 * パスポートのMRZをカメラで読み取る全画面ダイアログ。
 *
 * <p>読み取りに必要な鍵情報を入力する補助UIであり、独立した画面ではないため、
 * Activityではなく呼び出し元のActivity内に表示するFragmentとして実装している。
 *
 * <p>Activityとして実装すると、呼び出し元がonPause/onResumeを経由するため
 * NFCのリーダーモードが解除・再登録される。{@code NfcAdapter#enableReaderMode()}は
 * Activityに紐づくAPIであり、この再登録がカメラ解放の影響で無効化されて
 * カードを検出しなくなる端末が存在する。Fragmentであれば呼び出し元は
 * resumedのままなので、リーダーモードの登録は一度も解除されない。
 *
 * <p>スキャン中の二重読み取りは、呼び出し元が{@code enableNFC}をfalseにすることで抑止する。
 */
public class MrzScanFragment
    extends DialogFragment
{
    /** FragmentManagerへ登録する際のタグ。 */
    public static final String TAG_FRAGMENT = "mrzscan";

    private static final String TAG = BaseActivity.TAG;

    /**
     * 呼び出し元が実装するコールバック。
     */
    public interface Listener {
        /**
         * MRZの読み取りに成功したときに呼ばれます。
         *
         * @param documentNumber 旅券番号
         * @param birthDate 生年月日(YYYYMMDDの8桁)
         * @param expirationDate 有効期限(YYYYMMDDの8桁)
         */
        void onMrzScanned(String documentNumber, String birthDate, String expirationDate);

        /**
         * スキャン画面が閉じられたときに呼ばれます。成功・取消のいずれでも呼ばれます。
         */
        void onMrzScanFinished();
    }

    /**
     * 切り出すROIの範囲(プレビュー表示範囲に対する比率)。
     * ガイド枠(幅0.90、高さ0.16)より広く取り、多少のずれを許容する。
     */
    private static final float ROI_WIDTH_RATIO = 0.96f;
    private static final float ROI_HEIGHT_RATIO = 0.30f;

    /** 解析に要求する解像度。MRZ44文字が横幅に収まるため短辺1080を要求する。 */
    private static final Size ANALYSIS_RESOLUTION = new Size(1080, 1920);

    /** この時間内に読み取れない場合はヒントを表示する。 */
    private static final long HINT_DELAY_MILLIS = 15000L;

    private PreviewView previewView;
    private MrzGuideOverlayView overlayView;
    private TextView statusView;
    private Button torchButton;

    private ExecutorService analysisExecutor;
    private OcrEngine ocrEngine;
    private ImageAnalysis imageAnalysis;
    private Camera camera;
    private boolean torchEnabled;

    /** 検証を通過した直前のフレームの結果。2フレームの一致を確認するために保持する。 */
    private MrzScanResult pendingResult;
    /** 結果を返した後にフレームの処理を止めるためのフラグ。 */
    private volatile boolean completed;

    private final ActivityResultLauncher<String> permissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                previewView.post(this::startCamera);
            } else {
                Toast.makeText(requireContext(),
                        "カメラの使用が許可されていないため、MRZ読み取りを利用できません",
                        Toast.LENGTH_LONG).show();
                dismiss();
            }
        });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, getClass().getSimpleName() + "#onCreate()");
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, 0);
        analysisExecutor = Executors.newSingleThreadExecutor();
        ocrEngine = new MlKitOcrEngine();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mrz_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        previewView = (PreviewView)view.findViewById(R.id.mrz_preview);
        overlayView = (MrzGuideOverlayView)view.findViewById(R.id.mrz_overlay);
        statusView = (TextView)view.findViewById(R.id.mrz_status);
        torchButton = (Button)view.findViewById(R.id.mrz_button_torch);
        torchButton.setOnClickListener(this::toggleTorch);
        view.findViewById(R.id.mrz_button_manual).setOnClickListener(v -> dismiss());

        statusView.postDelayed(() -> {
            if (!completed && isAdded()) {
                statusView.setText("読み取れない場合は、明るい場所で券面を平らにして"
                        + "枠いっぱいに写してください。手入力に戻ることもできます。");
            }
        }, HINT_DELAY_MILLIS);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            previewView.post(this::startCamera);
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // ダイアログを全画面に広げる
        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, getClass().getSimpleName() + "#onDestroy()");
        super.onDestroy();
        completed = true;
        // CameraXからのフレーム供給を止める。これを行わないと、閉じた検出器に対して
        // キュー済みのフレームが認識要求を投げ「This detector is already closed!」になる。
        if (imageAnalysis != null) {
            imageAnalysis.clearAnalyzer();
        }
        if (analysisExecutor != null) {
            // 解析は単一スレッドで動くため、解放処理を最後のタスクとして積めば
            // 実行中・キュー済みの解析が終わった後に必ず閉じられる。
            // メインスレッドを待たせずに順序を保証できる。
            final OcrEngine engine = ocrEngine;
            if (engine != null) {
                try {
                    analysisExecutor.execute(engine::close);
                } catch (RejectedExecutionException e) {
                    engine.close();
                }
            }
            analysisExecutor.shutdown();
        } else if (ocrEngine != null) {
            ocrEngine.close();
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        Log.d(TAG, getClass().getSimpleName() + "#onDismiss()");
        super.onDismiss(dialog);
        Listener listener = listener();
        if (listener != null) {
            listener.onMrzScanFinished();
        }
    }

    private Listener listener() {
        if (getActivity() instanceof Listener) {
            return (Listener)getActivity();
        }
        return null;
    }

    private void startCamera() {
        if (!isAdded()) {
            return;
        }
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            if (!isAdded()) {
                return;
            }
            try {
                bindUseCases(future.get());
            } catch (Exception e) {
                Log.e(TAG, getClass().getSimpleName() + ": failed to start camera", e);
                Toast.makeText(requireContext(), "カメラを起動できませんでした",
                        Toast.LENGTH_LONG).show();
                dismiss();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindUseCases(ProcessCameraProvider cameraProvider) {
        ResolutionSelector previewSelector = new ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build();
        Preview preview = new Preview.Builder()
            .setResolutionSelector(previewSelector)
            .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ResolutionSelector analysisSelector = new ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(new ResolutionStrategy(ANALYSIS_RESOLUTION,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
            .build();
        imageAnalysis = new ImageAnalysis.Builder()
            .setResolutionSelector(analysisSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build();
        imageAnalysis.setAnalyzer(analysisExecutor, this::analyze);

        UseCaseGroup.Builder builder = new UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageAnalysis);
        // ViewPortを設定するとプレビューの表示範囲と解析フレームの範囲が一致し、
        // ガイド枠とROIの位置関係が保証される。
        ViewPort viewPort = previewView.getViewPort();
        if (viewPort != null) {
            builder.setViewPort(viewPort);
        }
        cameraProvider.unbindAll();
        // Viewのライフサイクルに紐づけるため、この画面が閉じられた時点で
        // CameraXがカメラを解放する。
        camera = cameraProvider.bindToLifecycle(getViewLifecycleOwner(),
                CameraSelector.DEFAULT_BACK_CAMERA, builder.build());
        torchButton.setEnabled(camera.getCameraInfo().hasFlashUnit());
        overlayView.setVisibility(View.VISIBLE);
    }

    private void toggleTorch(View view) {
        if (camera == null || !camera.getCameraInfo().hasFlashUnit()) {
            return;
        }
        torchEnabled = !torchEnabled;
        camera.getCameraControl().enableTorch(torchEnabled);
        torchButton.setText(torchEnabled ? "ライトを消す" : "ライトを点ける");
    }

    /**
     * 1フレームを解析する。ROIを切り出してOCRへ渡し、チェックデジット検証を通った
     * 結果が2フレーム分一致したところで確定する。
     */
    private void analyze(@NonNull ImageProxy image) {
        try {
            if (completed) {
                return;
            }
            Bitmap roi = extractRoi(image);
            if (roi == null) {
                return;
            }
            List<String> lines;
            try {
                lines = ocrEngine.recognizeLines(roi);
            } finally {
                roi.recycle();
            }
            MrzScanResult result = MrzTd3Parser.parse(lines);
            if (result == null) {
                return;
            }
            if (pendingResult != null && pendingResult.hasSameKeyFields(result)) {
                completed = true;
                Log.d(TAG, getClass().getSimpleName() + ": MRZ detected " + result);
                previewView.post(() -> deliver(result));
            } else {
                // 検証は通ったが1フレーム目。次に同じ値が出たら確定する。
                pendingResult = result;
            }
        } catch (Throwable t) {
            Log.w(TAG, getClass().getSimpleName() + ": failed to analyze frame", t);
        } finally {
            image.close();
        }
    }

    /**
     * プレビューの表示範囲から、MRZ帯を含むROIを切り出す。
     *
     * @param image 解析対象のフレーム
     * @return 切り出したビットマップ
     */
    private Bitmap extractRoi(ImageProxy image) {
        Bitmap full = image.toBitmap();
        Bitmap visible = full;
        Rect crop = image.getCropRect();
        if (crop.width() > 0 && crop.height() > 0
                && (crop.width() != full.getWidth() || crop.height() != full.getHeight())) {
            visible = Bitmap.createBitmap(full, crop.left, crop.top,
                    crop.width(), crop.height());
        }
        Bitmap upright = visible;
        int rotation = image.getImageInfo().getRotationDegrees();
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            upright = Bitmap.createBitmap(visible, 0, 0,
                    visible.getWidth(), visible.getHeight(), matrix, true);
        }
        int width = upright.getWidth();
        int height = upright.getHeight();
        int roiWidth = Math.max(1, Math.round(width * ROI_WIDTH_RATIO));
        int roiHeight = Math.max(1, Math.round(height * ROI_HEIGHT_RATIO));
        Bitmap roi = Bitmap.createBitmap(upright, (width - roiWidth) / 2,
                (height - roiHeight) / 2, roiWidth, roiHeight);
        recycleIfUnused(upright, roi, visible, full);
        recycleIfUnused(visible, roi, full);
        recycleIfUnused(full, roi);
        return roi;
    }

    private static void recycleIfUnused(Bitmap target, Bitmap... keep) {
        for (Bitmap bitmap : keep) {
            if (bitmap == target) {
                return;
            }
        }
        target.recycle();
    }

    private void deliver(MrzScanResult result) {
        if (!isAdded()) {
            return;
        }
        if (!result.isCompositeCheckDigitValid()) {
            // 任意データの誤認識で不一致になり得るため、破棄はせず記録のみ行う
            Log.i(TAG, getClass().getSimpleName()
                    + ": composite check digit mismatch: " + result.getLine2());
        }
        Listener listener = listener();
        if (listener != null) {
            listener.onMrzScanned(result.getDocumentNumber(),
                    result.getBirthDateYyyyMmDd(),
                    result.getExpirationDateYyyyMmDd());
        }
        dismiss();
    }
}
