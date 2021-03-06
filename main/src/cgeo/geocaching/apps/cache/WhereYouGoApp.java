package cgeo.geocaching.apps.cache;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;

import android.content.res.Resources;

class WhereYouGoApp extends AbstractGeneralApp implements GeneralApp {
    WhereYouGoApp(Resources res) {
        super(res.getString(R.string.cache_menu_whereyougo), "menion.android.whereyougo");
    }

    @Override
    public boolean isEnabled(cgCache cache) {
        return cache != null && cache.type != null && cache.type.equalsIgnoreCase("wherigo");
    }
}
