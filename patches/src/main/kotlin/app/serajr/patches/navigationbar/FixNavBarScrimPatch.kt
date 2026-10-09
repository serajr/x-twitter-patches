
package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction3rc
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TARGET_CLASS = "Lcom/x/home/tabbed/x;"

@Suppress("unused")
val fixNavBarScrimPatch = bytecodePatch(
    name = "Fix Navigation Bar Scrim",
    description = "Teste experimental de uma segunda chamada ao Haze.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        val classDef = mutableClassDefBy(TARGET_CLASS)

        val method = classDef.methods.firstOrNull {
            it.name == "q" && it.returnType == "Ljava/lang/Object;"
        } ?: error("Método q não encontrado.")

        val instructions = method.implementation
            ?.instructions
            ?.toList()
            ?: error("Método sem implementação.")

        val matches = instructions.mapIndexedNotNull { index, instruction ->
            if (
                instruction.opcode != Opcode.INVOKE_STATIC &&
                instruction.opcode != Opcode.INVOKE_STATIC_RANGE
            ) return@mapIndexedNotNull null

            val reference = (instruction as? ReferenceInstruction)
                ?.reference as? MethodReference
                ?: return@mapIndexedNotNull null

            if (
                reference.definingClass == "Lcom/x/ui/common/tabs/a;" &&
                reference.name == "h" &&
                reference.returnType == "V" &&
                reference.parameterTypes.size == 7
            ) index else null
        }

        check(matches.size == 1) {
            "Esperada uma chamada ao Haze; encontradas ${matches.size}."
        }

        val index = matches.single()
        val original = instructions[index]

        val reference = (original as ReferenceInstruction)
            .reference as MethodReference

        val descriptor = buildString {
            append(reference.definingClass)
            append("->")
            append(reference.name)
            append("(")
            reference.parameterTypes.forEach { append(it) }
            append(")")
            append(reference.returnType)
        }

        val invokeInstruction = when (original) {
            is Instruction35c -> {
                val registers = listOf(
                    original.registerC,
                    original.registerD,
                    original.registerE,
                    original.registerF,
                    original.registerG
                ).take(original.registerCount)

                val registerText = registers.joinToString(", ") { "v$it" }

                "invoke-static {$registerText}, $descriptor"
            }

            is Instruction3rc -> {
                val start = original.startRegister
                val count = original.registerCount

                check(count > 0) {
                    "Chamada Haze sem registradores."
                }

                val end = start + count - 1

                "invoke-static/range {v$start .. v$end}, $descriptor"
            }

            else -> error(
                "Formato de instrução não suportado: ${original.javaClass.name}"
            )
        }

        method.addInstructions(
            index + 1,
            invokeInstruction
        )

        println(
            "FixNavBarScrimPatch: segunda chamada ao Haze inserida " +
                "após a instrução $index (${original.opcode})."
        )
    }
}
