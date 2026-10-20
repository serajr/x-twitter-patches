package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val DEBUG = false

private const val TARGET_CLASS = "Lcom/x/ui/common/h2;"
private const val VISIBILITY_CLASS = "Lcom/x/ui/common/g2;"
private const val CONTENT_CLASS = "Lcom/x/ui/common/e2;"
private const val INSERTION_CLASS = "Lcom/x/ui/common/d2;"

private const val EXTENSION_CLASS =
    "Lapp/serajr/extension/navigationbar/PersistentNavigationHaze;"

private const val NAV_HAZE_STRIP_SMALI = """
    sget-object v0, Landroidx/compose/ui/q;->a:Landroidx/compose/ui/q;
    const/high16 v4, 0x3f800000
    invoke-static {v0, v4}, Landroidx/compose/foundation/layout/n3;->g(Landroidx/compose/ui/Modifier;F)Landroidx/compose/ui/Modifier;
    move-result-object v0
    move/from16 v4, p8
    invoke-static {v0, v4}, Landroidx/compose/foundation/layout/n3;->i(Landroidx/compose/ui/Modifier;F)Landroidx/compose/ui/Modifier;
    move-result-object v0
    move/from16 v4, p6
    if-eqz v4, :nav_haze_strip_plain
    const v4, 0x7a4e0001
    invoke-virtual {v11, v4}, Landroidx/compose/runtime/m0;->h0(I)V
    const/4 v4, 0x6
    invoke-static {v0, v2, v11, v4}, Lcom/x/compose/core/e0;->c(Landroidx/compose/ui/Modifier;Ldev/chrisbanes/haze/w0;Landroidx/compose/runtime/Composer;I)Landroidx/compose/ui/Modifier;
    move-result-object v0
    const/4 v5, 0x0
    invoke-virtual {v11, v5}, Landroidx/compose/runtime/m0;->t(Z)V
    goto :nav_haze_strip_modifier_ready
    :nav_haze_strip_plain
    const v4, 0x7a4e0002
    invoke-virtual {v11, v4}, Landroidx/compose/runtime/m0;->h0(I)V
    const/4 v5, 0x0
    invoke-virtual {v11, v5}, Landroidx/compose/runtime/m0;->t(Z)V
    :nav_haze_strip_modifier_ready
    sget-object v4, Landroidx/compose/ui/b;->i:Landroidx/compose/ui/i;
    sget-object v5, Landroidx/compose/foundation/layout/w;->a:Landroidx/compose/foundation/layout/w;
    invoke-virtual {v5, v0, v4}, Landroidx/compose/foundation/layout/w;->a(Landroidx/compose/ui/Modifier;Landroidx/compose/ui/d;)Landroidx/compose/ui/Modifier;
    move-result-object v0
    invoke-static {v11, v0}, Landroidx/compose/ui/a;->d(Landroidx/compose/runtime/Composer;Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;
    move-result-object v0
    sget-object v4, Landroidx/compose/ui/b;->a:Landroidx/compose/ui/i;
    const/4 v5, 0x0
    invoke-static {v4, v5}, Landroidx/compose/foundation/layout/r;->d(Landroidx/compose/ui/d;Z)Landroidx/compose/ui/layout/a1;
    move-result-object v4
    iget-wide v9, v11, Landroidx/compose/runtime/m0;->T:J
    invoke-static {v9, v10}, Ljava/lang/Long;->hashCode(J)I
    move-result v9
    invoke-virtual {v11}, Landroidx/compose/runtime/m0;->n()Landroidx/compose/runtime/internal/k;
    move-result-object v10
    invoke-virtual {v11}, Landroidx/compose/runtime/m0;->l0()V
    iget-boolean v5, v11, Landroidx/compose/runtime/m0;->S:Z
    sget-object v12, Landroidx/compose/ui/node/i0;->T:Landroidx/compose/material3/g1;
    if-eqz v5, :nav_haze_strip_reuse
    invoke-virtual {v11, v12}, Landroidx/compose/runtime/m0;->m(Lkotlin/jvm/functions/Function0;)V
    goto :nav_haze_strip_emitted
    :nav_haze_strip_reuse
    invoke-virtual {v11}, Landroidx/compose/runtime/m0;->u0()V
    :nav_haze_strip_emitted
    sget-object v5, Landroidx/compose/ui/node/g;->f:Landroidx/compose/ui/node/f;
    invoke-static {v11, v4, v5}, Landroidx/compose/runtime/i;->A(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V
    sget-object v4, Landroidx/compose/ui/node/g;->e:Landroidx/compose/ui/node/f;
    invoke-static {v11, v10, v4}, Landroidx/compose/runtime/i;->A(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V
    invoke-static {v9}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v4
    sget-object v5, Landroidx/compose/ui/node/g;->i:Landroidx/compose/ui/node/f;
    invoke-static {v11, v4, v5}, Landroidx/compose/runtime/i;->A(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V
    invoke-static {v11}, Landroidx/compose/runtime/i;->x(Landroidx/compose/runtime/Composer;)V
    sget-object v4, Landroidx/compose/ui/node/g;->c:Landroidx/activity/compose/z;
    invoke-static {v11, v0, v4}, Landroidx/compose/runtime/i;->A(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V
    invoke-virtual {v11, v3}, Landroidx/compose/runtime/m0;->t(Z)V
"""

