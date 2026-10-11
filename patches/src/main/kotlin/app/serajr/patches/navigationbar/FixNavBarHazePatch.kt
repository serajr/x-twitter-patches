
package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants

import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22x
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val DEBUG = false

private const val HAZE_STATE =
    "Ldev/chrisbanes/haze/w0;"

private const val BOTTOM_BAR_ANIMATION =
    "BottomBarOffsetAnimation"

private fun debugHaze(message: String) {
    if (DEBUG) {
        println("FixNavigationBarHaze -> $message")
    }
}

private fun Method.isBottomBarMethod(): Boolean {
    if (returnType != "V") return false

    if (parameterTypes.none {
            it.toString() == HAZE_STATE
        }
    ) return false

    return implementation?.instructions?.any { instruction ->
        val reference =
            (instruction as? ReferenceInstruction)?.reference
                as? StringReference

        reference?.string == BOTTOM_BAR_ANIMATION
    } == true
}

private fun Method.findOuterModifierCall(): Int {
    val instructions =
        implementation?.instructions?.toList()
            ?: return -1

    val matches = instructions.withIndex().filter { (index, instruction) ->
        if (instruction.opcode != Opcode.INVOKE_STATIC) {
            return@filter false
        }

        val reference =
            (instruction as? ReferenceInstruction)?.reference
                as? MethodReference
                ?: return@filter false

        if (
            reference.definingClass.toString() !=
                "Landroidx/compose/ui/a;" ||
            reference.name != "d" ||
            reference.parameterTypes.size != 2 ||
            reference.returnType !=
                "Landroidx/compose/ui/Modifier;"
        ) {
            return@filter false
        }

        val invoke = instruction as? Instruction35c
            ?: return@filter false

        if (
            invoke.registerCount != 2 ||
            invoke.registerC != 11 ||
            invoke.registerD != 15
        ) {
            return@filter false
        }

        // Verify that v15 receives the original p3 Modifier.
        val registerCount =
            implementation?.registerCount ?: return@filter false

        // h2.a() has ten single-width parameters.
        val p3Register = registerCount - 10 + 3

        instructions
            .subList(maxOf(0, index - 5), index)
            .any { previous ->
                val move = previous as? Instruction22x
                    ?: return@any false

                previous.opcode == Opcode.MOVE_OBJECT_FROM16 &&
                    move.registerA == 15 &&
                    move.registerB == p3Register
            }
    }

    return if (matches.size == 1) {
        matches.single().index
    } else {
        -1
    }
}

@Suppress("unused")
val fixNavigationBarHazePatch = bytecodePatch(
    name = "Fix Navigation Bar Haze",
    description =
        "Experimentally applies the existing Haze effect " +
        "to the bottom navigation container.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        val candidates = mutableListOf<Method>()

        classDefForEach { classDef ->
            classDef.methods.forEach { method ->
                if (
                    method.isBottomBarMethod() &&
                    method.findOuterModifierCall() >= 0
                ) {
                    candidates.add(method)
                }
            }
        }

        require(candidates.size == 1) {
            "FixNavigationBarHaze: expected one target method, " +
                "found ${candidates.size}."
        }

        val method = candidates.single()

        debugHaze("Target method: $method")

        val mutableClass =
            mutableClassDefBy(method.definingClass)

        val mutableMethod =
            mutableClass.methods.firstOrNull {
                it.toString() == method.toString()
            } ?: error(
                "FixNavigationBarHaze: mutable method not found."
            )

        val index = mutableMethod.findOuterModifierCall()

        require(index >= 0) {
            "FixNavigationBarHaze: outer Modifier call not found."
        }

        debugHaze("Insertion index: $index")

        /*
         * At this point in X 12.31.0:
         *
         * v15 = original outer Modifier (p3)
         * v11 = current Composer
         * p1  = existing HazeState
         *
         * v16, v17 and v18 are overwritten later
         * by the original method.
         *
         * This patch targets the validated 12.31.0
         * register layout, not future X versions.
         */

        mutableMethod.addInstructions(
            index,
            """
            move-object/from16 v16, p1
            move-object v17, v11
            const/16 v18, 0x6

            invoke-static/range {v15 .. v18}, Lcom/x/compose/core/e0;->c(Landroidx/compose/ui/Modifier;Ldev/chrisbanes/haze/w0;Landroidx/compose/runtime/Composer;I)Landroidx/compose/ui/Modifier;

            move-result-object v15
            """.trimIndent()
        )

        debugHaze("Outer container Haze injected.")
    }
}
