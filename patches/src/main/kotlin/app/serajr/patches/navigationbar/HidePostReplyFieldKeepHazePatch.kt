package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val REPLY_COMPOSER_CLASS =
    "Lcom/x/composer/minimal/b;"

private val RENDER_CALLS = listOf(
    "Lcom/x/ui/common/user/z;->l(",
    "Lcom/x/composer/minimal/b;->d(",
    "Landroidx/compose/animation/g;->i(",
    "Landroidx/compose/material3/x;->h(",
    "Landroidx/compose/animation/g;->h("
)

@Suppress("unused")
val hidePostReplyBarKeepHazePatch = bytecodePatch(
    name = "Hide Post Reply Bar (Keep Haze)",
    description = "Hides post reply controls while preserving the original Haze container.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY_X)

    execute {
        var targetMethod: Method? = null

        classDefForEach { classDef ->
            if (classDef.type != REPLY_COMPOSER_CLASS) {
                return@classDefForEach
            }

            targetMethod = classDef.methods.firstOrNull { method ->
                method.name == "e" &&
                    method.returnType == "V" &&
                    method.parameterTypes.size == 23 &&
                    method.parameterTypes.firstOrNull()
                        ?.toString() == "Ldev/chrisbanes/haze/a1;" &&
                    method.parameterTypes.getOrNull(20)
                        ?.toString() == "Landroidx/compose/runtime/Composer;"
            }
        }

        val original = requireNotNull(targetMethod) {
            "HidePostReplyBarKeepHaze: reply composer method not found."
        }

        val method = mutableClassDefBy(original.definingClass)
            .methods.first { it.toString() == original.toString() }

        val implementation = requireNotNull(method.implementation) {
            "HidePostReplyBarKeepHaze: method has no implementation."
        }

        val instructions = implementation.instructions.toList()

        val matches = instructions.indices.filter { index ->
            val instruction = instructions[index]

            if (
                instruction.opcode != Opcode.INVOKE_STATIC &&
                instruction.opcode != Opcode.INVOKE_STATIC_RANGE
            ) {
                return@filter false
            }

            val reference = (
                instruction as? ReferenceInstruction
            )?.reference?.toString() ?: return@filter false

            RENDER_CALLS.any { reference.startsWith(it) }
        }

        val expectedCounts = mapOf(
            "Lcom/x/ui/common/user/z;->l(" to 1,
            "Lcom/x/composer/minimal/b;->d(" to 1,
            "Landroidx/compose/animation/g;->i(" to 2,
            "Landroidx/compose/material3/x;->h(" to 1,
            "Landroidx/compose/animation/g;->h(" to 1
        )

        expectedCounts.forEach { (prefix, expected) ->
            val actual = matches.count { index ->
                val reference = (
                    instructions[index] as ReferenceInstruction
                ).reference.toString()

                reference.startsWith(prefix)
            }

            require(actual == expected) {
                "HidePostReplyBarKeepHaze: expected $expected " +
                    "calls for $prefix, found $actual."
            }
        }

        require(matches.size == 6) {
            "HidePostReplyBarKeepHaze: unexpected render call count."
        }

        matches.sortedDescending().forEach { index ->
            method.replaceInstruction(index, "nop")
        }
    }
}
