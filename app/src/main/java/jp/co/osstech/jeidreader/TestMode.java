package jp.co.osstech.jeidreader;

/**
 * 開発者向けの隠し機能(テストモード)の有効・無効を保持します。
 * Aboutダイアログのロゴを5回タップすると切り替わります。
 *
 * <p>意図せず有効なまま使われることを防ぐため状態は永続化せず、プロセス内でのみ
 * 保持します。アプリを再起動すると必ず無効に戻ります。</p>
 */
public final class TestMode
{
    private static volatile boolean enabled = false;

    private TestMode() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }
}
