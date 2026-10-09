
package app.serajr.patches.navigationbar

import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TARGET_CLASS = "Lcom/x/home/tabbed/x;"

@Suppress("unused")
val fixNavBarScrimPatch = bytecodePatch(
    name = "Fix Navigation Bar Scrim",
    description = "Diagnóstico do ponto de integração do Haze na tela inicial.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        val classDef = mutableClassDefBy(TARGET_CLASS)

        val matches = classDef.methods.flatMap { method ->
            val instructions = method.implementation
                ?.instructions
                ?.toList()
                ?: return@flatMap emptyList()

            instructions.mapIndexedNotNull { index, instruction ->
                if (
                    instruction.opcode != Opcode.INVOKE_STATIC &&
                    instruction.opcode != Opcode.INVOKE_STATIC_RANGE
                ) {
                    return@mapIndexedNotNull null
                }

                val reference = (instruction as? ReferenceInstruction)
                    ?.reference as? MethodReference
                    ?: return@mapIndexedNotNull null

                if (
                    reference.definingClass == "Lcom/x/ui/common/tabs/a;" &&
                    reference.name == "h" &&
                    reference.returnType == "V" &&
                    reference.parameterTypes.size == 7
                ) {
                    "${method.name}: instrução $index"
                } else {
                    null
                }
            }
        }

        check(matches.size == 1) {
            "Esperada uma chamada ao Haze superior; " +
                "encontradas ${matches.size}: $matches"
        }

        println(
            "FixNavBarScrimPatch: ponto de integração encontrado em ${matches.single()}"
        )
    }
}
