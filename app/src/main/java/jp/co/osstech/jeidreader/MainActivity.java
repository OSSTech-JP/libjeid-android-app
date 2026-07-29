package jp.co.osstech.jeidreader;

public class MainActivity
    extends MenuActivity
{
    @Override
    protected MenuEntry[] getMenuEntries() {
        return new MenuEntry[] {
            new MenuEntry(R.string.in_menu, INMenuActivity.class),
            new MenuEntry(R.string.dl_reader, DLReaderActivity.class),
            new MenuEntry(R.string.ep_reader, EPReaderActivity.class),
            new MenuEntry(R.string.rc_menu, RCMenuActivity.class),
            new MenuEntry(R.string.pinstatus, PinStatusActivity.class),
        };
    }
}
