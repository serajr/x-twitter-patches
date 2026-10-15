package app.serajr.extension.navigationbar;

import android.util.Log;

public final class PersistentNavigationHaze {

    private static final String TAG = "SerajrNavHaze";

    private PersistentNavigationHaze() {
    }

    public static void initialize() {
        Log.i(TAG, "PersistentNavigationHaze extension loaded successfully!");
    }
}
