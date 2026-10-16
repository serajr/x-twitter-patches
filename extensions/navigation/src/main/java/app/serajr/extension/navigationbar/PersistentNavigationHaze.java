package app.serajr.extension.navigationbar;

import android.util.Log;

public final class PersistentNavigationHaze {

    private static final String TAG = "SerajrNavHaze";

    private static Boolean lastVisibility = null;
    private static int lastBottomBarSize = Integer.MIN_VALUE;

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

    public static void onBottomBarContentComposed() {
        Log.i(TAG, "Bottom bar content composed (e2.invoke)");
    }

    public static void onBottomBarSizeChanged(int size) {
        if (lastBottomBarSize != size) {
            lastBottomBarSize = size;
            Log.i(TAG, "Bottom bar animated size: " + size);
        }
    }

    // EXPERIMENT 3:
    // Keep approximately 35% of the observed full bar height.
    //
    // This value is intentionally fixed for this diagnostic test.
    // It must not be used as a production solution.

    private static final int EXPERIMENTAL_MIN_SIZE_PX = 103;

    public static int getExperimentalBottomBarSize(int originalSize) {
        return Math.max(originalSize, EXPERIMENTAL_MIN_SIZE_PX);
    }
}
