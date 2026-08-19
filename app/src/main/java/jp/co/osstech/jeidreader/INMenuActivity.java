package jp.co.osstech.jeidreader;

import jp.co.osstech.libjeid.JPKIAP;

public class INMenuActivity
    extends MenuActivity
{
    @Override
    protected MenuEntry[] getMenuEntries() {
        return new MenuEntry[] {
            new MenuEntry(R.string.in_reader, INReaderActivity.class)
                .icon(R.drawable.ic_menu_card_in)
                .description(R.string.in_reader_desc),
            new MenuEntry(R.string.indl_reader, INDLReaderActivity.class)
                .icon(R.drawable.ic_menu_card_dl)
                .description(R.string.indl_reader_desc),
            new MenuEntry(R.string.show_cert, JPKICertSelectActivity.class)
                .icon(R.drawable.ic_menu_cert)
                .description(R.string.show_cert_desc),
            new MenuEntry(R.string.sign_jpki_auth, JPKISignActivity.class)
                .icon(R.drawable.ic_menu_sign)
                .description(R.string.sign_jpki_auth_desc)
                .extra("TYPE", JPKIAP.TYPE_AUTH)
                .testModeOnly(),
            new MenuEntry(R.string.sign_jpki_sign, JPKISignActivity.class)
                .icon(R.drawable.ic_menu_sign)
                .description(R.string.sign_jpki_sign_desc)
                .extra("TYPE", JPKIAP.TYPE_SIGN)
                .testModeOnly(),
            new MenuEntry(R.string.in_test, INTestActivity.class)
                .icon(R.drawable.ic_menu_test)
                .description(R.string.in_test_desc)
                .testModeOnly(),
        };
    }
}
