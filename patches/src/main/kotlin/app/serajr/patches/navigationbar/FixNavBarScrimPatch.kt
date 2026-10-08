package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TARGET_CLASS = "Lcom/x/compose/navbars/h;"
private const val TARGET_METHOD = "f"

@Suppress("unused")
val fixNavBarScrimPatch = bytecodePatch(
    name = "Fix Navigation Bar Scrim",
    description = "Testa a preservação da configuração original de contraste da navigation bar sem alterar o edge-to-edge.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        val classDef = mutableClassDefBy(TARGET_CLASS)

        val method = classDef.methods.firstOrNull {
            it.name == TARGET_METHOD &&
            it.returnType == "V" &&
            it.parameterTypes.size == 3 &&
            it.parameterTypes[0].toString() == "Lcom/x/compose/navbars/l;" &&
            it.parameterTypes[1].toString() == "Z" &&
            it.parameterTypes[2].toString() == "Landroid/app/Activity;"
        } ?: error("Método de configuração das barras não encontrado.")

        val instructions = method.implementation
            ?.instructions
            ?.toList()
            ?: error("Método sem implementação.")

        val matches = instructions.mapIndexedNotNull { index, instruction ->
            if (instruction.opcode != Opcode.INVOKE_VIRTUAL) {
                return@mapIndexedNotNull null
            }

            val reference = (instruction as? ReferenceInstruction)
                ?.reference as? MethodReference
                ?: return@mapIndexedNotNull null

            if (
                reference.definingClass == "Landroid/view/Window;" &&
                reference.name == "setNavigationBarContrastEnforced" &&
                reference.parameterTypes.size == 1 &&
                reference.parameterTypes[0].toString() == "Z" &&
                reference.returnType == "V"
            ) index else null
        }

        check(matches.size == 1) {
            "Esperada uma chamada de contraste; encontradas ${matches.size}."
        }

        method.replaceInstruction(matches.single(), "nop")

        println(
            "FixNavBarScrimPatch: sobrescrita de contraste removida."
        )
    }
}
