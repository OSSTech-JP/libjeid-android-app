package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import java.util.ArrayList;
import java.util.List;

/**
 * ボタンを縦に並べて別の画面へ遷移するだけのメニュー画面の共通実装です。
 * サブクラスは{@link #getMenuEntries()}でメニュー項目を返すだけでよく、
 * 画面ごとのレイアウトXMLは不要です。
 */
public abstract class MenuActivity
    extends BaseActivity
{
    /**
     * メニュー項目1件を表します。
     * (android.view.MenuItemとの衝突を避けるためMenuEntryという名前にしています)
     */
    protected static class MenuEntry
    {
        final int labelResId;
        final Class<?> activity;
        final String extraKey;
        final int extraValue;
        /** テストモードのときだけ表示する項目 */
        final boolean testModeOnly;

        MenuEntry(int labelResId, Class<?> activity) {
            this(labelResId, activity, null, 0, false);
        }

        MenuEntry(int labelResId, Class<?> activity, boolean testModeOnly) {
            this(labelResId, activity, null, 0, testModeOnly);
        }

        MenuEntry(int labelResId, Class<?> activity,
                  String extraKey, int extraValue, boolean testModeOnly) {
            this.labelResId = labelResId;
            this.activity = activity;
            this.extraKey = extraKey;
            this.extraValue = extraValue;
            this.testModeOnly = testModeOnly;
        }
    }

    // テストモードの切り替えで表示/非表示を追従させるボタン
    private final List<Button> testModeButtons = new ArrayList<>();

    /**
     * この画面に並べるメニュー項目を返します。
     *
     * @return メニュー項目
     */
    protected abstract MenuEntry[] getMenuEntries();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        boolean testMode = TestMode.isEnabled();
        LayoutInflater inflater = getLayoutInflater();
        ViewGroup container = (ViewGroup)findViewById(R.id.menu_container);
        for (MenuEntry entry : getMenuEntries()) {
            Button button =
                (Button)inflater.inflate(R.layout.menu_button, container, false);
            button.setText(entry.labelResId);
            button.setOnClickListener(view -> startMenuEntry(entry));
            container.addView(button);
            if (entry.testModeOnly) {
                button.setVisibility(testMode ? View.VISIBLE : View.GONE);
                testModeButtons.add(button);
            }
        }
    }

    // Aboutダイアログのロゴ連打でテストモードが切り替わったら表示を更新します
    @Override
    protected void onTestModeChanged(boolean enabled) {
        int visibility = enabled ? View.VISIBLE : View.GONE;
        for (Button button : testModeButtons) {
            button.setVisibility(visibility);
        }
    }

    private void startMenuEntry(MenuEntry entry) {
        if (entry.activity == null) {
            return;
        }
        Intent intent = new Intent(getApplication(), entry.activity);
        if (entry.extraKey != null) {
            intent.putExtra(entry.extraKey, entry.extraValue);
        }
        startActivity(intent);
    }
}
