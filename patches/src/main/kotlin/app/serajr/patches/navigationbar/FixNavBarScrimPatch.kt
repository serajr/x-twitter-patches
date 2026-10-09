
package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
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

        val method = classDef.methods.firstOrNull { it.name == "q" }
            ?: error("Método q não encontrado.")

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
                reference.returnType == "V"
            ) index else null
        }

        check(matches.size == 1) {
            "Esperada uma chamada ao Haze; encontradas ${matches.size}."
        }

        val index = matches.single()
        val original = instructions[index]

        check(original is Instruction35c) {
            "Formato de instrução inesperado: ${original.javaClass.name}"
        }

        val registers = listOf(
            original.registerC,
            original.registerD,
            original.registerE,
            original.registerF,
            original.registerG
        ).take(original.registerCount)

        val registerText = registers.joinToString(", ") { "v$it" }

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

        method.addInstructions(
            index + 1,
            """
            invoke-static {$registerText}, $descriptor
            """.trimIndent()
        )

        println(
            "FixNavBarScrimPatch: segunda chamada ao Haze inserida após a instrução $index."
        )
    }
}