private fun debugNavHaze(message: String) {
    if (DEBUG) println("FixNavigationBarHaze -> $message")
}

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description = "Preserves native navigation bar Haze when the bottom bar hides (X 12.31 experimental).",
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

        // Phase 1: Persistent navigation-bar haze strip.
        val targetImplementation = mutableTarget.implementation
            ?: error("FixNavigationBarHaze: h2.a() has no implementation.")

        val instructions = targetImplementation.instructions.toList()

        val insertionPoint = instructions.indices.filter { index ->
            val invoke = instructions[index]
            if (invoke.opcode != Opcode.INVOKE_STATIC) return@filter false

            val reference = (invoke as? ReferenceInstruction)
                ?.reference as? MethodReference ?: return@filter false
            val call = invoke as? Instruction35c ?: return@filter false

            val matchesContentCall =
                reference.definingClass == INSERTION_CLASS &&
                    reference.name == "a" &&
                    reference.returnType == "V" &&
                    reference.parameterTypes.map { it.toString() } == listOf(
                        "Lcom/x/ui/common/a2;",
                        "Landroidx/compose/runtime/internal/f;",
                        "Landroidx/compose/runtime/Composer;",
                        "I"
                    ) &&
                    call.registerCount == 4 &&
                    call.registerC == 0 &&
                    call.registerD == 4 &&
                    call.registerE == 11 &&
                    call.registerF == 5

            if (!matchesContentCall || index + 1 >= instructions.size) return@filter false

            val close = instructions[index + 1]
            if (close.opcode != Opcode.INVOKE_VIRTUAL) return@filter false

            val closeReference = (close as? ReferenceInstruction)
                ?.reference as? MethodReference ?: return@filter false
            val closeCall = close as? Instruction35c ?: return@filter false

            closeReference.definingClass == "Landroidx/compose/runtime/m0;" &&
                closeReference.name == "t" &&
                closeReference.parameterTypes.map { it.toString() } == listOf("Z") &&
                closeReference.returnType == "V" &&
                closeCall.registerCount == 2 &&
                closeCall.registerC == 11 &&
                closeCall.registerD == 3
        }

        require(insertionPoint.size == 1) {
            "FixNavigationBarHaze: d2.a()/t(Z) insertion point not unique (found=${insertionPoint.size})."
        }

        val contentIndex = insertionPoint.first()

        // Version-specific guard: this exact build loads 0x6000 into v5
        // immediately before the screen-content call.
        val previous = instructions.getOrNull(contentIndex - 1)
            as? com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21s
        require(
            previous?.opcode == Opcode.CONST_16 &&
                previous.registerA == 5 &&
                previous.narrowLiteral.toInt() == 0x6000
        ) {
            "FixNavigationBarHaze: unexpected X 12.31 screen-content call site."
        }

        // This experimental build uses the register map verified in h2.a():
        // v2 = p1 (HazeState), v3 = true, v11 = Composer,
        // p6 = useHaze, p8 = navigation inset height in dp.
        // Scratch: v0, v4, v5, v9, v10, v12.
        // The guards above are version-specific, not a general data-flow proof.

        mutableTarget.addInstructions(
            contentIndex + 1,
            NAV_HAZE_STRIP_SMALI
        )

        debugNavHaze("Nav haze strip inserted at index ${contentIndex + 1}.")
        debugNavHaze("Diagnostic hooks installed.")
    }
}
