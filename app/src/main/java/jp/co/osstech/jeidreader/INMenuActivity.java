package jp.co.osstech.jeidreader;

import jp.co.osstech.libjeid.JPKIAP;

public class INMenuActivity
    extends MenuActivity
{
    @Override
    protected MenuEntry[] getMenuEntries() {
        return new MenuEntry[] {
            new MenuEntry(R.string.in_reader, INReaderActivity.class),
            new MenuEntry(R.string.indl_reader, INDLReaderActivity.class),
            new MenuEntry(R.string.show_cert, JPKICertSelectActivity.class),
            new MenuEntry(R.string.sign_jpki_auth, JPKISignActivity.class,
                          "TYPE", JPKIAP.TYPE_AUTH, true),
            new MenuEntry(R.string.sign_jpki_sign, JPKISignActivity.class,
                          "TYPE", JPKIAP.TYPE_SIGN, true),
            new MenuEntry(R.string.in_test, INTestActivity.class, true),
        };
    }
}
