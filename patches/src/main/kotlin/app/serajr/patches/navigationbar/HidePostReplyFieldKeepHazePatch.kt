package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val REPLY_FIELD_TAG =
    "post-detail-reply-text-field"

private const val TEXT_FIELD_RENDERER =
    "Landroidx/compose/foundation/text/d0;->b("

@Suppress("unused")
val hidePostReplyFieldKeepHazePatch = bytecodePatch(
    name = "Hide Post Reply Field (Keep Haze)",
    description = "Hides the post reply text field while preserving its surrounding Compose container and navigation bar Haze.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        val candidates = mutableListOf<Method>()

        classDefForEach { classDef ->
            classDef.methods.forEach { method ->
                if (method.name != "invoke" ||
                    method.returnType != "Ljava/lang/Object;" ||
                    method.parameterTypes.size != 2
                ) {
                    return@forEach
                }

                val instructions = method.implementation
                    ?.instructions
                    ?.toList()
                    ?: return@forEach

                val hasReplyTag = instructions.any { instruction ->
                    instruction.opcode == Opcode.CONST_STRING &&
                        (instruction as? ReferenceInstruction)
                            ?.reference?.toString() == REPLY_FIELD_TAG
                }

                val hasTextRenderer = instructions.any { instruction ->
                    instruction.opcode == Opcode.INVOKE_STATIC_RANGE &&
                        (instruction as? ReferenceInstruction)
                            ?.reference?.toString()
                            ?.startsWith(TEXT_FIELD_RENDERER) == true
                }

                if (hasReplyTag && hasTextRenderer) {
                    candidates.add(method)
                }
            }
        }

        require(candidates.size == 1) {
            "HidePostReplyFieldKeepHaze: expected exactly one reply field renderer, found ${candidates.size}."
        }

        val original = candidates.single()

        val method = mutableClassDefBy(original.definingClass)
            .methods.first { it.toString() == original.toString() }

        val instructions = requireNotNull(method.implementation) {
            "HidePostReplyFieldKeepHaze: method has no implementation."
        }.instructions.toList()

        val rendererIndices = instructions.indices.filter { index ->
            val instruction = instructions[index]

            instruction.opcode == Opcode.INVOKE_STATIC_RANGE &&
                (instruction as? ReferenceInstruction)
                    ?.reference?.toString()
                    ?.startsWith(TEXT_FIELD_RENDERER) == true
        }

        require(rendererIndices.size == 1) {
            "HidePostReplyFieldKeepHaze: expected exactly one text field invocation."
        }

        method.removeInstruction(rendererIndices.single())
    }
}
