package jp.co.osstech.jeidreader;

public class RCMenuActivity
    extends MenuActivity
{
    @Override
    protected MenuEntry[] getMenuEntries() {
        return new MenuEntry[] {
            new MenuEntry(R.string.rc1_reader, RCReaderActivity.class),
            new MenuEntry(R.string.rc2_reader, RC2ReaderActivity.class, true),
            // 特定在留カード等は個人番号カード上の在留APだが、在留カード等の
            // データを読み出す機能なのでこのメニューに置く
            new MenuEntry(R.string.rcs_reader, RCSReaderActivity.class, true),
        };
    }
}
