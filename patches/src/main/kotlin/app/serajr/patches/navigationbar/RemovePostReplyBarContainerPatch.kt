package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val REPLY_CLASS = "Lcom/x/composer/minimal/b;"

private const val REPLY_CONTENT =
    "Lcom/x/composer/minimal/b;->c("

@Suppress("unused")
val removePostReplyContainerPatch = bytecodePatch(
    name = "Remove Post Reply Container (Experimental)",
    description = "Tests structural removal of the post reply content while preserving the parent layout and native Haze.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        // 1. Find the parent composable by its parameter signature.
        var original: Method? = null

        classDefForEach { classDef ->
            if (classDef.type == REPLY_CLASS) {
                original = classDef.methods.singleOrNull { method ->
                    method.name == "h" &&
                        method.returnType == "V" &&
                        method.parameterTypes.map { it.toString() } == listOf(
                            "Lcom/x/composer/minimal/r;",
                            "Ldev/chrisbanes/haze/a1;",
                            "Landroidx/compose/ui/Modifier;",
                            "Z",
                            "Lkotlin/jvm/functions/Function1;",
                            "Landroidx/compose/runtime/Composer;",
                            "I",
                            "I"
                        )
                }
            }
        }

        val target = requireNotNull(original) {
            "D3-I1: parent reply composable not found."
        }

        val method = mutableClassDefBy(target.definingClass)
            .methods.first { it.toString() == target.toString() }

        val instructions = requireNotNull(method.implementation) {
            "D3-I1: missing method implementation."
        }.instructions.toList()

        // 2. Identify the unique call that inserts reply content.
        val matches = instructions.indices.filter { index ->
            val instruction = instructions[index]

            instruction.opcode == Opcode.INVOKE_STATIC_RANGE &&
                (instruction as? ReferenceInstruction)
                    ?.reference?.toString()
                    ?.startsWith(REPLY_CONTENT) == true
        }

        require(matches.size == 1) {
            "D3-I1: expected one reply content call, found ${matches.size}."
        }

        // 3. Skip only the reply content composable.
        // Keep the surrounding layout and Compose group closures.
        method.replaceInstruction(matches.single(), "nop")
    }
}
