package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val DEBUG = false

private const val TARGET_CLASS = "Lcom/x/ui/common/h2;"
private const val VISIBILITY_CLASS = "Lcom/x/ui/common/g2;"
private const val CONTENT_CLASS = "Lcom/x/ui/common/e2;"

private const val EXTENSION_CLASS =
    "Lapp/serajr/extension/navigationbar/PersistentNavigationHaze;"

private fun debugNavHaze(message: String) {
    if (DEBUG) println("FixNavigationBarHaze -> $message")
}

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description = "Monitors bottom bar visibility for persistent navigation Haze.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    extendWith("extensions/navigation.mpe")

    execute {
        var targetMethod: Method? = null
        var visibilityMethod: Method? = null
        var contentMethod: Method? = null
        var sizeMethod: Method? = null

        classDefForEach { classDef ->
            when (classDef.type.toString()) {
                TARGET_CLASS -> {
                    targetMethod = classDef.methods.firstOrNull { method ->
                        method.name == "a" &&
                            method.returnType == "V" &&
                            method.parameterTypes.map { it.toString() } == listOf(
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

                VISIBILITY_CLASS -> {
                    visibilityMethod = classDef.methods.firstOrNull { method ->
                        method.name == "setVisible" &&
                            method.returnType == "V" &&
                            method.parameterTypes.map { it.toString() } == listOf("Z")
                    }

                    sizeMethod = classDef.methods.firstOrNull { method ->
                        method.name == "getSize" &&
                            method.returnType == "I" &&
                            method.parameterTypes.isEmpty()
                    }
                }

                CONTENT_CLASS -> {
                    contentMethod = classDef.methods.firstOrNull { method ->
                        method.name == "invoke" &&
                            method.returnType == "Ljava/lang/Object;" &&
                            method.parameterTypes.map { it.toString() } == listOf(
                                "Ljava/lang/Object;",
                                "Ljava/lang/Object;",
                                "Ljava/lang/Object;"
                            )
                    }
                }
            }
        }

        // Hook 1: Initialize the navigation Haze extension.
        val originalTarget = targetMethod
            ?: error("FixNavigationBarHaze: h2.a() not found.")

        val mutableTarget = mutableClassDefBy(originalTarget.definingClass)
            .methods.firstOrNull { it.toString() == originalTarget.toString() }
            ?: error("FixNavigationBarHaze: mutable h2.a() not found.")

        require(mutableTarget.implementation?.registerCount == 33) {
            "FixNavigationBarHaze: unexpected h2.a() register layout."
        }

        mutableTarget.addInstructions(
            0,
            "invoke-static {}, ${EXTENSION_CLASS}->initialize()V"
        )

        // Hook 2: Monitor bottom bar visibility changes.
        val originalVisibility = visibilityMethod
            ?: error("FixNavigationBarHaze: g2.setVisible(Z) not found.")

        val mutableVisibility = mutableClassDefBy(originalVisibility.definingClass)
            .methods.firstOrNull { it.toString() == originalVisibility.toString() }
            ?: error("FixNavigationBarHaze: mutable g2.setVisible(Z) not found.")

        require(mutableVisibility.implementation?.registerCount == 2) {
            "FixNavigationBarHaze: unexpected g2.setVisible(Z) register layout."
        }

        mutableVisibility.addInstructions(
            0,
            "invoke-static {p1}, ${EXTENSION_CLASS}->onBottomBarVisibilityChanged(Z)V"
        )

        // Hook 3: Monitor bottom bar content composition.
        val originalContent = contentMethod
            ?: error("FixNavigationBarHaze: e2.invoke() not found.")

        val mutableContent = mutableClassDefBy(originalContent.definingClass)
            .methods.firstOrNull { it.toString() == originalContent.toString() }
            ?: error("FixNavigationBarHaze: mutable e2.invoke() not found.")

        require(mutableContent.implementation?.registerCount == 22) {
            "FixNavigationBarHaze: unexpected e2.invoke() register layout."
        }

        mutableContent.addInstructions(
            0,
            "invoke-static {}, ${EXTENSION_CLASS}->onBottomBarContentComposed()V"
        )

        // Hook 4: Monitor the animated bottom bar size.
        val originalSize = sizeMethod
            ?: error("FixNavigationBarHaze: g2.getSize() not found.")

        val mutableSize = mutableClassDefBy(originalSize.definingClass)
            .methods.firstOrNull { it.toString() == originalSize.toString() }
            ?: error("FixNavigationBarHaze: mutable g2.getSize() not found.")

        val sizeImplementation = mutableSize.implementation
            ?: error("FixNavigationBarHaze: g2.getSize() has no implementation.")

        debugNavHaze(
            "g2.getSize() registerCount=${sizeImplementation.registerCount}"
        )

        val sizeInstructions = sizeImplementation.instructions.toList()

        val returnIndex = sizeInstructions.indexOfLast {
            it.opcode == Opcode.RETURN
        }

        require(returnIndex >= 0) {
            "FixNavigationBarHaze: g2.getSize() return not found."
        }

        val returnInstruction = sizeInstructions[returnIndex]
            as? OneRegisterInstruction
            ?: error("FixNavigationBarHaze: unexpected getSize() return instruction.")

        val resultRegister = returnInstruction.registerA

        mutableSize.addInstructions(
            returnIndex,
            "invoke-static {v$resultRegister}, ${EXTENSION_CLASS}->onBottomBarSizeChanged(I)V"
        )

        debugNavHaze("Diagnostic hooks installed.")
    }
}
