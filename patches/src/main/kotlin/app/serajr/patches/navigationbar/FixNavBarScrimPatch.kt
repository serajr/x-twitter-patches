package app.morphe.patches.twitter.misc.navbar

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22x
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val fixNavBarScrimPatch = bytecodePatch(
    name = "Fix navigation bar scrim",
    description = "Diagnostic test 6: keep X bottom bar AnimatedVisibility active."
) {
    execute {
        val targetClass = findClass("Lcom/x/ui/common/h2;")
            ?: error("Classe h2 não encontrada")

        val method = targetClass.methods.singleOrNull {
            it.name == "a" &&
                it.parameterTypes.size == 10 &&
                it.returnType == "V"
        } ?: error("Método h2.a não encontrado ou ambíguo")

        val instructions = method.implementation?.instructions
            ?: error("Método h2.a sem instruções")

        val matches = instructions.withIndex().filter { (_, instruction) ->
            if (instruction.opcode != Opcode.INVOKE_STATIC_RANGE) {
                false
            } else {
                val reference = (instruction as? ReferenceInstruction)
                    ?.reference as? MethodReference

                reference?.definingClass == "Landroidx/compose/animation/p;" &&
                    reference.name == "h" &&
                    reference.parameterTypes.size == 10
            }
        }

        require(matches.size == 1) {
            "Esperada uma chamada AnimatedVisibility; encontradas ${matches.size}"
        }

        val callIndex = matches.single().index

        // A instrução original está 6 posições antes da chamada.
        val replacementIndex = callIndex - 6

        val original = instructions.elementAt(replacementIndex)

        require(
            original.opcode == Opcode.MOVE_FROM16 &&
                (original as? Instruction22x)?.registerA == 11 &&
                original.registerB == 19
        ) {
            "Instrução esperada 'move/from16 v11, v19' não encontrada " +
                "na posição prevista."
        }

        method.replaceInstruction(
            replacementIndex,
            "const/4 v11, 0x1"
        )
    }
}
