package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TARGET_CLASS = "Lcom/x/compose/navbars/h;"

@Suppress("unused")
val fixNavBarScrimPatch = bytecodePatch(
    name = "Fix Navigation Bar Scrim",
    description = "Teste 5: pintar a barra de navegação de vermelho.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        val classDef = mutableClassDefBy(TARGET_CLASS)

        val method = classDef.methods.firstOrNull {
            it.name == "f" &&
                it.returnType == "V" &&
                it.parameterTypes.size == 3 &&
                it.parameterTypes[2].toString() == "Landroid/app/Activity;"
        } ?: error("Método navbars.h.f não encontrado.")

        val instructions = method.implementation
            ?.instructions
            ?.toList()
            ?: error("Método sem implementação.")

        val matches = instructions.mapIndexedNotNull { index, instruction ->
            if (
                instruction.opcode != Opcode.INVOKE_VIRTUAL &&
                instruction.opcode != Opcode.INVOKE_VIRTUAL_RANGE
            ) return@mapIndexedNotNull null

            val reference = (instruction as? ReferenceInstruction)
                ?.reference as? MethodReference
                ?: return@mapIndexedNotNull null

            if (
                reference.definingClass == "Landroid/view/Window;" &&
                reference.name == "setNavigationBarContrastEnforced" &&
                reference.parameterTypes.size == 1
            ) index else null
        }

        check(matches.size == 1) {
            "Esperada uma chamada ao contraste da navbar; " +
                "encontradas ${matches.size}."
        }

        val index = matches.single()
        val original = instructions[index]

        // Recupera o registrador que contém o Window.
        val windowRegister = when (original) {
            is Instruction35c -> original.registerC
            is Instruction3rc -> original.startRegister
            else -> error("Formato de invoke não suportado.")
        }

        // Reutiliza o registrador do booleano como inteiro de cor.
        val colorRegister = when (original) {
            is Instruction35c -> original.registerD
            is Instruction3rc -> original.startRegister + 1
            else -> error("Formato de invoke não suportado.")
        }

        // Substitui a chamada original por NOP e insere o teste.
        method.addInstructions(
            index,
            """
            const v$colorRegister, -0x10000
            invoke-virtual {v$windowRegister, v$colorRegister}, Landroid/view/Window;->setNavigationBarColor(I)V
            """.trimIndent()
        )

        println(
            "FixNavBarScrimPatch: teste de cor vermelha inserido " +
                "antes de setNavigationBarContrastEnforced()."
        )
    }
}
