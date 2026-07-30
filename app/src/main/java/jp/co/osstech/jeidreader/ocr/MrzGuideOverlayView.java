package jp.co.osstech.jeidreader.ocr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * MRZを合わせる位置を示すガイド枠を描画するオーバーレイ。
 *
 * <p>枠は{@link jp.co.osstech.jeidreader.MrzScanActivity}が切り出すROIより内側に取り、
 * 利用者が枠に合わせればROIに確実に収まるようにしている。
 * ViewPortによりプレビューの表示範囲と解析フレームの範囲が一致するため、
 * このビューの寸法に対する比率が解析フレームに対する比率と一致する。
 */
public class MrzGuideOverlayView extends View {
    /** ガイド枠の幅(ビュー幅に対する比率)。 */
    private static final float GUIDE_WIDTH_RATIO = 0.90f;
    /** ガイド枠の高さ(ビュー高さに対する比率)。 */
    private static final float GUIDE_HEIGHT_RATIO = 0.16f;
    private static final float CORNER_RADIUS_DP = 4f;
    private static final float BORDER_WIDTH_DP = 2f;

    private final Paint dimPaint = new Paint();
    private final Paint borderPaint = new Paint();
    private final RectF guide = new RectF();
    private final float cornerRadius;

    public MrzGuideOverlayView(Context context) {
        this(context, null);
    }

    public MrzGuideOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        cornerRadius = CORNER_RADIUS_DP * density;
        dimPaint.setColor(Color.argb(140, 0, 0, 0));
        dimPaint.setStyle(Paint.Style.FILL);
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(BORDER_WIDTH_DP * density);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        float guideWidth = width * GUIDE_WIDTH_RATIO;
        float guideHeight = height * GUIDE_HEIGHT_RATIO;
        float left = (width - guideWidth) / 2f;
        float top = (height - guideHeight) / 2f;
        guide.set(left, top, left + guideWidth, top + guideHeight);

        // ガイド枠の外側を暗くする
        canvas.drawRect(0, 0, width, guide.top, dimPaint);
        canvas.drawRect(0, guide.bottom, width, height, dimPaint);
        canvas.drawRect(0, guide.top, guide.left, guide.bottom, dimPaint);
        canvas.drawRect(guide.right, guide.top, width, guide.bottom, dimPaint);
        canvas.drawRoundRect(guide, cornerRadius, cornerRadius, borderPaint);
    }
}
