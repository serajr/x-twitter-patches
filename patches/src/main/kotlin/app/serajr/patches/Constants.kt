package app.serajr.patches

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_X = Compatibility(
        name = "X (Twitter)", // App name as it appears in the Android launcher.
        packageName = "com.twitter.android",
        apkFileType = ApkFileType.APK, // Preferred or recommended file type.
        appIconColor = 0x000000, // Ícone preto do X.
        targets = listOf(
            AppTarget(
                version = "12.33.0-alpha.02", // Experimental
                isExperimental = true
            ),  
            AppTarget(
                version = "12.31.0-prod.01", // Experimental
                isExperimental = true
            ),
            AppTarget(
                version = "12.29.1-prod.01", // Recomendada
                isExperimental = false
            )
        )
    )
}
