package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.iface.Method

private const val DEBUG = false

private const val TARGET_CLASS = "Lcom/x/ui/common/h2;"
private const val VISIBILITY_CLASS = "Lcom/x/ui/common/g2;"

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
                }
            }
        }

        // Hook 1: extension initialization in h2.a()
        val method = targetMethod
            ?: error("FixNavigationBarHaze: h2.a() not found.")

        val mutableClass = mutableClassDefBy(method.definingClass)

        val mutableMethod = mutableClass.methods.firstOrNull {
            it.toString() == method.toString()
        } ?: error("FixNavigationBarHaze: mutable h2.a() not found.")

        val implementation = mutableMethod.implementation
            ?: error("FixNavigationBarHaze: h2.a() has no implementation.")

        require(implementation.registerCount == 33) {
            "FixNavigationBarHaze: unexpected h2.a() register layout."
        }

        mutableMethod.addInstructions(
            0,
            "invoke-static {}, ${EXTENSION_CLASS}->initialize()V"
        )

        // Hook 2: actual visibility state changes in g2.setVisible(Z)
        val g2Method = visibilityMethod
            ?: error("FixNavigationBarHaze: g2.setVisible(Z) not found.")

        val mutableG2Class = mutableClassDefBy(g2Method.definingClass)

        val mutableG2Method = mutableG2Class.methods.firstOrNull {
            it.toString() == g2Method.toString()
        } ?: error("FixNavigationBarHaze: mutable g2.setVisible(Z) not found.")

        val g2Implementation = mutableG2Method.implementation
            ?: error("FixNavigationBarHaze: g2.setVisible(Z) has no implementation.")

        require(g2Implementation.registerCount == 2) {
            "FixNavigationBarHaze: unexpected g2.setVisible(Z) register layout."
        }

        mutableG2Method.addInstructions(
            0,
            "invoke-static {p1}, ${EXTENSION_CLASS}->onBottomBarVisibilityChanged(Z)V"
        )

        // Hook 3: Diagnostic hook: check whether e2 content is invoked
        // while the bottom tabs are being hidden.
        var e2Method: Method? = null

        classDefForEach { classDef ->
            if (classDef.type.toString() == "Lcom/x/ui/common/e2;") {
                e2Method = classDef.methods.firstOrNull { method ->
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

        val contentMethod = e2Method
            ?: error("FixNavigationBarHaze: e2.invoke() not found.")

        val mutableE2Class = mutableClassDefBy(contentMethod.definingClass)

        val mutableE2Method = mutableE2Class.methods.firstOrNull {
            it.toString() == contentMethod.toString()
        } ?: error("FixNavigationBarHaze: mutable e2.invoke() not found.")

        require(mutableE2Method.implementation?.registerCount == 22) {
            "FixNavigationBarHaze: unexpected e2.invoke() register layout."
        }

        mutableE2Method.addInstructions(
            0,
            "invoke-static {}, ${EXTENSION_CLASS}->onBottomBarContentComposed()V"
        )
        
        // Hook 4
        var sizeMethod: Method? = null

        classDefForEach { classDef ->
            if (classDef.type.toString() == VISIBILITY_CLASS) {
                sizeMethod = classDef.methods.firstOrNull { method ->
                    method.name == "getSize" &&
                        method.returnType == "I" &&
                        method.parameterTypes.isEmpty()
                }
            }
        }
        
        val originalSizeMethod = sizeMethod
            ?: error("FixNavigationBarHaze: g2.getSize() not found.")
        
        val mutableSizeClass =
            mutableClassDefBy(originalSizeMethod.definingClass)
        
        val mutableSizeMethod = mutableSizeClass.methods.firstOrNull {
            it.toString() == originalSizeMethod.toString()
        } ?: error("FixNavigationBarHaze: mutable g2.getSize() not found.")
        
        val sizeImplementation = mutableSizeMethod.implementation
            ?: error("FixNavigationBarHaze: g2.getSize() has no implementation.")
        
        debugNavHaze(
            "g2.getSize() registerCount=${sizeImplementation.registerCount}"
        )
        
        // The original method returns an integer.
        // Inject immediately before its RETURN instruction,
        // using the same register as the return value.
        val sizeInstructions = sizeImplementation.instructions.toList()
        
        val returnIndex = sizeInstructions.indexOfLast {
            it.opcode == com.android.tools.smali.dexlib2.Opcode.RETURN
        }
        
        require(returnIndex >= 0) {
            "FixNavigationBarHaze: getSize() return not found."
        }
        
        val returnInstruction = sizeInstructions[returnIndex]
            as? com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
            ?: error("FixNavigationBarHaze: unexpected getSize() return instruction.")
        
        val resultRegister = returnInstruction.registerA
        
        mutableSizeMethod.addInstructions(
            returnIndex,
            """
                invoke-static {v$resultRegister}, ${EXTENSION_CLASS}->onBottomBarSizeChanged(I)V
            """.trimIndent()
        )


        // EXPERIMENT: Remove the bottom bar container clip.
        // Keep the original visibility and size animations unchanged.

        val clipInstructions = mutableMethod.implementation
            ?.instructions
            ?.toList()
            ?: error("FixNavigationBarHaze: h2.a() has no instructions.")

        val clipMatches = clipInstructions.withIndex().filter { (_, instruction) ->
            val referenceInstruction = instruction as?
                com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

            referenceInstruction?.reference?.toString() ==
                "Landroidx/compose/ui/draw/h;->d(Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;"
        }

        require(clipMatches.size == 1) {
            "FixNavigationBarHaze: expected one bottom bar clip, found ${clipMatches.size}."
        }

        val clipIndex = clipMatches.single().index

        // Original:
        // invoke-static {v1}, ...draw/h;->d(...)Modifier;
        // move-result-object v12
        //
        // Experimental:
        // move-object v12, v1
        //
        // Replace both original instructions with a direct Modifier copy.
        mutableMethod.removeInstructions(clipIndex, 2)
        mutableMethod.addInstructions(
            clipIndex,
            "move-object v12, v1"
        )

        
        debugNavHaze("Hooks injected.")
    }
}
