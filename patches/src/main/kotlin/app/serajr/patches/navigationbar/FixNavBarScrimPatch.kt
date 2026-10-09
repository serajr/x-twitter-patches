package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val TARGET_CLASS = "Lcom/x/ui/common/tabs/a;"

@Suppress("unused")
val fixNavBarScrimPatch = bytecodePatch(
    name = "Fix Navigation Bar Scrim",
    description = "Teste 3: permitir Modifier personalizado no Haze.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        val classDef = mutableClassDefBy(TARGET_CLASS)

        val method = classDef.methods.firstOrNull {
            it.name == "h" &&
                it.returnType == "V" &&
                it.parameterTypes.size == 7 &&
                it.parameterTypes[2].toString() ==
                    "Landroidx/compose/ui/Modifier;"
        } ?: error("Método tabs.a.h não encontrado.")

        val instructions = method.implementation
            ?.instructions
            ?.toList()
            ?: error("Método sem implementação.")

        val matches = instructions.mapIndexedNotNull { index, instruction ->
            if (instruction.opcode != Opcode.SGET_OBJECT) {
                return@mapIndexedNotNull null
            }

            val reference = (instruction as? ReferenceInstruction)
                ?.reference as? FieldReference
                ?: return@mapIndexedNotNull null

            if (
                reference.definingClass == "Landroidx/compose/ui/q;" &&
                reference.name == "a" &&
                reference.type == "Landroidx/compose/ui/q;"
            ) index else null
        }

        check(matches.size == 1) {
            "Esperada uma referência ao Modifier vazio; " +
                "encontradas ${matches.size}."
        }

        val index = matches.single()

        method.addInstructions(
            index + 1,
            """
            if-eqz p2, :modifier_ready
            move-object v0, p2
            :modifier_ready
            """.trimIndent()
        )

        println(
            "FixNavBarScrimPatch: suporte ao Modifier personalizado " +
                "inserido em tabs.a.h(), após a instrução $index."
        )
    }
}
