package jp.co.osstech.jeidreader;

public class RCMenuActivity
    extends MenuActivity
{
    @Override
    protected MenuEntry[] getMenuEntries() {
        return new MenuEntry[] {
            new MenuEntry(R.string.rc1_reader, RCReaderActivity.class)
                .icon(R.drawable.ic_menu_card_rc)
                .description(R.string.rc1_reader_desc),
            new MenuEntry(R.string.rc2_reader, RC2ReaderActivity.class)
                .icon(R.drawable.ic_menu_card_rc)
                .description(R.string.rc2_reader_desc),
            // 特定在留カード等は個人番号カード上の在留APだが、在留カード等の
            // データを読み出す機能なのでこのメニューに置く
            new MenuEntry(R.string.rcs_reader, RCSReaderActivity.class)
                .icon(R.drawable.ic_menu_card_rc)
                .description(R.string.rcs_reader_desc),
        };
    }
}
