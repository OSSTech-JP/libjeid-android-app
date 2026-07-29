package jp.co.osstech.jeidreader;

public class RCMenuActivity
    extends MenuActivity
{
    @Override
    protected MenuEntry[] getMenuEntries() {
        return new MenuEntry[] {
            new MenuEntry(R.string.rc1_reader, RCReaderActivity.class),
            new MenuEntry(R.string.rc2_reader, RC2ReaderActivity.class, true),
        };
    }
}
