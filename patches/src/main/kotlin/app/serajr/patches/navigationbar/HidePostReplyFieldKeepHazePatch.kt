package app.serajr.patches.navigationbar

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.serajr.patches.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction

private const val REPLY_COMPOSER_CLASS =
    "Lcom/x/composer/minimal/b;"

private const val OUTER_SURFACE =
    "Landroidx/compose/foundation/p;->l("

private const val INNER_CLICKABLE =
    "Landroidx/compose/foundation/e0;->c("

private const val HAZE_MODIFIER =
    "Lcom/x/compose/core/m0;->e("

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
    description = "Hides post reply controls, surface and touch modifier while preserving Haze.",
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
            "D3-D: reply composer method not found."
        }

        val method = mutableClassDefBy(original.definingClass)
            .methods.first { it.toString() == original.toString() }

        val implementation = requireNotNull(method.implementation) {
            "D3-D: method has no implementation."
        }

        val instructions = implementation.instructions.toList()

        fun referenceAt(index: Int): String? =
            (instructions[index] as? ReferenceInstruction)
                ?.reference?.toString()

        fun matchingInvokes(prefix: String): List<Int> =
            instructions.indices.filter { index ->
                val opcode = instructions[index].opcode

                (opcode == Opcode.INVOKE_STATIC ||
                    opcode == Opcode.INVOKE_STATIC_RANGE) &&
                    referenceAt(index)?.startsWith(prefix) == true
            }

        // Preserve the six successful D3-B removals.
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
                "D3-D: expected $expected calls for $prefix, found $actual."
            }
        }

        require(renderIndices.size == 6) {
            "D3-D: unexpected renderer call count."
        }

        // Verify the original Haze hook is still present.
        require(matchingInvokes(HAZE_MODIFIER).size == 1) {
            "D3-D: expected exactly one Haze modifier."
        }

        // Remove only the outer visual modifier.
        // The original v2 Modifier remains available.
        val outerCandidates = matchingInvokes(OUTER_SURFACE)
            .filter { index ->
                val call = instructions[index] as? FiveRegisterInstruction
                val result = instructions.getOrNull(index + 1)
                    as? OneRegisterInstruction

                call != null &&
                    call.registerC == 2 &&
                    call.registerD == 3 &&
                    call.registerE == 4 &&
                    call.registerF == 5 &&
                    call.registerG == 11 &&
                    instructions.getOrNull(index + 1)
                        ?.opcode == Opcode.MOVE_RESULT_OBJECT &&
                    result?.registerA == 2
            }

        require(outerCandidates.size == 1) {
            "D3-D: expected one outer surface, " +
                "found ${outerCandidates.size}."
        }

        val outerIndex = outerCandidates.single()

        // Remove only the inner clickable modifier.
        // Pass its incoming v16 Modifier directly to v6.
        val clickableCandidates = matchingInvokes(INNER_CLICKABLE)
            .filter { index ->
                val call = instructions[index] as? RegisterRangeInstruction
                val result = instructions.getOrNull(index + 1)
                    as? OneRegisterInstruction

                call?.startRegister == 16 &&
                    call.registerCount == 7 &&
                    instructions.getOrNull(index + 1)
                        ?.opcode == Opcode.MOVE_RESULT_OBJECT &&
                    result?.registerA == 6
            }

        require(clickableCandidates.size == 1) {
            "D3-D: expected one inner clickable, " +
                "found ${clickableCandidates.size}."
        }

        val clickableIndex = clickableCandidates.single()

        val replacements = mutableListOf<Pair<Int, String>>()

        renderIndices.forEach { index ->
            replacements += index to "nop"
        }

        // Keep the incoming v2 Modifier instead of the
        // rounded surface modifier.
        replacements += outerIndex to "nop"
        replacements += (outerIndex + 1) to "nop"

        // Keep the incoming v16 Modifier instead of the
        // clickable modifier.
        replacements += clickableIndex to "nop"
        replacements += (clickableIndex + 1) to
            "move-object/from16 v6, v16"

        require(replacements.map { it.first }.distinct().size ==
            replacements.size) {
            "D3-D: overlapping replacement indices."
        }

        replacements.sortedByDescending { it.first }
            .forEach { (index, smali) ->
                method.replaceInstruction(index, smali)
            }
    }
}
