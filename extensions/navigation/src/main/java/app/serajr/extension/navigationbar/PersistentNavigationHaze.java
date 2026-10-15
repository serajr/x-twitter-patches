package app.serajr.extension.navigationbar;

import android.util.Log;

public final class PersistentNavigationHaze {

    private static final String TAG = "SerajrNavHaze";

    private static Boolean lastVisibility = null;

    private PersistentNavigationHaze() {
    }

    public static void initialize() {
        Log.i(TAG, "PersistentNavigationHaze extension loaded successfully!");
    }

    public static void onVisibilityChanged(boolean visible) {
        Log.i(TAG, "bottomTabsVisible=" + visible);
    }

    public static void onBottomBarVisibilityChanged(boolean visible) {
        Log.i(TAG, "Bottom bar visibility changed: " + visible);
    }
}
