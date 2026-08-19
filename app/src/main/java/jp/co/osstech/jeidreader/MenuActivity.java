package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/**
 * カードを縦に並べて別の画面へ遷移するだけのメニュー画面の共通実装です。
 * サブクラスは{@link #getMenuEntries()}でメニュー項目を返すだけでよく、
 * 画面ごとのレイアウトXMLは不要です。
 */
public abstract class MenuActivity
    extends BaseActivity
{
    /**
     * メニュー項目1件を表します。
     * (android.view.MenuItemとの衝突を避けるためMenuEntryという名前にしています)
     *
     * <p>ラベルと遷移先だけをコンストラクタで受け取り、任意の項目は
     * メソッドチェーンで指定します。位置引数が増えると
     * {@code new MenuEntry(R.string.x, Foo.class, null, 0, true)}のように
     * 呼び出し側から意味が読み取れなくなるためです。
     */
    protected static class MenuEntry
    {
        final int labelResId;
        final Class<?> activity;
        int iconResId;
        int descResId;
        String extraKey;
        int extraValue;
        /** テストモードのときだけ表示する項目 */
        boolean testModeOnly;

        MenuEntry(int labelResId, Class<?> activity) {
            this.labelResId = labelResId;
            this.activity = activity;
        }

        /** 項目の左に表示するアイコンを指定します。 */
        MenuEntry icon(int resId) {
            this.iconResId = resId;
            return this;
        }

        /** タイトルの下に表示する説明文を指定します。 */
        MenuEntry description(int resId) {
            this.descResId = resId;
            return this;
        }

        /** 遷移先へ渡すIntentのextraを指定します。 */
        MenuEntry extra(String key, int value) {
            this.extraKey = key;
            this.extraValue = value;
            return this;
        }

        /** テストモードのときだけ表示する項目にします。 */
        MenuEntry testModeOnly() {
            this.testModeOnly = true;
            return this;
        }
    }

    // テストモードの切り替えで表示/非表示を追従させる項目
    private final List<View> testModeItems = new ArrayList<>();

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
            View item = inflater.inflate(R.layout.menu_item, container, false);
            ((TextView)item.findViewById(R.id.menu_item_title))
                .setText(entry.labelResId);

            ImageView icon = (ImageView)item.findViewById(R.id.menu_item_icon);
            if (entry.iconResId != 0) {
                icon.setImageResource(entry.iconResId);
            } else {
                // タイトルの開始位置を他の項目と揃えるためGONEではなくINVISIBLEにします
                icon.setVisibility(View.INVISIBLE);
            }

            TextView desc =
                (TextView)item.findViewById(R.id.menu_item_description);
            if (entry.descResId != 0) {
                desc.setText(entry.descResId);
            } else {
                desc.setVisibility(View.GONE);
            }

            item.setOnClickListener(view -> startMenuEntry(entry));
            container.addView(item);
            if (entry.testModeOnly) {
                item.setVisibility(testMode ? View.VISIBLE : View.GONE);
                testModeItems.add(item);
            }
        }
    }

    // Aboutダイアログのロゴ連打でテストモードが切り替わったら表示を更新します
    @Override
    protected void onTestModeChanged(boolean enabled) {
        int visibility = enabled ? View.VISIBLE : View.GONE;
        for (View item : testModeItems) {
            item.setVisibility(visibility);
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
