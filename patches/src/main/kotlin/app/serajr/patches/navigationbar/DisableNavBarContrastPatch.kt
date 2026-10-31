
package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
val disableNavBarContrastPatch = bytecodePatch(
    name = "Disable Navigation Bar Contrast",
    description = "Disables Android's automatic navigation bar contrast scrim.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        val targets = setOf(
            "Landroidx/activity/x;",
            "Landroidx/activity/z;"
        )

        val methods = mutableListOf<Method>()

        classDefForEach { classDef ->
            if (classDef.type.toString() in targets) {
                methods += classDef.methods.filter { method ->
                    method.name == "b" &&
                        method.returnType == "V" &&
                        method.parameterTypes.map { it.toString() } == listOf(
                            "Landroidx/activity/m0;",
                            "Landroidx/activity/m0;",
                            "Landroid/view/Window;",
                            "Landroid/view/View;",
                            "Z",
                            "Z"
                        )
                }
            }
        }

        require(methods.size == 2) {
            "DisableNavBarContrast: expected two AndroidX methods."
        }

        methods.forEach { original ->
            val method = mutableClassDefBy(original.definingClass)
                .methods.first { it.toString() == original.toString() }

            val instructions = method.implementation!!.instructions.toList()

            val matches = instructions.indices.filter { index ->
                val instruction = instructions[index]
                instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                    (instruction as? ReferenceInstruction)?.reference?.toString() ==
                    "Landroid/view/Window;->setNavigationBarContrastEnforced(Z)V"
            }

            require(matches.size == 1)

            val index = matches.single()
            val call = instructions[index] as FiveRegisterInstruction
            val windowRegister = call.registerC
            val booleanRegister = call.registerD

            require(windowRegister != booleanRegister)

            method.addInstructions(
                index + 1,
                "const/4 v$booleanRegister, 0x1"
            )

            method.addInstructions(
                index,
                "const/4 v$booleanRegister, 0x0"
            )
        }
    }
}
