package jp.co.osstech.jeidreader;

public class MainActivity
    extends MenuActivity
{
    @Override
    protected MenuEntry[] getMenuEntries() {
        return new MenuEntry[] {
            new MenuEntry(R.string.in_menu, INMenuActivity.class)
                .icon(R.drawable.ic_menu_card_in)
                .description(R.string.in_menu_desc),
            new MenuEntry(R.string.dl_reader, DLReaderActivity.class)
                .icon(R.drawable.ic_menu_card_dl)
                .description(R.string.dl_reader_desc),
            new MenuEntry(R.string.ep_reader, EPReaderActivity.class)
                .icon(R.drawable.ic_menu_card_ep)
                .description(R.string.ep_reader_desc),
            new MenuEntry(R.string.rc_menu, RCMenuActivity.class)
                .icon(R.drawable.ic_menu_card_rc)
                .description(R.string.rc_menu_desc),
            new MenuEntry(R.string.pinstatus, PinStatusActivity.class)
                .icon(R.drawable.ic_menu_pin)
                .description(R.string.pinstatus_desc),
        };
    }
}
