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

private const val TARGET_CLASS = "Lcom/x/ui/common/p2;"
private const val VISIBILITY_CLASS = "Lcom/x/ui/common/o2;"
private const val CONTENT_CLASS = "Lcom/x/ui/common/m2;"
private const val INSERTION_CLASS = "Lcom/x/ui/common/k2;"

private const val EXTENSION_CLASS =
    "Lapp/serajr/extension/navigationbar/PersistentNavigationHaze;"

private const val PHASE_A_EMPTY_BOX_SMALI = """
    sget-object v3, Landroidx/compose/ui/q;->a:Landroidx/compose/ui/q;
    sget-object v4, Landroidx/compose/foundation/layout/k;->c:Landroidx/compose/foundation/layout/f;
    sget-object v5, Landroidx/compose/ui/b;->m:Landroidx/compose/ui/g;
    const/4 v6, 0x0
    invoke-static {v4, v5, p2, v6}, Landroidx/compose/foundation/layout/z;->a(Landroidx/compose/foundation/layout/j;Landroidx/compose/ui/c;Landroidx/compose/runtime/Composer;I)Landroidx/compose/foundation/layout/a0;
    move-result-object v4
    iget-wide v5, p2, Landroidx/compose/runtime/o0;->T:J
    invoke-static {v5, v6}, Ljava/lang/Long;->hashCode(J)I
    move-result v5
    invoke-virtual {p2}, Landroidx/compose/runtime/o0;->o()Landroidx/compose/runtime/internal/l;
    move-result-object v6
    invoke-static {p2, v3}, Landroidx/compose/ui/a;->d(Landroidx/compose/runtime/Composer;Landroidx/compose/ui/Modifier;)Landroidx/compose/ui/Modifier;
    move-result-object v3
    invoke-virtual {p2}, Landroidx/compose/runtime/o0;->n0()V
    iget-boolean v7, p2, Landroidx/compose/runtime/o0;->S:Z
    if-eqz v7, :phase_a_box_use
    sget-object v7, Landroidx/compose/ui/node/i0;->U:Landroidx/compose/material3/r;
    invoke-virtual {p2, v7}, Landroidx/compose/runtime/o0;->n(Lkotlin/jvm/functions/Function0;)V
    goto :phase_a_box_slots
    :phase_a_box_use
    invoke-virtual {p2}, Landroidx/compose/runtime/o0;->x0()V
    :phase_a_box_slots
    sget-object v7, Landroidx/compose/ui/node/g;->f:Landroidx/compose/ui/node/f;
    invoke-static {p2, v4, v7}, Landroidx/compose/runtime/i;->y(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V
    sget-object v7, Landroidx/compose/ui/node/g;->e:Landroidx/compose/ui/node/f;
    invoke-static {p2, v6, v7}, Landroidx/compose/runtime/i;->y(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V
    invoke-static {v5}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v5
    sget-object v7, Landroidx/compose/ui/node/g;->i:Landroidx/compose/ui/node/f;
    invoke-static {p2, v5, v7}, Landroidx/compose/runtime/i;->y(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V
    invoke-static {p2}, Landroidx/compose/runtime/i;->w(Landroidx/compose/runtime/Composer;)V
    sget-object v7, Landroidx/compose/ui/node/g;->c:Landroidx/activity/compose/y;
    invoke-static {p2, v3, v7}, Landroidx/compose/runtime/i;->y(Landroidx/compose/runtime/Composer;Ljava/lang/Object;Lkotlin/jvm/functions/Function2;)V
"""

private fun debugNavHaze(message: String) {
    if (DEBUG) println("FixNavigationBarHaze -> $message")
}

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description = "Phase A diagnostic: validates an empty Compose node and bottom bar hooks on X 12.35!",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    extendWith("extensions/navigation.mpe")

    execute {
        var targetMethod: Method? = null
        var visibilityMethod: Method? = null
        var contentMethod: Method? = null
        var sizeMethod: Method? = null
        var insertionMethod: Method? = null

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

                INSERTION_CLASS -> {
                    insertionMethod = classDef.methods.firstOrNull { method ->
                        method.name == "a" && method.returnType == "V" &&
                            method.parameterTypes.map { it.toString() } == listOf(
                                "Lcom/x/ui/common/o2;",
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

        // Phase A: emit a visually neutral Box only on the executed movable-group path.
        // In k2.a, v0/v1/v2 and p0..p3 are live across the insertion point;
        // only v3..v7 may be used as scratch. No register-count changes.
        val originalInsertion = insertionMethod
            ?: error("FixNavigationBarHaze: k2.a() not found.")
        val mutableInsertion = mutableClassDefBy(originalInsertion.definingClass)
            .methods.firstOrNull { it.toString() == originalInsertion.toString() }
            ?: error("FixNavigationBarHaze: mutable k2.a() not found.")
        require(mutableInsertion.implementation?.registerCount == 12) {
            "FixNavigationBarHaze: unexpected k2.a() register layout."
        }
        val injectionInstructions = mutableInsertion.implementation!!.instructions.toList()
        val anchorIndices = injectionInstructions.indices.filter { index ->
            val instruction = injectionInstructions[index]
            val ref = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            val call = instruction as? Instruction35c
            if (instruction.opcode != Opcode.INVOKE_STATIC || ref == null || call == null) {
                return@filter false
            }
            val matches = ref.definingClass == "Landroidx/compose/runtime/i;" &&
                ref.name == "c" && ref.returnType == "V" &&
                ref.parameterTypes.map { it.toString() } == listOf(
                    "Landroidx/compose/runtime/i2;",
                    "Lkotlin/jvm/functions/Function2;",
                    "Landroidx/compose/runtime/Composer;",
                    "I"
                ) && call.registerCount == 4 &&
                call.registerC == 0 && call.registerD == 1 &&
                call.registerE == 10 && call.registerF == 2
            if (!matches || index == 0) return@filter false
            val preceding = injectionInstructions[index - 1]
                as? com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21s
            preceding?.opcode == Opcode.CONST_16 &&
                preceding.registerA == 2 && preceding.narrowLiteral.toInt() == 0x38
        }
        require(anchorIndices.size == 1) {
            "FixNavigationBarHaze: expected one safe k2.a() anchor; found ${anchorIndices.size}."
        }
        mutableInsertion.addInstructions(anchorIndices.single(), PHASE_A_EMPTY_BOX_SMALI)
        debugNavHaze("Phase A empty Box injected into k2.a().")
        debugNavHaze("Four diagnostic hooks installed.")
    }
}
