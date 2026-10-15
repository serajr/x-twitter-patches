package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants

import com.android.tools.smali.dexlib2.iface.Method

private const val DEBUG = false

private const val TARGET_CLASS =
    "Lcom/x/ui/common/h2;"

private const val EXTENSION_METHOD =
    "Lapp/serajr/extension/navigationbar/PersistentNavigationHaze;->initialize()V"

private fun debugNavHaze(message: String) {
    if (DEBUG) {
        println("FixNavigationBarHaze -> $message")
    }
}

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description =
        "Tests the persistent navigation Haze extension integration.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    // Merge our compiled Java extension into the patched APK.
    extendWith("extensions/navigation.mpe")

    execute {
        var targetMethod: Method? = null

        classDefForEach { classDef ->
            if (classDef.type.toString() != TARGET_CLASS) {
                return@classDefForEach
            }

            targetMethod = classDef.methods.firstOrNull { method ->
                method.name == "a" &&
                    method.returnType == "V" &&
                    method.parameterTypes.map { it.toString() } ==
                    listOf(
                        "Landroidx/compose/runtime/internal/f;",
                        "Ldev/chrisbanes/haze/w0;",
                        "Lkotlin/jvm/functions/Function1;",
                        "Landroidx/compose/ui/Modifier;",
                        "Landroidx/compose/runtime/internal/f;",
                        "Z",
                        "Z",
                        "Landroidx/compose/runtime/internal/f;",
                        "Landroidx/compose/runtime/Composer;",
                        "I"
                    )
            }
        }

        val method = targetMethod
            ?: error(
                "FixNavigationBarHaze: target method not found."
            )

        val mutableClass =
            mutableClassDefBy(method.definingClass)

        val mutableMethod =
            mutableClass.methods.firstOrNull {
                it.toString() == method.toString()
            } ?: error(
                "FixNavigationBarHaze: mutable method not found."
            )

        val implementation =
            mutableMethod.implementation
                ?: error(
                    "FixNavigationBarHaze: no implementation."
                )

        require(implementation.registerCount == 33) {
            "FixNavigationBarHaze: unexpected register layout."
        }

        // Diagnostic only. No registers are modified.
        mutableMethod.addInstructions(
            0,
            "invoke-static {}, $EXTENSION_METHOD"
        )

        debugNavHaze(
            "PersistentNavigationHaze.initialize() hook injected."
        )
    }
}
