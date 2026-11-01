package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val REPLY_COMPOSER_CLASS =
    "Lcom/x/composer/minimal/b;"

private const val BACKGROUND_COLOR_FIELD =
    "Lcom/x/compose/theme/e;->r:J"

private const val BACKGROUND_MODIFIER =
    "Landroidx/compose/foundation/p;->l("

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
    description = "Hides post reply controls and their background while preserving the original navigation bar Haze.",
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

        fun referenceAt(index: Int): String? =
            (instructions[index] as? ReferenceInstruction)
                ?.reference?.toString()

        // D3-B: Suppress only the six rendering calls.
        val renderIndices = instructions.indices.filter { index ->
            val opcode = instructions[index].opcode

            (opcode == Opcode.INVOKE_STATIC ||
                opcode == Opcode.INVOKE_STATIC_RANGE) &&
                RENDER_CALLS.any { prefix ->
                    referenceAt(index)?.startsWith(prefix) == true
                }
        }

        val expectedCounts = mapOf(
            "Lcom/x/ui/common/user/z;->l(" to 1,
            "Lcom/x/composer/minimal/b;->d(" to 1,
            "Landroidx/compose/animation/g;->i(" to 2,
            "Landroidx/compose/material3/x;->h(" to 1,
            "Landroidx/compose/animation/g;->h(" to 1
        )

        expectedCounts.forEach { (prefix, expected) ->
            val actual = renderIndices.count { index ->
                referenceAt(index)?.startsWith(prefix) == true
            }

            require(actual == expected) {
                "HidePostReplyBarKeepHaze: expected $expected " +
                    "render calls for $prefix, found $actual."
            }
        }

        require(renderIndices.size == 6) {
            "HidePostReplyBarKeepHaze: unexpected render call count."
        }

        // D3-C: Locate the theme color feeding the rounded
        // background modifier. Do not remove the modifier.
        val backgroundIndices = instructions.indices.filter { index ->
            if (instructions[index].opcode != Opcode.IGET_WIDE ||
                referenceAt(index) != BACKGROUND_COLOR_FIELD
            ) {
                return@filter false
            }

            val next = index + 1
            val following = index + 2

            next < instructions.size &&
                following < instructions.size &&
                instructions[next].opcode == Opcode.INVOKE_STATIC &&
                referenceAt(next)?.startsWith(BACKGROUND_MODIFIER) == true &&
                instructions[following].opcode == Opcode.MOVE_RESULT_OBJECT
        }

        require(backgroundIndices.size == 1) {
            "HidePostReplyBarKeepHaze: expected one background color site, " +
                "found ${backgroundIndices.size}."
        }

        // Apply modifications in descending order to avoid
        // shifting instruction indices.
        val replacements = (
            renderIndices.map { it to "nop" } +
                listOf(
                    backgroundIndices.single() to
                        "const-wide/16 v4, 0x0"
                )
            ).sortedByDescending { it.first }

        replacements.forEach { (index, smali) ->
            method.replaceInstruction(index, smali)
        }
    }
}
