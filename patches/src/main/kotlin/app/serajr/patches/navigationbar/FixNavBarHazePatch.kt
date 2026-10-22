package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val DEBUG = false

private const val TARGET_CLASS = "Lcom/x/ui/common/p2;"
private const val VISIBILITY_CLASS = "Lcom/x/ui/common/o2;"
private const val CONTENT_CLASS = "Lcom/x/ui/common/m2;"

private const val EXTENSION_CLASS =
    "Lapp/serajr/extension/navigationbar/PersistentNavigationHaze;"

private fun debugNavHaze(message: String) {
    if (DEBUG) println("FixNavigationBarHaze -> $message")
}

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description = "Phase A isolation: tests bottom bar diagnostic hooks without Compose node injection on X 12.35!",
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
                                "Landroidx/compose/runtime/internal/g;",
                                "Ldev/chrisbanes/haze/x0;",
                                "Lkotlin/jvm/functions/Function1;",
                                "Landroidx/compose/ui/Modifier;",
                                "Landroidx/compose/runtime/internal/g;",
                                "Z",
                                "Z",
                                "Z",
                                "Landroidx/compose/runtime/internal/g;",
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
            ?: error("FixNavigationBarHaze: p2.a() not found.")

        val mutableTarget = mutableClassDefBy(originalTarget.definingClass)
            .methods.firstOrNull { it.toString() == originalTarget.toString() }
            ?: error("FixNavigationBarHaze: mutable p2.a() not found.")

        require(mutableTarget.implementation?.registerCount == 37) {
            "FixNavigationBarHaze: unexpected p2.a() register layout."
        }

        mutableTarget.addInstructions(
            0,
            "invoke-static {}, ${EXTENSION_CLASS}->initialize()V"
        )

        // Hook 2: Monitor bottom bar visibility changes.
        val originalVisibility = visibilityMethod
            ?: error("FixNavigationBarHaze: o2.setVisible(Z) not found.")

        val mutableVisibility = mutableClassDefBy(originalVisibility.definingClass)
            .methods.firstOrNull { it.toString() == originalVisibility.toString() }
            ?: error("FixNavigationBarHaze: mutable o2.setVisible(Z) not found.")

        require(mutableVisibility.implementation?.registerCount == 2) {
            "FixNavigationBarHaze: unexpected o2.setVisible(Z) register layout."
        }

        mutableVisibility.addInstructions(
            0,
            "invoke-static {p1}, ${EXTENSION_CLASS}->onBottomBarVisibilityChanged(Z)V"
        )

        // Hook 3: Monitor bottom bar content composition.
        val originalContent = contentMethod
            ?: error("FixNavigationBarHaze: m2.invoke() not found.")

        val mutableContent = mutableClassDefBy(originalContent.definingClass)
            .methods.firstOrNull { it.toString() == originalContent.toString() }
            ?: error("FixNavigationBarHaze: mutable m2.invoke() not found.")

        require(mutableContent.implementation?.registerCount == 22) {
            "FixNavigationBarHaze: unexpected m2.invoke() register layout."
        }

        mutableContent.addInstructions(
            0,
            "invoke-static {}, ${EXTENSION_CLASS}->onBottomBarContentComposed()V"
        )

        // Hook 4: Monitor the animated bottom bar size.
        val originalSize = sizeMethod
            ?: error("FixNavigationBarHaze: o2.getSize() not found.")

        val mutableSize = mutableClassDefBy(originalSize.definingClass)
            .methods.firstOrNull { it.toString() == originalSize.toString() }
            ?: error("FixNavigationBarHaze: mutable o2.getSize() not found.")

        val sizeImplementation = mutableSize.implementation
            ?: error("FixNavigationBarHaze: o2.getSize() has no implementation.")

        debugNavHaze(
            "o2.getSize() registerCount=${sizeImplementation.registerCount}"
        )

        val sizeInstructions = sizeImplementation.instructions.toList()

        val returnIndex = sizeInstructions.indexOfLast {
            it.opcode == Opcode.RETURN
        }

        require(returnIndex >= 0) {
            "FixNavigationBarHaze: o2.getSize() return not found."
        }

        val returnInstruction = sizeInstructions[returnIndex]
            as? OneRegisterInstruction
            ?: error("FixNavigationBarHaze: unexpected getSize() return instruction.")

        val resultRegister = returnInstruction.registerA

        mutableSize.addInstructions(
            returnIndex,
            "invoke-static {v$resultRegister}, ${EXTENSION_CLASS}->onBottomBarSizeChanged(I)V"
        )

        // Phase A isolation: do not modify k2.a().
        // The prior inline Box produced malformed DEX branch targets.
        // Validate the four independent diagnostic hooks before reintroducing UI nodes.
        debugNavHaze("Four diagnostic hooks installed.")
    }
}
